from collections import defaultdict
from Input import Input, AnnotatedInput
import Classifier, math, Logger, utils.myMath
from Feature import FeatureChain
from KNFeatureChain import KNFeatureChain
from KNFeatureGrid import KNFeatureGrid

class ContextClassifier(Classifier.Classifier):
   """Performs classification by associating counts with values of various
   features.  Mixer determines how to mix probabilities estimated by
   individual features."""
   def __init__(self, mixer, features):
      Classifier.Classifier.__init__(self)
      self.mixer = mixer
      mixer.classifier = self
      self.featureCounters = [self._featureCounter(feature)  for feature in
                              features]

   def _featureCounter(self, feature):
      return KNFeatureChain(feature, self) \
             if isinstance(feature, FeatureChain) \
             else KNFeatureGrid(feature, self) \
                  if isinstance(feature, list) \
                  else _FeatureCounter(feature, self)

   def _resetFile(self, path):
      for featureCounter in self.featureCounters:
         featureCounter.resetFile(path)

   def _trainOneWord(self, input, word, weightTraining, isTestFile):
      if (weightTraining or self.mixer.alwaysTrain) and \
         word in self.words: 
         self.mixer.update(input, word)
      else:
         self.words.add(word)
      for featureCounter in self.featureCounters:
         featureCounter.trainOneWord(input, word)

   def _logContext(self, input):
      output = '{'
      sep = ''
      for featureCounter in self.featureCounters:
         if isinstance(featureCounter, KNFeatureChain) or \
            isinstance(featureCounter, KNFeatureGrid):
            output += sep + featureCounter.logContext(input)
            sep = '\n'
         else:
            feature = featureCounter.feature
            output += sep + "{}:{}({})".format(feature.name,
                  feature.contextFunc(input), featureCounter.contextCount(input))
            sep = ', '
      output += '}'
      return output

   def _logScore(self, input, word, totalMass):
      return self.mixer.logScore(input, word, totalMass)

   def _score(self, input, word):
      return self.mixer.predict(input, word) 

   def _totalMass(self, prediction):
      return utils.myMath.totalMass([prob for w,prob in prediction])

   def _preparePrediction(self, input):
      pass

   def logParams(self, name):
      sep = ''
      paramsOut = Logger.logFile(name + 'Params')
      contextsOut = Logger.logFile(name + 'Contexts')
      for featureCounter in self.featureCounters:
         if isinstance(featureCounter, KNFeatureChain) or \
            isinstance(featureCounter, KNFeatureGrid):
            featureCounter.logParams(paramsOut, contextsOut)
            continue
         featureName = featureCounter.feature.name
         contexts = sorted(featureCounter.contextCounts.iteritems(),
               key=lambda (key, count): (count, key), reverse=True)
         for key,count in contexts:
            if count > 0:
               contextsOut.log(featureName + '\t' + _contextToStr(key) + '\t' +
                     str(count))
         counts = sorted(featureCounter.counts.iteritems(),
               key=lambda (key, count): (key[0], count, key[1]), reverse=True)
         for key,count in counts:
            if count > 0:
               paramsOut.log(featureName + '\t' + _contextToStr(key[0]) + '\t'
                     + _contextToStr(key[1]) + '\t' + str(count))
      self.mixer.logParams(name)
      paramsOut.close()
      contextsOut.close()

def _contextToStr(context):
   if isinstance(context, tuple):
      return ' '.join(context)
   return str(context)

class _FeatureCounter:
   """Maintains counts associated with a particular feature"""
   def __init__(self, feature, classifier):
      self.feature = feature
      self.classifier = classifier

      # Counts how many times we've seen each value of the feature in each
      # context
      self.counts = defaultdict(int)
      # Counts how many times we've seen each context
      self.contextCounts = defaultdict(int)

      # These counts are the same as above, but keep track of the counts for
      # every file, so that we can remove the counts for a file to avoid
      # double-counting
      self.fileCounts = defaultdict(lambda: defaultdict(int))
      self.fileContextCounts = defaultdict(lambda: defaultdict(int))

      # Set of all contexts we've seen
      self.contexts = set()

   def resetFile(self, path):
      # If we have already counted this file before, remove its counts so that
      # we are not double-counting
      if path in self.fileCounts:
         localContextCounts = self.fileContextCounts[path]
         localCounts = self.fileCounts[path]
         for context,count in self.fileContextCounts[path].iteritems():
            self.contextCounts[context] -= count
         for key,count in self.fileCounts[path].iteritems():
            self.counts[key] -= count

      # Reset the counts associated with the file, and cache the current dicts
      self.localCounts = self.fileCounts[path] = defaultdict(int)
      self.localContextCounts = self.fileContextCounts[path] = defaultdict(int)

   def trainOneWord(self, input, word):
      context = self.feature.contextFunc(input)
      featureVal = self.feature.featureFunc(input, word)

      self.contextCounts[context] += 1
      self.counts[(context, featureVal)] += 1
      self.localContextCounts[context] += 1
      self.localCounts[(context, featureVal)] += 1

      self.contexts.add(context)
     
   def contextCount(self, input):
      context = self.feature.contextFunc(input)
      return self.contextCounts[context]

   def featureCount(self, input, word):
      context = self.feature.contextFunc(input)
      featureVal = self.feature.featureFunc(input, word)
      return self.counts[(context, featureVal)]

   def count(self, input, word):
      context = self.feature.contextFunc(input)
      featureVal = self.feature.featureFunc(input, word)
      contextCount = self.contextCounts[context]
      featureCount = self.counts[(context, featureVal)]
      return (contextCount, featureCount)

   def probability(self, input, word):
      context = self.feature.contextFunc(input)
      featureVal = self.feature.featureFunc(input, word)
      contextCount = self.contextCounts[context]
      featureCount = self.counts[(context, featureVal)]
      if False:
         print word + "\t" + str(context) + ":" + str(contextCount) + "\t" + \
               str(featureVal) + ":" + str(featureCount)
      # FixMe: [correctness] Smooth
      return featureCount / float(contextCount) if featureCount else 0.00005
