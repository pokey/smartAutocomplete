package smartAutocomplete;

import java.io.*;

import fig.exec.*;
import fig.basic.*;

import edu.berkeley.nlp.lm.*;
import edu.berkeley.nlp.lm.util.*;
import edu.berkeley.nlp.lm.io.*;
import edu.berkeley.nlp.lm.cache.*;

public class BerkeleyLM extends Statistic {
  private KneserNeyLmReaderCallback<String> kneserNeyReader = null;
  private StringWordIndexer wordIndexer = new StringWordIndexer();
  private ContextEncodedNgramLanguageModel<String> lm = null;

  public ContextEncodedNgramLanguageModel<String> getLm() {
    return lm;
  }

  public BerkeleyLM(Corpus corpus, Statistics statistics) {
    wordIndexer.setStartSymbol(Vocab.BEGIN);
    wordIndexer.setEndSymbol(Vocab.END);
    wordIndexer.setUnkSymbol(Vocab.OOV);
    kneserNeyReader =
      new KneserNeyLmReaderCallback<String>(wordIndexer,
          Main.ngramOrder, new ConfigOptions());
  }

  @Override
  public void count(PredictionContext context, String val) {
    // FixMe: [performance] Would be much faster to let BerkeleyLM
    // count the whole doc at once
    SubList<String> tokenList =
      CountingNgram.get(context, val).getTokens();
    final String[] tokens = new String[tokenList.size()];
    for (int i = 0; i < tokens.length; ++i)
      tokens[i] = tokenList.get(i);
    final long[][] scratch =
      new long[Main.ngramOrder][tokens.length];
    kneserNeyReader.callJustLast(tokens, new LongRef(1L), scratch);
  }

  @Override
  public void uncount(PredictionContext context, String val) {
    throw new RuntimeException("BerkeleyLM not designed to uncount");
  }

  @Override
  public void doneCounting() {
    kneserNeyReader.cleanup();

    final File tmpFile = getTempFile();
    kneserNeyReader.parse(new
        KneserNeyFileWritingLmReaderCallback<String>(tmpFile,
          wordIndexer));
    ContextEncodedNgramLanguageModel<String> uncachedLm =
      LmReaders.readContextEncodedLmFromArpa(tmpFile.getPath(),
          wordIndexer, new ConfigOptions(), Main.ngramOrder);
    lm = ContextEncodedCachingLmWrapper.
           wrapWithCacheNotThreadSafe(uncachedLm);
  }

  private static File getTempFile() {
    try {
      final File tmpFile =
        File.createTempFile("smartAutocomplete", "arpa");
      tmpFile.deleteOnExit();
      return tmpFile;
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
