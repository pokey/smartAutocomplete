package smartAutocomplete.httpServer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import com.sun.net.httpserver.*;

import fig.basic.*;
import fig.exec.*;
import static fig.basic.LogInfo.begin_track;
import static fig.basic.LogInfo.logs;
import static fig.basic.LogInfo.end_track;

import org.yaml.snakeyaml.Yaml;

import smartAutocomplete.*;
import smartAutocomplete.util.*;

public class CompletionServer {
  @Option(gloss="URL to forward requests to")
    public static String forwardUri = null;

  @Option(gloss="Port on which to listen for completion requests")
    public static int port = -1;

  private HttpServer server;

  private Statistics statistics;
  private FeatureDomain[] featureDomains;
  private Params params;

  private KillHandler killHandler = new KillHandler();

  private ForwardHandler forwardHandler;
  private Map<String, Document> docs = new HashMap<String, Document>();
  private List<File> trainingDirs = new ArrayList<File>();

  public CompletionServer(Dataset dataset, FeatureDomain[] featureDomains,
                          Params params, Statistics statistics) {
    try {
      for (Document doc : dataset.documents) {
        docs.put(new File(doc.path).getCanonicalPath(), doc);
      }
      for (String path : ReadData.inPaths) {
        trainingDirs.add(new File(path).getCanonicalFile());
      }
      this.featureDomains = featureDomains;
      this.statistics = statistics;
      this.params = params;

      server = HttpServer.create(new InetSocketAddress(port), 0);

      forwardHandler = new ForwardHandler(forwardUri);

      server.createContext("/complete", new CompletionHandler());
      server.createContext("/eligible", new EligibleHandler());
      server.createContext("/train", new TrainingHandler());
      server.createContext("/accepted", new AcceptedHandler());

      server.createContext("/kill", killHandler);

      server.setExecutor(null); // creates a default executor
    } catch (IOException e) {
      logs("Couldn't start server because " + e.toString());
      System.exit(-1);
    }
  }

  public void start() {
    begin_track("Completion server");
    logs("Server started...");
    server.start();
    try {
      killHandler.waitForKill();
    } catch (InterruptedException e) {
      logs("Couldn't start server because " + e.toString());
      System.exit(-1);
    }
    logs("Server killed.");
    end_track();
  }

  private boolean inTrainingDir(String path) throws IOException {
    File requested = new File(path).getCanonicalFile();
    boolean contained = false;
    for (File trainingDir : trainingDirs) {
      if (FileUtil.contains(trainingDir, requested)) {
        contained = true;
        break;
      }
    }
    return contained;
  }

  private void handleDoc(String path, Document newDoc) {
    handleDoc(path, newDoc, false);
  }

  private void handleDoc(String path, Document newDoc, boolean docCount) {
    Document oldDoc = docs.get(path);
    List<Statistic> corpusStatistics =
      statistics.getCorpusStatistics(statistics.getProjectLangCorpus(path));
    if (oldDoc != null) {
      DocUtils.uncountDoc(oldDoc, corpusStatistics);
    }
    docs.put(path, newDoc);
    DocUtils.countDoc(newDoc, corpusStatistics);
  }

  static private final Pattern trainingRegex =
    Pattern.compile("path=([^&]+)&event=([^&]+)");

  class TrainingHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {
      try {
        URI uri = t.getRequestURI();
        String content = IOUtil.convertStreamToString(t.getRequestBody());
        Matcher match = trainingRegex.matcher(uri.getQuery());
        match.find();
        String path = URLDecoder.decode(match.group(1));
        FileEvent event = FileEvent.fromId(Integer.parseInt(match.group(2)));

        if (inTrainingDir(path) && Main.supportsLanguage(path)) {
          handleDoc(path,
                    new Document(path, Tokenizer.tokenize(content, path)));
          logs("Training: path=%s, event=%s", path, event);
        }

        forwardHandler.forward(uri, content);

        Http.send(t, "text/plain", "Thanks");
      } catch (Exception e) {
        System.err.println("Caught exception in completion: " + e);
        e.printStackTrace();
        Http.sendError(t);
      }
    }
  }

  public static Comparator<Token> tokenComparator =
    new Comparator<Token>() {
      public int compare(Token token1, Token token2) {
        return token1.loc() - token2.loc();
      }
    };

  static private final Pattern eligibleRegex =
    Pattern.compile("path=([^&]+)&loc=(\\d+)&up=(\\d+)");

  class EligibleHandler implements HttpHandler {
    private String recordSep = Character.toString((char) 30);

    private WhitespaceModel whitespaceModel = new WhitespaceModel();

    public void handle(HttpExchange t) throws IOException {
      try {
        URI uri = t.getRequestURI();
        String content = IOUtil.convertStreamToString(t.getRequestBody());
        Matcher match = eligibleRegex.matcher(uri.getQuery());
        match.find();
        String path = match.group(1);
        int loc = Integer.parseInt(match.group(2));
        boolean up = match.group(3).equals("1");

        List<Token> tokens = Tokenizer.tokenize(content, path);
        int pos = Collections.binarySearch(tokens, new Token("", loc),
                                           tokenComparator);
        if (pos < 0) pos = -(pos+1);

        logs("Eligibility check: path=%s, loc=%d, up=%b, pos=%s",
             path, loc, up, pos);

        boolean eligible = inTrainingDir(path) && Main.supportsLanguage(path);

        Http.send(t, "text/plain", eligible ? "1" : "0");
      } catch (Exception e) {
        System.err.println("Caught exception in eligibility check: " + e);
        e.printStackTrace();
        Http.sendError(t);
      }
    }
  }

  static private final Pattern completionRegex =               
    Pattern.compile("path=([^&]+)&base=([^&]*)&loc=(\\d+)&up=(\\d+)");

  class CompletionHandler implements HttpHandler {
    private String recordSep = Character.toString((char) 30);
    BufferedWriter yamlOut;
    Yaml yaml;

    public CompletionHandler() {
      yamlOut =
        new BufferedWriter(IOUtils.openOutEasy(Execution.getFile("completions")));
      yaml = new Yaml();
    }

    private WhitespaceModel whitespaceModel = new WhitespaceModel();

    public void handle(HttpExchange t) throws IOException {
      try {
        URI uri = t.getRequestURI();
        String content =
          IOUtil.convertStreamToString(t.getRequestBody());
        Matcher match = completionRegex.matcher(uri.getQuery());
        match.find();
        String path = match.group(1);
        String base = match.group(2);
        int loc = Integer.parseInt(match.group(3));
        boolean up = match.group(4).equals("1");

        List<Token> tokens = Tokenizer.tokenize(content, path);
        int pos =
          Collections.binarySearch(tokens, new Token("", loc),
                                   tokenComparator);
        if (pos < 0) pos = -(pos+1);

        logs("Completion: path=%s, base=%s, loc=%d, up=%b, pos=%s",
             path, base, loc, up, pos);

        SplitDocument doc = new SplitDocument(path, tokens, pos);
        handleDoc(path, doc, true);

        MultiTokenCandidateList candidates =
          new MultiTokenCandidateList(featureDomains, params,
                                      statistics, doc, base);

        forwardHandler.forward(uri, content);

        StringBuilder ret = new StringBuilder();
        String sep = "";
        for (int i=0; i<Math.min(candidates.size(), 50); i++) {
          ret.append(sep);
          ret.append(whitespaceModel.toString(candidates.get(i)));
          sep = recordSep;
        }

        Http.send(t, "text/plain", ret.toString());
        Map<String, Object> info =
          MultiTokenCandidateListToMap.toMap(candidates, loc,
                                             whitespaceModel);
        yaml.dump(info, yamlOut);
        yamlOut.flush();
      } catch (Exception e) {
        System.err.println("Caught exception in completion: " + e);
        e.printStackTrace();
        Http.sendError(t);
      }
    }
  }

  static private final Pattern acceptedRegex =
    Pattern.compile("selection=([^&]+)&path=([^&]+)&base=([^&]*)&loc=(\\d+)&up=(\\d+)");

  static private final Yaml yaml = new Yaml();
  private final BufferedWriter yamlOut =
    new BufferedWriter(IOUtils.openOutEasy(Execution.getFile("candidates")));

  class AcceptedHandler implements HttpHandler {
    RunningGradient batchGrad = new RunningGradient();
/*
    private void learn(Document doc, int pos) {
      CandidateList candidates =
        new CandidateList(featureDomains, summary, params, doc, pos,
                          new CaseInsensitivePrefixFilter(base));
      InferState state =
        new InferState(doc, pos, summary, featureDomains, params,
                       new Ngram(doc, pos), candidates);
      yaml.dump(InferStateToMap.toMap(state, 5), yamlOut);
      yamlOut.flush();

      if (state.isOracle()) {
        HashMap<String, Double> grad =
          Gradient.computeGradient(state.getTrueCandidate(),
                                   state.getCandidates().list());
        batchGrad.add(grad);
        params.update(grad);

        params.write(Execution.getFile("currParams"));
        batchGrad.write(Execution.getFile("gradient"));
      }
    }
    */

    public void handle(HttpExchange t) throws IOException {
      try {
        URI uri = t.getRequestURI();
        String content = IOUtil.convertStreamToString(t.getRequestBody());
        Matcher match = acceptedRegex.matcher(uri.getQuery());
        match.find();
        String selection = URLDecoder.decode(match.group(1));
        String path = URLDecoder.decode(match.group(2));
        String base = URLDecoder.decode(match.group(3));
        int loc = Integer.parseInt(match.group(4));
        boolean up = match.group(5).equals("1");

        List<Token> tokens = Tokenizer.tokenize(content, path);
        int pos = Collections.binarySearch(tokens, new Token("", loc),
                                           tokenComparator);

        logs("Accepted: selection=%s, path=%s, base=%s, loc=%d, up=%b, pos=%s",
             selection, path, base, loc, up, pos);

        Document doc = new Document(path, tokens);
        handleDoc(path, doc);

        // learn(doc, pos);

        forwardHandler.forward(uri, content);

        Http.send(t, "text/plain", "Thanks");
      } catch (Exception e) {
        System.err.println("Caught exception in completion: " + e);
        e.printStackTrace();
        Http.sendError(t);
      }
    }
  }
}
