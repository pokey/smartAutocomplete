import diff_match_patch, os, datetime, sys

# Event types
# FixMe: [maintainability] Find a way to sync this with the list kept in
# ../plugin/smartautocomplete.vim
SCANNED_FILE = 0
ANALYZE = 1
ACCEPTED = 2
BUF_LEAVE = 3
BUF_WRITE = 6
BUF_UNLOAD = 7
INSERT_ENTER = 9
INSERT_LEAVE = 10

# Directory to use for keeping records
DIR = os.path.expanduser('~/.smartautocomplete')

# Separator for entries in event log
ENTRY_SEPARATOR_CHAR = chr(28)
# Use file separator to separate fields in event log
FIELD_SEPARATOR_CHAR = chr(29)
# Separator for subfields of event info
EI_SEPARATOR_CHAR = chr(30)

class _FileInfo:
   def __init__(self, file, content):
      self.file = file
      self.content = content

class Tracker:
   """Keeps track of all changes to files."""
   def __init__(self):
      if not os.path.exists(DIR):
         os.mkdir(DIR)
      self.outFile = open(DIR + '/events.log', 'a+')
      self.fileInfoMap = {}
      self.dmp = diff_match_patch.diff_match_patch()

   def _handleEvent(self, path, content, mode, extraInfo, closeFile=False):
      """Handle a file being updated"""
      # FixMe: [performance] Figure out way to do this after we have responded
      # to client
      if path in self.fileInfoMap:
         fileInfo = self.fileInfoMap[path]
      else:
         localPath = DIR + '/localCopies' + os.path.abspath(path)
         if os.path.isdir(localPath):
            return
         if os.path.exists(localPath):
            f = open(localPath, 'r+')
            self.fileInfoMap[path] = fileInfo = _FileInfo(f, f.read())
         else:
            parentDir = os.path.dirname(localPath)
            if not os.path.exists(parentDir):
               os.makedirs(parentDir)
            f = open(localPath, 'w')
            self.fileInfoMap[path] = fileInfo = _FileInfo(f, '')
      f = fileInfo.file
      oldContent = fileInfo.content
      print path
      sys.stdout.flush()
      f.seek(0)
      f.write(content)
      #f.truncate() 
      if closeFile:
         f.close()
         del self.fileInfoMap[path]
      else:
         fileInfo.content = content
      diff = self.dmp.patch_toText(self.dmp.patch_make(oldContent, content))
      if not diff and (mode != BUF_WRITE and mode != INSERT_ENTER and
                       mode != INSERT_LEAVE and mode != ANALYZE and
                       mode != ACCEPTED):
         return
      self.outFile.write(str(datetime.datetime.now()) + FIELD_SEPARATOR_CHAR +
                         path + FIELD_SEPARATOR_CHAR + diff +
                         FIELD_SEPARATOR_CHAR + str(mode) +
                         FIELD_SEPARATOR_CHAR + extraInfo +
                         ENTRY_SEPARATOR_CHAR + '\n')
      self.outFile.flush()

   def scannedFile(self, path, content):
      self._handleEvent(path, content, SCANNED_FILE, '', True)

   def event(self, path, content, e):
      self._handleEvent(path, content, e, '', e == BUF_UNLOAD)

   def analyze(self, input):
      self._handleEvent(input.path, input.content, ANALYZE,
                        input.base + EI_SEPARATOR_CHAR + str(input.location) +
                        EI_SEPARATOR_CHAR + str(input.up))

   def accepted(self, input, selection):
      self._handleEvent(input.path, input.content, ACCEPTED,
                        input.base + EI_SEPARATOR_CHAR + str(input.location) +
                        EI_SEPARATOR_CHAR + str(input.up) + EI_SEPARATOR_CHAR
                        + selection)
