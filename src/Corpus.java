package smartAutocomplete;

public class Corpus {
  private String language;
  private boolean isGlobal;

  public Corpus(String language, boolean isGlobal) {
    this.language = language;
    this.isGlobal = isGlobal;
  }

  public String getLanguage() { return language; }

  public String getName() {
    return (isGlobal ? "global" : "proj") +
           (language.equals("") ? "" : "." + language);
  }
}
