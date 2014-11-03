package smartAutocomplete;

import java.util.*;

import fig.basic.*;

public class PatternKNCounts extends Statistic {
  @Option(gloss = "Number of tokens which should be considered common")
    public static int numTokens = 50;

  // N-grams with rare tokens replaced by pattern token
  private DataSummary summary = new DataSummary();

  private Rarity rarity;

  private static final String rareToken = "<rare>";

  public PatternKNCounts(Corpus corpus, Statistics statistics) {
    rarity = new OnlineRare(corpus, statistics);
  }

  public DataSummary getSummary() { return summary; }

  public String getRare(String token) {
    return isRare(token) ? rareToken : token;
  }

  public boolean isRare(String token) {
    return rarity.isRare(token);
  }

  private String idxToRareToken(int idx) {
    return "<" + idx + ">";
  }

  private class Decorator implements TokenDecorator {
    public ArrayList<String> rareTokens = null;

    private int findToken(String token) {
      for (int i=0; i<rareTokens.size(); i++) {
        if (token.equals(rareTokens.get(i))) return i;
      }
      return -1;
    }

    @Override
    public void decorate(ArrayList<String> tokens, Mode mode) {
      if (mode != TokenDecorator.Mode.updateCandidate) {
        rareTokens = new ArrayList<String>();
        for (int i=tokens.size()-2; i>=0; i--) {
          String token = tokens.get(i);
          if (isRare(token)) {
            int idx = findToken(token);
            if (idx == -1) {
              idx = rareTokens.size();
              rareTokens.add(token);
            }
            tokens.set(i, idxToRareToken(idx));
          }
        }
      }

      int lastIdx = tokens.size()-1;
      String token = tokens.get(lastIdx);
      if (isRare(token)) {
        int idx = findToken(token);
        if (idx == -1) {
          tokens.set(lastIdx, rareToken);
        } else {
          tokens.set(lastIdx, idxToRareToken(idx));
        }
      }
    }
  }

  private Decorator decorator = new Decorator();
  public Decorator getDecorator() { return decorator; }

  @Override
  public void count(PredictionContext context, String val) {
    summary.count(CountingNgram.get(context, val, decorator));
  }

  @Override
  public void uncount(PredictionContext context, String val) {
    summary.uncount(CountingNgram.get(context, val, decorator));
  }
}
