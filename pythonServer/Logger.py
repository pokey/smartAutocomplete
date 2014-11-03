import datetime, os
from utils.fileUtils import uniqueDir

_tab = "   "
dirName = ''

# Set execution directory
def execDirIs(name):
   global dirName
   dirName = name

def init(name):
   global dirName
   def _counterToName(c):
      return name + '/' + c + '.exec'
   if not os.path.exists(name):
      os.makedirs(name)
   dirName = uniqueDir(_counterToName)

_logFileMap = {}

def logFile(name):
   global _logFileMap
   if name not in _logFileMap:
      _logFileMap[name] = LogFile(name)
   return _logFileMap[name]

class LogFile:
   def __init__(self, name):
      self.file = open(dirName + "/" + name, 'w+')
      self.depth = 0
      self.times = []

   def start(self, name):
      self.file.write(self.depth * _tab + name + " {\n")
      self.depth += 1
      self.times.append(datetime.datetime.now())

   def end(self):
      self.depth -= 1
      diff = datetime.datetime.now() - self.times[-1]
      self.file.write(self.depth * _tab + "} " + str(diff.total_seconds()) + "\n")
      self.times = self.times[:-1]

   def log(self, string):
      self.file.write(self.depth * _tab)

      currCol = self.depth * len(_tab)
      bracket = -1
      for c in str(string):
         currCol += 1
         self.file.write(c)
         if c == '{' and bracket == -1:
            bracket = currCol
         elif c == '\n':
            if bracket == -1:
               self.file.write(self.depth * _tab)
            else:
               self.file.write(bracket * ' ')

      self.file.write("\n")

   def flush(self):
      self.file.flush()

   def close(self):
      self.file.close()

_mapFileMap = {}

def mapFile(name):
   global _mapFileMap
   if name not in _mapFileMap:
      _mapFileMap[name] = MapFile(name)
   return _mapFileMap[name]

class MapFile:
   def __init__(self, name):
      self.file = open(dirName + "/" + name + ".map", 'w+')

   def put(self, key, val):
      self.file.write(str(key) + '\t' + str(val) + '\n')

   def flush(self):
      self.file.flush()
