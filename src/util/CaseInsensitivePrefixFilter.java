package smartAutocomplete.util;

public class CaseInsensitivePrefixFilter implements Filter<String> {
  private String prefix;

  public CaseInsensitivePrefixFilter(String prefix) {
    this.prefix = prefix.toLowerCase();
  }

  @Override
  public boolean isTrue(String t) {
    return t.toLowerCase().startsWith(prefix);
  }
}
