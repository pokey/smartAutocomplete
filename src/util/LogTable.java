package smartAutocomplete;

// Stores table of logs of integers up to a fixed number
// For performance
class LogTable {
  static double[] logTable = new double[1000];

  static {
    for (int i=0; i<logTable.length; i++) {
      logTable[i] = Math.log(i);
    }
  }

  static public double log(int val) {
    return val < LogTable.logTable.length ? logTable[val] : Math.log(val);
  }
}
