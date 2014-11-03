package smartAutocomplete;

import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;

import fig.basic.*;
import static fig.basic.LogInfo.logs;

import smartAutocomplete.util.*;

public class ReadData {
  @Option(gloss="Set of files/directories to crawl") public static List<String> inPaths = new ArrayList<String>();
  @Option(gloss="Set of files/directories to do global counting on")
    public static List<String> globCountPaths = new ArrayList<String>();
  @Option(gloss="Maximum number of documents to read") public static int maxDocuments = Integer.MAX_VALUE;
  @Option(gloss="One document per line") public static boolean splitDocumentByLine = false;
  @Option(gloss="Tokenize code") public static boolean tokenizeCode = false;
  @Option(gloss="Convert words to lower case") public static boolean toLower = false;
  @Option(gloss="Single beginning of sentence token") public static boolean singleBOS = true;

  // Specific train / test sets
  @Option(gloss="Set of files/directories to train on") public static List<String> trainPaths = new ArrayList<String>();
  @Option(gloss="Set of files/directories to test on") public static List<String> testPaths = new ArrayList<String>();
  @Option(gloss="Set of files/directories to test and count on") public static List<String> testCountPaths = new ArrayList<String>();

  private enum Mark { NONE, TRAIN, TEST, TEST_COUNT }

  private static FileFilter docFilter = new FileFilter() {
    public boolean accept(File file) {
      return !file.getName().equals(".git");
    }
  };

  static public void iterDocuments(List<String> paths, DocHandler handler) {
    for (String path : paths) {
      for (File file : IOUtils.getFilesUnder(path, docFilter)) {
        if (!FileUtil.isText(file) ||
            !Main.supportsLanguage(file.getPath()))
          continue;
        if (!readFile(file.toString(), handler)) return;
      }
    }
  }

  static public void readInPaths(List<Document> documents) {
    iterDocuments(inPaths, new DefaultHandler(documents, Mark.NONE));
  }

  static public void readFixedPaths(List<Document> documents) {
    iterDocuments(trainPaths, new DefaultHandler(documents, Mark.TRAIN));
    iterDocuments(testPaths, new DefaultHandler(documents, Mark.TEST));
    iterDocuments(testCountPaths,
                  new DefaultHandler(documents, Mark.TEST_COUNT));
  }

  public static interface DocHandler {
    // Return false if no more documents should be handled
    public boolean handle(Document doc);
  }

  private static class DefaultHandler implements DocHandler {
    private List<Document> documents;
    private Mark mark;

    public DefaultHandler(List<Document> documents, Mark mark) {
      this.documents = documents;
      this.mark = mark;
    }

    private void markDoc(Document doc) {
      switch (mark) {
        case TRAIN:
          doc.count = true;
          break;
        case TEST:
          doc.eval = true;
          break;
        case TEST_COUNT:
          doc.count = true;
          doc.eval = true;
          break;
      }
    }

    @Override
    public boolean handle(Document doc) {
      if (documents.size() >= maxDocuments) return false;
      markDoc(doc);
      documents.add(doc);
      return true;
    }
  }

  static boolean readFile(String path, DocHandler handler) {
    try {
      if (tokenizeCode && !splitDocumentByLine) {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        String text = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(encoded)).toString();
        List<Token> tokens = Tokenizer.tokenize(text, path);
        if (tokens != null) {
          return handler.handle(new Document(path, tokens));
        }
        return true;
      }

      BufferedReader in = IOUtils.openIn(path);
      String line;
      List<Token> tokens = null;
      int lineNum = 0;
      while ((line = in.readLine()) != null) {
        lineNum++;
        if (tokens == null) {
          tokens = new ArrayList<Token>();
          if (singleBOS) {
            tokens.add(new Token(Vocab.BEGIN, -Vocab.BEGIN.length(),
                                 Token.Type.BOS, Token.Context.NONE));
          } else {
            for (int i=-(Main.ngramOrder-1); i<0; i++)
              tokens.add(new Token(Vocab.BEGIN, i*Vocab.BEGIN.length(),
                                   Token.Type.BOS, Token.Context.NONE));
          }
        }
        // TODO: add newline
        // TODO: remove comments (especially the ones in the header)
        //logs("LINE: %s", line);
        for (String token : line.trim().split("\\s+")) {
          if (toLower) token = token.toLowerCase();
          // FixMe: [correctness] Track token positions
          if (Main.isFixedVocab() && !Main.getVocab().contains(token)) {
            tokens.add(new Token(Vocab.OOV, -1, Token.Type.OOV,
                                 Token.Context.NONE));
          } else {
            tokens.add(new Token(token, -1, Token.Type.ACTUAL,
                                 Token.Context.NONE));
          }
        }
        if (splitDocumentByLine) {
          // FixMe: [correctness] Track token positions
          tokens.add(new Token(Vocab.END, -1, Token.Type.EOS,
                               Token.Context.NONE));
          if (!handler.handle(new Document(path, tokens))) return false;
          tokens = null;
        }
      }
      in.close();
      if (!splitDocumentByLine && tokens != null) {
        if (!handler.handle(new Document(path, tokens))) return false;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return true;
  }
}
