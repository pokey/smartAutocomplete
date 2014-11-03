package smartAutocomplete;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fig.basic.Fmt;
import fig.basic.LogInfo;
import fig.basic.MapUtils;
import fig.basic.ValueComparator;
import static fig.basic.LogInfo.logs;

/**
 * A FeatureVector represents a mapping from feature (string) to value (double).
 *
 * We enforce the convention that each feature is (domain, name), so that the key space isn't a free-for-all.
 *
 * @author Percy Liang
 */
public class FeatureVector {
  // These features map to the value 1 (most common case in NLP).
  private List<String> indicatorFeatures;
  // General features
  private StringDoubleArrayList generalFeatures;
  
  // ============================================================
  // Add features
  // ============================================================

  public static String toFeature(String domain, String name) {
    //return (domain + " :: " + name).intern();
    return domain + " :: " + name;
  }

  public void add(String domain, String name) {
    add(toFeature(domain, name));
  }
  private void add(String feature) {
    if (indicatorFeatures == null) indicatorFeatures = new ArrayList<String>();
    indicatorFeatures.add(feature);
  }

  public void add(String domain, String name, double value) {
    add(toFeature(domain, name), value);
  }
  private void add(String feature, double value) {
    if (generalFeatures == null) generalFeatures = new StringDoubleArrayList();
    generalFeatures.add(feature, value);
  }

  public void addWithBias(String domain, String name, double value) {
    add(domain, name, value);
    add(domain, name + "-bias", 1);
  }

  public void addFromString(String feature, double value) {
    assert feature.contains(" :: ") : feature;
    if (value == 1) add(feature);
    else add(feature, value);
  }

  public void add(FeatureVector that) { add(that, AllFeatureMatcher.matcher); }
  public void add(FeatureVector that, FeatureMatcher matcher) {
    if (that.indicatorFeatures != null) {
      for (String f : that.indicatorFeatures)
        if (matcher.matches(f))
          add(f);
    }
    if (that.generalFeatures != null) {
      for (StringDoublePair pair : that.generalFeatures)
        if (matcher.matches(pair.getFirst()))
          add(pair.getFirst(), pair.getSecond());
    }
  }
  
  // ============================================================
  // Dot product
  // ============================================================

  // Return the dot product between this feature vector and the weight vector (parameters).
  public double dotProduct(Params params) {
    double sum = 0;
    if (indicatorFeatures != null) {
      for (String f : indicatorFeatures)
        sum += params.getWeight(f);
    }
    if (generalFeatures != null) {
      for (StringDoublePair pair : generalFeatures)
        sum += params.getWeight(pair.getFirst()) * pair.getSecond();
    }
    return sum;
  }
  public double dotProduct(Params params, FeatureDecorator decorator) {
    double sum = 0;
    if (indicatorFeatures != null) {
      for (String f : indicatorFeatures)
        sum += params.getWeight(decorator.decorate(f));
    }
    if (generalFeatures != null) {
      for (StringDoublePair pair : generalFeatures) {
        String f = pair.getFirst();
        String decorated = decorator.decorate(f);
        double weight = params.getWeight(decorated);
        double val = pair.getSecond();
        if (weight ==0 || val == 0) continue;
        sum += weight * val;
      }
    }
    return sum;
  }
  public double dotProduct(Params params, FeatureMatcher matcher) {
    double sum = 0;
    if (indicatorFeatures != null) {
      for (String f : indicatorFeatures)
        if (matcher.matches(f))
          sum += params.getWeight(f);
    }
    if (generalFeatures != null) {
      for (StringDoublePair pair : generalFeatures)
        if (matcher.matches(pair.getFirst()))
          sum += params.getWeight(pair.getFirst()) * pair.getSecond();
    }
    return sum;
  }
  
  // ============================================================
  // Increment map
  // ============================================================

  // Increment |map| by a factor times this feature vector.
  public void increment(double factor, Map<String, Double> map) {
    if (indicatorFeatures != null) {
      for (String feature : indicatorFeatures)
        MapUtils.incr(map, feature, factor);
    }
    if (generalFeatures != null) {
      for (StringDoublePair pair : generalFeatures)
        MapUtils.incr(map, pair.getFirst(), factor * pair.getSecond());
    }
  }
  public void increment(double factor, Map<String, Double> map, FeatureDecorator decorator) {
    if (indicatorFeatures != null) {
      for (String feature : indicatorFeatures)
        MapUtils.incr(map, decorator.decorate(feature), factor);
    }
    if (generalFeatures != null) {
      for (StringDoublePair pair : generalFeatures)
        MapUtils.incr(map, decorator.decorate(pair.getFirst()), factor * pair.getSecond());
    }
  }
  public void increment(double factor, Map<String, Double> map, FeatureMatcher matcher) {
    if (indicatorFeatures != null) {
      for (String feature : indicatorFeatures)
        if (matcher.matches(feature))
          MapUtils.incr(map, feature, factor);
    }
    if (generalFeatures != null) {
      for (StringDoublePair pair : generalFeatures)
        if (matcher.matches(pair.getFirst()))
          MapUtils.incr(map, pair.getFirst(), factor * pair.getSecond());
    }
  }
  
  // ============================================================
  // Persistence
  // ============================================================

  public Map<String, Double> toMap() {
    HashMap<String, Double> map = new HashMap<String, Double>();
    increment(1, map);
    return map;
  }

  public static FeatureVector fromMap(Map<String, Double> m) {
    // TODO: Encoding is lossy.  We guess that value of 1 means indicator, but we could be wrong.
    FeatureVector fv = new FeatureVector();
    for (Map.Entry<String, Double> entry : m.entrySet()) {
      if (entry.getValue() == 1.0d)
        fv.add(entry.getKey());
      else
        fv.add(entry.getKey(), entry.getValue());
    }
    return fv;
  }
  
  // ============================================================
  // Logging
  // ============================================================

  public static void logChoices(String prefix, Map<String, Integer> choices) {
    LogInfo.begin_track("%s choices", prefix);
    for (Map.Entry<String, Integer> e : choices.entrySet()) {
      int value = e.getValue();
      if (value == 0) continue;
      LogInfo.logs("%s %s", value > 0 ? "+" + value : value, e.getKey());
    }
    LogInfo.end_track();
  }

  public void logFeatureVals(String prefix, double factor, FeatureDecorator decorator) {
    LogInfo.begin_track_printAll("%s features (format is feature value * factor)", prefix);
    if (indicatorFeatures != null) {
      for (String feature : indicatorFeatures) {
         String wrappedFeature = decorator.decorate(feature);
         LogInfo.logs("%-50s %6s = 1 * %s", "[ " + wrappedFeature + " ]",
             Fmt.D(factor), Fmt.D(factor));
      }
    }
    if (generalFeatures != null) {
      for (StringDoublePair pair : generalFeatures) {
         String feature = pair.getFirst();
         String wrappedFeature = decorator.decorate(feature);
         double value = pair.getSecond();
         LogInfo.logs("%-50s %6s = %s * %s", "[ " + wrappedFeature + " ]",
             Fmt.D(factor * value), Fmt.D(value), Fmt.D(factor));
      }
    }
    LogInfo.end_track();
  }

  public static void logFeatureWeights(String prefix, Map<String, Double> features, Params params) {
     logFeatureWeights(prefix, features, params, IdentityFeatureDecorator.decorator);
  }

  public static List<Map.Entry<String, Double>>
  getFeatureWeights(Map<String, Double> features, Params params) {
    return getFeatureWeights(features, params,
                             IdentityFeatureDecorator.decorator);
  }

  public static List<Map.Entry<String, Double>>
  getFeatureWeights(Map<String, Double> features, Params params,
                    FeatureDecorator decorator) {
    List<Map.Entry<String, Double>> entries = new ArrayList<Map.Entry<String, Double>>();
    for (Map.Entry<String, Double> entry : features.entrySet()) {
      String feature = entry.getKey();
      String wrappedFeature = decorator.decorate(feature);
      double param = params.getWeight(wrappedFeature);
      if (entry.getValue() == 0 || param == 0) continue;
      double value = entry.getValue() * params.getWeight(wrappedFeature);
      entries.add(new java.util.AbstractMap.SimpleEntry<String, Double>(feature, value));
    }
    Collections.sort(entries, new ValueComparator<String, Double>(false));
    return entries;
  }

  public static void logFeatureWeights(String prefix, Map<String, Double> features, Params params, FeatureDecorator decorator) {
    List<Map.Entry<String, Double>> entries =
      getFeatureWeights(features, params, decorator);
    double sumValue = 0;
    for (Map.Entry<String, Double> entry : entries) sumValue += entry.getValue();
    LogInfo.begin_track_printAll("%s features [sum = %s] (format is feature value * weight)", prefix, Fmt.D(sumValue));
    for (Map.Entry<String, Double> entry : entries) {
      String feature = entry.getKey();
      String wrappedFeature = decorator.decorate(feature);
      double value = entry.getValue();
      double weight = params.getWeight(wrappedFeature);
      LogInfo.logs("%-50s %6s = %s * %s", "[ " + wrappedFeature + " ]",
          Fmt.D(value), Fmt.D(MapUtils.getDouble(features, feature, 0)), Fmt.D(weight));
    }
    LogInfo.end_track();
  }

  public static Map<String, Double>
  getFeatureDiff(FeatureVector trueFeatures, FeatureVector predFeatures) {
    HashMap<String, Double> featureDiff = new HashMap<String, Double>();
    trueFeatures.increment(+1, featureDiff);
    predFeatures.increment(-1, featureDiff);
    return featureDiff;
  }

  public static void logFeatureDiff(String prefix, FeatureVector trueFeatures, FeatureVector predFeatures, Params params) {
    logFeatureWeights(prefix, getFeatureDiff(trueFeatures, predFeatures),
                      params);
  }
}
