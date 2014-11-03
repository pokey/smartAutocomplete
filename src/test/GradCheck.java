package smartAutocomplete;

import com.google.common.collect.Lists;

import java.util.*;

import fig.basic.*;
import fig.prob.*;
import static fig.basic.LogInfo.logs;

public class GradCheck {
  private static double epsilon = 1e-4;

  public interface FunctionState {
    // The current point (owned by FunctionState but is modified by the optimizer).
    // Contract: the optimizer will mutate the array returned by point().
    public HashMap<String, Double> point();

    // Return the function value at the current point, computing it if necessary.
    public double value();

    // Return gradient at the current point, computing it if necessary.
    // Contract: the optimizer will not modify the returned array, and the
    // FunctionState can reuse the same one.
    public HashMap<String, Double> gradient();

    // Called after optimizer mutates the current point.
    // When this function is called, make a note to recompute the value and
    // gradient.
    public void invalidate();
  }

  public static boolean doGradientCheck(FunctionState state) {
    // Save point
    state.invalidate();
    HashMap<String, Double> point = state.point();
    HashMap<String, Double> gradient = state.gradient();
    HashMap<String, Double> currentGradient = (HashMap)gradient.clone();
    HashMap<String, Double> currentPoint = (HashMap)point.clone();

    // Set point to be +/- gradient
    for (Map.Entry<String, Double> entry : currentPoint.entrySet()) {
      String key = entry.getKey();
      double value = entry.getValue();

      point.put(key, value + epsilon);
      state.invalidate();
      double valuePlus = state.value();

      point.put(key, value - epsilon);
      state.invalidate();
      double valueMinus = state.value();

      point.put(key, value);
      state.invalidate();

      double expectedValue = (valuePlus - valueMinus)/(2*epsilon);
      double actualValue = currentGradient.get(key);
      if( !NumUtils.equals(expectedValue, actualValue, 1e-4) ) {
        logs("Grad check failed because delta=%s but grad=%s",
             Fmt.D(expectedValue), Fmt.D(actualValue));
        assert false;
        return false;
      }
    }

    return true;
  }
};
