package smartAutocomplete.util;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;

public class FileUtil {
  private static char fileSeparator =
    System.getProperty("file.separator").charAt(0);

  // FixMe: [security][correctness] Better way to handle infinite directory loops?
  public static boolean contains(File parent, File child) {
    int count = 0;
    while(child != null && count < 50) {
      if(parent.equals(child)) return true;
      child = child.getParentFile();
      count++;
    }

    return false;
  }

  public static String readFile(String path) throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(encoded)).toString();
  }

  // Determines if a file is binary or text
  // Adapted from http://stackoverflow.com/a/1446870
  public static boolean isText(File file) {
    try {
      // Get first 512 bytes of file
      byte[] buf = new byte[512];
      FileInputStream in = new FileInputStream(file);
      int len = in.read(buf);
      in.close();

      // Empty files are considered text
      if (len == 0) return true;

      int numNonTextChars = 0;
      for (int i=0; i<len; i++) {
        // Files with null bytes are likely binary
        if (buf[i] == 0) return false;

        // Count non-text characters
        if (buf[i] < 32 || buf[i] >= 127) numNonTextChars++;
      }
      // Filse with less than 30% non-text characters are considered text
      return numNonTextChars / (double)len < 0.30;
    } catch (IOException e) {
      return false;
    }
  }

  public static String getExtension(String path) {
    for (int i=path.length()-1; i>=0; i--) {
      char c = path.charAt(i);
      if (c == '.' && i>0 && path.charAt(i-1) != fileSeparator) {
        return path.substring(i+1, path.length());
      }
      if (c == fileSeparator) return "";
    }
    return "";
  }

  public static String getFilenameNoExtension(String path) {
    int dotLoc = path.length();
    for (int i=path.length()-1; i>=0; i--) {
      char c = path.charAt(i);
      if (c == '.') dotLoc = i;
      if (c == fileSeparator) {
        return i+1 == dotLoc ? path.substring(i+1, path.length())
                             : path.substring(i+1, dotLoc);
      }
    }
    return dotLoc == 0 ? path.substring(0, path.length())
                       : path.substring(0, dotLoc);
  }
}
