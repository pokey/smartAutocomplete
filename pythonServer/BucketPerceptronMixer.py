import math, Logger, Mixer
from Input import Input, AnnotatedInput
from collections import defaultdict

def _bucket(count):
   return int(math.log10(count)) if count > 0 else -1


class BucketPerceptronMixer(Mixer.Mixer):
   def __init__(self):
      Mixer.Mixer.__init__(self)
      self.weights = defaultdict(float)
      self.l_rate = 1.0
      self.t = 1.0
      self.alwaysTrain = False

   def update(self, input, word):
      prediction = self.classifier._predictAnnotated(input)
      self.classifier._logPrediction(input, prediction, word)
      topChoice = prediction[0][0]
      for featureCounter in self.classifier.featureCounters:
         bucket = _bucket(featureCounter.contextCount(input))
         self.weights[(featureCounter.feature.name, bucket)] += \
            self.l_rate * (math.log(featureCounter.probability(input, word)) - \
            math.log(featureCounter.probability(input, topChoice)))
      self.t += 1.0
      self.l_rate = 1.0/math.sqrt(self.t)
      logFile = Logger.logFile('log')
      weightString = ', '.join(['{}:{:.2e}'.format(*it)
                                for it in self.weights.items()])
      logFile.log('weights = {{{}}}'.format(weightString))

   def predict(self, input, word):
      score = 0.0
      for feature in self.classifier.featureCounters:
         bucket = _bucket(feature.contextCount(input))
         score += \
            math.log(feature.probability(input,word))*self.weights[(feature.feature.name,bucket)]
      return score

   def logParams(self, name):
      out = Logger.mapFile(name + 'WeightParams')
      for item in self.weights.iteritems():
         out.put(*item)
      out.flush()

   def logScore(self, input, word, totalMass):
      output = '{'
      sep = ''
      score = 0.0
      for feature in self.classifier.featureCounters:
         bucket = _bucket(feature.contextCount(input))
         val = \
            math.log(feature.probability(input,word))*self.weights[(feature.feature.name,bucket)]
         output += sep + '{}:{:.1f}({})'.format(feature.feature.name,
               val, feature.featureCount(input, word))
         sep = ', '
         score += val
      output += '}'
      return '{:.2e} {}'.format(math.exp(score-totalMass), output) 

