from collections import defaultdict, Counter
from Input import Input, AnnotatedInput
from utils.format import numStr
import Logger

DISCOUNT_PARAMS = 3

class KNFeatureChain:
   """Maintains counts associated with a particular feature"""
   def __init__(self, features, classifier):
      self.features = features
      self.feature = features
      self.classifier = classifier

      self.uniformCache = 0
      self.discountCache = {}
      self.backoffDiscountCache = {}
      self.poolCache = Counter()
      self.contextFreqFreqs = defaultdict(lambda: [0] * DISCOUNT_PARAMS)
      self.backoffContextFreqFreqs = defaultdict(lambda: [0] * DISCOUNT_PARAMS)
      self.freqFreqs = defaultdict(lambda: [0] * (DISCOUNT_PARAMS+1))
      self.backoffFreqFreqs = defaultdict(lambda: [0] * (DISCOUNT_PARAMS+1))

      # Counts how many times we've seen each value of the feature in each
      # context
      self.counts = [Counter() for f in features.features]
      self.backoffCounts = [Counter() for f in features.features]
      # Counts how many times we've seen each context
      self.contextCounts = [Counter() for f in features.features]
      self.backoffContextCounts = [Counter() for f in features.features]

      # These counts are the same as above, but keep track of the counts for
      # every file, so that we can remove the counts for a file to avoid
      # double-counting
      self.fileCounts = defaultdict(lambda: defaultdict(int))
      self.fileContextCounts = defaultdict(lambda: defaultdict(int))

      # Set of all contexts we've seen
      self.contexts = defaultdict(set)

   def _logContextFreqFreqs(self, name, paramsOut, m):
      for featureId in range(len(self.features.features)):
         feature = self.features[featureId]
         for context in sorted(self.contexts[featureId]):
            contextFreqFreq = m[(featureId, context)]
            for i,freq in enumerate(contextFreqFreq):
               paramsOut.log('\t'.join(str(o) for o in
                             [name, feature.name, _contextToStr(context), i+1,
                              freq]))

   def _logFreqFreqs(self, name, paramsOut, m):
      for featureId in range(len(self.features.features)):
         feature = self.features[featureId]
         freqFreq = m[featureId]
         for i,freq in enumerate(freqFreq):
            paramsOut.log('\t'.join(str(o) for o in
                          [name, feature.name, i+1, freq]))

   def _logContextCounts(self, name, paramsOut, m):
      for featureId in range(len(self.features.features)):
         feature = self.features[featureId]
         contextCounts = m[featureId]
         contexts = sorted(contextCounts.iteritems(),
                           key=lambda (key, count): (count, key),
                           reverse=True)
         for context,count in contexts:
            if count > 0:
               paramsOut.log('\t'.join(str(o) for o in
                             [name, feature.name, _contextToStr(context), count]))

   def _logCounts(self, name, paramsOut, m):
      for featureId in range(len(self.features.features)):
         feature = self.features[featureId]
         counts = sorted(m[featureId].iteritems(),
               key=lambda (key, count): (key[0], count, key[1]), reverse=True)
         for key,count in counts:
            if count > 0:
               paramsOut.log('\t'.join(str(o) for o in
                             [name, feature.name, _contextToStr(key[0]),
                                _contextToStr(key[1]), count]))

   def logParams(self, paramsOut, contextsOut):
      self._logContextFreqFreqs('cff', paramsOut, self.contextFreqFreqs)
      self._logContextFreqFreqs('bcff', paramsOut,
                                self.backoffContextFreqFreqs)
      self._logFreqFreqs('ff', paramsOut, self.freqFreqs)
      self._logFreqFreqs('bff', paramsOut, self.backoffFreqFreqs)
      self._logContextCounts('cc', paramsOut, self.contextCounts)
      self._logContextCounts('bcc', paramsOut, self.backoffContextCounts)
      self._logCounts('c', paramsOut, self.counts)
      self._logCounts('bc', paramsOut, self.backoffCounts)

   def logContext(self, input):
      # FixMe: output context and pool
      output = ''
      sep = ''
      for featureId in range(self.features.first(input),-1,-1):
         feature = self.features[featureId]
         context = feature.contextFunc(input)
         output += sep + "{}:'{}' ({},{})".format(feature.name,
               _contextToStr(context), self.contextCounts[featureId][context],
               numStr(self.poolCache[(featureId, context)]))
         sep = ', '
      return output

   def _updateCounts(self, contextCounts, counts, freqFreq, contextFreqFreqs,
                     context, featureVal, featureId):
      contextCount = contextCounts[context] + 1
      count = counts[(context, featureVal)] + 1

      contextCounts[context] = contextCount
      counts[(context, featureVal)] = count

      idx = (featureId, context)
      contextFreqFreq = contextFreqFreqs[idx]
      if idx in self.poolCache:
         del self.poolCache[idx]
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

   def _trainFeature(self, input, word, featureId, doBackoff):
      if doBackoff:
         if featureId in self.backoffDiscountCache:
            del self.backoffDiscountCache[featureId]
      else:
         if featureId in self.discountCache:
            del self.discountCache[featureId]

      feature = self.features[featureId]

      context = feature.contextFunc(input)
      featureVal = feature.featureFunc(input, word)

      count = self._updateCounts(self.contextCounts[featureId],
                                 self.counts[featureId],
                                 self.freqFreqs[featureId],
                                 self.contextFreqFreqs, context, featureVal,
                                 featureId)

      if doBackoff:
         self._updateCounts(self.backoffContextCounts[featureId],
                            self.backoffCounts[featureId],
                            self.backoffFreqFreqs[featureId],
                            self.backoffContextFreqFreqs, context,
                            featureVal, featureId)

      self.contexts[featureId].add(context)

      nextFeatureId = self.features.succ(featureId)
      if nextFeatureId != None:
         self._trainFeature(input, word, nextFeatureId, count == 1)

   def resetFile(self, path):
      pass

   def trainOneWord(self, input, word):
      self.uniformCache = 0
      first = self.features.first(input)
      for _ in range(len(self.contextCounts)-1, first):
         self.counts.append(Counter())
         self.backoffCounts.append(Counter())
         self.contextCounts.append(Counter())
         self.backoffContextCounts.append(Counter())
      self._trainFeature(input, word, first, False)

   def discount(self, featureId, useBackoff, count):
      discountCache = self.backoffDiscountCache if useBackoff else \
                      self.discountCache
      if featureId not in discountCache:
         cache = []
         freqFreq = self.backoffFreqFreqs[featureId] if useBackoff else \
                    self.freqFreqs[featureId]
         n1 = freqFreq[0]
         y = n1 / float(n1 + 2*freqFreq[1])
         discountOut = Logger.logFile('discounts.log')
         discountOut.log(self.features[featureId].name)
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
         discountCache[featureId] = cache
      if count == 0: return 0
      return discountCache[featureId][min(count, DISCOUNT_PARAMS)-1]

   def pool(self, featureId, context, useBackoff):
      idx = (featureId, context)
      if idx in self.poolCache:
         return self.poolCache[idx]
      discount = self.backoffDiscountCache[featureId] if useBackoff else \
                 self.discountCache[featureId]
      freqFreq = self.backoffContextFreqFreqs[idx] if useBackoff else \
                 self.contextFreqFreqs[idx]
      ret = sum(freqFreq[i]*discount[i] for i in range(DISCOUNT_PARAMS))
      self.poolCache[idx] = ret
      return ret

   # For now assume featureFunc is always identity
   def _prob(self, input, word, featureId, useBackoff, doLogging):
      if featureId == None:
         if not self.uniformCache:
            self.uniformCache = 1 / float(len(self.classifier.words))
         return self.uniformCache
      else:
         feature = self.features[featureId]
         context = feature.contextFunc(input)
         featureVal = feature.featureFunc(input, word)
         contextCounts = self.backoffContextCounts[featureId] if useBackoff \
                         else self.contextCounts[featureId]
         total = contextCounts[context]
         if total == 0:
            return self._prob(input, word, self.features.succ(featureId),
                              True, doLogging)
         counts = self.backoffCounts[featureId] if useBackoff else \
                  self.counts[featureId]
         count = counts[(context, featureVal)]
         discount = self.discount(featureId, useBackoff, count)
         pool = self.pool(featureId, context, useBackoff)
         if doLogging:
            # We cache logStr here because it will be overwritten by the
            # recursive call to _prob
            baseStr = self.logStr + self.logSep
            self.logSep = ', '
         backoffProb = self._prob(input, word, self.features.succ(featureId),
                                  True, doLogging)
         prob = (count - discount + pool*backoffProb) / total
         if doLogging:
            # Note that the self.logStr at the end is the one set by the
            # recursive call to _prob
            self.logStr = baseStr + \
               '{}:{} ({}-{}+{})'.format(feature.name, numStr(prob), count,
                                        numStr(discount),
                                        numStr(pool*backoffProb)) + \
               self.logStr
         return prob

   def probability(self, input, word, doLogging=False):
      if doLogging:
         self.logStr = ''
         self.logSep = ''
      return self._prob(input, word, self.features.first(input), False,
                        doLogging)

def _contextToStr(context):
   if isinstance(context, tuple):
      return ' '.join(context)
   return str(context)
