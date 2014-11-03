package smartAutocomplete.httpServer;

import java.util.*;

import com.google.gson.*;

import smartAutocomplete.*;

public class CodeInfo {
  private String path_;
  private StringBuilder json = new StringBuilder(); 
  private String jsonSep = "";
  private Gson gson;

  public String path() { return path_; }

  CodeInfo(String _path) {
    path_ = _path;
    gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
  }

  public void addToken(int loc, InferState state) {
    addToken(loc, state.isIdent(), state.isOov(), state.isOracle(),
             state.getEntropy(), state.getReciprocalRank());
  }

  public void addToken(Map<String, Object> map) {
    json.append(jsonSep + gson.toJson(map));
    jsonSep = ",";
  }

  public void addToken(int loc, boolean isIdent, boolean oov,
                       boolean oracle, double entropy,
                       double reciprocalRank) {
    String entropyStr = oracle ? String.valueOf(entropy) :
                        (oov ? "\"oov\"" : "\"offBeam\"");
    json.append(jsonSep + "[" + loc + "," + 
                                (isIdent ? "1" : "0") + "," +
                                entropyStr + "," +
                                String.valueOf(reciprocalRank) + "]");
    jsonSep = ",";
  }

  public String getJson() {
    return json.toString();
  }
}
