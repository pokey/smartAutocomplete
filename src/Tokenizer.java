package smartAutocomplete;

import java.util.*;
import java.util.regex.*;

import fig.basic.*;

import com.google.common.base.Joiner;

public class Tokenizer {
  static private final String numbers = "[0-9]+(?:\\.[0-9]+)?";
  static private final String words = "\\w+";
  static private final String symbols = "!=|\\+\\+|--|&&|\\|\\||===|==|//|<=|>=|\\+=|-=|/\\*|\\*/";
  static private final String newLine = "(\r\n|\n|\r)[ \t\f\\v]*";
  static private final String python = "\"\"\"";
  static private final String other = "[^\\s\\w]";

  static private Pattern tokenizerRegex =
    Pattern.compile(Joiner.on("|").join(Arrays.asList(numbers, words, symbols,
                                                      newLine, python,
                                                      other)));

  static private String matching(String left) {
    if (left.equals("(")) return ")";
    if (left.equals("[")) return "]";
    if (left.equals("{")) return "}";
    throw new RuntimeException("Unexpected input to matching: " + left);
  }

  static public List<Token> tokenize(String text, String path) {
    boolean isPython = path.endsWith(".py");

    List<Token> tokens = new ArrayList<Token>();

    if (ReadData.singleBOS) {
      tokens.add(new Token(Vocab.BEGIN, -Vocab.BEGIN.length(), Token.Type.BOS,
                           Token.Context.NONE));
    } else {
      for (int i = -(Main.ngramOrder-1); i<0; i++) {
        tokens.add(new Token(Vocab.BEGIN, i*Vocab.BEGIN.length(), Token.Type.BOS,
                             Token.Context.NONE));
      }
    }

    boolean empty = true;
    boolean inComment = false;
    boolean isLongComment = false;
    String inQuote = "";
    HashMap<String, Integer> parenDepth = new HashMap<String, Integer>();
    int sumParenDepth = 0;
    String prevToken = "";
    String prevPrevToken = "";
    String newLine = "";
    ArrayList<Integer> indentStack = new ArrayList<Integer>();
    indentStack.add(0);
    int start = 0;
    String token = "";
    int prevStart=0;
    Matcher match = tokenizerRegex.matcher(text);
    Token.Context context = Token.Context.NONE;
    while (match.find()) {
      empty = false;
      token = match.group();
      start = match.start();
      if (ReadData.toLower) token = token.toLowerCase();
      if (token.charAt(0) == '\r' || token.charAt(0) == '\n') {
        if (!(inQuote.length()>0 || sumParenDepth > 0
                                 || (!inComment && prevToken.equals("\\")))) {
          newLine = token;
        }
      } else {

        // Emit last '\' token if it wasn't the last character in the line
        if (prevToken.equals("\\")) {
          tokens.add(new Token("\\", prevStart, Token.Type.ACTUAL, context));
        }

        // Beginning of line
        if (newLine.length() > 0) {
          if (!isLongComment) {
            inComment = false;
            context = Token.Context.NONE;
          }
          if (isPython) {
            tokens.add(new Token(Vocab.NEWLINE, start, Token.Type.NEWLINE, context));
            int indent = 0;
            int j=0;
            // Find end of newline sequence
            for (j=0; j < token.length(); j++) {
              if (token.charAt(j) != '\r' && token.charAt(j) != '\f' &&
                  token.charAt(j) != '\n')
                break;
            }
            // Compute new indent
            for (int i=j; i < newLine.length(); i++) {
              if (newLine.charAt(i) == ' ') indent += 1;
              else if (newLine.charAt(i) == '\t') {
                indent += 8;
                indent = (indent / 8) * 8;
              }
            }
            // Add INDENT or DEDENT tokens and update indentStack
            if (indent > indentStack.get(indentStack.size()-1)) {
              indentStack.add(indent);
              tokens.add(new Token(Vocab.INDENT, start, Token.Type.INDENT, context));
            } else {
              while (indent < indentStack.get(indentStack.size()-1)) {
                indentStack.remove(indentStack.size()-1);
                tokens.add(new Token(Vocab.DEDENT, start, Token.Type.DEDENT, context));
              }
            }
          }
          // Reset newLine
          newLine = "";
        }

        // Python ignores newlines within brackets
        if (isPython) {
          if ((token.equals("(") || token.equals("[") ||
               token.equals("{")) && !inComment && inQuote.length()==0) {
            MapUtils.incr(parenDepth, matching(token), 1);
            sumParenDepth++;
          } else if ((token.equals(")") || token.equals("]") ||
                      token.equals("}")) && !inComment && inQuote.length()==0) {
            MapUtils.incr(parenDepth, token, -1);
            sumParenDepth--;
          }
        }
        
        // Add token, unless it is '\', in which case we don't emit it
        // if it's the last token of the line
        if (!token.equals("\\")) {
          if (Main.isFixedVocab() && !Main.getVocab().contains(token)) {
            tokens.add(new Token(Vocab.OOV, start, Token.Type.OOV, context));
          } else {
            tokens.add(new Token(token, start, Token.Type.ACTUAL, context));
          }
        }

        // Comments and strings
        if ((token.equals("#") || token.equals("//")) && inQuote.length()==0) {
          inComment = true;
          context = Token.Context.COMMENT;
        } else if (token.equals("/*") && inQuote.length()==0 && !inComment) {
          inComment = true;
          isLongComment = true;
          context = Token.Context.COMMENT;
        } else if (token.equals("*/") && isLongComment) {
          inComment = false;
          isLongComment = false;
          context = Token.Context.NONE;
        } else if ((token.equals("\"\"\"") || token.equals("\"") ||
                    token.equals("'")) && !inComment) {
          if (inQuote.equals(token) &&
              (prevPrevToken.equals("\\") || !prevToken.equals("\\"))) {
            inQuote = "";
            context = Token.Context.NONE;
          } else if (inQuote.length()==0) {
            inQuote = token;
            context = Token.Context.STRING;
          }
        }
      }

      prevPrevToken = prevToken;
      prevToken = token;
      prevStart = start;
    }

    // Add final newline and clear indentStack by adding DEDENT tokens as
    // necessary to close all scopes
    if (isPython) {
      tokens.add(new Token(Vocab.NEWLINE, start, Token.Type.NEWLINE, Token.Context.NONE));
      for (int i=0; i<indentStack.size()-1; i++) {
        tokens.add(new Token(Vocab.DEDENT, start+token.length(), Token.Type.DEDENT, Token.Context.NONE));
      }
    }

    return empty ? null : tokens;
  }

  public static boolean isIdentifier(String token) {
    return token.matches("\\w+");
  }
}
