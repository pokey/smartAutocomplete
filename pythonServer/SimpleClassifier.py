from collections import defaultdict
import tokenizer

class SimpleClassifier:
   def __init__(self):
      self.fileCounts = defaultdict(lambda: defaultdict(int))
      self.counts = defaultdict(int);

   def train(self, path, content):
      if path in self.fileCounts:
         for word,count in self.fileCounts[path].iteritems():
            self.counts[word] -= count
      self.fileCounts[path] = defaultdict(int)
      words = tokenizer.tokenize(content)
      localCounts = self.fileCounts[path]
      for word in words:
         self.counts[word] += 1
         localCounts[word] += 1

   def predict(self, path, content, base, location):
      words = [(word,count) for word,count in self.counts.iteritems()
               if word.startswith(base)]
      sortedWords = sorted(words, key=lambda (k,v): v, reverse=True)[:50]
      return [k for k,v in sortedWords]
