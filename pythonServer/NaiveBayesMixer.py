import math, Logger, Mixer

class NaiveBayesMixer(Mixer.Mixer):
   """Make prediction simply by multiplying probabilities given by individual
   features"""
   def __init__(self):
      Mixer.Mixer.__init__(self)
      self.alwaysTrain = False

   def predict(self, input, word):
      for feature in self.classifier.featureCounters:
         if feature.feature.name == 'simple':
            wordCount = feature.featureCount(input, word)
            totalCount = feature.contextCount(input)
            break
      prob = \
         math.log((wordCount+1)/float(totalCount+len(self.classifier.words)))
      for feature in self.classifier.featureCounters:
         if feature.feature.name == 'simple':
            continue
         featureCount = feature.featureCount(input, word)
         contextCount = feature.contextCount(input)
         contexts = len(feature.contexts)
         priorProb = (contextCount+1)/float(totalCount+contexts)
         prob += \
            math.log((featureCount+priorProb*contexts)/(wordCount+contexts))
      return prob

   def update(self, input, word):
      pass
   
   def logParams(self, name):
      pass

   def logScore(self, input, word, totalMass):
      for feature in self.classifier.featureCounters:
         if feature.feature.name == 'simple':
            wordCount = feature.featureCount(input, word)
            totalCount = feature.contextCount(input)
            break
      prob = \
         math.log((wordCount+1)/float(totalCount+len(self.classifier.words)))
      output = 'simple:{:.2f}({}) {{'.format(prob, wordCount)
      sep = ''
      for feature in self.classifier.featureCounters:
         if feature.feature.name == 'simple':
            continue
         featureCount = feature.featureCount(input, word)
         contextCount = feature.contextCount(input)
         contexts = len(feature.contexts)
         priorProb = (contextCount+1)/float(totalCount+contexts)
         featureProb = math.log(feature.probability(input, word))
         output += sep + '{}:{:.1f}({})'.format(feature.feature.name,
               featureProb, featureCount)
         sep = ', '
         prob += featureProb
      output += '}'
      return '{:.2e} {}'.format(math.exp(prob-totalMass), output)
