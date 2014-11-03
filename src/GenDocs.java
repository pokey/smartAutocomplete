package smartAutocomplete;

import java.util.*;

import fig.prob.*;
import fig.basic.*;

public class GenDocs {
  // Generate artificial data
  @Option(gloss="Number of artificial examples to generate") public static int numDocuments = 0;
  @Option public static int docLength = 10;
  @Option public static int numTokens = 100;
  @Option public static boolean bigram = false;
  @Option(gloss="Exponent for power law") public static double alpha = 2.0;

  private static void genPowerLaw(double[] probs, double alpha) {
    int vals = probs.length;
    double sum = 0;
    for (int i=1; i<vals; i++) {
      double val = (alpha - 1) * Math.pow(i+1, -alpha);
      probs[i] = val;
      sum += val;
    }
    probs[0] = 1-sum;
  }

  public static void addArtificialDocuments(List<Document> documents) {
    double[] probs = new double[numTokens];
    genPowerLaw(probs, alpha);
    Random random = new Random(1);
    for (int d = 0; d < numDocuments; d++) {
      List<Token> tokens = new ArrayList<Token>();
      int lastToken = -1;
      int loc = 0;
      for (int i = 0; i < docLength; i++) {
        int token = Multinomial.sample(random, probs);
        if (lastToken != -1 && i % 2 == 0) {
          token = (lastToken - token + numTokens) % numTokens;
        }
        // if (lastToken == 0) {
        //   token = 9;
        // }
        // int token = lastToken == -1 ? random.nextInt(numTokens) : (lastToken + random.nextInt(numTokens)) % numTokens;
        String tokenStr = "w" + token;
        tokens.add(new Token(tokenStr, loc));
        loc += tokenStr.length();
        if (bigram)
          lastToken = token;
      }
      Document doc = new Document("artificial", tokens);
      documents.add(doc);
    }
  }
};
