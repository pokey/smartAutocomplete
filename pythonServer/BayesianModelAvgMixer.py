import math, Logger, utils.myMath, \
      Mixer

MEMOIZED_CNT = 1000

class BayesianModelAvgMixer(Mixer.Mixer):
   """Make prediction using dirichlet-multinomials combined with bayesian
   model averaging"""
   def __init__(self):
      Mixer.Mixer.__init__(self)
      self.weights = {}
      self.logFactTable = [0.0]
      self.weightsCached = False
      self.alwaysTrain = True
      total = 0
      for i in range(1,MEMOIZED_CNT):
         total += math.log(i)
         self.logFactTable.append(total)

   def _logFact(self, n):
      if n < MEMOIZED_CNT:
         return self.logFactTable[n]
      return n * math.log(n) - n + 1

   def predict(self, input, word):
      self._computeWeights()
      prob = 0
      for feature in self.classifier.featureCounters:
         # FixMe: [correctness] Smooth
         prob += self.weights[feature.feature.name]*feature.probability(input, word)
      return prob

   def _computeWeights(self):
      if self.weightsCached:
         return
      logFile = Logger.logFile('log')
      wordCount = len(self.classifier.words)
      logFactWordCount = self._logFact(wordCount-1)
      weights = []
      for feature in self.classifier.featureCounters:
         logWeight = 0
         logFile.start(feature.feature.name)
         for context,contextCount in feature.contextCounts.iteritems():
            logWeight += logFactWordCount - \
                         self._logFact(wordCount + contextCount - 1)
            for word in self.classifier.words:
               logWeight += self._logFact(feature.counts[(context, word)])
         weights.append((feature.feature.name, logWeight))
      totalMass = utils.myMath.totalMass([w for n,w in weights])
      for name,weight in weights:
         self.weights[name] = math.exp(weight-totalMass)
      self.weightsCached = True

      weightString = ', '.join(['{}:{:.2e}'.format(*it)
                                for it in self.weights.items()])
      logFile.log('weights = {{{}}}'.format(weightString))
      print weightString

   def update(self, input, word):
      self.weightsCached = False

   def logParams(self, name):
      out = Logger.mapFile(name + 'WeightParams')
      for item in self.weights.iteritems():
         out.put(*item)
      out.flush()

   def logScore(self, input, word, totalMass):
      self._computeWeights()
      output = '{'
      sep = ''
      prob = 0
      for feature in self.classifier.featureCounters:
         val = self.weights[feature.feature.name]*feature.probability(input, word)
         output += sep + '{}:{:.2e}({})'.format(feature.feature.name,
               val, feature.featureCount(input, word))
         sep = ', '
         prob += val
      output += '}'
      return '{:.2e} {}'.format(prob, output)
