import re, collections

# note that numbers also handles '.'
numbers = r'[0-9]+(?:\.[0-9]+)?'
words = r'\w+'
symbols = r'==|//'
newLine = r'(\r\n|\n|\r)[ \t\f\v]*'
python = r'"""'
other = r'[^\s\w]'

tokenizerRegex = re.compile('|'.join([numbers, words, symbols, other]))
pythonRegex = re.compile('|'.join([numbers, words, symbols, newLine, python,
                                   other]), re.MULTILINE)

NOTHING = 0
INDENT = 1
DEDENT = 2
NEWLINE = 3

matching = {'(': ')', '[': ']', '{': '}'}
def tokenize(path, string):
   if path.endswith('.py'):
      inComment = False
      inQuote = ''
      parenDepth = collections.Counter()
      prevToken = ''
      prevPrevToken = ''
      newLine = ''
      indentStack = [0]
      start = 0
      for match in pythonRegex.finditer(string):
         token = match.group()
         start = match.start()
         if token[0] in '\r\n':
            if not (inQuote or sum(parenDepth.values()) > 0
                   or (not inComment and prevToken == '\\')):
               newLine = token
         else:
            if prevToken == '\\':
               yield ('\\', prevStart, NOTHING)
            if newLine:
               inComment = False
               yield ('', start, NEWLINE)
               indent = 0
               for j in range(len(token)):
                  if token[j] not in '\r\f\n':
                     break
               for i in range(j, len(newLine)):
                  if newLine[i] == ' ':
                     indent += 1
                  elif newLine[i] == '\t':
                     indent += 8
                     indent = (indent / 8) * 8
               if indent > indentStack[-1]:
                  indentStack.append(indent)
                  yield ('', start, INDENT)
               else:
                  while indent < indentStack[-1]:
                     indentStack.pop()
                     yield ('', start, DEDENT)
            if token == '#' and not inQuote:
               inComment = True
            elif token in '([{' and not inComment and not inQuote:
               parenDepth[matching[token]] += 1
            elif token in ')]}' and not inComment and not inQuote:
               parenDepth[token] -= 1
            elif token in '"""\'' and not inComment:
               if inQuote == token and (prevPrevToken == '\\' or
                                        prevToken != '\\'):
                  inQuote = ''
               elif not inQuote:
                  inQuote = token
            if token != '\\':
               yield (token, start, NOTHING)
            newLine = ''
         prevPrevToken = prevToken
         prevToken = token
         prevStart = start
      yield ('', start, NEWLINE)
      for _ in range(len(indentStack)-1):
         yield ('', start+len(token), DEDENT)
   else:
      for m in tokenizerRegex.finditer(string):
         yield (m.group(), m.start(), NOTHING)

def isIdentifier(word):
   return re.match(r'\w+', word)
