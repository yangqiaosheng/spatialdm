package spade.analysis.tools.time_series;

import spade.time.TimeMoment;
import spade.time.TimeCycleDescriptor;
import spade.lib.util.XMLUtil;
import net.sourceforge.openforecast.ForecastingModel;
import net.sourceforge.openforecast.DataSet;
import net.sourceforge.openforecast.models.*;

import java.util.Vector;
import java.util.StringTokenizer;
import java.io.StringWriter;

import org.w3c.dom.Element;

/**
 * Created by IntelliJ IDEA.
 * User: natascha
 * Date: Jun 28, 2011
 * Time: 12:30:29 PM
 * Describes a model representing temporal variation (time series).
 */
public class TSModelDescriptor {
  /**
   * Names of the possible types of models
   */
  public static final String modelNames[] = {
      "linear regression",                             // 0
      "polynomial regression",                         // 1
      "simple exponential smoothing",                  // 2
      "double exponential smoothing (Holt)",           // 3
      "triple exponential smoothing (Holt-Winters)"    // 4
  };
  /**
   * The name of the class (cluster) of the time series for which this model
   * has been built.
   */
  public String className=null;
  /**
   * The RGB components of the class color
   */
  public int classColorRed=0, classColorGreen=0, classColorBlue=0;
  /**
   * The index of the type of the model in the array modelNames
   * @see modelNames
   */
  public int modelIdx=-1;
  /**
   * The original time series used for model building
   */
  public double origData[]=null;
  /**
   * The original time moments (possibly, dates) of the data
   * used for model building.
   */
  public TimeMoment origTimes[]=null;
  /**
   * The time moment of the first element of the data used for model building.
   */
  public TimeMoment startTime=null;
  /**
   * The time moment of the last element of the data used for model building.
   */
  public TimeMoment endTime=null;
  /**
   * Smoothing parameters:
   * alpha - overall smoothing,
   * beta - trend smoothing,
   * gamma - trend smoothing or seasonal smoothing
   */
  public double alpha=Double.NaN, beta=Double.NaN, gamma=Double.NaN;
  /**
   * TripleExponentialSmoothingModel requires N of periods (or, generally, time steps)
   * per year (or, generally, time cycle)
   */
  public int nStepsInCycle =0;
  /**
   * The range of the modelled values
   */
  public double modMin=Double.NaN, modMax=Double.NaN;
  /**
   * The quartiles of the modelled values
   */
  public double modQ1=Double.NaN, modMedian=Double.NaN, modQ3=Double.NaN;

  /**
   * Copies all internal fields in the provided structure.
   * If the argument is null, the structure is created.
   * Returns the reference to the original or created structure.
   */
  public TSModelDescriptor getCopy (TSModelDescriptor md) {
    if (md==null)
      md=new TSModelDescriptor();
    if (className!=null) md.className=className;
    md.classColorRed=classColorRed;
    md.classColorGreen=classColorGreen;
    md.classColorBlue=classColorBlue;
    if (modelIdx>=0) md.modelIdx=modelIdx;
    if (origTimes!=null) md.origTimes=origTimes;
    if (startTime!=null) md.startTime=startTime;
    if (endTime!=null) md.endTime=endTime;
    if (origData!=null) md.origData=origData;
    if (!Double.isNaN(alpha)) md.alpha=alpha;
    if (!Double.isNaN(beta)) md.beta=beta;
    if (!Double.isNaN(gamma)) md.gamma=gamma;
    if (nStepsInCycle>0) md.nStepsInCycle =nStepsInCycle;
    if (!Double.isNaN(modMin))
      md.modMin=modMin;
    if (!Double.isNaN(modMax))
      md.modMax=modMax;
    if (!Double.isNaN(modQ1))
      md.modQ1=modQ1;
    if (!Double.isNaN(modMedian))
      md.modMedian=modMedian;
    if (!Double.isNaN(modQ3))
      md.modQ3=modQ3;
    return md;
  }
  /**
   * Stores the information about the model in XML format
   */
  public String toXML() {
    if (modelIdx<0 || origData==null)
      return null;
    StringWriter writer = new StringWriter();
    writer.write("  <NumTSModel type=\""+modelNames[modelIdx]+"\"");
    if (className!=null) {
      writer.write(" className=\""+className+"\"");
      if (classColorRed>0 || classColorGreen>0 || classColorBlue>0)
        writer.write(" classColorRGB=\"("+classColorRed+","+classColorGreen+","+
            classColorBlue+")\"");
    }
    writer.write(" >\r\n");
    writer.write("    <InitData> ");
    for (int i=0; i<origData.length; i++) {
      writer.write(((i==0)?"":";")+origData[i]);
    }
    writer.write(" </InitData>\r\n");
    if (nStepsInCycle>0)
      writer.write("    <NStepsInCycle>"+nStepsInCycle+"</NStepsInCycle>\r\n");
    if (!Double.isNaN(alpha) || !Double.isNaN(beta) || !Double.isNaN(gamma)) {
      writer.write("    <Parameters");
      if (!Double.isNaN(alpha))
        writer.write(" alpha=\""+alpha+"\"");
      if (!Double.isNaN(beta))
        writer.write(" beta=\""+beta+"\"");
      if (!Double.isNaN(gamma))
        writer.write(" gamma=\""+gamma+"\"");
      writer.write(" />\r\n");
    }
    if (!Double.isNaN(modMin) || !Double.isNaN(modMax) || !Double.isNaN(modMedian)) {
      writer.write("    <Statistics ");
      if (!Double.isNaN(modMin))
        writer.write(" minValue=\""+modMin+"\"");
      if (!Double.isNaN(modMax))
        writer.write(" maxValue=\""+modMax+"\"");
      if (!Double.isNaN(modQ1))
        writer.write(" q1Value=\""+modQ1+"\"");
      if (!Double.isNaN(modMedian))
        writer.write(" medianValue=\""+modMedian+"\"");
      if (!Double.isNaN(modQ3))
        writer.write(" q3Value=\""+modQ3+"\"");
      writer.write(" />\r\n");
    }
    writer.write("  </NumTSModel>");
    return writer.toString();
  }
  /**
   * Restores itself (i.e. all necessary internal settings) from
   * the given element of an XML document. Returns true if successful.
   */
  public boolean restoreFromXML(Element elem) {
    if (elem==null) return false;
    if (!elem.getTagName().equalsIgnoreCase("NumTSModel")) return false;
    modelIdx=-1;
    if (elem.hasAttribute("type")) {
      String typeTxt=elem.getAttribute("type");
      if (typeTxt==null)
        return false;
      for (int i=0; i<modelNames.length; i++)
        if (typeTxt.equalsIgnoreCase(modelNames[i]))
          modelIdx=i;
      if (modelIdx<0)
        return false;
    }
    if (elem.hasAttribute("className"))
      className=elem.getAttribute("className");
    if (elem.hasAttribute("classColorRGB")) {
      String str=elem.getAttribute("classColorRGB");
      if (str!=null) {
        StringTokenizer st=new StringTokenizer(str,"(,) \r\n");
        if (st.countTokens()>=3) {
          try {
            classColorRed=Integer.parseInt(st.nextToken());
          } catch (Exception ex){}
          try {
            classColorGreen=Integer.parseInt(st.nextToken());
          } catch (Exception ex){}
          try {
            classColorBlue=Integer.parseInt(st.nextToken());
          } catch (Exception ex){}
        }
      }
    }
    Element dataElem =XMLUtil.getElementByTagName(elem.getChildNodes(),"InitData");
    if (dataElem ==null) return false;
    String dataStr=XMLUtil.getTextFromNode(dataElem);
    if (dataStr==null)
      return false;
    StringTokenizer tok=new StringTokenizer(dataStr," ;");
    int nTok=tok.countTokens();
    if (nTok<2)
      return false;
    origData=new double[nTok];
    int nValidValues=0;
    for (int i=0; i<nTok; i++) {
      origData[i]=XMLUtil.getDouble(tok.nextToken());
      if (!Double.isNaN(origData[i]))
        ++nValidValues;
    }
    if (nValidValues<2)
      return false;
    Element subElem=XMLUtil.getElementByTagName(elem.getChildNodes(),"NStepsInCycle");
    if (subElem!=null)
      this.nStepsInCycle=XMLUtil.getInt(XMLUtil.getTextFromNode(subElem));
    subElem=XMLUtil.getElementByTagName(elem.getChildNodes(),"Parameters");
    if (subElem!=null) {
      if (subElem.hasAttribute("alpha"))
        alpha=XMLUtil.getDouble(subElem.getAttribute("alpha"));
      if (subElem.hasAttribute("beta"))
        beta=XMLUtil.getDouble(subElem.getAttribute("beta"));
      if (subElem.hasAttribute("gamma"))
        gamma=XMLUtil.getDouble(subElem.getAttribute("gamma"));
    }

    subElem=XMLUtil.getElementByTagName(elem.getChildNodes(),"Statistics");
    if (subElem!=null) {
      if (subElem.hasAttribute("minValue"))
        modMin=XMLUtil.getDouble(subElem.getAttribute("minValue"));
      if (subElem.hasAttribute("maxValue"))
        modMax=XMLUtil.getDouble(subElem.getAttribute("maxValue"));
      if (subElem.hasAttribute("q1Value"))
        modQ1=XMLUtil.getDouble(subElem.getAttribute("q1Value"));
      if (subElem.hasAttribute("medianValue"))
        modMedian=XMLUtil.getDouble(subElem.getAttribute("medianValue"));
      if (subElem.hasAttribute("q3Value"))
        modQ3=XMLUtil.getDouble(subElem.getAttribute("q3Value"));
    }

    return modelIdx>=0 && origData!=null;
  }

  /**
   * Reconstructs the model using the original data and model parameters
   */
  public ForecastingModel getModel() {
    if (modelIdx<0)
      return null;
    if (origData==null)
      return null;
    DataSet data=new DataSet();
    for (int i=0; i<origData.length; i++)
      data.add(new TimeSeriesPoint(i,origData[i]));
    data.setTimeVariable("time");
    if (nStepsInCycle>0)
      data.setPeriodsPerYear(nStepsInCycle);
    if (modelIdx==0) {
      RegressionModel lrModel=new RegressionModel("time");
      lrModel.init(data);
      return lrModel;
    }
    if (modelIdx==1) {
      PolynomialRegressionModel prModel=new PolynomialRegressionModel("time");
      prModel.init(data);
      return prModel;
    }
    else
    if (modelIdx==2) {
      SimpleExponentialSmoothingModel es1Model=null;
      if (Double.isNaN(alpha)) {
        try {
          es1Model =SimpleExponentialSmoothingModel.getBestFitModel(data);
        } catch (Exception ex) {
          return null;
        }
      }
      else {
        try {
          es1Model=new SimpleExponentialSmoothingModel(alpha);
        } catch (Exception ex) {
          return null;
        }
        es1Model.init(data);
      }
      setModel(es1Model);
      return es1Model;
    }
    if (modelIdx==3) {
      DoubleExponentialSmoothingModel es2Model=null;
      if (Double.isNaN(alpha) || Double.isNaN(gamma))  {
        try {
          es2Model =DoubleExponentialSmoothingModel.getBestFitModel(data);
        } catch (Exception ex) {
          return null;
        }
      }
      else  {
        try {
          es2Model=new DoubleExponentialSmoothingModel(alpha,gamma);
        } catch (Exception ex) {
          return null;
        }
        es2Model.init(data);
      }
      setModel(es2Model);
      return es2Model;
    }
    if (modelIdx==4) {
      TripleExponentialSmoothingModel es3Model =null;
      if (Double.isNaN(alpha) || Double.isNaN(beta) || Double.isNaN(gamma))  {
        try {
          es3Model =TripleExponentialSmoothingModel.getBestFitModel(data);
        } catch (Exception ex) {
          return null;
        }
      }
      else  {
        try {
          es3Model =new TripleExponentialSmoothingModel(alpha,beta,gamma);
        } catch (Exception ex) {
          return null;
        }
        es3Model.init(data);
      }
      setModel(es3Model);
      return es3Model;
    }
    return null;
  }

  /**
   * Stores model parameters, if any
   */
  public void setModel(ForecastingModel model) {
    if (model==null)
      return;
    if (model instanceof RegressionModel)
      modelIdx=0;
    else
    if (model instanceof PolynomialRegressionModel)
      modelIdx=1;
    else
    if (model instanceof SimpleExponentialSmoothingModel) {
      modelIdx=2;
      SimpleExponentialSmoothingModel esModel=(SimpleExponentialSmoothingModel)model;
      this.alpha=esModel.getAlpha();
    }
    else
    if (model instanceof DoubleExponentialSmoothingModel) {
      modelIdx=3;
      DoubleExponentialSmoothingModel esModel=(DoubleExponentialSmoothingModel)model;
      this.alpha=esModel.getAlpha();
      this.gamma=esModel.getGamma();
    }
    else
    if (model instanceof TripleExponentialSmoothingModel) {
      modelIdx=4;
      TripleExponentialSmoothingModel esModel=(TripleExponentialSmoothingModel)model;
      this.alpha=esModel.getAlpha();
      this.beta=esModel.getBeta();
      this.gamma=esModel.getGamma();
    }
  }

}
