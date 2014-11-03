#!/usr/bin/python
import http, sys, completionLister, Features, SimpleMixer, PerceptronMixer, \
       argparse, Logger, Tracker, NaiveBayesMixer
from ContextClassifier import ContextClassifier
from KNClassifier import KNClassifier
from Feature import Feature

parser = argparse.ArgumentParser(description='run autocomplete server')
parser.add_argument("-p", "--port", type=int, default=8082,
                  help="set port on which server listens for http requests")
parser.add_argument("directories", nargs="+",
                  help="list of directories on which to train classifier")
args = parser.parse_args()

Logger.init(Tracker.DIR + '/logs')

features = Features.feature.values()

# cls = ContextClassifier(NaiveBayesMixer.NaiveBayesMixer(), features)
cls = ContextClassifier(SimpleMixer.SimpleMixer(),
                        [Features.NgramFeatureChain(4)])

lister = completionLister.CompletionLister(cls)
lister.scanDirs(args.directories)

server = http.Server(args.port, lister)
server.run()
