import re, Logger, tokenizer
from BenchMarker import PREFIX_SIZE

class _Counter():
   def __init__(self):
      self.hits = 0
      self.seen = 0
      self.total = 0

class Filter():
   def __init__(self, name, filt):
      self.name = name
      self.filt = filt

class FirstN(object):
   def __init__(self, n, filt):
      self.n = n
      self.filt = filt
      self.counters = [_Counter() for i in range(PREFIX_SIZE+1)]

   def update(self, prediction, word, prefixSize):
      if self.filt.filt(word):
         counter = self.counters[prefixSize]
         counter.total += 1
         words = [w for w,prob in prediction]
         if word in words:
            counter.seen += 1
         if word in words[:self.n]:
            counter.hits += 1

   def output(self, out):
      for index,counter in enumerate(self.counters):
         frac = "{:.4}".format(counter.hits / float(counter.total)) \
                if counter.total else 'none'
         out.put("inTop_{}_{}_prefix={}_all".format(self.n, self.filt.name, index),
                 frac)
         seenFrac = "{:.4}".format(counter.hits / float(counter.seen)) \
                    if counter.seen else 'none'
         out.put("inTop_{}_{}_prefix={}_seen".format(self.n, self.filt.name, index),
                 seenFrac)

def FirstNClass(n, filt):
   name = "First" + str(n) + str(filt)
   def __init__(self):
      FirstN.__init__(self, n, filt)
   return type(name, (FirstN,),{"__init__": __init__})

# Filters

def none(word):
   return True

noFilter = Filter('all', none)
identifierFilter = Filter('identifiers', tokenizer.isIdentifier)
