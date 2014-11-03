import math, Logger, Mixer
from Input import Input, AnnotatedInput
from collections import defaultdict
from KNFeatureChain import KNFeatureChain
from utils.format import numStr

class PerceptronMixer(Mixer.Mixer):
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
      logFile = Logger.logFile('log')
      for featureCounter in self.classifier.featureCounters:
         logFile.log('{}, {}, {}'.format(featureCounter.feature.name,
                                         numStr(featureCounter.probability(input,
                                                word)),
                                         numStr(featureCounter.probability(input,
                                                topChoice))))
         self.weights[featureCounter.feature.name] += \
            self.l_rate * (math.log(featureCounter.probability(input, word)) - \
            math.log(featureCounter.probability(input, topChoice)))
      self.t += 1.0
      self.l_rate = 1.0/math.sqrt(self.t)
      weightString = ', '.join(['{}:{}'.format(it[0],numStr(it[1]))
                                for it in self.weights.items()])
      logFile.log('weights = {{{}}}'.format(weightString))

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
      score = 0.0
      for feature in self.classifier.featureCounters:
         if isinstance(feature, KNFeatureChain):
            val = math.log(feature.probability(input,word,True))*self.weights[feature.feature.name]
            output += sep + feature.logStr
            sep = '\n'
            score += val
         else:
            val = math.log(feature.probability(input,word))*self.weights[feature.feature.name]
            output += sep + '{}:{:.1f}({})'.format(feature.feature.name,
                  val, feature.featureCount(input, word))
            sep = ', '
            score += val
      output += '}'
      return '{} {}'.format(numStr(math.exp(score-totalMass)), output) 
