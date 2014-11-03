package smartAutocomplete;

public class MultiTokenCandidate {
  private MultiTokenCandidate prev;
  private Candidate last;
  private double prob;
  private double exclusiveProb;
  private int length;

  public double getProb() { return prob; }
  public double getExclusiveProb() { return exclusiveProb; }
  public int getLength() { return length; }
  public Candidate getLast() { return last; }
  public MultiTokenCandidate getPrev() { return prev; }

  public MultiTokenCandidate(MultiTokenCandidate prev, Candidate last) {
    this.prev = prev;
    this.last = last;
    this.prob = prev == null ? last.prob : prev.prob * last.prob;
    this.length = prev == null ? 1 : prev.length + 1;
    this.exclusiveProb = this.prob;
  }

  public void subtractProbFromPrev() {
    if (prev != null) prev.exclusiveProb -= this.prob;
  }
}
