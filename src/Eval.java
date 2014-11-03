package smartAutocomplete;

import java.io.*;
import java.util.*;

import fig.exec.*;
import fig.basic.*;
import static fig.basic.LogInfo.begin_track;
import static fig.basic.LogInfo.logs;
import static fig.basic.LogInfo.end_track;

class Eval {
  // Predictions text file: context, actual, predicted, rank
  private static final PrintWriter predOut =
    IOUtils.openOutEasy(Execution.getFile("predictions"));

  // Machine-readable entropies: file, location, isIdent, entropy, reciprocal
  // rank
  private static final PrintWriter entOut =
    IOUtils.openOutEasy(Execution.getFile("entropies"));

  static public Evaluation eval(InferState state) {
    // Print out information about how well we're doing.
    Evaluation evaluation = new Evaluation();
    Candidate trueCandidate = state.getTrueCandidate();
    Candidate predCandidate = state.getCandidates().get(0);

    PredictionContext context = state.getContext();
    NgramContext ngramContext = NgramContext.get(context);
    Statistics statistics = state.getStatistics();
    Corpus corpus = statistics.getProjectLangCorpus(context.getPath());
    DataSummary summary =
      statistics.getStatistic(NgramKNCounts.class, corpus).getSummary();
    Params params = state.getParams();
    boolean oracle = state.isOracle();
    int rank = state.getRank();
    double entropy = state.getEntropy();
    double reciprocalRank = state.getReciprocalRank();
    boolean isIdent = state.isIdent();
    boolean correct = state.isCorrect();

    String path = context.getPath();

    String trueTokenStr = trueCandidate.token;
    String predToken = predCandidate.token;
    evaluation.add("accuracy", correct);
    evaluation.add("oracle", oracle);
    evaluation.add("rank", rank);
    evaluation.add("reciprocalRank", reciprocalRank);
    if (oracle) {
      evaluation.add("entropy", entropy);
    }
    if (isIdent) {
      evaluation.add("identAccuracy", correct);
      evaluation.add("identOracle", oracle);
      if (oracle) {
        evaluation.add("identEntropy", entropy);
        evaluation.add("identReciprocalRank", reciprocalRank);
        for (int i=0; i<Main.clusters; i++) {
          evaluation.add("identEntropy" + i, -Math.log(trueCandidate.clusterProbs[i]));
        }
      }
    }
    String contextStr = ngramContext.contextStr();
    if (Main.verbose >= 2) {
      String entropyStr = oracle ? Fmt.D(entropy) : "N/A";
      begin_track("Example %s [%s]: %s (%d candidates, rank %s, entropy %s)",
          path, correct ? "CORRECT" : "WRONG", contextStr,
          state.getCandidates().size(), rank, entropyStr);
      logs("True (prob= %s): [%s]", Fmt.D(trueCandidate.prob), trueTokenStr);
      logs("Pred (prob= %s): [%s]", Fmt.D(predCandidate.prob), predToken);
      if (oracle) {
        KneserNey.logKNIs(true);
        KneserNey.computeProb(CandidateNgram.get(context, trueCandidate),
                              summary);
        KneserNey.logKNIs(false);
      }
      // begin_track("True");
      FeatureVector.logFeatureWeights("True", trueCandidate.features.toMap(),
          params);
      // for (int i = 0; i < Main.clusters; i++) {
      //   logs("cluster=%d, score %s, prob %s", i, Fmt.D(trueCandidate.clusterScores[i]), Fmt.D(trueCandidate.clusterProbs[i]));
      //   FeatureVector.logFeatureWeights("cluster=" + i,
      //                                   trueCandidate.clusterFeatures.toMap(),
      //                                   params, Main.clusterDecorators[i]);
      // }
      // end_track();
      KneserNey.logKNIs(true);
      KneserNey.computeProb(CandidateNgram.get(context, predCandidate),
                            summary);
      KneserNey.logKNIs(false);
      FeatureVector.logFeatureWeights("Pred", predCandidate.features.toMap(), params);
      // for (Candidate candidate : candidates) {
      //   begin_track("Candidate " + candidate.token);
      //   for (int i = 0; i < Main.clusters; i++) {
      //     logs("cluster=%d, score %s, prob %s", i, Fmt.D(candidate.clusterScores[i]), Fmt.D(candidate.clusterProbs[i]));
      //     FeatureVector.logFeatureWeights("cluster=" + i,
      //                                     candidate.clusterFeatures.toMap(),
      //                                     params, Main.clusterDecorators[i]);
      //   }
      //   end_track();
      // }
      FeatureVector.logFeatureDiff("True - Pred", trueCandidate.features, predCandidate.features, params);
      end_track();
    }
    // Longest context that has been seen
    int context_max_n = ngramContext.getMax_n() - 1;
    while (context_max_n > 0 &&
        !summary.counts[context_max_n].containsKey(ngramContext.subContext(context_max_n+1)))
      context_max_n--;
    evaluation.add("context_max_n", context_max_n);
    predOut.println(
        "path=" + path +
        "\tident=" + (isIdent ? 1 : 0) +
        "\tcontext=" + contextStr +
        "\tcontext_max_n=" + context_max_n +
        "\ttrue=" + trueTokenStr +
        "\tpred=" + predToken +
        "\trank=" + rank +
        "\tentropy=" + entropy);
    predOut.flush();
    entOut.println(
        path +
        "\t" + state.getTrueToken().loc() +
        "\t" + (isIdent ? 1 : 0) +
        "\t" + (oracle ? entropy : (state.isOov() ? "oov" : "offBeam")) +
        "\t" + reciprocalRank);
    entOut.flush();

    return evaluation;
  }
}
