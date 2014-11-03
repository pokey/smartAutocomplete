#!/usr/bin/python
import subprocess, re, time, os, errno, sys
from utils.fileUtils import FileLock

DIR = os.path.expanduser('~/tasks')
if not os.path.exists(DIR):
   os.makedirs(DIR)
LOCK = DIR + '/lock'
SLEEP_TIME = 300

def availableMachines():
   mstatOut = subprocess.check_output('mstat -le', shell=True)
   # mstatOut = open('mstat.out').read()
   mstatLines = mstatOut.split('\n')[1:-2]
   machines = []
   for line in mstatLines:
      if not line[:10].isspace() and float(line[10:18].strip()) < 100.00 \
         and int(line[18:26].strip()) > 5000:
         machines.append(re.match('\S+', line.strip()).group())

   # FixMe: [performance] Remove this line once I figure out why I can't use
   # jude machines
   machines = [m for m in machines
               if (re.match('jude\d+$', m) == None and
                   re.match('john\d+$', m) == None) or
                  re.match('jude2\d$', m)]

   return machines

isSub = False

def run(machine, execDir, cmd):
   global isSub
   print 'Running on {} in {}:'.format(machine, execDir)
   print '   ' + cmd
   pid = os.fork()
   if pid == 0:
      isSub = True
      outFile = execDir + '/out'
      srcLocFile = open(execDir + '/srcLoc')
      srcDir = srcLocFile.read()
      srcLocFile.close()
      scriptFile = open(execDir + '/script', 'w+')
      scriptFile.write('cd {} ; EXEC_DIR={} {} &> {}'.format(srcDir, execDir,
                                                             cmd, outFile))
      scriptFile.close()
      os.execl('/usr/bin/ssh', 'ssh', machine, 'sh ' + execDir + '/script')
      sys.exit(1)

def main():
   global isSub
   while True:
      l = FileLock(LOCK)
      try:
         l.acquire()
      except OSError as exception:
         if exception.errno != errno.EEXIST:
            raise
         else:
            time.sleep(SLEEP_TIME)
            continue
      try:
         try:
            machines = availableMachines()
         except:
            machines = []
         machineIndex = 0
         files = list(os.listdir(DIR))
         for f in files:
            if not re.match('\d+.task', f):
               continue
            fileName = os.path.join(DIR, f)
            file = open(fileName, 'r+')
            remaining = ''
            for cmd in file:
               if machineIndex >= len(machines):
                  remaining += cmd
               else:
                  idx = cmd.index('\t')
                  run(machines[machineIndex], cmd[0:idx], cmd[idx+1:].strip())
                  machineIndex += 1
            if remaining:
               file.seek(0)
               file.write(remaining)
               file.truncate()
               file.close()
            else:
               file.close()
               os.remove(fileName)
      finally:
         if not isSub:
            l.release()
      time.sleep(SLEEP_TIME)

if __name__ == "__main__":
   main()
