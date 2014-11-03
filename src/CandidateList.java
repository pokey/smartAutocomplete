package smartAutocomplete;

import java.util.*;

import fig.basic.*;

import smartAutocomplete.util.*;

public class CandidateList implements Iterable<Candidate> {
  @Option(gloss = "Always use file name as candidate")
    public static boolean alwaysUseFileName = true;

  private List<Candidate> candidates = new ArrayList<Candidate>();

  private FeatureDomain[] featureDomains;
  private Params params;
  private Statistics statistics;
  private Filter<String> filter;
  private PredictionContext context;
  private int maxCandidates;

  public int size() { return candidates.size(); }
  public List<Candidate> list() { return candidates; }
  public Candidate get(int idx) { return candidates.get(idx); }

  public Iterator<Candidate> iterator() {
    return candidates.iterator();
  }

  private void addWords(DataSummary summary, SubList<String> subContext,
                        Set<String> hitTokens) {
    Set<String> words = summary.candidateTokens.get(subContext);
    if (words == null) return;
    for (String token : words) {
      if (candidates.size() >= maxCandidates) break;
      if (hitTokens.contains(token) || !filter.isTrue(token)) continue;
      hitTokens.add(token);
      candidates.add(createCandidate(token));
    }
  }

  // Create a candidate which corresponds to predicting the given token.
  // Populate the candidate with features and compute its score.
  private Candidate createCandidate(String token) {
    Candidate candidate = new Candidate(token, Main.clusters);

    // Setup candidateBuffer so that features can access candidate_ngram
    candidate.computeScores(featureDomains, params, context, Main.clusters);

    return candidate;
  }

  private static final Filter<String> trueFilter = new TrueFilter<String>();

  public CandidateList(FeatureDomain[] featureDomains, Params params,
                       Statistics statistics, PredictionContext context) {
    this(featureDomains, params, statistics, context, Main.maxCandidates);
  }

  public CandidateList(FeatureDomain[] featureDomains, Params params,
                       Statistics statistics, PredictionContext context,
                       int maxCandidates) {
    this.featureDomains = featureDomains;
    this.params = params;
    this.statistics = statistics;
    this.context = context;
    this.maxCandidates = maxCandidates;

    String base = context.getBase();
    this.filter = base.length() == 0 ? trueFilter
                                     : new CaseInsensitivePrefixFilter(base);;

    NgramContext ngramContext = NgramContext.get(context);
    int max_n = ngramContext.getMax_n();
    Corpus corpus = statistics.getProjectLangCorpus(context.getPath());
    DataSummary inDomainSummary =
      statistics.getStatistic(NgramKNCounts.class, corpus).getSummary();
    FileTokenKNCounts fileTokenStatistic =
      statistics.getStatistic(FileTokenKNCounts.class, corpus);
    // DataSummary globalSummary = LangSpecificKN.getGlobalSummary(context);

    // Generate a set of candidates.
    // Predict based on only the most frequent words.
    if (Main.isFixedVocab()) {
      for (String token : Main.getVocab()) {
        if (!filter.isTrue(token)) continue;
        candidates.add(createCandidate(token));
      }
    } else {
      Set<String> hitTokens = new HashSet<String>();  // Keep track of candidate words so that we don't generate duplicates

      // Add file name if we've seen file name in this context
      if (alwaysUseFileName || (fileTokenStatistic.useFileName(context))) {
        String token = fileTokenStatistic.getFileName(context);
        hitTokens.add(token);
        candidates.add(createCandidate(token));
      }

      // Add words that have been seen after progressively shorter contexts
      for (int n = max_n; n > 1; n--) {  // Length of context
        SubList<String> subContext = ngramContext.subContext(n);
        addWords(inDomainSummary, subContext, hitTokens);
        // addWords(globalSummary, subContext, hitTokens);
      }

      // Add words that have been seen previously in this file
      SubList<Token> prefix = context.getPrefix();
      for (int i=prefix.size()-1; i>=0; i--) {
        if (candidates.size() >= maxCandidates) break;
        String token = prefix.get(i).str();
        if (hitTokens.contains(token) || !filter.isTrue(token)) continue;
        hitTokens.add(token);
        candidates.add(createCandidate(token));
      }

      // Add the rest of the words
      addWords(inDomainSummary, ngramContext.subContext(1),
               hitTokens);
    }

    LogLinear.computeProbabilities(candidates, Main.clusters);

    // Sort candidates by decreasing probability
    Collections.sort(candidates, new Comparator<Candidate>() {
      public int compare(Candidate c1, Candidate c2) {
        if (c1.prob > c2.prob) return -1;
        if (c1.prob < c2.prob) return +1;
        return 0;
      }
    });
  }
}
