class o:
   def __init__(self, opt, val):
      self.opt = opt
      self.val = val

   def argList(self):
      return [_stringify(self.opt, self.val)]

class f:
   def __init__(self, flag):
      self.flag = flag

   def argList(self):
      return ['--{}'.format(self.flag)]

class selo_base:
   def argList(self):
      if self.which == None:
         return ['']
      if self.which == '*':
         return [self._optStr(val) for val in self.vals]
      if isinstance(self.which, list):
         return [self._optStr(self.vals[i]) for i in self.which]
      return [self._optStr(self.vals[self.which])]

class selo(selo_base):
   def __init__(self, opt, which, *vals):
      self.opt = opt
      self.which = which
      self.vals = vals

   def _optStr(self, val):
      return _stringify(self.opt, val)

class selmo(selo_base):
   """Given a tuple of options, enumerates filling in the tuples of values for
   the given options"""
   def __init__(self, optTuple, which, *valTuples):
      self.optTuple = optTuple
      self.which = which
      self.vals = valTuples

   def _optStr(self, valTuple):
      return ' '.join([_stringify(a,v) for a,v in zip(self.optTuple,
                                                      valTuple)])

def cmdList(cmd, *args):
   argList = []
   for arg in args:
      argList.append(arg.argList())

   return [cmd + ' ' + ' '.join(opt) for opt in _cross(argList)]

def printCmds(cmd, *args):
   print '\n'.join(cmdList(cmd, *args))

def _cross(args):
   ans = [[]]
   for arg in args:
      ans = [x+[y] for x in ans for y in arg]
   return ans

def _stringify(arg, val):
   if val == None:
      return ''
   if isinstance(val, (tuple, list)):
      val = ' '.join(val)
   else:
      val = str(val)
   return '--{} {}'.format(arg, val)
