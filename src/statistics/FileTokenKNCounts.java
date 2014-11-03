package smartAutocomplete;

import java.util.*;

import fig.basic.*;

import smartAutocomplete.util.*;

public class FileTokenKNCounts extends Statistic {
  // N-grams with file name replaced by special token
  private DataSummary summary = new DataSummary();

  public DataSummary getSummary() { return summary; }

  public FileTokenKNCounts(Corpus corpus, Statistics statistics) {}

  private static final String fileNameToken = "<fileName>";

  private static class Decorator implements TokenDecorator {
    private final String fileName;

    public Decorator(String path) {
      this.fileName = FileUtil.getFilenameNoExtension(path);
    }

    @Override
    public void decorate(ArrayList<String> tokens, Mode mode) {
      int start =
        mode == TokenDecorator.Mode.updateCandidate ? tokens.size()-1 : 0;
      for (int i=start; i<tokens.size(); i++) {
        if (tokens.get(i).equals(fileName)) {
          tokens.set(i, fileNameToken);
        }
      }
    }
  }

  private String cachedPath = null;
  private Decorator cachedDecorator = null;

  public Decorator getDecorator(PredictionContext context) {
    if (context.getPath() != cachedPath) {
      cachedPath = context.getPath();
      cachedDecorator = new Decorator(context.getPath());
    }

    return cachedDecorator;
  }

  public String getFileName(PredictionContext context) {
    return FileUtil.getFilenameNoExtension(context.getPath());
  }

  // Return true if we have some confidence that the file name might show up
  // in this context
  public boolean useFileName(PredictionContext context) {
    SubList<String> candidate_ngram =
      CandidateNgram.get(context, new Candidate(fileNameToken),
                         getDecorator(context));
    double prob = KneserNey.computeProb(candidate_ngram, summary);
    return prob > .5;
  }

  @Override
  public void uncount(PredictionContext context, String val) {
    summary.uncount(CountingNgram.get(context, val, getDecorator(context)));
  }

  @Override
  public void count(PredictionContext context, String val) {
    summary.count(CountingNgram.get(context, val, getDecorator(context)));
  }
}
