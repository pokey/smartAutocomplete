import scanDir, Logger, tokenizer, datetime, utils.memUsage, os, re, sys, \
       random

PREFIX_SIZE = 3

class _Phase:
   def __init__(self, name, func, tokens):
      self.name = name
      self.func = func
      self.tokens = tokens

class BenchMarker:
   def __init__(self, testingFrac, startAt, trainTokens, trainTokensPct,
         weightTraining, sample, maxSamples, filters, classifier, metrics):
      # FixMe: Support multiple classifiers
      self.testingFrac = testingFrac
      self.startAt = startAt
      self.trainTokens = trainTokens
      self.trainTokensPct = trainTokensPct
      self.weightTraining = weightTraining
      self.sample = sample
      self.maxSamples = maxSamples
      self.filters = filters
      self.classifier = classifier
      self.metrics = [metric() for metric in metrics]

   def matchesExtension(self, path):
      if self.filters.extension != None:
         extension = re.search("\.[^\.]+$", path)
         extension = extension.group(0) if extension else ''
         if extension != self.filters.extension:
            return False
      return True

   def benchmark(self, dataset):
      tokenCount = [0]
      identifierCount = [0]
      fileList = []
      def addTokens(path, content):
         if not self.matchesExtension(path):
            return
         fileList.append(path)
         words = list(tokenizer.tokenize(path, content))
         identifierCount[0] += sum([1 for word in words
                                    if tokenizer.isIdentifier(word[0])])
         tokenCount[0] += len(words)
      scanDir.scanDir(dataset, addTokens)
      trainTokens = self.trainTokens if self.trainTokens \
                    else int(self.trainTokensPct * tokenCount[0])
      metrics = self.metrics
      logFile = Logger.logFile('log')
      random.shuffle(fileList)

      def train(path, content, maxTokens):
         logFile.log('Training on {}'.format(path))
         return self.classifier.train(path, content, maxTokens, False, 1)
      def weightTrain(path, content, maxTokens):
         return self.classifier.train(path, content, maxTokens, True,
                                      self.sample)
      def benchmark(path, content, maxTokens):
         return self.classifier.benchmark(path, content, metrics,
                                        self.sample, maxTokens, self.filters)
      phases = [_Phase('Training', train, trainTokens)]
      if self.weightTraining > 0:
         phases.append(_Phase('WeightTraining', weightTrain,
                              self.weightTraining))
      finalPhase = _Phase('Testing', benchmark,
                          self.maxSamples if self.maxSamples else \
                          self.testingFrac*tokenCount[0])
      if not self.startAt:
         phases.append(finalPhase)

      phaseStart = [datetime.datetime.now()]
      phaseIdx = [0]
      phaseTokenCounter = [0]
      logFile.start(phases[phaseIdx[0]].name)
      def handleFile(path):
         f = open(path)
         # FixMe: [performance] Would it be better to iterate through
         # lines to avoid loading file into memory?
         content = f.read()
         f.close()
         path = os.path.relpath(path, dataset)
         if phaseIdx[0] == len(phases):
            return
         phase = phases[phaseIdx[0]]
         if phaseTokenCounter[0] < phase.tokens:
            sys.stdout.write("\r{} {:.2%}".format(phase.name,
               phaseTokenCounter[0] / float(phase.tokens)))
            sys.stdout.flush()
            phaseTokenCounter[0] += phase.func(path, content, phase.tokens -
                  phaseTokenCounter[0])
         else:
            self.classifier.logParams('post' + phase.name)
            nextPhaseStart = datetime.datetime.now()
            phaseTime = nextPhaseStart - phaseStart[0]
            phaseStart[0] = nextPhaseStart
            lowerName = phase.name[:1].lower() + phase.name[1:]
            performanceMap.put(lowerName + 'Time', phaseTime.total_seconds())
            outputMap.put(lowerName + 'Tokens', phaseTokenCounter[0])
            sys.stdout.write("\r{} 100.00%\n".format(phase.name))
            sys.stdout.flush()
            phaseIdx[0] += 1
            phaseTokenCounter[0] = 0
            logFile.end()
            if phaseIdx[0] < len(phases):
               logFile.start(phases[phaseIdx[0]].name)

      outputMap = Logger.mapFile('output')
      performanceMap = Logger.mapFile('performance')
      killed = False
      outputMap.put("totalTokens", tokenCount[0])
      outputMap.put("totalIdentifiers", identifierCount[0])

      startTime = datetime.datetime.now()
      try:
         for path in fileList:
            handleFile(path)
         if self.startAt:
            phases.append(finalPhase)
            logFile.start(phases[phaseIdx[0]].name)
            for path in fileList[int(self.startAt*len(fileList)):]:
               handleFile(path)
      except KeyboardInterrupt:
         print '^C received, stopping benchmark'
         killed = True
      endTime = datetime.datetime.now()
      totalTime = endTime - startTime

      performanceMap.put('totalTime', totalTime.total_seconds())
      performanceMap.put('memory', utils.memUsage.memory())
      performanceMap.put('stackSize', utils.memUsage.stacksize())
      performanceMap.put('resident', utils.memUsage.resident())
      outputMap.put("tokensTrained", self.classifier.tokensTrained)
      outputMap.put("uniqueTokens", len(self.classifier.words))
      outputMap.put("uniqueIdentifiers",
                    sum([1 for word in self.classifier.words
                         if tokenizer.isIdentifier(word)]))
      for metric in metrics:
         metric.output(outputMap)
      return killed
