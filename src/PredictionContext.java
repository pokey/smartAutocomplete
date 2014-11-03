package smartAutocomplete;

import fig.basic.*;

/**
 * A context in which we wish to do prediction.
 *
 */
public interface PredictionContext {
  // Tokens from the beginning of the doc up to the point of prediction
  SubList<Token> getPrefix();

  // Tokens from the point of prediction up to the end of the doc
  SubList<Token> getSuffix();

  // The path of the document in which we are predicting
  String getPath();

  // The string typed so far by the user
  String getBase();
}
