from collections import defaultdict, Counter
from Input import Input, AnnotatedInput
from utils.format import numStr
import Logger, itertools, Classifier, math
from itertools import izip

DISCOUNT_PARAMS = 3

class KNFeatureGrid(Classifier.Classifier):
   """Maintains counts associated with a particular feature"""
   def __init__(self, features):
      Classifier.Classifier.__init__(self)
      self.features = features

      self.uniformCache = 0
      self.discountCache = {}
      self.backoffDiscountCache = [{} for _ in self.features]
      self.poolCache = Counter()
      self.backoffPoolCache = [Counter() for _ in self.features]
      self.contextFreqFreqs = defaultdict(lambda: [0] * DISCOUNT_PARAMS)
      self.backoffContextFreqFreqs = [defaultdict(lambda: [0] *
                                      DISCOUNT_PARAMS) for _ in self.features]
      self.freqFreqs = defaultdict(lambda: [0] * (DISCOUNT_PARAMS+1))
      self.backoffFreqFreqs = [defaultdict(lambda: [0] * (DISCOUNT_PARAMS+1))
                               for _ in self.features]

      # Counts how many times we've seen each value of the feature in each
      # context
      self.counts = defaultdict(Counter)
      self.backoffCounts = [defaultdict(Counter) for _ in self.features]
      # Counts how many times we've seen each context
      self.contextCounts = defaultdict(Counter)
      self.backoffContextCounts = [defaultdict(Counter) for _ in self.features]

      # These counts are the same as above, but keep track of the counts for
      # every file, so that we can remove the counts for a file to avoid
      # double-counting
      self.fileCounts = defaultdict(lambda: defaultdict(int))
      self.fileContextCounts = defaultdict(lambda: defaultdict(int))

      # Set of all contexts we've seen
      self.contexts = defaultdict(set)

   def _logFreqFreq(self, name, paramsOut, m, featureIds, featureStr):
      freqFreq = m[featureIds]
      for i,freq in enumerate(freqFreq):
         paramsOut.log('\t'.join(str(o) for o in
                       [name, featureStr, i+1, freq]))

   def _logContextFreqFreq(self, name, paramsOut, m, key, contextStr,
                           featureStr):
      contextFreqFreq = m[key]
      for i,freq in enumerate(contextFreqFreq):
         paramsOut.log('\t'.join(str(o) for o in
                       [name, featureStr, contextStr, i+1, freq]))

   def _logContextCount(self, name, paramsOut, m, featureIds, contexts,
                        contextStr, featureStr):
      count = m[featureIds][contexts]
      if count > 0:
         paramsOut.log('\t'.join(str(o) for o in
                       [name, featureStr, contextStr, count]))

   def _logCount(self, name, paramsOut, m, featureIds, featureStr):
      counts = sorted(m[featureIds].iteritems(),
            key=lambda (key, count): (key[0], count, key[1]), reverse=True)
      for key,count in counts:
         if count > 0:
            paramsOut.log('\t'.join(str(o) for o in
                          [name, featureStr, _contextsToStr(key[0]),
                             _contextToStr(key[1]), count]))

   def logParams(self, name):
      paramsOut = Logger.logFile(name + 'Params')
      contextsOut = Logger.logFile(name + 'Contexts')
      idxs = [range(len(feature.features)) for feature in self.features]
      for featureIds in itertools.product(*idxs):
         print featureIds
         features = [feature[featureId] for feature,featureId in
                     zip(self.features, featureIds)]
         featureStr = ' '.join(feature.name for feature in features)
         self._logFreqFreq('ff', paramsOut, self.freqFreqs, featureIds,
                           featureStr)
         for i in range(len(self.features)):
            self._logFreqFreq('{}bff'.format(i), paramsOut,
                              self.backoffFreqFreqs[i], featureIds, featureStr)
         for contexts in sorted(self.contexts[featureIds]):
            contextStr = _contextsToStr(contexts)
            self._logContextCount('cc', paramsOut, self.contextCounts,
                                  featureIds, contexts, contextStr, featureStr)
            for i in range(len(self.features)):
               self._logContextCount('{}bcc'.format(i), paramsOut,
                                        self.backoffContextCounts[i],
                                        featureIds, contexts, contextStr,
                                        featureStr)
            key = (featureIds, contexts)
            self._logContextFreqFreq('cff', paramsOut, self.contextFreqFreqs,
                                     key, contextStr, featureStr)
            for i in range(len(self.features)):
               self._logContextFreqFreq('{}bcff'.format(i), paramsOut,
                                        self.backoffContextFreqFreqs[i],
                                        key, contextStr, featureStr)
         self._logCount('c', paramsOut, self.counts, featureIds, featureStr)
         for i in range(len(self.features)):
            self._logCount('{}bc'.format(i), paramsOut, self.backoffCounts[i],
                           featureIds, featureStr)
      paramsOut.close()
      contextsOut.close()

   def _logContext(self, input):
      output = '{'
      sep = ''
      idxs = (range(feature.first(input),-1,-1) for feature in self.features)
      first = True
      for featureIds in itertools.product(*idxs):
         features = [feature[featureId] for feature,featureId in
                     zip(self.features, featureIds)]
         contexts = tuple(feature.contextFunc(input) for feature in features)
         if first or self.contextCounts[featureIds][contexts] > 0:
            first = False
            featureStr = ' '.join(feature.name for feature in features)
            isStartPoint = '*' if featureIds in self.startPoints else ' '
            countStr = ' '.join('({},{})'.format(
                  self.backoffContextCounts[i][featureIds][contexts],
                  numStr(self.backoffPoolCache[i][(featureIds, contexts)]))
                  for i in range(len(self.features)))
            output += sep + "{}{}:'{}' ({},{}) {}".format(isStartPoint,
                  featureStr, _contextsToStr(contexts),
                  self.contextCounts[featureIds][contexts],
                  numStr(self.poolCache[(featureIds, contexts)]), countStr)
            sep = ',\n'
      output += '}'
      return output

   def _logScore(self, input, word, totalMass):
      output = '{'
      sep = ''
      idxs = (range(feature.first(input),-1,-1) for feature in self.features)
      for featureIds in itertools.product(*idxs):
         features = [feature[featureId] for feature,featureId in
                     zip(self.features, featureIds)]
         contexts = tuple(feature.contextFunc(input) for feature in features)
         featureStr = ' '.join(feature.name for feature in features)
         countStr = ''
         subSep = ''
         count = self.counts[featureIds][(contexts, word)]
         if count > 0:
            isStartPoint = '*' if featureIds in self.startPoints else ' '
            for i in range(len(self.features)):
               backoffCount = self.backoffCounts[i][featureIds][(contexts, word)]
               discount = self.discount(featureIds, i, backoffCount, featureStr)
               countStr += subSep + '({},{})'.format(backoffCount, numStr(discount))
               subSep = ' '
            discount = self.discount(featureIds, None, count, featureStr)
            output += sep + "{}{}:'{}' ({},{}) {}".format(isStartPoint,
                  featureStr, _contextsToStr(contexts), count,
                  numStr(discount), countStr)
            sep = ',\n'
      output += '}'
      return '{} {}'.format(numStr(self.probability(input, word)), output)

   def _updateCounts(self, contextCounts, counts, freqFreq, contextFreqFreqs,
                     contexts, featureVal, featureIds, poolCache,
                     discountCache):
      if featureIds in discountCache:
         del discountCache[featureIds]
      contextCount = contextCounts[contexts] + 1
      count = counts[(contexts, featureVal)] + 1

      contextCounts[contexts] = contextCount
      counts[(contexts, featureVal)] = count

      idx = (featureIds, contexts)
      contextFreqFreq = contextFreqFreqs[idx]
      if idx in poolCache:
         del poolCache[idx]
      if count > 1:
         if count <= DISCOUNT_PARAMS:
            contextFreqFreq[count-2] -= 1
         if count <= DISCOUNT_PARAMS + 2:
            freqFreq[count-2] -= 1
      if count <= DISCOUNT_PARAMS:
         contextFreqFreq[count-1] += 1
      if count <= DISCOUNT_PARAMS + 1:
         freqFreq[count-1] += 1
      return count

   def _trainFeature(self, input, word, featureIds):
      features = [feature[featureId] for feature,featureId in
                  zip(self.features, featureIds)]

      contexts = tuple(feature.contextFunc(input) for feature in features)
      # FixMe: [correctness] Figure out how to handle nontrivial featureVal
      featureVal = word

      count = self._updateCounts(self.contextCounts[featureIds],
                                 self.counts[featureIds],
                                 self.freqFreqs[featureIds],
                                 self.contextFreqFreqs, contexts, featureVal,
                                 featureIds, self.discountCache,
                                 self.poolCache)
      # print featureIds, contexts, count

      for backoffOrigin in self.backoffOrigins[featureIds]:
         # print '  ',backoffOrigin
         self._updateCounts(self.backoffContextCounts[backoffOrigin][featureIds],
                            self.backoffCounts[backoffOrigin][featureIds],
                            self.backoffFreqFreqs[backoffOrigin][featureIds],
                            self.backoffContextFreqFreqs[backoffOrigin],
                            contexts, featureVal, featureIds,
                            self.backoffDiscountCache[backoffOrigin],
                            self.backoffPoolCache[backoffOrigin])

      self.contexts[featureIds].add(contexts)

      if count == 1:
         for i,featureId in enumerate(featureIds):
            # print i,featureId
            nextFeatureId = self.features[i].succ(featureId)
            if nextFeatureId is not None:
               nextFeatureIds = list(featureIds)
               nextFeatureIds[i] = nextFeatureId
               self.backoffOrigins[tuple(nextFeatureIds)].append(i)

   def generate(self, feature, input, isTestFile):
      featureId = feature.first(input)
      if not isTestFile and feature.name == 'scope':
         featureId = 1
      while featureId is not None:
         yield featureId
         featureId = feature.succ(featureId)

   def _resetFile(self, path):
      pass

   def _trainOneWord(self, input, word, weightTraining, isTestFile):
      self.words.add(word)
      self.uniformCache = 0
      # line = input.lineIndex + 1
      # lastNewLine = input.lines[input.lineIndex][1]
      # loc = input.input.location
      # locString = '{}:{},{}'.format(input.input.path, line,
      #                               loc - lastNewLine + 1)
      # print locString, word
      self.backoffOrigins = defaultdict(list)
      idxs = (self.generate(feature, input, isTestFile) for feature in self.features)
      for featureIds in itertools.product(*idxs):
         self._trainFeature(input, word, featureIds)

   def discount(self, featureIds, backoffOrigin, count, featureStr):
      discountCache = self.backoffDiscountCache[backoffOrigin] \
                      if backoffOrigin is not None \
                      else self.discountCache
      if featureIds not in discountCache:
         cache = []
         freqFreq = self.backoffFreqFreqs[backoffOrigin][featureIds] \
                    if backoffOrigin is not None \
                    else self.freqFreqs[featureIds]
         n1 = freqFreq[0]
         y = n1 / float(n1 + 2*freqFreq[1]) if freqFreq[1] > 0 else .5
         discountOut = Logger.logFile('discounts.log')
         discountOut.log(featureStr)
         discountOut.log('y: {:.2}={}/({}+2*{})'.format(y, n1, n1,
                                                        freqFreq[1]))
         for i in range(DISCOUNT_PARAMS):
            val = i+1 - (i+2)*y*freqFreq[i+1]/freqFreq[i] if freqFreq[i] > 0 \
                  else .5
            if val < 0 or val > 1:
               val = .5
            cache.append(val)
            discountOut.log('{}: {:.2}={}-{}*{}*{}/{}'.format(i, val, i+1,
                              (i+2), y, freqFreq[i+1], freqFreq[i]))
         discountCache[featureIds] = cache
      if count == 0: return 0
      return discountCache[featureIds][min(count, DISCOUNT_PARAMS)-1]

   def pool(self, featureIds, contexts, backoffOrigin):
      idx = (featureIds, contexts)
      poolCache = self.backoffPoolCache[backoffOrigin] \
                  if backoffOrigin is not None else self.poolCache
      if idx in poolCache:
         return poolCache[idx]
      discount = self.backoffDiscountCache[backoffOrigin][featureIds] \
                 if backoffOrigin is not None \
                 else self.discountCache[featureIds]
      freqFreq = self.backoffContextFreqFreqs[backoffOrigin][idx] \
                 if backoffOrigin is not None \
                 else self.contextFreqFreqs[idx]
      ret = sum(freqFreq[i]*discount[i] for i in range(DISCOUNT_PARAMS))
      poolCache[idx] = ret
      return ret

   # For now assume featureFunc is always identity
   def _prob(self, input, word, featureIds, backoffOrigin, doLogging):
      features = [feature[featureId] for feature,featureId in
                  zip(self.features, featureIds)]

      contexts = tuple(feature.contextFunc(input) for feature in features)
      # FixMe: [correctness] Figure out how to handle nontrivial featureVal
      featureVal = word
      contextCounts = self.backoffContextCounts[backoffOrigin][featureIds] \
                      if backoffOrigin is not None \
                      else self.contextCounts[featureIds]
      total = contextCounts[contexts]

      if doLogging:
         # We cache logStr here because it will be overwritten by the
         # recursive call to _prob
         baseStr = self.logStr + self.logSep
         self.logSep = ', '
      if featureIds in self.backoffTots:
         numTot, denomTot = self.backoffTots[featureIds]
      else:
         numTot, denomTot = 0, 0
         for i,featureId in enumerate(featureIds):
            # print i,featureId
            nextFeatureId = self.features[i].succ(featureId)
            if nextFeatureId is None:
               if not self.uniformCache:
                  self.uniformCache = float(len(self.words))
               numTot += 1
               denomTot += self.uniformCache
            else:
               nextFeatureIds = list(featureIds)
               nextFeatureIds[i] = nextFeatureId
               num, denom = self._prob(input, word, tuple(nextFeatureIds),
                                       i, doLogging)
               numTot += num
               denomTot += denom
         self.backoffTots[featureIds] = (numTot, denomTot)
      backoffProb = numTot / denomTot

      featureStr = ' '.join(feature.name for feature in features)

      if total == 0:
         return numTot, denomTot
      counts = self.backoffCounts[backoffOrigin][featureIds] \
               if backoffOrigin is not None else self.counts[featureIds]
      count = counts[(contexts, featureVal)]
      discount = self.discount(featureIds, backoffOrigin, count, featureStr)
      pool = self.pool(featureIds, contexts, backoffOrigin)
      modCount = count - discount + pool*backoffProb
      if doLogging:
         # Note that the self.logStr at the end is the one set by the
         # recursive call to _prob
         self.logStr = baseStr + \
            '{}:{} ({}-{}+{})'.format(featureStr, numStr(modCount / total),
                                      count, numStr(discount),
                                      numStr(pool*backoffProb)) + \
            self.logStr
      return modCount, total

   def probability(self, input, word):
      self.backoffTots = {}
      numTot, denomTot = 0, 0
      for startPoint in self.startPoints:
         num, denom = self._prob(input, word, startPoint, None, False)
         numTot += num
         denomTot += denom
      return numTot / denomTot

   def _preparePrediction(self, input):
      idxs = (self.generate(feature, input, True) for feature in
              self.features)
      startPoints = []
      for featureIds in itertools.product(*idxs):
         isStartPoint = True
         for startPoint in startPoints:
            isShadowed = True
            for featureId,spFeatureId in izip(featureIds, startPoint):
               if featureId > spFeatureId:
                  isShadowed = False
                  break
            if isShadowed:
               isStartPoint = False
               break
         if isStartPoint:
            features = [feature[featureId] for feature,featureId in
                        zip(self.features, featureIds)]

            contexts = tuple(feature.contextFunc(input) for feature in
                             features)
            if self.contextCounts[featureIds][contexts] > 0:
               startPoints.append(featureIds)
      self.startPoints = startPoints


   def _score(self, input, word):
      return math.log(self.probability(input, word))

def _contextsToStr(contexts):
   return ','.join(_contextToStr(c) for c in contexts)

def _contextToStr(context):
   if isinstance(context, tuple):
      return ' '.join(context)
   return str(context)
