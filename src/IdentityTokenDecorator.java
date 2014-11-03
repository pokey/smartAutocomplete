package smartAutocomplete;

import java.util.*;

public class IdentityTokenDecorator implements TokenDecorator {
  private static IdentityTokenDecorator singleton =
    new IdentityTokenDecorator();

  public static IdentityTokenDecorator getSingleton() { return singleton; }

  @Override
  public void decorate(ArrayList<String> tokens, Mode mode) {}

  private IdentityTokenDecorator() {}
}

