package smartAutocomplete;

import java.util.*;
import java.lang.reflect.*;

import static fig.basic.LogInfo.logs;

import smartAutocomplete.util.*;

public class Statistics {
  private Map<Class, Map<Corpus, Statistic>> statistics =
    new HashMap<Class, Map<Corpus, Statistic>>();

  private Map<Corpus, List<Statistic>> corpusStatistics =
    new HashMap<Corpus, List<Statistic>>();

  private Map<String, Corpus> projectLangCorpus =
    new HashMap<String, Corpus>();

  public void setLanguages(HashSet<String> languages) {
    for (String language : languages) {
      projectLangCorpus.put(language, new Corpus(language, false));
    }
  }

  public Collection<Corpus> getProjectLangCorpi() {
    return projectLangCorpus.values();
  }

  // Get the within-project corpus for the language associated with the file
  // given by $path.  If the language was not specified by setLanguages, then
  // use the default within-project corpus
  public Corpus getProjectLangCorpus(String path) {
    return projectLangCorpus.get(Main.getLanguage(path));
  }

  public List<Statistic> getCorpusStatistics(Corpus corpus) {
    return corpusStatistics.get(corpus);
  }

  public void write() {
    for (Map.Entry<Corpus, List<Statistic>> entry : corpusStatistics.entrySet()) {
      String prefix = entry.getKey().getName();
      for (Statistic statistic : entry.getValue()) {
        statistic.write(prefix);
      }
    }
  }

  private void addCorpusStatistic(Corpus corpus, Statistic statistic) {
    List<Statistic> list;
    if (!corpusStatistics.containsKey(corpus)) {
      list = new ArrayList<Statistic>();
      corpusStatistics.put(corpus, list);
    } else list = corpusStatistics.get(corpus);

    list.add(statistic);
  }

  public <T extends Statistic>
  T getStatistic(Class<T> type, Corpus corpus) {
    return (T)statistics.get(type).get(corpus);
  }

  // Request that a particular statistic be kept about a particular corpus
  public <T extends Statistic>
  T requireStatistic(Class<T> type, Corpus corpus) {
    Map<Corpus, Statistic> corpusStatisticsMap;
    if (!statistics.containsKey(type)) {
      corpusStatisticsMap = new HashMap<Corpus, Statistic>();
      statistics.put(type, corpusStatisticsMap);
    } else corpusStatisticsMap = statistics.get(type);

    if (!corpusStatisticsMap.containsKey(corpus)) {
      try {
        Constructor<T> constructor =
          type.getDeclaredConstructor(Corpus.class, Statistics.class);
        T statistic = constructor.newInstance(corpus, this);
        corpusStatisticsMap.put(corpus, statistic);
        addCorpusStatistic(corpus, statistic);
        return statistic;
      } catch(InvocationTargetException|IllegalAccessException|NoSuchMethodException|InstantiationException ex) {
        throw new RuntimeException(ex);
      }
    } else return (T)corpusStatisticsMap.get(corpus);
  }
}

