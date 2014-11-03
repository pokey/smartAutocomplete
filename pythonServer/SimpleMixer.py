import math, Logger, Mixer
from KNFeatureChain import KNFeatureChain
from KNFeatureGrid import KNFeatureGrid
from utils.format import numStr

class SimpleMixer(Mixer.Mixer):
   """Make prediction simply by multiplying probabilities given by individual
   features"""
   def predict(self, input, word):
      prob = 0
      for feature in self.classifier.featureCounters:
         prob += math.log(feature.probability(input, word))
      return prob

   def update(self, input, word):
      pass
   
   def logParams(self, name):
      pass

   def logScore(self, input, word, totalMass):
      output = '{'
      sep = ''
      prob = 0
      for feature in self.classifier.featureCounters:
         if isinstance(feature, KNFeatureChain) or \
            isinstance(feature, KNFeatureGrid):
            prob += math.log(feature.probability(input, word, True))
            output += sep + feature.logStr
            sep = '\n'
         else:
            output += sep + '{}:{}({})'.format(feature.feature.name,
                  numStr(feature.probability(input, word)), feature.featureCount(input,
                     word))
            prob += math.log(feature.probability(input, word))
            sep = ', '
      output += '}'
      return '{} {}'.format(numStr(math.exp(prob-totalMass)), output)
