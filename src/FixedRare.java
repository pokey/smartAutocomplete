package smartAutocomplete;

import java.util.*;
import java.io.*;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fig.basic.*;

import static fig.basic.LogInfo.begin_track;
import static fig.basic.LogInfo.logs;
import static fig.basic.LogInfo.end_track;

public class FixedRare implements Rarity {
  @Option(gloss = "File to import rarity statistics from")
    public static String file = null;

  private int numTokens;

  private Set<String> commonTokens = new HashSet<String>();

  public FixedRare(int numTokens) {
    this.numTokens = numTokens;
    read(file);
  }

  /**
   * Read common tokens from |path|
   */
  private void read(String path) {
    List<String[]> lines = new ArrayList<String[]>();
    try {
      BufferedReader in = IOUtils.openIn(path);
      String line;
      int count = 0;
      while ((line = in.readLine()) != null) {
        commonTokens.add(line.substring(0, line.indexOf('\t')));
        count++;
        if (count == numTokens) break;
      }
      in.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    LogInfo.logs("Read %s common tokens.", commonTokens.size());
  }

  public boolean isRare(String token) {
    return !commonTokens.contains(token);
  }
}
