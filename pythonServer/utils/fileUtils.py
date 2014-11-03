from __future__ import division
import os, sys, string, shutil, errno
from contextlib import contextmanager

# from http://stackoverflow.com/a/1446870
def istext(filename):
   """Determines if a file is binary or text"""
   s=open(filename).read(512)
   text_characters = "".join(map(chr, range(32, 127)) + list("\n\r\t\b"))
   _null_trans = string.maketrans("", "")
   if not s:
      # Empty files are considered text
      return True
   if "\0" in s:
      # Files with null bytes are likely binary
      return False
   # Get the non-text characters (maps a character to itself then
   # use the 'remove' option to get rid of the text characters.)
   t = s.translate(_null_trans, text_characters)
   # If more than 30% non-text characters, then
   # this is considered a binary file
   if len(t)/len(s) > 0.30:
      return False
   return True

class FileLock:
   def __init__(self, file):
      self.file = file

   def acquire(self):
      fd = os.open(self.file, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
      os.write(fd, '1')
      os.close(fd)

   def release(self):
      os.remove(self.file)

def copyDirFiltered(src, dst, filter):
   for root, dirs, files in os.walk(src):
      newDirs = []
      for d in dirs:
         path = os.path.join(root, d)
         relPath = path[len(src)+1:]
         if filter(relPath):
            newDirs.append(d)
            os.makedirs(os.path.normpath(dst + '/' + relPath))
      dirs[:] = newDirs
      for f in files:
         path = os.path.join(root, f)
         relPath = path[len(src)+1:]
         if filter(path):
            shutil.copyfile(path, os.path.normpath(dst + '/' + relPath))

def _unique(creator, nameFunc):
   counter = 0
   while True:
      counter += 1
      name = nameFunc(str(counter))
      try:
         ret = creator(name)
         break
      except OSError as exception:
         if exception.errno != errno.EEXIST:
            raise
   return ret

def _fileCreator(name):
   return os.open(name, os.O_CREAT | os.O_EXCL | os.O_WRONLY)

def _dirCreator(name):
   os.mkdir(name)
   return name

def uniqueFile(nameFunc):
   return _unique(_fileCreator, nameFunc)

def uniqueDir(nameFunc):
   return _unique(_dirCreator, nameFunc)
