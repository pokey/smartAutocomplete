package smartAutocomplete;

import java.util.*;
import java.io.*;

class Vocab implements Iterable<String> {
  public static final String OOV = "<oov>";
  public static final String BEGIN = "<s>";
  public static final String END = "</s>";
  public static final String NEWLINE = "<nl>";
  public static final String INDENT = "<ind>";
  public static final String DEDENT = "<ded>";

  private Set<String> vocab = new HashSet<String>();
  private int size = 0;

  public int getSize() { return size; }

  Set<String> getVocab() { return vocab; }

  boolean contains(String s) { return vocab.contains(s); }

  public Iterator<String> iterator() {
    return vocab.iterator();
  }

  public void add(String s) {
    vocab.add(s);
    size++;
  }

  public void remove(String s) {
    vocab.remove(s);
    size--;
  }

  public void readFile(String vocabFile) {
    try {
      BufferedReader br = new BufferedReader(new FileReader(vocabFile));  
      String line = null;  
      while ((line = br.readLine()) != null)  {  
        String word = line.trim();
        word = ReadData.toLower ? word.toLowerCase() : word;
        add(word);
      } 
      if (vocab.contains(BEGIN) || vocab.contains(END) ||
          vocab.contains(OOV)) {
        throw new RuntimeException("Disallowed token in vocabulary");
      }
    } catch (Exception e) {
      throw new RuntimeException("Error opening vocab file " + vocabFile + ": " + e);
    }
    add("</s>");
  }
}
