# Replace words not in vocab with <oov> token
import re, os, sys

dir = sys.argv[1]
vocabFile = sys.argv[2]

def replaceOov(f, vocab):
  return '\n'.join(' '.join(w.lower() if w.lower() in vocab else '<oov>'
                            for w in line.strip().split(' '))
                   for line in f)

def getVocab(vocabFile):
  vocab = set()
  for line in open(vocabFile):
    vocab.add(line.strip())
  return vocab

vocab = getVocab(vocabFile)

for section in os.listdir(dir):
   sectionPath = os.path.join(dir, section)

   for doc in os.listdir(sectionPath):
      path = os.path.join(sectionPath, doc)

      f = open(path)
      outStr = replaceOov(f, vocab)
      f.close()

      f = open(path, 'w')
      f.write(outStr);
      f.close()
