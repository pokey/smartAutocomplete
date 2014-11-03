package smartAutocomplete;

import java.util.*;

import fig.basic.*;

public class DocUtils {
  public static void uncountDoc(Document doc, List<Statistic> statistics) {
    for (int i = doc.start(); i < doc.end(); i++) {
      PredictionContext context = new SequentialContext(doc, i);
      String val = doc.tokens.get(i).str();
      for (Statistic statistic : statistics) {
        statistic.uncount(context, val);
      }
    }
  }

  public static void countDoc(Document doc, List<Statistic> statistics) {
    for (int i = doc.start(); i < doc.end(); i++) {
      PredictionContext context = new SequentialContext(doc, i);
      String val = doc.tokens.get(i).str();
      for (Statistic statistic : statistics) {
        statistic.count(context, val);
      }
    }
  }
}
