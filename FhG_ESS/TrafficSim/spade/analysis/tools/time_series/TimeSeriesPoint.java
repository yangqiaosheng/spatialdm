package spade.analysis.tools.time_series;

import net.sourceforge.openforecast.DataPoint;

/**
 * Created by IntelliJ IDEA.
 * User: natascha
 * Date: May 9, 2011
 * Time: 4:16:01 PM
 * Represents one point in a time series. The time moment is specified as a long value.
 */
public class TimeSeriesPoint implements DataPoint {
  public long timeStamp=0;
  public double numVal =Double.NaN;

  public TimeSeriesPoint(long time, double value) {
    timeStamp=time; this.numVal =value;
  }

  public void setDependentValue(double value) {
    this.numVal =value;
  }
  public double getDependentValue() {
    return numVal;
  }

  /**
   * Sets the value of the independent variable, i.e., the time stamp
   * @param name - ignored
   * @param value - transformed to long
   */
  public void setIndependentValue(java.lang.String name,
                                double value) {
    timeStamp=Math.round(value);
  }
  /**
   * Returns the value of the independent variable, i.e., the time stamp
   * @param name - ignored
   */
  public double getIndependentValue(java.lang.String name) {
    return timeStamp;
  }

  public java.lang.String[] getIndependentVariableNames() {
    String names[]={"time"};
    return names;
  }

  public boolean equals(DataPoint dp) {
    if (!(dp instanceof TimeSeriesPoint))
      return false;
    TimeSeriesPoint tp=(TimeSeriesPoint)dp;
    return tp.timeStamp==this.timeStamp && tp.numVal==this.numVal;
  }

}
