package spade.analysis.tools.time_series;

import org.w3c.dom.Element;

import java.io.StringWriter;
import java.util.StringTokenizer;

import spade.lib.util.XMLUtil;
import net.sourceforge.openforecast.ForecastingModel;
import net.sourceforge.openforecast.DataSet;
import net.sourceforge.openforecast.Observation;
import net.sourceforge.openforecast.models.*;

/**
 * Created by IntelliJ IDEA.
 * User: natascha
 * Date: Aug 19, 2011
 * Time: 3:36:28 PM
 * Describes a model of dependency between two attributes.
 */
public class DependencyModelDescriptor {
  /**
   * Names of the possible types of models
   */
  public static final String modelNames[] = {
      "linear regression",                             // 0
      "polynomial regression"                          // 1
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
   * The names of the independent and dependent variables
   */
  public String indepVarName=null, depVarName=null;
  /**
   * The values of the independent attribute (parameter) used for model building.
   */
  public double indepValues[]=null;
  /**
   * The values of the dependent attribute used for model building
   */
  public double depAttrValues[]=null;
  /**
   * The minimum and maximum values of the independent variable (parameter)
   * for which this model has been built.
   */
  public double minIndepValue=Double.NaN, maxIndepValue=Double.NaN;
  /**
   * The order of the polynomial, for the polynomial regression model
   */
  public int polyOrder=-1;
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
  public DependencyModelDescriptor getCopy(DependencyModelDescriptor md) {
    if (md==null)
      md=new DependencyModelDescriptor();
    if (modelIdx>=0) md.modelIdx=modelIdx;
    if (polyOrder>0)
      md.polyOrder=polyOrder;
    if (className!=null) md.className=className;
    md.classColorRed=classColorRed;
    md.classColorGreen=classColorGreen;
    md.classColorBlue=classColorBlue;
    if (indepVarName!=null)
      md.indepVarName=indepVarName;
    if (depVarName!=null)
      md.depVarName=depVarName;
    if (depAttrValues !=null) md.depAttrValues =depAttrValues;
    if (indepValues!=null) md.indepValues=indepValues;
    if (!Double.isNaN(minIndepValue) && !Double.isNaN(maxIndepValue)) {
      md.minIndepValue=minIndepValue;
      md.maxIndepValue=maxIndepValue;
    }
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
    if (modelIdx<0 || depAttrValues ==null || indepValues==null)
      return null;
    StringWriter writer = new StringWriter();
    writer.write("  <NumDepModel type=\""+modelNames[modelIdx]+"\"");
    if (polyOrder>0)
      writer.write(" order=\""+polyOrder+"\"");
    if (className!=null) {
      writer.write(" className=\""+className+"\"");
      if (classColorRed>0 || classColorGreen>0 || classColorBlue>0)
        writer.write(" classColorRGB=\"("+classColorRed+","+classColorGreen+","+
            classColorBlue+")\"");
    }
    if (indepVarName!=null)
      writer.write(" independentVariable=\""+indepVarName+"\"");
    if (depVarName!=null)
      writer.write(" dependentVariable=\""+depVarName+"\"");
    writer.write(" >\r\n");
    writer.write("    <IndependentValues> ");
    for (int i=0; i<indepValues.length; i++) {
      writer.write(((i==0)?"":";")+indepValues[i]);
    }
    writer.write(" </IndependentValues>\r\n");
    writer.write("    <DependentValues> ");
    for (int i=0; i<depAttrValues.length; i++) {
      writer.write(((i==0)?"":";")+depAttrValues[i]);
    }
    writer.write(" </DependentValues>\r\n");
    if (!Double.isNaN(minIndepValue) && !Double.isNaN(maxIndepValue)) {
      writer.write("    <Limits");
      writer.write(" min=\""+minIndepValue+"\"");
      writer.write(" max=\""+maxIndepValue+"\"");
      writer.write(" />\r\n");
    }
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
    writer.write("  </NumDepModel>");
    return writer.toString();
  }
  /**
   * Restores itself (i.e. all necessary internal settings) from
   * the given element of an XML document. Returns true if successful.
   */
  public boolean restoreFromXML(Element elem) {
    if (elem==null) return false;
    if (!elem.getTagName().equalsIgnoreCase("NumDepModel")) return false;
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
    if (elem.hasAttribute("order"))
      polyOrder=XMLUtil.getInt(elem.getAttribute("order"));
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

    if (elem.hasAttribute("independentVariable"))
      indepVarName=elem.getAttribute("independentVariable");
    if (elem.hasAttribute("dependentVariable"))
      depVarName=elem.getAttribute("dependentVariable");

    Element dataElem = XMLUtil.getElementByTagName(elem.getChildNodes(),"IndependentValues");
    if (dataElem ==null) return false;
    String dataStr=XMLUtil.getTextFromNode(dataElem);
    if (dataStr==null)
      return false;
    StringTokenizer tok=new StringTokenizer(dataStr," ;");
    int nTok=tok.countTokens();
    if (nTok<2)
      return false;
    indepValues=new double[nTok];
    int nValidValues=0;
    for (int i=0; i<nTok; i++) {
      indepValues[i]=XMLUtil.getDouble(tok.nextToken());
      if (!Double.isNaN(indepValues[i]))
        ++nValidValues;
    }
    if (nValidValues<2)
      return false;

    dataElem = XMLUtil.getElementByTagName(elem.getChildNodes(),"DependentValues");
    if (dataElem ==null) return false;
    dataStr=XMLUtil.getTextFromNode(dataElem);
    if (dataStr==null)
      return false;
    tok=new StringTokenizer(dataStr," ;");
    nTok=tok.countTokens();
    if (nTok<2)
      return false;
    depAttrValues =new double[nTok];
    nValidValues=0;
    for (int i=0; i<nTok; i++) {
      depAttrValues[i]=XMLUtil.getDouble(tok.nextToken());
      if (!Double.isNaN(depAttrValues[i]))
        ++nValidValues;
    }
    if (nValidValues<2)
      return false;

    Element subElem=XMLUtil.getElementByTagName(elem.getChildNodes(),"Limits");
    if (subElem!=null) {
      if (subElem.hasAttribute("min"))
        minIndepValue=XMLUtil.getDouble(subElem.getAttribute("min"));
      if (subElem.hasAttribute("max"))
        maxIndepValue=XMLUtil.getDouble(subElem.getAttribute("max"));
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

    return modelIdx>=0 && depAttrValues !=null && indepValues!=null;
  }
  /**
   * Reconstructs the model using the original data and model parameters
   */
  public ForecastingModel getModel() {
    if (modelIdx<0)
      return null;
    if (depAttrValues ==null || indepValues==null)
      return null;
    DataSet data=new DataSet();
    String ivName=(indepVarName==null)?"time":indepVarName;
    data.setTimeVariable(ivName);
    for (int i=0; i<Math.min(depAttrValues.length,indepValues.length); i++) {
      Observation dataPoint=new Observation(depAttrValues[i]);
      dataPoint.setIndependentValue(ivName,indepValues[i]);
      data.add(dataPoint);
    }
    if (modelIdx==0) {
      RegressionModel lrModel=new RegressionModel(ivName);
      lrModel.init(data);
      return lrModel;
    }
    if (modelIdx==1) {
      if (polyOrder<1) polyOrder=2;
      PolynomialRegressionModel prModel=new PolynomialRegressionModel(ivName,polyOrder);
      prModel.init(data);
      return prModel;
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
    if (model instanceof PolynomialRegressionModel) {
      polyOrder=((PolynomialRegressionModel)model).getOrder();
      modelIdx=1;
    }
  }
}
