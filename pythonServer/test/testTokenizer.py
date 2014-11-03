import os, sys, tokenizer

path = 'tokenizer.in.py'
out = ''
for token, start, type in tokenizer.tokenize(path, open(path).read()):
   if type == tokenizer.NOTHING:
      out += token + ' '
   elif type == tokenizer.NEWLINE:
      out += '\n'
   elif type == tokenizer.DEDENT:
      out += '<<'
   elif type == tokenizer.INDENT:
      out += '>>'
print out
