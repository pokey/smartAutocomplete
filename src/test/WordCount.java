import java.io.*;

public class WordCount {
  public static void main(String[] args) {
    try {
      BufferedReader br = new BufferedReader(new FileReader(args[0]));  
      int count = 0;
      while (br.readLine() != null) {  
        count++;
      }
      System.out.println(count);
    } catch (IOException e) { 
      System.err.println("Problem reading file " + args[0]);
    }
  }
}
