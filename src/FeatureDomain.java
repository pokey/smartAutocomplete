package smartAutocomplete;

import java.util.*;
import java.lang.reflect.*;

import fig.basic.*;
import static fig.basic.LogInfo.logs;

public abstract class FeatureDomain {
  protected String domainName;
  protected Corpus corpus;
  protected Statistics statistics;

  // Constructs list of FeatureDomain objects based on names in featureNames.  
  // Each FeatureDomain is allowed to add starting parameters to params
  static public void
  addFeatureDomains(List<FeatureDomain> domains, HashSet<String> featureNames,
                    Params params, Statistics statistics, Corpus corpus) {
    int index = 0;
    for (String featureName : featureNames) {
      try {
        Class klass = Class.forName("smartAutocomplete." + featureName);
        FeatureDomain domain =
          (FeatureDomain)klass.getDeclaredConstructor(Corpus.class, Statistics.class).newInstance(corpus, statistics);
        domain.setCorpus(corpus);
        domain.statistics = statistics;
        domain.initParams(params);
        domains.add(domain);
      } catch (ClassNotFoundException ex) {
        throw new RuntimeException("Invalid feature domain " + featureName);
      } catch (IllegalAccessException|InstantiationException|NoSuchMethodException|InvocationTargetException ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) cause = cause.getCause();
        throw new RuntimeException("Problem instantiating feature " + featureName, cause);
      } 
    }
  }

  private void setCorpus(Corpus corpus) {
    this.corpus = corpus;
    setDomainName(corpus.getName());
  }

  // Override this to have a custom domainName
  private void setDomainName(String prefix) {
    domainName = prefix + "." + getClass().getSimpleName();
  }

  String getDomainName() { return domainName; }

  private String cachedPath = null;
  private boolean cachedIsActive;

  public boolean isActive(String path) {
    if (cachedPath != path) {
      cachedPath = path;
      cachedIsActive = computeIsActive(path);
    }

    return cachedIsActive;
  }

  // Override this to change circumstances where feature is active.  By
  // default active iff language of feature's corpus matches language of
  // context
  protected boolean computeIsActive(String path) {
    String contextLang = Main.getLanguage(path);
    String corpusLang = corpus.getLanguage();

    return contextLang.equals(corpusLang);
  }

  // Override to initialize parameters with specific values
  protected void initParams(Params params) { }

  // Returns feature name to use for setting parameters
  protected String toFeature(String name) {
    return FeatureVector.toFeature(domainName, name);
  }

  // Add a feature to a feature vector
  protected void putFeatureInfo(Map<String, Object> map,
                                String name, Object value) {
    map.put(FeatureVector.toFeature(domainName, name), value);
  }

  // Add a feature to a feature vector
  protected void addFeature(FeatureVector features, String name, double value) {
    features.add(domainName, name, value);
  }

  // Add features to candidate using summary and context to compute feature
  // values.  Summary summarizes the training data, and context contains the
  // contents of the document up to the current location.  For performance,
  // candidate_ngram contains the previous n-1 words prepended to the
  // candidate.
  abstract void addFeatures(PredictionContext context, Candidate candidate);

  // Structured info about how this featureDomain treats the given candidate
  void putInfo(Map<String, Object> map, PredictionContext context,
               Candidate candidate) { }
}
