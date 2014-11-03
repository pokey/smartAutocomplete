class TokenCounter():
   def __init__(self):
      self.counter = 0

   def update(self, prediction, word, prefixSize):
      if prefixSize == 0:
         self.counter += 1

   def output(self, out):
      out.put("tokensTested", self.counter)
