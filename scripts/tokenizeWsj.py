# Wsj corpus is in parsed format.  This script uses a regex to turn it into
# text.
import re, os, sys

inDir = sys.argv[1]
outDir = sys.argv[2]

def handleFile(st, outDoc):
   str = ''
   delim = ''
   lastWasPeriod = False
   for match in re.finditer(r"\(([^-][^\s]*)\s([^\s)]+)\)", st):
      type = match.group(1)
      token = match.group(2)
      if lastWasPeriod:
         lastWasPeriod = False
         outDoc.write(str + '\n')
         if token == "''":
            str = ''
            delim = ''
         else:
            str = token
            delim = ' '
      elif type != '.':
         str += delim + token
         delim = ' '
      if type == '.':
         lastWasPeriod = True
   if lastWasPeriod:
      outDoc.write(str + '\n')

for section in os.listdir(inDir):
   if section == "MERGE.LOG":
      continue
   inSectionPath = os.path.join(inDir, section)
   outSectionPath = os.path.join(outDir, section)
   if not os.path.exists(outSectionPath):
      os.mkdir(outSectionPath)
   for doc in os.listdir(inSectionPath):
      inDoc = open(os.path.join(inSectionPath, doc))
      st = inDoc.read()
      inDoc.close()
      outDoc = open(os.path.join(outSectionPath, doc), 'w+')
      handleFile(st, outDoc)
      outDoc.close()
