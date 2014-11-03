package smartAutocomplete;

import java.util.*;

import fig.basic.*;
import fig.exec.*;

public class FileKNCounts extends Statistic {
  private DataSummary summary = new DataSummary();
  public DataSummary getSummary() { return summary; }

  public FileKNCounts(Corpus corpus, Statistics statistics) {}

  @Override
  public void count(PredictionContext context, String val) {
    List<String> fileTokenNgram = new ArrayList<String>();
    fileTokenNgram.add(context.getPath());
    fileTokenNgram.add(val);
    summary.count(new SubList<String>(fileTokenNgram, 0,
                                      fileTokenNgram.size()), 2);
  }

  @Override
  public void uncount(PredictionContext context, String val) {
    List<String> fileTokenNgram = new ArrayList<String>();
    fileTokenNgram.add(context.getPath());
    fileTokenNgram.add(val);
    summary.uncount(new SubList<String>(fileTokenNgram, 0,
                                        fileTokenNgram.size()), 2);
  }

  @Override
  public void write(String prefix) {
    summary.writeNgrams(Execution.getFile(prefix +
                                                   ".fileDiversityCounts"), 1,
                                 true);
  }
}
