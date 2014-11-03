package smartAutocomplete;

import java.io.*;

import fig.basic.*;
import fig.exec.*;

class RecencyExperiment {
  static int outCounter = 0;
  private static final PrintWriter patternOut =
    IOUtils.openOutEasy(Execution.getFile("patterns"));

  private Evaluation recencyExperiment(Document doc, int pos, InferState state) {
    Evaluation evaluation = new Evaluation();
    boolean found = false;
    for (int i=2; i<500; i++) {
      boolean eq = pos>i && doc.tokens.get(pos-i).str().equals(doc.tokens.get(pos).str());
      evaluation.add("prevFirst" + i, !found && eq);
      if (!found && eq) {
        found = eq;
        if (pos>i+3) {
          outCounter++;
          if (outCounter % 1000 == 0) {
            patternOut.println(
                i + ": " +
                doc.tokens.get(pos-i-3) + " " +
                doc.tokens.get(pos-i-2) + " " +
                doc.tokens.get(pos-i-1) + " " +
                doc.tokens.get(pos-i) + " " +
                doc.tokens.get(pos-i+1) + " ... " +
                doc.tokens.get(pos-2) + " " +
                doc.tokens.get(pos-1) + " " +
                doc.tokens.get(pos));
            patternOut.flush();
          }
        }
      }
      evaluation.add("prevEq" + i, eq);
      evaluation.add("prevContains" + i, found);
    }

    return evaluation;
  }
}
