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
  @Option(gloss="Read 1 out of every x documents") public static int docPeriod = 1;
  @Option(gloss="Maximum document size, in bytes") public static int maxFileSize = Integer.MAX_VALUE;
  @Option(gloss="One document per line") public static boolean splitDocumentByLine = false;
  @Option(gloss="Tokenize code") public static boolean tokenizeCode = false;
  @Option(gloss="Convert words to lower case") public static boolean toLower = false;
  @Option(gloss="Single beginning of sentence token") public static boolean singleBOS = true;

  // Specific train / test sets
  @Option(gloss="Set of files/directories to train on") public static List<String> trainPaths = new ArrayList<String>();
  @Option(gloss="Set of files/directories to test on") public static List<String> testPaths = new ArrayList<String>();
  @Option(gloss="Set of files/directories to test and count on") public static List<String> testCountPaths = new ArrayList<String>();

  @Option(gloss="File containing paths to ignore") public static String ignoreList = "";

  public static List<File> exclusions = new ArrayList<File>();

  private enum Mark { NONE, TRAIN, TEST, TEST_COUNT }

  public static boolean isExcluded(File file) {
    for (File f : exclusions) {
      if (FileUtil.contains(f, file)) return true;
    }
    return false;
  }

  private static FileFilter docFilter = new FileFilter() {
    public boolean accept(File file) {
      return !isExcluded(file) && !file.getName().equals(".git");
    }
  };

  static private int docCount = 0;

  static public void iterDocuments(List<String> paths, DocHandler handler) {
    for (String path : paths) {
      iterPath(path, handler);
    }
  }

  static public void iterPath(String path, DocHandler handler) {
    for (File file : IOUtils.getFilesUnder(path, docFilter)) {
      if (!FileUtil.isText(file) ||
          !Main.supportsLanguage(file.getPath()))
        continue;
      docCount++;
      if (docCount % docPeriod != 0) continue;
      if (!readFile(file.toString(), handler)) return;
    }
  }

  static public void initIgnoreList() {
    if (!ignoreList.equals("")) {
      try {
        BufferedReader br =
          new BufferedReader(new FileReader(ignoreList));
        String line = null;
        File parent = new File(ignoreList).getParentFile();
        while ((line = br.readLine()) != null) {  
          File f = new File(parent, line);
          exclusions.add(f);
        } 
      } catch (IOException e) {
        System.err.println("Error reading ignoreList " + ignoreList);
        System.exit(-1);
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
        if (encoded.length > maxFileSize) return true;
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
