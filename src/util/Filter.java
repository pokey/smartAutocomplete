package smartAutocomplete.util;

public interface Filter<T> {
  public boolean isTrue(T t);
}
