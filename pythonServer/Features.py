import re, math
from Feature import Feature, decorate, FeatureChain
from collections import OrderedDict

# Decorators
regexes = [(r"[A-Z]+", 'A'),
           (r"[a-z]+", 'a'),
           (r"[0-9]+", '0')]
def wordForm(word):
   abstract = ""
   while word != "":
      matched = False
      for regex,symbol in regexes:
         m = re.match(regex, word)
         if m:
            abstract += symbol
            word = word[m.end():]
            matched = True
            break
      if not matched:
         abstract += word[0]
         word = word[1:]
   return abstract

# Contexts
def noContext(input):
   return 0

def path(input):
   return input.input.path

def filetype(input):
   extension = re.search("\.[^\.]+$", input.input.path)
   return extension.group(0) if extension else ''

def prevWord(input):
   return '' if input.index == 0 else input.words[input.index-1][0]

def prevTwoWords(input):
   return '' if input.index < 2 else \
          (input.words[input.index-2][0], input.words[input.index-1][0])

def prevThreeWords(input):
   return '' if input.index < 3 else \
          (input.words[input.index-3][0],
           input.words[input.index-2][0], 
           input.words[input.index-1][0])

def prevPrevWord(input):
   return '' if input.index < 2 else input.words[input.index-2][0]

def isLineStart(input):
   loc = input.input.location
   lineStart = input.lines[input.lineIndex][1]
   return loc == lineStart or input.input.content[lineStart:loc].isspace()

# FixMe: [correctness] Need to escape tabs when outputing to params file
def indentLevel(input):
   loc = input.input.location
   lineStart = input.lines[input.lineIndex][1]
   return re.match('^\s*', input.input.content[lineStart:loc]).group()

# FixMe: [correctness] Need to escape tabs when outputing to params file
def linePrefix(input):
   loc = input.input.location
   lineStart = input.lines[input.lineIndex][1]
   return input.input.content[lineStart:loc]

def dirHierarchy(input):
   results =  re.findall(r"/", input.input.path)
   if results:
      return len(results)
   else:
      return 0

def recency(input):
   def increment(l, word):
      if re.match(r'\w+', word):
         l[0] += 1
      return l[0]

   l = [0]
   identifiers = {word : increment(l, word) for word,loc in input.words[:input.index]
                  if re.match(r'\w+', word)}
   
   word = input.words[input.index][0]
   if word in identifiers.keys():
      last_used = l[0] - identifiers[word] + 1
      return int(math.log(last_used, 2))
   return -1

prevWordForm = decorate(prevWord, wordForm)

# Feature values
def word(input, word): 
   return word

def nGram(n):
   def context(input):
      return tuple('' if i < 0 else input.words[i][0]
              for i in range(input.index-n+1, input.index))
   return Feature('{}gram'.format(n), context, word)

class NgramFeatureChain(FeatureChain):
   def __init__(self, n):
      self.features = [nGram(i) for i in range(1,n+1)]
      self.name = 'ngram'

   def first(self, input):
      return len(self.features)-1

   def __getitem__(self,index):
      return self.features[index]

   def succ(self, featureId):
      return featureId-1 if featureId > 0 else None

class ScopeFeatureChain(FeatureChain):
   def __init__(self):
      self.features = []
      self.cachedScopeChain = None
      self.cachedInputId = None
      self.cachedDepth = None
      self.name = 'scope'

   def _feature(self, i):
      def context(input):
         line = input.words[input.index][2]
         inputId = (input.input.path, line[0])
         if inputId != self.cachedInputId:
            raise Exception('Unexpected call to feature')
         return self.cachedScopeChain[self.cachedDepth-1-i]
      return Feature('{}scope'.format(i), context, word)

   def first(self, input):
      # FixMe: [correctness] Is this the right thing to do?
      line = input.words[input.index][2]
      path = input.input.path
      inputId = (path, line[0])
      if inputId == self.cachedInputId:
         return self.cachedDepth-1
      self.cachedInputId = inputId
      node = line[1]
      scopeChain = ['{}l{}'.format(path, line[0]),
                    '{}n{}'.format(path, node.id)]
      depth = 3
      while node.parent != None:
         node = node.parent
         scopeChain.append('{}n{}'.format(path, node.id))
         depth += 1
      scopeChain.append('')
      self.cachedScopeChain = scopeChain
      self.cachedDepth = depth
      for i in range(len(self.features), depth):
         self.features.append(self._feature(i))
      return depth-1

   def __getitem__(self,index):
      return self.features[index]

   def succ(self, featureId):
      return featureId-1 if featureId > 0 else None

# Features
feature = {
   "simple": Feature("simple", noContext, word),
   "path": Feature("path", path, word),
   "filetype": Feature("filetype", filetype, word),
   "prev": Feature("prev", prevWord, word),
   "prevTwo": Feature("prevTwo", prevTwoWords, word),
   "prevThree": Feature("prevThree", prevThreeWords, word),
   "prevPrev": Feature("prevPrev", prevPrevWord, word),
   "prevForm": Feature("prevForm", prevWordForm, word),
   "isLineStart": Feature("isLineStart", isLineStart, word),
   "indentLevel":Feature("indentLevel", indentLevel, word),
   "linePrefix":Feature("linePrefix", linePrefix, word),
   "scope":ScopeFeatureChain(),
   "ngram":NgramFeatureChain(4),
   "dirH":Feature("dirH", dirHierarchy, word)}
"""
   "recency":Feature("recency", recency, word)
}"""
