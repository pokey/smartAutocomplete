package smartAutocomplete.util;

import fig.basic.*;

public class StrUtil {
  public static String join(SubList<String> list, String delim) {
    StringBuilder ret = new StringBuilder();
    String space = "";
    for (int i=0; i<list.size(); i++) {
      ret.append(space);
      ret.append(list.get(i));
      space = delim;
    }
    return ret.toString();
  }
}

