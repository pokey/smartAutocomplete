package smartAutocomplete.httpServer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import com.sun.net.httpserver.*;

import fig.html.*;
import fig.basic.*;
import static fig.basic.LogInfo.logs;

import org.yaml.snakeyaml.Yaml;

import smartAutocomplete.*;
import smartAutocomplete.util.*;

public class Exec {
  private static HtmlUtils H = new HtmlUtils();
  private static final Template template = createTemplate("webapp/list.html");
  private static Template createTemplate(final String path) {
    try {
      return new Template(path);
    } catch (final IOException exc) {
      throw new Error(exc);
    }  
  }

  static private Pattern execIdRegex = Pattern.compile("\\d+");

  static private boolean isExecId(String execId) {
    return execIdRegex.matcher(execId).matches();
  }

  String id = "none";
  String features;

  public static String statePath = null;

  private static HashMap<String, Exec> execs = new HashMap<String, Exec>();
  private HashMap<String, CodeInfo> codeInfoMap = new HashMap<String, CodeInfo>();

  private List<CodeInfo> codeInfos = new ArrayList<CodeInfo>();

  private String codeInfoType = "basic";

  static private Pattern featuresRegex =
    Pattern.compile("main\\.featureDomains\t(.*)");

  public Exec(HashSet<String> features) {
    this.features = features.toString();
  }

  private Exec(String id) throws IOException {
    this.id = id;
    String execDir = statePath + "/execs/" + id + ".exec/";

    String optionsPath = execDir + "options.map";
    Matcher match = featuresRegex.matcher(FileUtil.readFile(optionsPath));
    match.find();
    features = match.group(1);

    String candidatesPath = execDir + "candidates";
    BufferedReader in = IOUtils.openInEasy(candidatesPath);
    if (in != null) {
      readCandidatesFile(in);
    } else {
      String entropiesPath = execDir + "entropies";
      readEntropiesFile(IOUtils.openIn(entropiesPath));
    }
  }

  private void readCandidatesFile(BufferedReader in) throws IOException {
    codeInfoType = "candidates";
    String line;
    List<Token> tokens = null;
    int lineNum = 0;
    CodeInfo codeInfo = null;
    Yaml yaml = new Yaml();
    for (Object data : yaml.loadAll(in)) {
      Map<String, Object> map = (Map<String, Object>) data;
      String path = (String)map.get("path");
      if (codeInfo == null || !codeInfo.path().equals(path)) {
        codeInfo = addCodeInfo(path);
      }
      codeInfo.addToken(map);
    }
  }

  private void readEntropiesFile(BufferedReader in) throws IOException {
    String line;
    List<Token> tokens = null;
    int lineNum = 0;
    CodeInfo codeInfo = null;
    while ((line = in.readLine()) != null) {
      String[] vals = line.trim().split("\\t");
      String path = vals[0];
      if (codeInfo == null || !codeInfo.path().equals(path)) {
        codeInfo = addCodeInfo(path);
      }
      boolean oov = vals[3].equals("oov");
      boolean oracle = !(oov || vals[3].equals("offBeam"));
      double entropy = oracle ? Double.parseDouble(vals[3]) : Double.NaN;
      codeInfo.addToken(Integer.parseInt(vals[1]),
                        vals[2].equals("1"), oov, oracle,
                        entropy, Double.parseDouble(vals[4]));
    }
  }

  String getJson(String path) {
    return "[\"" + codeInfoType + "\",\"" + id + "\",\"" + features + "\",[" +
           codeInfoMap.get(path).getJson() + "]]";
  }

  public CodeInfo addCodeInfo(String path) {
    CodeInfo codeInfo = null;
    if(!codeInfoMap.containsKey(path)) {
      codeInfo = new CodeInfo(path);
      codeInfoMap.put(path, codeInfo);
      codeInfos.add(codeInfo);
    } else {
      codeInfo = codeInfoMap.get(path);
    }
    return codeInfo;
  }

  String getListHtml(String query) {
    StringBuilder codeInfoList = new StringBuilder();
    query = query == null ? "" : "?" + query;

    String prefix = (id == null ? "/code/" : "/code/" + id + "/");
    for (CodeInfo codeInfo : codeInfos) {
      codeInfoList.append(H.li().open());
      codeInfoList.append(H.a().attr("href", prefix + codeInfo.path()
                                             + query).open());
      codeInfoList.append(codeInfo.path());
      codeInfoList.append(H.a().close());
      codeInfoList.append(H.li().close());
    }

    HashMap<String, String> context = new HashMap<String, String>();
    context.put("docs", codeInfoList.toString());

    return template.instantiate(context);
  }

  static Exec getExec(String id) {
    Exec exec = null;
    if(!execs.containsKey(id)) {
      if (!isExecId(id)) return null;
      try {
        exec = new Exec(id);
        execs.put(id, exec);
      } catch (Exception exc) {
        logs("Exception creating exec: %s", exc);
        return null;
      }  
    } else {
      exec = execs.get(id);
    }
    return exec;
  }
}
