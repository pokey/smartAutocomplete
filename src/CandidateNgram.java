package smartAutocomplete;

import java.util.*;

import fig.basic.*;

/**
 * For performance.  Each time a feature needs to compute feature values for a
 * candidate, we set the last string in this buffer to the candidate token.
 */
public class CandidateNgram {
  private static class CachedBuffer {
    private PredictionContext cachedContext = null;
    private ArrayList<String> candidateBuffer;
    private SubList<String> candidate_ngram;
    private int lastIdx;
    private TokenDecorator decorator;

    public CachedBuffer(TokenDecorator decorator) {
      this.decorator = decorator;
    }

    public SubList<String> get(PredictionContext context,
                               Candidate candidate) {
      if (context != cachedContext) {
        cachedContext = context;
        NgramContext ngramContext = NgramContext.get(context);
        candidateBuffer = new ArrayList<String>();
        SubList<String> tokens = ngramContext.getTokens();
        for (int i = 0; i < tokens.size(); i++) {
          candidateBuffer.add(tokens.get(i));
        }
        candidateBuffer.add(candidate.token);
        decorator.decorate(candidateBuffer,
                           TokenDecorator.Mode.initCandidate);
        candidate_ngram = new SubList<String>(candidateBuffer, 0,
                                              ngramContext.getMax_n());
        lastIdx = ngramContext.getMax_n() - 1;
      } else {
        candidateBuffer.set(lastIdx, candidate.token);
        decorator.decorate(candidateBuffer,
                           TokenDecorator.Mode.updateCandidate);
      }

      return candidate_ngram;
    }
  }

  private static Map<TokenDecorator, CachedBuffer> cachedBuffers =
    new HashMap<TokenDecorator, CachedBuffer>();

  /**
   * Get the n-gram that would result from putting the given candidate in the
   * given context.
   */
  static public SubList<String> get(PredictionContext context,
                                    Candidate candidate) {
    return get(context, candidate, IdentityTokenDecorator.getSingleton());
  }

  static public SubList<String> get(PredictionContext context,
                                    Candidate candidate,
                                    TokenDecorator decorator) {
    CachedBuffer cachedBuffer;
    if (!cachedBuffers.containsKey(decorator)) {
      cachedBuffer = new CachedBuffer(decorator);
      cachedBuffers.put(decorator, cachedBuffer);
    } else {
      cachedBuffer = cachedBuffers.get(decorator);
    }

    return cachedBuffer.get(context, candidate);
  }
}

