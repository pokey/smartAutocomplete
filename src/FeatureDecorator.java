package smartAutocomplete;

/**
 * Used to modify features
 */
public interface FeatureDecorator {
  public String decorate(String feature);
}

/** Leaves feature unchanged **/
class IdentityFeatureDecorator implements FeatureDecorator {
  private IdentityFeatureDecorator() { }
  
  @Override
  public String decorate(String feature) { return feature; }
  
  public static final IdentityFeatureDecorator decorator = new IdentityFeatureDecorator();
}


/** Adds suffix to the name of each feature **/
class SuffixFeatureDecorator implements FeatureDecorator {
  private final String suffix;
  public SuffixFeatureDecorator(String suffix) { this.suffix = suffix; }
  
  @Override
  public String decorate(String feature) { return feature + suffix; }
}
