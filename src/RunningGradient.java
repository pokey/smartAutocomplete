package smartAutocomplete;

import com.google.common.collect.Lists;

import java.util.*;
import java.io.*;

import fig.basic.*;
import static fig.basic.LogInfo.begin_track;
import static fig.basic.LogInfo.logs;
import static fig.basic.LogInfo.end_track;

public class RunningGradient {
  private HashMap<String, Double> mean = new HashMap<String, Double>();
  private int count;

  public int getCount() {
    return count;
  }

  public void add(HashMap<String, Double> grad) {
    for (Map.Entry<String, Double> entry : grad.entrySet()) {
      String feature = entry.getKey();
      double value = entry.getValue();
      if(!mean.containsKey(feature)) mean.put(feature, value);
      else {
        double oldValue = mean.get(feature);
        mean.put(feature, oldValue + (value - oldValue)/count);
      }
    }
    count++;
  }

  public void log() {
    begin_track("Gradient");
    List<Map.Entry<String, Double>> entries =
      Lists.newArrayList(mean.entrySet());
    Collections.sort(entries, new ValueComparator<String, Double>(true));
    for (Map.Entry<String, Double> entry : entries) {
      double value = entry.getValue();
      LogInfo.logs("%s\t%s", entry.getKey(), value);
    }
    end_track();
  }

  public void write(PrintWriter out) { write(null, out); }

  public void write(String prefix, PrintWriter out) {
    List<Map.Entry<String, Double>> entries = Lists.newArrayList(mean.entrySet());
    Collections.sort(entries, new ValueComparator<String, Double>(true));
    for (Map.Entry<String, Double> entry : entries) {
      double value = entry.getValue();
      out.println((prefix == null ? "" : prefix + "\t") + entry.getKey() + "\t" + value);
    }
  }

  public void write(String path) {
    PrintWriter out = IOUtils.openOutHard(path);
    write(out);
    out.close();
  }

  public void reset() {
    count = 0;
    mean = new HashMap<String, Double>(); 
  }

  public HashMap<String, Double> getMean() {
    return mean;
  }
}

