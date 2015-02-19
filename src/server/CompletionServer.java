package smartAutocomplete.httpServer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.util.concurrent.*;

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

  @Option(gloss="Number of candidates to send to server")
    public static int numCandidates = 50;

  private HttpServer server;

  private Statistics statistics;
  private FeatureDomain[] featureDomains;
  private Params params;

  private KillHandler killHandler = new KillHandler();

  private ForwardHandler forwardHandler;
  private Map<String, Document> docs = new HashMap<String, Document>();
  private List<File> trainingDirs = new ArrayList<File>();

  private final ExecutorService executor =
    Executors.newFixedThreadPool(1);

  private MultiTokenCandidateList lastCompletionCandidates = null;
  private SplitDocument lastCompletionDoc = null;
  private int lastCompletionPos = -1;

  BufferedWriter yamlOut;
  Yaml yaml;

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

      yamlOut =
        new BufferedWriter(IOUtils.openOutEasy(Execution.getFile("completions")));
      yaml = new Yaml();

      server = HttpServer.create(new InetSocketAddress(port), 0);

      forwardHandler = new ForwardHandler(forwardUri);

      server.createContext("/complete", new CompletionHandler());
      server.createContext("/eligible", new EligibleHandler());
      server.createContext("/train", new TrainingHandler());
      server.createContext("/addDir", new AddDirHandler());
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

  // Uncount old version of doc and count new version.  If onlyNew
  // is true, then return if the old doc exists.
  private void handleDoc(String path, Document newDoc, boolean onlyNew) {
    Document oldDoc = docs.get(path);
    List<Statistic> corpusStatistics =
      statistics.getCorpusStatistics(statistics.getProjectLangCorpus(path));
    if (oldDoc != null) {
      if (onlyNew) return;
      DocUtils.uncountDoc(oldDoc, corpusStatistics);
    }
    docs.put(path, newDoc);
    DocUtils.countDoc(newDoc, corpusStatistics);
  }

  static private final Pattern addDirRegex =
    Pattern.compile("dir=([^&]+)");

  private class HandleDocAction implements Runnable {
    private Document doc;

    public HandleDocAction(Document doc) {
      this.doc = doc;
    }

    @Override
    public void run() {
      synchronized(CompletionServer.this) {
        logs("Counting %s", doc.path);
        handleDoc(doc.path, doc, true);
      }
    }
  }

  private class AddDirAction implements Runnable {
    private String dir;

    public AddDirAction(String dir) {
      this.dir = dir;
    }

    @Override
    public void run() {
      synchronized(CompletionServer.this) {
        try {
          trainingDirs.add(new File(dir).getCanonicalFile());
        } catch (IOException e) {
          logs("Couldn't start server because " + e.toString());
          System.exit(-1);
        }
      }
      ReadData.iterPath(dir, new ReadData.DocHandler() {
        @Override
        public boolean handle(Document doc) {
          executor.execute(new HandleDocAction(doc));
          return true;
        }
      });
      logs("Finished adding dir %s", dir);
    }
  }

  class AddDirHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {
      try {
        URI uri = t.getRequestURI();
        Matcher match = addDirRegex.matcher(uri.getQuery());
        match.find();
        String dir = URLDecoder.decode(match.group(1));

        if (inTrainingDir(dir)) {
          logs("Dir %s already in training path", dir);
        } else {
          executor.execute(new AddDirAction(dir));
          logs("Add dir %s", dir);
        }

        forwardHandler.forward(uri, "");

        Http.send(t, "text/plain", "Thanks");
      } catch (Exception e) {
        System.err.println("Caught exception in addDir: " + e);
        System.err.println("Uri: " + t.getRequestURI());
        e.printStackTrace();
        Http.sendError(t);
      }
    }
  }

  static private final Pattern trainingRegex =
    Pattern.compile("path=([^&]+)&event=([^&]+)");

  // Returns true if this training event is an addition at the
  // place where the user last asked for a completion
  boolean isAcceptedDiff(List<Token> oldTokens,
                         List<Token> newTokens,
                         int lastCompletionPos) {
    for (int i=0; i<lastCompletionPos; i++) {
      if (!oldTokens.get(i).str().equals(newTokens.get(i).str())) {
        return false;
      }
    }
    int tokensAdded = newTokens.size() - oldTokens.size();
    if (tokensAdded <= 0) return false;
    for (int i=lastCompletionPos; i<oldTokens.size(); i++) {
      String oldToken = oldTokens.get(i).str();
      String newToken = newTokens.get(i+tokensAdded).str();
      if (!oldToken.equals(newToken)) {
        return false;
      }
    }
    return true;
  }

  private class TrainingAction implements Runnable {
    private String path;
    private String content;

    public TrainingAction(String path, String content) {
      this.path = path;
      this.content = content;
    }

    @Override
    public void run() {
      synchronized(CompletionServer.this) {
        List<Token> tokens = Tokenizer.tokenize(content, path);
        if (lastCompletionDoc != null &&
            path.equals(lastCompletionDoc.path)) {
          if (isAcceptedDiff(lastCompletionDoc.tokens, tokens,
                             lastCompletionPos)) {
            handleAccepted(tokens);
          }
        }
        handleDoc(path, new Document(path, tokens), false);
        lastCompletionDoc = null;
      }
    }
  }

  private void handleAccepted(List<Token> tokens) {
    try {
      int tokensAdded =
        tokens.size() - lastCompletionDoc.tokens.size();

      int len =
        Math.min(lastCompletionCandidates.size(), numCandidates);
      MultiTokenCandidate chosen = null;
      int actualIdx = -1;
      int longestMatch = 0;
      for (int i=0; i<len; i++) {
        MultiTokenCandidate candidate =
          lastCompletionCandidates.get(i);
        boolean match = true;
        int j = lastCompletionPos;
        for (Candidate curr : candidate.candidateList()) {
          if (j == lastCompletionPos + tokensAdded ||
              !tokens.get(j).str().equals(curr.token)) {
            match = false;
            break;
          }
          j++;
        }
        int matchLen = j - lastCompletionPos;
        if (matchLen >= longestMatch) {
          longestMatch = matchLen;
          chosen = candidate;
          actualIdx = i;
        }
        if (longestMatch == tokensAdded && match) break;
      }
      Map<String, Object> map = new HashMap<String, Object>();
      if (longestMatch != 0) {
        map.put("actualIdx", actualIdx);
        if (actualIdx != 0) {
          map.put("actual",
              MultiTokenCandidateListToMap.toMap(chosen,
                whitespaceModel, featureDomains, params,
                statistics, lastCompletionDoc,
                lastCompletionCandidates.getBase(), false));
        }
      } else {
        map.put("notFound", true);
      }
      yaml.dump(map, yamlOut);
      yamlOut.flush();
      // learn(doc, pos);
    } catch (Exception e) {
      System.err.println("Caught exception in accepted async: " + e);
      e.printStackTrace();
    }
  }

  class TrainingHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {
      try {
        URI uri = t.getRequestURI();
        String content = IOUtil.convertStreamToString(t.getRequestBody());
        Matcher match = trainingRegex.matcher(uri.getQuery());
        match.find();
        String path = URLDecoder.decode(match.group(1));
        FileEvent event = FileEvent.fromId(Integer.parseInt(match.group(2)));

        synchronized(CompletionServer.this) {
          if (isEligible(path, content)) {
            executor.execute(new TrainingAction(path, content));
            logs("Training: path=%s, event=%s", path, event);
          }
        }

        forwardHandler.forward(uri, content);

        Http.send(t, "text/plain", "Thanks");
      } catch (Exception e) {
        System.err.println("Caught exception in training: " + e);
        System.err.println("Uri: " + t.getRequestURI());
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

  private boolean isEligible(String path, String content) throws IOException {
    return content.length() < ReadData.maxFileSize &&
           !ReadData.isExcluded(new File(path)) &&
           inTrainingDir(path) && Main.supportsLanguage(path);
  }

  class EligibleHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {
      try {
        URI uri = t.getRequestURI();
        String content = IOUtil.convertStreamToString(t.getRequestBody());
        Matcher match = eligibleRegex.matcher(uri.getQuery());
        match.find();
        String path = match.group(1);
        int loc = Integer.parseInt(match.group(2));
        boolean up = match.group(3).equals("1");

        logs("Eligibility check: path=%s, loc=%d, up=%b", path,
             loc, up);

        boolean eligible;
        synchronized(CompletionServer.this) {
          eligible = isEligible(path, content);
        }

        Http.send(t, "text/plain", eligible ? "1" : "0");
      } catch (Exception e) {
        System.err.println("Caught exception in eligibility check: " + e);
        System.err.println("Uri: " + t.getRequestURI());
        e.printStackTrace();
        Http.sendError(t);
      }
    }
  }

  static private final Pattern completionRegex =               
    Pattern.compile("path=([^&]+)&base=([^&]*)&loc=(\\d+)&up=(\\d+)");

  private WhitespaceModel whitespaceModel = new WhitespaceModel();

  class CompletionHandler implements HttpHandler {
    private String recordSep = Character.toString((char) 30);

    public CompletionHandler() {
    }

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
        handleDoc(path, doc, false);

        MultiTokenCandidateList candidates =
          new MultiTokenCandidateList(featureDomains, params,
                                      statistics, doc, base);
        lastCompletionCandidates = candidates;
        lastCompletionDoc = doc;
        lastCompletionPos = pos;

        forwardHandler.forward(uri, content);

        StringBuilder ret = new StringBuilder();
        String sep = "";
        for (int i=0; i<Math.min(candidates.size(), numCandidates); i++) {
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
        System.err.println("Uri: " + t.getRequestURI());
        e.printStackTrace();
        Http.sendError(t);
      }
    }
  }

  static private final Pattern acceptedRegex =
    Pattern.compile("selection=([^&]*)&path=([^&]+)&base=([^&]*)&loc=(\\d+)&up=(\\d+)");

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
        String content =
           IOUtil.convertStreamToString(t.getRequestBody());
        Matcher match = acceptedRegex.matcher(uri.getQuery());
        match.find();
        String selection = URLDecoder.decode(match.group(1));
        String path = URLDecoder.decode(match.group(2));
        String base = URLDecoder.decode(match.group(3));
        int loc = Integer.parseInt(match.group(4));
        boolean up = match.group(5).equals("1");

        synchronized(CompletionServer.this) {
          if (isEligible(path, content)) {
            executor.execute(new TrainingAction(path, content));
            logs("Accepted: selection=%s, path=%s, base=%s, loc=%d, up=%b",
                selection, path, base, loc, up);
          }
        }

        forwardHandler.forward(uri, content);

        Http.send(t, "text/plain", "Thanks");
      } catch (Exception e) {
        System.err.println("Caught exception in accepted: " + e);
        System.err.println("Uri: " + t.getRequestURI());
        e.printStackTrace();
        Http.sendError(t);
      }
    }
  }
}
