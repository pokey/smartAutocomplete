package smartAutocomplete.util;

public class TrueFilter<T> implements Filter<T> {
  @Override
  public boolean isTrue(T t) {
    return true;
  }
}


