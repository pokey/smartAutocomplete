import re, Logger, tokenizer
from BenchMarker import PREFIX_SIZE

class _Counter():
   def __init__(self):
      self.actual_ks = 0
      self.max_ks = 0
      self.total = 0

class Filter():
   def __init__(self, name, filt):
      self.name = name
      self.filt = filt

class NumkeystrokesN(object):
   def __init__(self, n, filt):
      self.n = n
      self.filt = filt
      self.counters = [_Counter() for i in range(PREFIX_SIZE+1)]

   def update(self, prediction, word, prefixSize):
      if self.filt.filt(word):
         counter = self.counters[prefixSize]
         counter.total += 1
         length = len(word)
         words = [w for w,prob in prediction]
         if word in words[:self.n]:
            index = words[:self.n].index(word)
            #You can naviagte through autocompletion menu in two directions
            actual = prefixSize + 1 + min(index, self.n+1-index)
            if actual > length:
               actual = length
            counter.actual_ks += actual
         else:
            counter.actual_ks += length
         counter.max_ks += length

   def output(self, out):
      for index,counter in enumerate(self.counters):
         frac = "{}_actual/{}_maximum".format(counter.actual_ks, counter.max_ks)
         out.put("inTop_{}_{}_prefix={}_all".format(self.n, self.filt.name, index),
                 frac)
         averageSavings = "average_{}_saved_per_token"\
            .format(float(counter.max_ks - counter.actual_ks)/counter.total)\
            if counter.total > 0 else "undefined average per token"
         out.put("inTop_{}_{}_prefix={}_seen".format(self.n, self.filt.name, index),
                 averageSavings)

def NumkeystrokesNClass(n, filt):
   name = "Numkeystrokes" + str(n) + str(filt)
   def __init__(self):
      NumkeystrokesN.__init__(self, n, filt)
   return type(name, (NumkeystrokesN,),{"__init__": __init__})

# Filters

def none(word):
   return True

noFilter = Filter('all', none)
identifierFilter = Filter('identifiers', tokenizer.isIdentifier)
