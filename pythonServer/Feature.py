class Feature:
   """Feature for ContextClassifier to keep track of"""
   def __init__(self, name, contextFunc, featureFunc):
      # The name is purely for documentation purposes
      self.name = name

      # Map from (path, file content, cursor location) --> context
      self.contextFunc = contextFunc

      # Map from (path, file content, cursor location, word) --> feature value
      self.featureFunc = featureFunc

class FeatureChain:
   """Chain of features for KNFeatureChain to keep track of"""
   def first(self, input):
      return None

   def succ(self, featureId):
      return None

def decorate(func, decorator):
   return lambda *x: decorator(func(*x))
