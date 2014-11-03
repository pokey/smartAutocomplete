#!/usr/bin/python
import sys, scriptRunnerDaemon, os, iterateRuns
from iterateRuns import constants
from scriptRunnerDaemon import DIR, LOCK
from utils.fileUtils import FileLock, copyDirFiltered, uniqueDir, uniqueFile

def cpFilter(fileName):
   if fileName == '.git' or \
      fileName.endswith('.swp') or \
      fileName.endswith('.swo') or \
      fileName.endswith('.pyc') or \
      fileName.endswith('~'):
      return False
   return True

def srcDirLoc(c):
   return constants['statePath'] + '/src/' + c

srcDir = uniqueDir(srcDirLoc)
copyDirFiltered(os.path.expanduser(sys.argv[1]), srcDir, cpFilter)

def fileName(c):
   return DIR + '/' + c + '.task'

def execName(c):
   return constants['statePath'] + '/execs/' + c + '.exec'

l = FileLock(LOCK)
l.acquire()
try:
   fd = uniqueFile(fileName)
   if len(sys.argv) > 2:
      addToView = sys.argv[2]
   else:
      addToView = ""
   for cmd in sys.stdin.readlines():
      execDir = uniqueDir(execName)
      srcLocFile = open(execDir + '/srcLoc', 'w')
      srcLocFile.write(srcDir)
      srcLocFile.close()
      if addToView:
         addToViewFile = open(execDir + '/addToView', 'w')
         addToViewFile.write(addToView)
         addToViewFile.close()
      os.write(fd, execDir + '\t' + cmd)
   os.close(fd)
finally:
   l.release()
