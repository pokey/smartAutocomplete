import scanDir, os
from Tracker import Tracker

class CompletionLister:
   def __init__(self, classifier):
      self.classifier = classifier
      self.tracker = Tracker()

   def scanDirs(self, directories):
      """Recursively walk directory to train classifier"""
      self.directories = directories
      for directory in directories:
         print "Scanning directory " + directory
         scanDir.scanDir(directory,
                         lambda path,content: self._handleFile(path, content))

   def _handleFile(self, path, content):
      self.classifier.train(path, content)
      self.tracker.scannedFile(path, content)

   def train(self, path, content, event):
      """Train the classifier using the given content"""
      isSubdir = False
      for directory in self.directories:
         canonDir = os.path.realpath(directory)
         canonPath = os.path.realpath(path)
         if canonPath.startswith(canonDir):
            isSubdir = True
            break
      if isSubdir:
         self.classifier.train(path, content)
         self.tracker.event(path, content, event)

   def analyze(self, input):
      """Return a list of completions ordered by probability"""
      self.classifier.train(input.path, input.content)
      self.tracker.analyze(input)
      return self.classifier.predict(input)[:50]

   def accepted(self, input, selection):
      """Train classifier based on what was actually selected"""
      # FixMe: [correctness] Train
      self.tracker.accepted(input, selection)
