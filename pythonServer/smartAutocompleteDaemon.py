#!/usr/bin/python
import sys, os, time, atexit
from signal import SIGTERM

#SmartAutocomplete imports
import http, completionLister, Features, SimpleMixer, PerceptronMixer, \
       Logger, Tracker
from ContextClassifier import ContextClassifier
from VimClassifier import VimClassifier
from Feature import Feature
 
class Daemon:
   """
   A generic daemon class.
       
   Usage: subclass the Daemon class and override the run() method
   """
   def __init__(self, pidfile, 
                stdin='/dev/null', 
                stdout=os.path.expanduser('~/.smartautocomplete/server.out'), 
                stderr=os.path.expanduser('~/.smartautocomplete/server.out')):
      self.stdin = stdin
      self.stdout = stdout
      self.stderr = stderr
      self.pidfile = pidfile
       
   def daemonize(self):
      """
      do the UNIX double-fork magic, see Stevens' "Advanced
      Programming in the UNIX Environment" for details (ISBN 0201563177)
      http://www.erlenstar.demon.co.uk/unix/faq_2.html#SEC16
      """
      try:
         pid = os.fork()
         if pid > 0:
            # exit first parent
            sys.exit(0)
      except OSError, e:
         sys.stderr.write("fork #1 failed: %d (%s)\n" % (e.errno, e.strerror))
         sys.exit(1)
       
      # decouple from parent environment
      os.chdir("/")
      os.setsid()
      os.umask(0)
       
      # do second fork
      try:
         pid = os.fork()
         if pid > 0:
            # exit from second parent
            sys.exit(0)
      except OSError, e:
         sys.stderr.write("fork #2 failed: %d (%s)\n" % (e.errno, e.strerror))
         sys.exit(1)
       
      # redirect standard file descriptors
      sys.stdout.flush()
      sys.stderr.flush()

      si = file(self.stdin, 'r')
      so = file(self.stdout, 'a+')
      se = file(self.stderr, 'a+', 0)
      os.dup2(si.fileno(), sys.stdin.fileno())
      os.dup2(so.fileno(), sys.stdout.fileno())
      os.dup2(se.fileno(), sys.stderr.fileno())

      # write pidfile
      atexit.register(self.delpid)
      pid = str(os.getpid())
      file(self.pidfile,'w+').write("%s\n" % pid)
       
   def delpid(self):
      os.remove(self.pidfile)
 
   def start(self, port, trainingDirs):
      """
      Start the daemon
      """
      # Check for a pidfile to see if the daemon already runs
      try:
         pf = file(self.pidfile,'r')
         pid = int(pf.read().strip())
         pf.close()
      except IOError:
         pid = None
       
      if pid:
         message = "pidfile %s already exist. Daemon already running?\n"
         sys.stderr.write(message % self.pidfile)
         sys.exit(0)
          
      # Start the daemon
      self.daemonize()
      self.run(port, trainingDirs)
 
   def stop(self):
      """
      Stop the daemon
      """
      # Get the pid from the pidfile
      try:
         pf = file(self.pidfile,'r')
         pid = int(pf.read().strip())
         pf.close()
      except IOError:
         pid = None
      
      if not pid:
         return
          
      # Try killing the daemon process       
      try:
         while 1:
            os.kill(pid, SIGTERM)
            time.sleep(0.1)
      except OSError, err:
         err = str(err)
         if err.find("No such process") > 0:
            if os.path.exists(self.pidfile):
               os.remove(self.pidfile)
         else:
            sys.exit(0)
 
   def restart(self, port, trainingDirs):
      """
      Restart the daemon
      """
      self.stop()
      self.start(port, trainingDirs)
 
   def run(self, port, trainingDirs):
      print "hello"
      Logger.init(Tracker.DIR + '/logs')

      features = Features.feature.values()

      cls = ContextClassifier(PerceptronMixer.PerceptronMixer(), features)

      lister = completionLister.CompletionLister(cls)
      lister.scanDirs(trainingDirs)

      server = http.Server(port, lister)
      server.run()
