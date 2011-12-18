package spade.analysis.tools.time_series;

import net.sourceforge.openforecast.models.TripleExponentialSmoothingModel;
import net.sourceforge.openforecast.ForecastingModel;
import net.sourceforge.openforecast.DataSet;
import net.sourceforge.openforecast.DataPoint;

/**
 * Created by IntelliJ IDEA.
 * User: natascha
 * Date: Jul 13, 2011
 * Time: 9:23:25 AM
 * Uses one or several Holt-Winters models to model time series with
 * one or two temporal cycles
 */
public class HWModel implements ForecastingModel {
  /**
   * Describes the model(s)
   */
  public HWModelDescriptor hwd =null;
  /**
   * The value of the time variable corresponding to the first element
   * of the time series
   */
  public int startTimeValue=0;
  /**
   * The origin of the first temporal cycle in the original data
   * (the time series may be shifted, e.g., start not from the midnight
   * or not from Monday
   */
  public int cycleStart=0;
  /**
   * Number of values in the original time series after cycleStart
   */
  public int nValSinceCycleStart=0;
  /**
   * The index of the first element of the original time series that has
   * been actually used for the model building (some values may be skipped
   * if they correspond to the previous incomplete time cycle)
   */
  public int dataStart=0;
  /**
   * In case of one temporal cycle, the model for the whole time series
   */
  protected TripleExponentialSmoothingModel es3Model=null;
  /**
   * In case of two temporal cycles,
   * this array contains Holt-Winters models for each position of the inner cycle;
   * the modelling is applied to the values in this position over the outer cycle
   */
  protected TripleExponentialSmoothingModel hwModels[]=null;
  /**
   * Retrieves the model(s) according to the given descriptor.
   * Returns true if successful.
   */
  public boolean retrieveModel(HWModelDescriptor hwd) {
    this.hwd =hwd;
    if (hwd==null || hwd.hwModelDescr==null || hwd.hwModelDescr.length<1)
      return false;
    if (hwd.nCycles==1) {
      if (hwd.hwModelDescr[0]==null)
        return false;
      ForecastingModel model=hwd.hwModelDescr[0].getModel();
      if (model==null)
        return false;
      if (model instanceof TripleExponentialSmoothingModel) {
        es3Model=(TripleExponentialSmoothingModel)model;
        return true;
      }
      return false;
    }
    hwModels=new TripleExponentialSmoothingModel[hwd.hwModelDescr.length];
    for (int i=0; i<hwd.hwModelDescr.length; i++) {
      if (hwd.hwModelDescr[i]==null)
        return false;
      ForecastingModel model=hwd.hwModelDescr[i].getModel();
      if (model==null)
        return false;
      if (!(model instanceof TripleExponentialSmoothingModel))
        return false;
      hwModels[i]=(TripleExponentialSmoothingModel)model;
    }
    return true;
  }
  /**
   * Retrieves the model(s) according to the given descriptor.
   * Returns an instance of HWModel if successful.
   */
  public static HWModel retrieveHWModel (HWModelDescriptor hwd) {
    HWModel hwMod=new HWModel();
    if (hwMod.retrieveModel(hwd))
      return hwMod;
    return null;
  }

  public boolean canWork() {
    if (es3Model!=null)
      return true;
    if (hwModels==null)
      return false;
    int nModels=0;
    for (int i=0; i<hwModels.length; i++)
      if (hwModels[i]!=null)
        ++nModels;
    return nModels==hwModels.length;
  }

  public int getFullCycleLength() {
    if (hwd==null)
      return 0;
    if (hwd.nCycles<2)
      return hwd.nStepsInCycle;
    return hwd.nInnerCyclesInOuter*hwd.innerCycleLength;
  }
  /**
   * Using the current model parameters (initialized in init), apply the forecast model
   * to the given time step.
   */
  public double forecast (long timeStep) {
    double result=Double.NaN;
    if (es3Model!=null) {
      try {
        result=es3Model.forecast(new TimeSeriesPoint(timeStep,Double.NaN));
      } catch (Exception ex) {}
    }
    else {
      int ii=(int)(timeStep %hwd.innerCycleLength);
      int io=(int)((timeStep /hwd.innerCycleLength)%hwd.nInnerCyclesInOuter);
      //System.out.print(i+"("+io+","+ii+") ");
      if (hwd.wayToModel==HWModelDescriptor.HW_for_inner_cycle_positions) {
        try {
          result=hwModels[ii].forecast(new TimeSeriesPoint(io+hwd.nInnerCyclesInOuter,Double.NaN));
        } catch (Exception ex) {}
      }
      else {
        try {
          result=hwModels[io].forecast(new TimeSeriesPoint(ii+hwd.innerCycleLength,Double.NaN));
        } catch (Exception ex) {}
      }
    }
    return result;
  }
  /**
   * Returns a one or two word name of this type of forecasting model.
   */
  public String getForecastType() {
    if (es3Model!=null)
      return es3Model.getForecastType();
    if (hwModels==null || hwModels.length<1)
      return null;
    return "Holt-Winters for 2 cycles";
  }
  /**
   * Returns the bias - the arithmetic mean of the errors -
   * obtained from applying the current forecasting model to
   * the initial data set to try and predict each data point.
   */
  public double getBias() {
    if (es3Model!=null)
      return es3Model.getBias();
    if (hwModels==null || hwModels.length<1)
      return Double.NaN;
    double sum=0;
    int nv=0;
    for (int i=0; i<hwModels.length; i++) {
      if (hwModels[i]==null) continue;
      double d=hwModels[i].getBias();
      if (Double.isNaN(d))
        continue;
      sum+=d;
      ++nv;
    }
    if (nv<1) return Double.NaN;
    return sum/nv;
  }
  /**
   * Returns the mean absolute deviation obtained from applying the
   * current forecasting model to the initial data set to try and predict each data point.
   */
  public double getMAD() {
    if (es3Model!=null)
      return es3Model.getMAD();
    if (hwModels==null || hwModels.length<1)
      return Double.NaN;
    double sum=0;
    int nv=0;
    for (int i=0; i<hwModels.length; i++) {
      if (hwModels[i]==null) continue;
      double d=hwModels[i].getMAD();
      if (Double.isNaN(d))
        continue;
      sum+=d;
      ++nv;
    }
    if (nv<1) return Double.NaN;
    return sum/nv;
  }
  /**
   * Returns the mean absolute percentage error obtained
   * from applying the current forecasting model to the initial data set to
   * try and predict each data point.
   */
  public double getMAPE() {
    if (es3Model!=null)
      return es3Model.getMAPE();
    if (hwModels==null || hwModels.length<1)
      return Double.NaN;
    double sum=0;
    int nv=0;
    for (int i=0; i<hwModels.length; i++) {
      if (hwModels[i]==null) continue;
      double d=hwModels[i].getMAPE();
      if (Double.isNaN(d))
        continue;
      sum+=d;
      ++nv;
    }
    if (nv<1) return Double.NaN;
    return sum/nv;
  }
  /**
   * Returns the mean square of the errors (MSE) obtained from
   * applying the current forecasting model to the initial data set
   * to try and predict each data point.
   */
  public double getMSE() {
    if (es3Model!=null)
      return es3Model.getMSE();
    if (hwModels==null || hwModels.length<1)
      return Double.NaN;
    double sum=0;
    int nv=0;
    for (int i=0; i<hwModels.length; i++) {
      if (hwModels[i]==null) continue;
      double d=hwModels[i].getMSE();
      if (Double.isNaN(d))
        continue;
      sum+=d;
      ++nv;
    }
    if (nv<1) return Double.NaN;
    return sum/nv;
  }
  /**
   * Returns the sum of the absolute errors (SAE) obtained from applying
   * the current forecasting model to the initial data set to try and
   * predict each data point.
   */
  public double getSAE() {
    if (es3Model!=null)
      return es3Model.getSAE();
    if (hwModels==null || hwModels.length<1)
      return Double.NaN;
    double sum=0;
    int nv=0;
    for (int i=0; i<hwModels.length; i++) {
      if (hwModels[i]==null) continue;
      double d=hwModels[i].getSAE();
      if (Double.isNaN(d))
        continue;
      sum+=d;
      ++nv;
    }
    if (nv<1) return Double.NaN;
    return sum/nv;
  }

  /**
   * Provides a textual description of the current forecasting model including,
   * where possible, any derived parameters used.
   */
  public String toString() {
    if (es3Model!=null)
      return es3Model.toString();
    if (hwModels==null || hwModels.length<1)
      return null;
    String str=getForecastType()+": \n";
    if (hwd !=null)
      str+="Inner cycle length = "+hwd.innerCycleLength+
           "; \nN of inner cycles in the outer = "+
           hwd.nInnerCyclesInOuter+
          "; \nWay to model = "+
           HWModelDescriptor.getModelingWayAsString(hwd.wayToModel);
    for (int i=0; i<hwModels.length; i++) {
      if (hwModels[i]==null) continue;
      str+="; \n"+hwModels[i].toString();
    }
    return str;
  }
  /**
   * Used to initialize the model-specific parameters and customize them to the given data set.
   * This method does nothing, needed only to implement the interface.
   */
  public void init(DataSet dataSet) {
    //
  }
  /**
   * Using the current model parameters (initialized in init), apply the forecast model
   * to the given data point.
   */
  public double forecast(DataPoint dataPoint) {
    if (dataPoint==null)
      return Double.NaN;
    if (dataPoint instanceof TimeSeriesPoint) {
      TimeSeriesPoint tsp=(TimeSeriesPoint)dataPoint;
      return forecast(tsp.timeStamp);
    }
    long t=Math.round(dataPoint.getIndependentValue("time"));
    return forecast(t);
  }
  /**
   * Using the current model parameters (initialized in init), apply the
   * forecast model to the given data set.
   * This method does nothing, needed only to implement the interface.
   */
  public DataSet forecast(DataSet dataSet) {
    return null;
  }
  /**
   * If the model has been already built, returns the modelled time series
   * for the same time period as the original time series plus, possibly,
   * several time steps ahead.
   */
  public double[] getModelledTimeSeries (int endTimeStep) {
    if (!canWork())
      return null;
    double modTSeries[]=new double[endTimeStep +1];
    int nSkip =startTimeValue+dataStart;
    for (int i=0; i<nSkip; i++)
      modTSeries[i]=Double.NaN;
    int cStart=cycleStart-dataStart;

    for (int i=nSkip; i<=endTimeStep; i++) {
      //determine the position in the transformed time series
      int i0=i-nSkip;
      if (i0>=cStart) i0-=cStart; else i0+=nValSinceCycleStart;
      modTSeries[i]=forecast(i0);
    }
    return modTSeries;
  }
}
