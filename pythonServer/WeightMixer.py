import math, Logger, Mixer
from collections import defaultdict

class WeightMixer(Mixer.Mixer):
   def __init__(self):
      Mixer.Mixer.__init__(self)
      self.weights = defaultdict(lambda: 1.0)

   def update(self, input, word):
      for featureCounter in self.classifier.featureCounters:
         self.weights[featureCounter.feature.name] *= \
            featureCounter.probability(input, word)
      total = sum(self.weights.values())
      for key, value in self.weights.items():
         self.weights[key] /= total

   def predict(self, input, word):
      score = 0.0
      for feature in self.classifier.featureCounters:
         score += math.log(feature.probability(input,word))*self.weights[feature.feature.name]
      return score

   def logParams(self, name):
      out = Logger.mapFile(name + 'WeightParams')
      for item in self.weights.iteritems():
         out.put(*item)
      out.flush()

   def logScore(self, input, word, totalMass):
      output = '{'
      sep = ''
      prob = 0
      for feature in self.classifier.featureCounters:
         output += sep + '{}:{:.2e}({})'.format(feature.feature.name,
               feature.probability(input, word), feature.featureCount(input,
                  word))
         sep = ', '
         prob += math.log(feature.probability(input, word))
      output += '}'
      return '{:.2e} {}'.format(math.exp(prob-totalMass), output)
