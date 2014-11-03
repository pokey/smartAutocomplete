public class Triple {
  private double tripVal1;
  private double tripVal2;
  private double tripVal3;

  public Triple(double tripVal1, double tripVal2, double tripVal3) {
    this.tripVal1 = tripVal1;
    this.tripVal2 = tripVal2;
    this.tripVal3 = tripVal3;
  }

  public double getTripVal1() { return tripVal1; }
  public double getTripVal2() { return tripVal2; }
  public double getTripVal3() { return tripVal3; }

  public double norm() {
    return Math.sqrt(tripVal1 * tripVal1 + tripVal2 * tripVal2 +
                     tripVal3 * tripVal3);
  }

  public Triple add(Triple other) {
    return new Triple(this.tripVal1 + other.tripVal1,
                      this.tripVal2 + other.tripVal2,
                      this.tripVal3 + other.tripVal3);
  }
}
