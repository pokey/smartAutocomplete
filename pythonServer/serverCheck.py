#!/usr/bin/python
import urllib, urllib2, smartAutocompleteDaemon, sys, os, argparse

parser = argparse.ArgumentParser(description='run autocomplete server daemon')
parser.add_argument("-p", "--port", type=int, default=8080,
                    help="set port on which server listens for http requests")
parser.add_argument("-d", "--directories", nargs="?", default = "",
                    help="list of directories on which to train classifier")
args = parser.parse_args()

URI = 'http://localhost:' + str(args.port) + '/test?dirs=' + \
      urllib.quote_plus(args.directories)

directories = [os.path.expanduser(d) for d in args.directories.split()]

try:
   r = urllib2.Request(URI, '')
   urllib2.urlopen(r)
except Exception, e:
   daemon = smartAutocompleteDaemon.Daemon('/tmp/daemon-example.pid')
   daemon.restart(args.port, directories)
