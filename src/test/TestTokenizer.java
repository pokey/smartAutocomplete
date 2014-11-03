package smartAutocomplete.test;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;

import smartAutocomplete.Tokenizer;
import smartAutocomplete.Token;

public class TestTokenizer {
  static private final String pythonPath = "src/test/tokenizer.in.py";
  static private final String javaPath = "src/test/Tokenizer.java.in";

  public static void main(String[] args) throws IOException {
    if (args[0].equals("python")) testPython();
    else if (args[0].equals("java")) testJava();
    else throw new RuntimeException("Unexpected argument " + args[0]);
  }

  private static void testPython() throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(pythonPath));
    String text = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(encoded)).toString();
    StringBuilder out = new StringBuilder();
    for (Token token : Tokenizer.tokenize(text, pythonPath)) {
      Token.Type type = token.type();
      if (type == Token.Type.ACTUAL)
        out.append(token + " ");
      else if (type == Token.Type.NEWLINE)
        out.append("\n");
      else if (type == Token.Type.DEDENT)
        out.append("<<");
      else if (type == Token.Type.INDENT)
        out.append(">>");
    }
    System.out.println(out);
  }

  private static void testJava() throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(javaPath));
    String text = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(encoded)).toString();
    for (Token token : Tokenizer.tokenize(text, javaPath)) {
      System.out.println(token.str() + "\t" + token.type() + "\t" +
                         token.context());
    }
  }
}
