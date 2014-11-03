from collections import defaultdict
from Input import Input, AnnotatedInput
from collections import Counter
import Classifier, Logger, math

class PerceptronClassifier(Classifier.Classifier):
   def __init__(self, features):
      Classifier.Classifier.__init__(self)
      self.features = features
      self.weights = defaultdict(float)
      self.t = 1.0
      self.trainingCounter = 0

   def _train(self, path, content, wordList, lines, weightTraining,
              maxTokens, sample):
      # FixMe: [usability] Add file name as suggestion
      lineIndex = 0
      tokensTrained = 0
      for index,(word,loc) in enumerate(wordList):
         if tokensTrained >= maxTokens:
            return tokensTrained
         while (loc > lines[lineIndex][1] + len(lines[lineIndex][0])):
            lineIndex += 1
         input = AnnotatedInput(Input(path, content, loc, "", -1),
                                wordList, index, lines, lineIndex)
         self._trainOneWord(input, word, False)
         self.t += 1.0
         if self.trainingCounter % sample == 0:
            tokensTrained += 1
         self.trainingCounter += 1
      return tokensTrained

   def _trainOneWord(self, input, word, weightTraining):
      y_pred = self._predictAnnotated(input)[0]
      for f in self.features:
         feature_name = f.name
         context_name = f.contextFunc(input)

         alpha = 1.0/math.sqrt(self.t)
         self.weights[(feature_name, context_name, 
                      f.featureFunc(input,word))] += alpha
         self.weights[(feature_name, context_name, 
                      f.featureFunc(input,y_pred))] -= alpha

   def _logContext(self, input):
      output = '{'
      sep = ''
      for feature in self.features:
         output += sep + "{}:{}".format(feature.name,
               feature.contextFunc(input))
         sep = ', '
      output += '}'
      return output

   def logParams(self, name):
      out = Logger.mapFile(name + 'WeightParams')
      for item in self.weights.iteritems():
         out.put(*item)
      out.flush()

   def _score(self, input, word):
      score = 0
      for f in self.features:
         feature_name = f.name
         context_name = f.contextFunc(input)
         score += self.weights[(feature_name, context_name, f.featureFunc(input,word))]
      return score

   def _totalMass(self, prediction):
      return 0

   def _logScore(self, input, word, totalMass):
      output = '{'
      sep = ''
      score = 0.0
      for f in self.features:
         feature_name = f.name
         context_name = f.contextFunc(input)
         val = self.weights[(feature_name, context_name, f.featureFunc(input,word))]
         output += sep + '{}:{:.2e}'.format(f.name, val)
         sep = ', '
         score += val
      output += '}'
      return '{:.2e} {}'.format(score, output) 
