package smartAutocomplete;

import java.io.*;
import java.util.*;

import fig.basic.*;
import fig.exec.*;
import fig.prob.*;
import static fig.basic.LogInfo.begin_track;
import static fig.basic.LogInfo.logs;
import static fig.basic.LogInfo.end_track;

import org.yaml.snakeyaml.Yaml;

import smartAutocomplete.httpServer.*;
import smartAutocomplete.util.*;

public class Main implements Runnable {
  // Model
  @Option(gloss="Order of n-gram model") public static int ngramOrder = -1;
  @Option(gloss="Number of latent clusters") public static int clusters = 1;
  @Option(gloss="Maximum number of candidates to consider") public static int maxCandidates = Integer.MAX_VALUE;
  @Option(gloss="Features to use") public HashSet<String> featureDomains = new HashSet<String>();
  @Option(gloss="Supported languages") public static HashSet<String> languages = new HashSet<String>();
  @Option(gloss="Size of batches for gradient") public int batchSize = 1;
  @Option(gloss="Use vocabulary from file") public static String vocabFile = null;

  @Option(gloss="Init params from file") public String paramsInFile = null;

  @Option(gloss="Do gradient check") public boolean gradCheck = false;

  @Option(gloss="Spawn completion server") public boolean completionServer = false;

  @Option(gloss="Verbosity level") static public int verbose = 0;
  @Option(gloss="Whether to have features write out counts")
    static public boolean writeCounts = false;

  public static OptionsParser optionsParser = new OptionsParser();

  public static Set<String> supportedLanguages = null;

  private static Vocab vocab;

  // Returns true if the language of the file given by path is one of the
  // languages passed into the list of supported languages.  If no languages
  // are listed, then every file is considered supported.
  // Language is determined by file extension.
  public static boolean supportsLanguage(String path) {
    if (languages.size() == 0) return true;
    String language = FileUtil.getExtension(path);
    return supportedLanguages == null ? languages.contains(language)
                                      : supportedLanguages.contains(language);
  }

  // Returns the language of the file given by path, or "" if the file is of a
  // language that was not passed into the list of supported languages.
  // Language is determined by file extension.
  public static String getLanguage(String path) {
    String language = FileUtil.getExtension(path);
    return languages.contains(language) ? language : "";
  }

  public static boolean isFixedVocab() { return vocabFile != null; }
  public static Vocab getVocab() { return vocab; }

  private FeatureDomain[] featureDomainObjs;

  private Statistics statistics = new Statistics();

  private Dataset dataset;

  // Set by tuning and predicting
  private Params params;

  static FeatureDecorator[] clusterDecorators;

  public Main(Dataset dataset, Params params) {
    this.dataset = dataset;
    this.params = params;
  }

  public void run() {
    init();
    dataset.readData();
    this.supportedLanguages = dataset.supportedLanguages;
    trainAndEvalModel();
  }

  void init() {
    if (vocabFile != null) {
      vocab = new Vocab();
      vocab.readFile(vocabFile);
    }

    clusterDecorators = new FeatureDecorator[clusters];
    for (int i=0; i<clusters; i++)
      clusterDecorators[i] = new SuffixFeatureDecorator(",cluster=" + i);

    List<FeatureDomain> featureDomainList = new ArrayList<FeatureDomain>();
    statistics.setLanguages(languages);
    for (Corpus corpus : statistics.getProjectLangCorpi()) {
      FeatureDomain.addFeatureDomains(featureDomainList, featureDomains, params,
                                      statistics, corpus);
      statistics.requireStatistic(FileTokenKNCounts.class, corpus);
      statistics.requireStatistic(FileKNCounts.class, corpus);
    }
    featureDomainObjs = featureDomainList.toArray(new FeatureDomain[featureDomains.size()]);

    if (paramsInFile != null) params.read(paramsInFile);
  }

  void trainAndEvalModel() {
    begin_track("Train and eval");

    begin_track("Global counting");
    // ReadData.iterDocuments(ReadData.globCountPaths, new ReadData.DocHandler() {
    //   @Override
    //   public boolean handle(Document doc) {
    //     DocUtils.countDoc(doc, featureDomainObjs,
    //                       FeatureDomain.CountType.GLOBAL);
    //     logs("File: %s", doc.path);
    //     return true;
    //   }
    // });
    end_track();

    Evaluation evaluation = new Evaluation();
    RunningGradient batchGrad = new RunningGradient();
    RunningGradient totalGrad = new RunningGradient();
    int batchCounter = 0;
    Exec exec = new Exec(featureDomains);
    CodeInfo codeInfo = null;
    boolean startServer = false;
    List<Object> predictions = new ArrayList<Object>();
    int tuneCounter = 0;
    int tuningPeriod = dataset.tuningPeriod;
    for (Document doc : dataset.documents) {
      Corpus corpus = statistics.getProjectLangCorpus(doc.path);
      List<Statistic> corpusStatistics =
        statistics.getCorpusStatistics(corpus);
      if (dataset.tuningTotal != 0 && doc.tune) {
        tuningPeriod = dataset.getTuningPeriod(doc);
      }
      if (doc.web) {
        startServer = true;
        codeInfo = exec.addCodeInfo(doc.path);
      }
      for (int i = doc.start(); i < doc.end(); i++) {
        PredictionContext context = new SequentialContext(doc, i);
        Token trueToken = doc.tokens.get(i);
        HashMap<String, Double> grad = null;
        evaluation.addCumulative("numTokens", 1);
        if ((doc.eval || doc.tune || doc.web) &&
            tuneCounter < dataset.tuningMax) {
          if (tuneCounter % tuningPeriod == 0) {
            InferState state = new InferState(featureDomainObjs, params,
                                              statistics, context, trueToken);
            grad = Gradient.computeGradient(state.getTrueCandidate(),
                                            state.getCandidates().list());
            if (gradCheck && state.isOracle()) {
              FunctionState fs =
                new FunctionState(featureDomainObjs, params, statistics,
                                  context, trueToken);
              if (GradCheck.doGradientCheck(fs))
                logs("Grad check passed");
              else
                logs("Grad check failed");
            }
            if (doc.eval) {
              if (state.isOracle()) totalGrad.add(grad);
              evaluation.add(Eval.eval(state));
              predictions.add(InferStateToMap.toMap(state, 5));
            }
            if (doc.web) {
              codeInfo.addToken(trueToken.loc(), state);
            }
            if (doc.tune && state.isOracle()) {
              evaluation.addCumulative("tunedTokens", 1);
              batchGrad.add(grad);
              if (batchGrad.getCount() % batchSize == 0) {
                if (verbose >= 3) batchGrad.log();
                params.update(batchGrad.getMean());
                batchGrad.reset();
              }
            }
          }
          tuneCounter++;
        }
        if (doc.count) {
          for (Statistic statistic : corpusStatistics) {
            statistic.count(context, trueToken.str());
          }
          if (doc.lastCount) {
            for (Statistic statistic : corpusStatistics) {
              statistic.doneCounting();
            }
          }
        }
      }

      logs("%s\tcumulative: %s %s", doc.getModeStr(), evaluation.summary(),
          doc.path);
      evaluation.putOutput("eval");
      if (verbose >= 3)
        logs("Memory usage: %s", SysInfoUtils.getUsedMemoryStr());
    }

    try {
      BufferedWriter yamlOut =
        new BufferedWriter(IOUtils.openOutEasy(Execution.getFile("candidates")));
      Yaml yaml = new Yaml();
      yaml.dumpAll(predictions.iterator(), yamlOut);
      yamlOut.close();
    } catch (IOException e) {
      System.err.println("Exception " + e);
      System.exit(1);
    }

    totalGrad.log();
    params.log();
    params.write(Execution.getFile("params"));
    if (writeCounts) {
      statistics.write();
    }
    end_track();

    if (startServer) {
      EmbeddedHttpServer server = new EmbeddedHttpServer(exec);
      server.start();
    }
    if (completionServer) {
      CompletionServer server =
        new CompletionServer(dataset, featureDomainObjs, params, statistics);
      server.start();
    }
  }

  public static void main(String[] args) {
    // Register options
    // FixMe: [maintainability] Figure out how to let classes do this
    // themselves
    optionsParser.register("gen", GenDocs.class);
    optionsParser.register("kn", KneserNey.class);
    optionsParser.register("LatentKN", LatentKN.class);
    optionsParser.register("KNEm", KNEm.class);
    optionsParser.register("Recency", Recency.class);
    optionsParser.register("ReadData", ReadData.class);
    optionsParser.register("Test2", Test2.class);
    optionsParser.register("CompletionServer", CompletionServer.class);
    optionsParser.register("CandidateList", CandidateList.class);
    optionsParser.register("FixedRare", FixedRare.class);
    optionsParser.register("PatternKNCounts", PatternKNCounts.class);
    optionsParser.register("CommonTokenKNCounts", CommonTokenKNCounts.class);

    Dataset dataset = new Dataset();
    Params params = new Params();
    Execution.run(args, "main", new Main(dataset, params), optionsParser,
                  "data", dataset, "params", params);
  }
}
