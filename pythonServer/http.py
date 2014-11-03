#!/usr/bin/python
from BaseHTTPServer import BaseHTTPRequestHandler,HTTPServer
from Input import Input
import re, urllib

# This class handles any incoming request from the browser 
class AutocompletionHandler(BaseHTTPRequestHandler):
   # Handler for POST requests
   def do_POST(self):
      match = re.match('/complete\?path=([^&]+)&base=([^&]*)&loc=(\d+)&up=(\d+)', self.path)
      if match:
         content = self.rfile.read(int(self.headers['Content-Length']))
         path = urllib.unquote_plus(match.group(1))
         base = urllib.unquote_plus(match.group(2))
         location = int(match.group(3))
         up = int(match.group(4))
         identifierList = \
            self.server.lister.analyze(Input(path, content, location, base, up))
         ret = chr(30).join(identifierList)
         self.send_response(200)
         self.end_headers()
         self.wfile.write(ret)
         return         
      match = re.match('/eligible\?path=([^&]+)&loc=(\d+)&up=(\d+)', self.path)
      if match:
         content = self.rfile.read(int(self.headers['Content-Length']))
         path = urllib.unquote_plus(match.group(1))
         location = int(match.group(2))
         up = int(match.group(3))
         self.send_response(200)
         self.end_headers()
         self.wfile.write("1")
         return         
      match = re.match('/train\?path=([^&]+)&event=([^&]+)', self.path)
      if match:
         content = self.rfile.read(int(self.headers['Content-Length']))
         path = urllib.unquote_plus(match.group(1))
         event = int(match.group(2))
         self.server.lister.train(path, content, event)
         self.send_response(200)
         self.end_headers()
         self.wfile.write('Thanks')
         return         
      match = re.match('/accepted\?selection=([^&]+)&path=([^&]+)&base=([^&]*)&loc=(\d+)&up=(\d+)', self.path)
      if match:
         content = self.rfile.read(int(self.headers['Content-Length']))
         selection = urllib.unquote_plus(match.group(1))
         path = urllib.unquote_plus(match.group(2))
         base = urllib.unquote_plus(match.group(3))
         location = int(match.group(4))
         up = int(match.group(5))
         self.server.lister.accepted(Input(path, content, location, base, up),
                                     selection)
         self.send_response(200)
         self.end_headers()
         self.wfile.write('Thanks')
         return 
      match = re.match('/test\?dirs=([^&]+)', self.path)
      if match:
         dirs = urllib.unquote_plus(match.group(1)).split()
         for d in dirs:
            if d not in self.server.trainingDirs:
               self.server.trainingDirs[d] = True
               self.server.lister.scanDirs([d])
         self.send_response(200)
         self.end_headers()
         self.wfile.write(str(self.server.trainingDirs))
         return    

class AutocompletionHttpServer(HTTPServer):
   def __init__(self, port, lister):
      HTTPServer.__init__(self, ('', port), AutocompletionHandler)
      self.lister = lister
      self.trainingDirs = {}
      AutocompletionHttpServer

class Server:
   def __init__(self, port, lister):
      # Create a web server and define the handler to manage the
      # incoming request
      self.server = AutocompletionHttpServer(port, lister)
      self.port = port
   def run(self):
      try:
         print 'Started httpserver on port', self.port
         # Wait forever for incoming http requests
         self.server.serve_forever()
      except KeyboardInterrupt:
         print '^C received, shutting down the web server'
         self.server.socket.close()
