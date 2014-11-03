import tokenizer, bisect, re, Logger, math, sys, utils.myMath
from BenchMarker import PREFIX_SIZE
from Input import AnnotatedInput, Input
from collections import defaultdict, namedtuple
from utils.format import numStr

ParseNode = namedtuple('ParseNode', ['path', 'id', 'parent'])

class Classifier:
   """Performs classification by tokenizing contents of file and passing them
   along to derived type, which should implement _train, _trainOneWord, and
   _score"""
   def __init__(self):
      self.words = set()
      self.benchmarkCounter = 0
      self.tokensTrained = 0
      self.trainingCounter = 0

   def _getLines(self, content):
      return [(match.group(), match.start())
              for match in re.finditer('^.*$', content, re.MULTILINE)]

   def _parse(self, path, content, addWords):
      words = tokenizer.tokenize(path, content)
      wordList = []
      currNode = ParseNode(path, 0, None)
      currLine = [0, currNode]
      nodeId = 1
      for token, start, type in words:
         if type == tokenizer.NOTHING:
            if addWords:
               self.words.add(token)
            wordList.append((token, start, currLine))
         elif type == tokenizer.NEWLINE:
            wordList.append(('\\n', start, currLine))
            prevLine = currLine
            currLine = [currLine[0]+1, currNode]
         elif type == tokenizer.DEDENT:
            wordList.append(('\\d', start, currLine))
            currNode = currNode.parent
            currLine[1] = currNode
         elif type == tokenizer.INDENT:
            wordList.append(('\\i', start, currLine))
            currNode = ParseNode(path, nodeId, currNode)
            nodeId += 1
            prevLine[1] = currNode
            currLine[1] = currNode
      if len(wordList) == 0:
         wordList.append(('\\n', 0, currLine))
      return wordList

   def _totalMass(self, prediction):
      return utils.myMath.totalMass([prob for w,prob in prediction])

   def train(self, path, content, maxTokens=sys.maxint, weightTraining=False,
             sample=1):
      # FixMe: [usability] Add file name as suggestion
      wordList = self._parse(path, content, not weightTraining)
      lines = self._getLines(content);
      self.tokensTrained += min(len(wordList), maxTokens)

      self._resetFile(path)
      lineIndex = 0
      tokensTrained = 0
      for index,(word,loc,node) in enumerate(wordList):
         if tokensTrained >= maxTokens:
            return tokensTrained
         while (loc > lines[lineIndex][1] + len(lines[lineIndex][0])):
            lineIndex += 1
         input = AnnotatedInput(Input(path, content, loc, "", -1),
                                wordList, index, lines, lineIndex)
         weightTrain = False
         if self.trainingCounter % sample == 0:
            tokensTrained += 1
            weightTrain = weightTraining
         self._trainOneWord(input, word, weightTrain, False)
         self.trainingCounter += 1
      return tokensTrained

   def benchmark(self, path, content, benchmarks, sample, maxTokens,
                 filters):
      self._resetFile(path)
      logFile = Logger.logFile('log')
      wordList = self._parse(path, content, False)
      lines = self._getLines(content);
      lineIndex = 0
      tokensTested = 0
      for index,(word,loc,node) in enumerate(wordList):
         # Go backwards so we can use last prediction and input in subsequent
         # logging code
         if tokensTested >= maxTokens:
            return tokensTested
         while (loc > lines[lineIndex][1] + len(lines[lineIndex][0])):
            lineIndex += 1
         lineStart = lines[lineIndex][1]
         linePrefix = content[lineStart:loc]
         if self.benchmarkCounter % sample == 0 and \
            (not filters.onlySeen or word in self.words) and \
            len(word) >= filters.minWordLength and \
            not ('#' in linePrefix or '//' in linePrefix) and \
            (not filters.onlyIdentifiers or tokenizer.isIdentifier(word)):
            prediction = []
            for prefixSize in range(PREFIX_SIZE,-1,-1):
               input = AnnotatedInput(Input(path, content, loc,
                  word[:prefixSize], -1), wordList, index, lines, lineIndex)
               prediction = self._predictAnnotated(input)
               for benchmark in benchmarks:
                  benchmark.update(prediction, word, prefixSize)
            doLogging = True
            if filters.inFirst != -1:
               predictedWords = [w for (w, p) in prediction[:filters.inFirst]]
               if word not in predictedWords:
                  doLogging = False
            if filters.notInFirst != -1:
               predictedWords = [w for (w, p) in
                                 prediction[:filters.notInFirst]]
               if word in predictedWords:
                  doLogging = False
            if doLogging:
               self._logPrediction(input, prediction, word)
               tokensTested += 1
         else:
            input = AnnotatedInput(Input(path, content, loc, '', -1),
                  wordList, index, lines, lineIndex)
         self.words.add(word)
         self._trainOneWord(input, word, False, True)
         self.tokensTrained += 1
         self.benchmarkCounter += 1
      return tokensTested

   def _logPrediction(self, input, prediction, word):
      logFile = Logger.logFile('log')
      predictionSummaries = Logger.logFile('predictionSummaries')
      line = input.lineIndex + 1
      lastNewLine = input.lines[input.lineIndex][1]
      loc = input.input.location
      locString = '{}:{},{}'.format(input.input.path, line,
                                    loc - lastNewLine + 1)
      words = input.words
      logFile.start(locString)
      logFile.log('context = ' + self._logContext(input))
      totalMass = self._totalMass(prediction)
      logFile.log('totalMass = {}'.format(numStr(math.exp(totalMass))))
      predictionWords = [w for w,score in prediction]
      index = input.index
      prevWords = ' '.join([w for w,l,n in words[max(0, index-10):index]])
      firstChar = ''
      if len(words) > index:
         firstChar = words[index][0]
      predictionSummary = [locString, prevWords, firstChar,
                           ' '.join(predictionWords[:5])]
      if word:
         logFile.start('actual')
         if word in predictionWords:
            output = self._logScore(input, word, totalMass)
            actualIndex = predictionWords.index(word) + 1
            predictionSummary += [str(actualIndex)]
            logFile.log('{}/{}. {} {}'.format(actualIndex,
                                             len(self.words), word, output))
         else:
            predictionSummary += ['-1']
            logFile.log('-1/{}. {}'.format(len(self.words), word))
         logFile.end()
      predictionSummaries.log('\t'.join(predictionSummary))
      logFile.start('guesses')
      for index,guess in enumerate(predictionWords[:5]):
         output = self._logScore(input, guess, totalMass)
         logFile.log(str(index+1) + '. ' + guess + ' ' + output)
      logFile.end()
      logFile.end()

   def predict(self, input):
      logFile = Logger.logFile('log')
      wordList = self._parse(input.path, input.content, True);
      lines = self._getLines(input.content);
      index = bisect.bisect_left([start for word,start,node in wordList],
                                 input.location)
      lineIndex = bisect.bisect_left([start for line,start in lines],
                                     input.location)
      input = AnnotatedInput(input, wordList, index, lines, lineIndex)
      prediction = self._predictAnnotated(input)
      self._logPrediction(input, prediction, None)
      logFile.flush()
      return [word for word,score in prediction]

   def _predictAnnotated(self, input):
      self._preparePrediction(input)
      scores = [(word, self._score(input, word)) for word in self.words
                if _matchesBase(input.input.base, word)]
      return sorted(scores, key=lambda (word,score): score, reverse=True)

def _matchesBase(base, word):
   return word.lower().startswith(base) if base == base.lower() \
          else word.startswith(base)
