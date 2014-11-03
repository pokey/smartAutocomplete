import os, utils.fileUtils

def _shouldScan(f):
   """Determine whether we should scan the given file"""
   return utils.fileUtils.istext(f) and not f.endswith('.log')

def scanDir(directory, func):
   # FixMe: [usability] Look for .gitignore and use it to automatically
   # figure out files to ignore?  Problem is that even if a file is in
   # .gitignore doesn't mean it wasn't written by a user.  Maybe have
   # list of files / extensions that are generally not written by hand.
   # Maybe better even to just learn this list
   for root, dirs, files in os.walk(directory):
      # Don't crawl .git
      # FixMe: [correctness] Do people ever manually edit files in .git?
      # FixMe: [maintainability][usability] Hack to avoid training on
      # smartautocomplete record files
      dirs[:] = [d for d in dirs if d != '.git']
      dirs.sort()
      files.sort()
      for f in files:
         fullpath = os.path.join(root, f)
         if (_shouldScan(fullpath)):
            f = open(fullpath)
            # FixMe: [performance] Would it be better to iterate through
            # lines to avoid loading file into memory?
            content = f.read()
            f.close()
            func(os.path.abspath(fullpath), content)

