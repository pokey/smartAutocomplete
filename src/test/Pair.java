public class Pair {
  private double x;
  private double y;

  public Pair(double x, double y) {
    this.x = x;
    this.y = y;
  }

  public double getX() { return x; }
  public double getY() { return y; }

  public double norm() {
    return Math.sqrt(x * x + y * y);
  }

  public Pair add(Pair other) {
    return new Pair(this.x + other.x, this.y + other.y);
  }
}

