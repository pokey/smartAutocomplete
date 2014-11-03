#!/usr/bin/python
from execrunner import o, selo, selmo, cmdList, f
import subprocess, argparse

# Dataset prefix
dp = '/u/nlp/data/smart-autocomplete/datasets/'
constants = {
   'python': '/u/nlp/packages/python-2.7.4/bin/python2.7',
   'statePath': '/u/nlp/data/smart-autocomplete/state'
}

def main():
   parser = \
      argparse.ArgumentParser(description="iterate runs on nlp cluster")
   parser.add_argument("-s", "--nlpServer", default='jacob.stanford.edu',
                       help="url of nlp cluster")
   parser.add_argument("-d", "--srcDir", default='~/src/smartAutocomplete',
                       help="location of smartAutocomplete src on nlp cluster")
   parser.add_argument("-v", "--addToView", default='')

   args = parser.parse_args()

   constants['srcDir'] = args.srcDir
   constants['nlpServer'] = args.nlpServer
   constants['addToView'] = args.addToView

   cmds = \
      cmdList('{python} server/runBenchmarks.py'.format(**constants),

              selo('startAt', 2, 0, .5, .9),

              selmo(('trainTokens', 'weightTraining', 'maxSamples'), 0,
                    (100000, None, 500), (1000000, 10000, 1000)),

              selmo(('dataset', 'extension'), 1,
                    (dp + 'node', 'js'),
                    (dp + 'django', 'py'),
                    (dp + 'english-large-jokes', None),
                    (dp + 'english-small-sanity-check', None),
                    (dp + 'javascript-large-d3subset', 'js'),
                    (dp + 'javascript-medium-emile', 'js'),
                    (dp + 'python-large-web-py-subset', 'py'),
                    (dp + 'python-medium-singularity-chess', 'py')),

              selo('features', -2,

                   # KN
                   ['scope', 'ngram'],

                   # Ngrams
                   ['simple', 'prev', 'prevTwo', 'prevThree'],

                   # Basic features
                   ['simple', 'path', 'filetype', 'prev', 'prevTwo', 'prevThree',
                    'prevPrev'],

                   # Experiment
                   ['simple', 'path', 'prev'],

                   # Individual features
                   ['simple'],
                   ['path'],
                   ['filetype'],
                   ['prev'],
                   ['prevTwo'],
                   ['prevThree'],
                   ['prevPrev'],
                   ['prevForm'],
                   ['lineStart'],
                   ['indentLevel'],
                   ['dirH'],
                   ['linePrefix'],
                   ['scope'],
                   ['ngram'],

                   # All features
                   None),

              f("onlyIdentifiers"),

              o("samplePeriod", 50),
              o("statePath", constants["statePath"]),
              selo("learningAlgorithm", 0, "counter", "weights", "perceptron",
                   "perceptronMixer", "bucketPerceptronMixer", "naiveBayes",
                   "bma", "grid"))

   proc = subprocess.Popen('ssh {nlpServer} {python} '
                           '{srcDir}/server/populateTasks.py {srcDir} {addToView}'
                           .format(**constants), shell=True,
                           stdin=subprocess.PIPE)
   proc.communicate('\n'.join(cmds)+'\n')

if __name__ == "__main__":
   main()
