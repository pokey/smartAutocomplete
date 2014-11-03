#!/usr/bin/python
import sys, BenchMarker, Features, SimpleMixer, WeightMixer, \
       PerceptronMixer, BucketPerceptronMixer, argparse, Logger, platform, \
       datetime, getpass, tarfile, os, TokenCounter, subprocess, \
       BayesianModelAvgMixer, NaiveBayesMixer, random
from ContextClassifier import ContextClassifier
from PerceptronClassifier import PerceptronClassifier
from VimClassifier import VimClassifier
from KNFeatureGrid import KNFeatureGrid
from FirstN import FirstNClass, noFilter, identifierFilter
from NumkeystrokesN import NumkeystrokesNClass, noFilter, identifierFilter

parser = \
   argparse.ArgumentParser(description="run benchmarks to test classifier")
parser.add_argument("-d", "--dataset",
                    help="dataset on which to test and train classifier")
parser.add_argument("-p", "--statePath", required=True,
                    help="directory to output results to")
parser.add_argument("-r", "--startAt", type=float, default=0.0,
                          help="location in dataset (as fraction) to start "
                               "testing")
startPosArgs = parser.add_mutually_exclusive_group()
startPosArgs.add_argument("-c", "--trainTokensPct", type=float, default=0.0,
                          help="percent of tokens to train on before testing")
startPosArgs.add_argument("-k", "--trainTokens", type=int, default=0,
                          help="number of tokens to train on before testing")
parser.add_argument("-w", "--weightTraining", type=int, default=0,
                     help="number of tokens to train weights on")
parser.add_argument("-t", "--testingFrac", type=float, default=1.0,
                    help="fraction of dataset on which to do testing")
parser.add_argument("-s", "--samplePeriod", type=int, default=1, metavar="S",
                    help="test on 1 out of every S words")
parser.add_argument("-l", "--learningAlgorithm", default="counter",
                    help="learning algorithm to use")
parser.add_argument("-f", "--features", nargs="+", 
                    default=Features.feature.keys(),
                    help="features to use in classifier")
parser.add_argument("-m", "--maxSamples", type=int, default=sys.maxint,
                    help="maximum number of samples")
parser.add_argument("--seed", type=int, default=0,
                    help="number to seed random generator with")

filters = parser.add_argument_group("filters", "filter tokens to be sampled "
                                               "based on various criteria")
filters.add_argument("--onlyIdentifiers", action="store_true",
                     help="toggle identifier-only/non-punctuation sampling")
filters.add_argument("--minWordLength", type=int, default=0, metavar="L",
                     help="only sample identifiers of length at least L")
filters.add_argument("--onlySeen", action="store_true",
                     help="only sample identifiers that have been seen before")
filters.add_argument("--inFirst", type=int, default=-1, metavar="N",
                     help="only handle samples where actual word is in the "
                          "first N predictions")
filters.add_argument("--notInFirst", type=int, default=-1, metavar="N",
                     help="only handle samples where actual word is not in "
                          "the first N predictions")
filters.add_argument("--extension", default=None, metavar="EXT",
                     help="only test on files with extension EXT")

args = parser.parse_args()

args.extension = \
   args.extension if not args.extension or args.extension.startswith('.') \
                  else '.' + args.extension

random.seed(args.seed)

execDir = os.getenv('EXEC_DIR', '')

if execDir:
   Logger.execDirIs(execDir)
else:
   Logger.init(args.statePath + '/execs')

def tarFilter(tarinfo):
   if tarinfo.name == 'smartAutocomplete/.git' or \
      tarinfo.name.endswith('.swp') or \
      tarinfo.name.endswith('.swo') or \
      tarinfo.name.endswith('.pyc') or \
      tarinfo.name.endswith('~'):
      return None
   return tarinfo
tar = tarfile.open(Logger.dirName + "/code.tar.gz", "w:gz")
tar.add(os.path.dirname(os.path.dirname(os.path.realpath(__file__))),
        arcname="smartAutocomplete", filter=tarFilter)
tar.close()

datasetHash = subprocess.check_output('find ' + args.dataset + ' -type f -print0 | sort -z | xargs -0 sha1sum | sha1sum', shell=True)
datasetHashFile = Logger.logFile('datasetHash')
datasetHashFile.log(datasetHash)
datasetHashFile.flush()

optionsMap = Logger.mapFile("options")
optionsMap.put('dataset', os.path.basename(os.path.normpath(args.dataset)) +
               ' (' + datasetHash[:7] + ')')
optionsMap.put('startAt', args.startAt)
optionsMap.put('trainTokens', args.trainTokens)
optionsMap.put('trainTokensPct', args.trainTokensPct)
optionsMap.put('weightTraining', args.weightTraining)
optionsMap.put('testingFrac', args.testingFrac)
optionsMap.put('samplePeriod', args.samplePeriod)
optionsMap.put('learningAlgorithm', args.learningAlgorithm)
optionsMap.put('features', ' '.join(args.features))
optionsMap.put('maxSamples', args.maxSamples)
optionsMap.put('onlyIdentifiers', args.onlyIdentifiers)
optionsMap.put('minWordLength', args.minWordLength)
optionsMap.put('onlySeen', args.onlySeen)
optionsMap.put('inFirst', args.inFirst)
optionsMap.put('notInFirst', args.notInFirst)
optionsMap.put('extension', args.extension)
optionsMap.flush()

cpuInfo = Logger.logFile('cpuInfo')
cpuInfo.log(open('/proc/cpuinfo').read())
cpuInfo.flush()

systemInfoMap = Logger.mapFile("systemInfo")
systemInfoMap.put('time', datetime.datetime.now())
systemInfoMap.put('username', getpass.getuser())
systemInfoStats = ['system', 'node', 'release', 'version', 'machine',
                   'processor', 'python_version', 'python_implementation']
for stat in systemInfoStats:
   systemInfoMap.put(stat, getattr(platform, stat)())
systemInfoMap.flush()

features = [Features.feature[f] for f in args.features]

if args.learningAlgorithm == "counter":
   classifier = ContextClassifier(SimpleMixer.SimpleMixer(), features)
elif args.learningAlgorithm == "weights":
   classifier = ContextClassifier(WeightMixer.WeightMixer(), features)
elif args.learningAlgorithm == "perceptron":
   classifier = PerceptronClassifier(features)
elif args.learningAlgorithm == "perceptronMixer":
   classifier = ContextClassifier(PerceptronMixer.PerceptronMixer(), features)
elif args.learningAlgorithm == "bucketPerceptronMixer":
   classifier = ContextClassifier(BucketPerceptronMixer.BucketPerceptronMixer(), features)
elif args.learningAlgorithm == "naiveBayes":
   classifier = ContextClassifier(NaiveBayesMixer.NaiveBayesMixer(), features)
elif args.learningAlgorithm == "bma":
   classifier = \
      ContextClassifier(BayesianModelAvgMixer.BayesianModelAvgMixer(),
                        features)
elif args.learningAlgorithm == "vim":
   classifier = VimClassifier()
elif args.learningAlgorithm == "grid":
   classifier = KNFeatureGrid(features)
else:
   raise "Unknown classifier"

# Metrics
metrics = []
if not args.onlyIdentifiers:
   metrics += [FirstNClass(n, noFilter) for n in range(1,6)]
metrics += [FirstNClass(n, identifierFilter) for n in range(1,6)]

class Filters:
   def __init__(self, args):
      self.onlyIdentifiers = args.onlyIdentifiers
      self.minWordLength = args.minWordLength
      self.onlySeen = args.onlySeen
      self.inFirst = args.inFirst
      self.notInFirst = args.notInFirst
      self.extension = args.extension

benchmarker = \
      BenchMarker.BenchMarker(args.testingFrac, args.startAt,
            args.trainTokens, args.trainTokensPct, args.weightTraining,
            args.samplePeriod, args.maxSamples, Filters(args), classifier,
            metrics)
killed = benchmarker.benchmark(args.dataset)

Logger.mapFile('output').put('exec.status', 'killed' if killed else 'done')
