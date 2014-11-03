from Input import Input, AnnotatedInput
from collections import defaultdict
import Classifier, Logger, re, math, sys

class VimClassifier(Classifier.Classifier):

   def __init__(self):
      Classifier.Classifier.__init__(self)

   def _train(self, path, content, wordList, lines, weightTraining,
              maxTokens, sample):
      return min(len(wordList), maxTokens)

   def _trainOneWord(self, input, word, weightTraining):
      return

   def _logScore(self, input, word, totalMass):
      return ''

   def _logContext(self, input):
      return ''

   def _score(self, input, w):
      return 0

   def _totalMass(self, prediction):
      return 0

   def logParams(self, name):
      return

   def _predictAnnotated(self, input):
      scores = defaultdict(lambda: -sys.maxint)
      actual = input.words[input.index][0]
      best_distance = sys.maxint
      prev_tokens = True
      i = 0
      for word, loc in input.words:
         if word == actual and i != input.index:
            dist = input.index - i
            if abs(dist) < best_distance:
               best_distance = abs(dist)
            prev_tokens = dist >= 0

         if i < input.index:
            scores[word] = max(i, scores[word])
         elif i > input.index:
            scores[word] = max(i - len(input.words), scores[word])
         i += 1 
      
      results = {word:score for word, score in scores.items()
                 if Classifier._matchesBase(input.input.base, word)}
      for w in self.words:
         if len(results) > 50:
            break
         elif Classifier._matchesBase(input.input.base, w) and w not in results.keys():
            results[w] = -sys.maxint

      if prev_tokens:
         r = sorted(results.items(), key=lambda (word,score): score, reverse=True)
         return r
      else:
         r = sorted(results.items(), key=lambda (word,score): score, reverse=False)
         return r
         
