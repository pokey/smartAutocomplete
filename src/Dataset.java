package smartAutocomplete;

import java.util.*;

import fig.basic.*;
import fig.exec.*;
import fig.prob.*;
import static fig.basic.LogInfo.begin_track;
import static fig.basic.LogInfo.logs;
import static fig.basic.LogInfo.end_track;

public class Dataset {
  @Option(gloss="Permute documents") public static boolean permute = true;
  @Option(gloss="Random for permuting documents") public static Random dataRandom = new Random(1);

  // Specifies which examples to perform the four operations on.
  @Option(gloss="Range of examples for counting") public Pair<Double, Double> countRange = new Pair<Double, Double>(0.0, 0.0);
  @Option(gloss="Range of examples for tuning") public Pair<Double, Double> tuneRange = new Pair<Double, Double>(0.0, 0.0);
  @Option(gloss="Range of examples for evaluating") public Pair<Double, Double> evalRange = new Pair<Double, Double>(0.0, 0.0);
  @Option(gloss="Range of examples for web") public Pair<Double, Double> webRange = new Pair<Double, Double>(0.0, 0.0);
  @Option(gloss="Do tuning every x tokens") public int tuningPeriod = 1;
  @Option(gloss="Don't tune more than x tokens") public int tuningMax = Integer.MAX_VALUE;
  @Option(gloss="Tune on approximately x tokens, distributed evenly accross docs in range")
    public int tuningTotal = 0;
  @Option(gloss="Count on approximately x tokens, distributed evenly accross docs in range")
    public int countTotal = 0;
  @Option(gloss="Count on first x fraction of docs, and tune on rest")
    public double tuneCountSplit = -1.0;
  @Option(gloss="Count on first x fraction of docs, and tune on rest")
    public boolean countOnTuneDocs = true;
  @Option(gloss="Only consider languages with at least x docs")
    public int minDocsPerLang = 5;

  public Set<String> supportedLanguages = new HashSet<String>();

  // This is our dataset
  public List<Document> documents = new ArrayList<Document>();
  private Map<String, Integer> tuneTokensPerDoc =
    new HashMap<String, Integer>();
  public int getTuningPeriod(Document doc) {
    int tuneTokens = tuneTokensPerDoc.get(Main.getLanguage(doc.path));
    return Math.max(1, doc.tokens.size() / tuneTokens);
  }

  private double canonicalizeRangeValue(double x) {
    if (Math.abs(x) <= 1) x *= documents.size();
    if (x < 0) x += documents.size();
    return x;
  }

  private boolean inRange(Pair<Double, Double> range, int i) {
    double first = canonicalizeRangeValue(range.getFirst());
    return i >= canonicalizeRangeValue(range.getFirst()) &&
           i < canonicalizeRangeValue(range.getSecond());
  }

  public void readData() {
    begin_track("Read documents");

    ReadData.readInPaths(documents);

    // Permute the documents randomly
    if (permute) {
      documents = ListUtils.applyPermutation(documents, SampleUtils.samplePermutation(dataRandom, documents.size()));
    }

    GenDocs.addArtificialDocuments(documents);

    // Set flags in documents
    int D = documents.size();
    int count = 0;
    int eval = 0;
    int tune = 0;
    Map<String, Integer> docsPerLang = new HashMap<String, Integer>();
    for (int i = 0; i < D; i++) {
      Document doc = documents.get(i);
      String lang = Main.getLanguage(doc.path);
      if (lang.equals("")) continue;
      if (inRange(countRange, i)) doc.count = true;
      if (inRange(tuneRange, i)) {
        doc.tune = true;
        tune++;
      }
      if (inRange(evalRange, i)) doc.eval = true;
      if (inRange(webRange, i)) doc.web = true;
      if (doc.count) count++;
      if (doc.eval) eval++;
      MapUtils.incr(docsPerLang, lang);
    }

    ReadData.readFixedPaths(documents);

    if (tuneCountSplit >= 0.0) {
      Map<String, Integer> langTotal = new HashMap<String, Integer>();
      for (Map.Entry<String, Integer> e : docsPerLang.entrySet()) {
        String lang = e.getKey();
        int docs = e.getValue();
        if (docs < minDocsPerLang) {
          langTotal.put(lang, 0);
        } else {
          langTotal.put(lang, (int)((1-tuneCountSplit) * docs) + 1);
          supportedLanguages.add(lang);
        }
      }
      Map<String, Integer> langNum = new HashMap<String, Integer>();
      for (int i = D-1; i >= 0; i--) {
        Document doc = documents.get(i);
        String lang = Main.getLanguage(doc.path);
        if (!supportedLanguages.contains(lang) ||
            lang.equals("")) {
          doc.count = false;
          continue;
        }
        int langNumVal = MapUtils.get(langNum, lang, 0);
        if (langNumVal < langTotal.get(lang)) {
          doc.count = countOnTuneDocs;
          doc.tune = true;
        } else {
          if (langNumVal == langTotal.get(lang)) {
            doc.lastCount = true;
          }
          doc.count = true;
          doc.tune = false;
        }
        MapUtils.incr(langNum, lang);
      }
      if (tuningTotal != 0) {
        for (String lang : langTotal.keySet()) {
          int total = langTotal.get(lang);
          if (total != 0) {
            tuneTokensPerDoc.put(lang, tuningTotal / total);
          }
        }
      }
    } else {
      if (tuningTotal != 0) {
        tuneTokensPerDoc.put("", tuningTotal / tune);
      }
    }

    logs("count: %s", count);
    logs("eval: %s", eval);

    // Count number of tokens
    int numTokens = 0;
    for (Document doc : documents)
      numTokens += doc.tokens.size();
    Execution.putOutput("numDocuments", documents.size());
    Execution.putOutput("numTokens", numTokens);
    logs("%d documents, %d tokens (%s)", documents.size(), numTokens, MemUsage.getBytesStr(documents));
    logs("Memory usage: %s", SysInfoUtils.getUsedMemoryStr());
    end_track();
  }
};
