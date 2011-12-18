package spade.analysis.tools.time_series;

import org.w3c.dom.Element;

import spade.lib.util.XMLUtil;

/**
 * Created by IntelliJ IDEA.
 * User: natascha
 * Date: Jul 15, 2011
 * Time: 11:16:16 AM
 * Keeps information about a modelled object
 */
public class ModelledObjectInfo {
  public String id=null;
  public String name=null;
  public String className=null;
  /**
   * The range of the original data
   */
  public double dataMin=Double.NaN, dataMax=Double.NaN;
  /**
   * The quartiles of the original data
   */
  public double dataQ1=Double.NaN, dataMedian=Double.NaN, dataQ3=Double.NaN;
  /**
   * The range, the mean, and the standard deviation of the residuals
   * (deviations of the real values from the model-predicted values)
   */
  public double resMin=0, resMax=0, resMean=0, resStDev=0;
  /**
   * Stores the information about the object in XML format
   */
  public String toXML() {
    String str="<Object id=\"" +id+"\"";
    if (name!=null)
      str+=" name=\""+name+"\"";
    if (className!=null)
      str+=" className=\""+className+"\"";
    if (!Double.isNaN(dataMin))
      str+=" minValue=\""+dataMin+"\"";
    if (!Double.isNaN(dataMax))
      str+=" maxValue=\""+dataMax+"\"";
    if (!Double.isNaN(dataQ1))
      str+=" q1Value=\""+dataQ1+"\"";
    if (!Double.isNaN(dataMedian))
      str+=" medianValue=\""+dataMedian+"\"";
    if (!Double.isNaN(dataQ3))
      str+=" q3Value=\""+dataQ3+"\"";
    if (!Double.isNaN(resMin))
      str+=" minResidual=\""+resMin+"\"";
    if (!Double.isNaN(resMax))
      str+=" maxResidual=\""+resMax+"\"";
    if (!Double.isNaN(resMean))
      str+=" meanResidual=\""+resMean+"\"";
    if (!Double.isNaN(resStDev))
      str+=" stDevResidual=\""+resStDev+"\"";
    str+=" />";
    return str;
  }
  /**
   * Restores itself (i.e. all necessary internal settings) from
   * the given element of an XML document. Returns true if successful.
   */
  public boolean restoreFromXML(Element elem) {
    if (elem==null) return false;
    if (!elem.getTagName().equalsIgnoreCase("Object")) return false;
    if (elem.hasAttribute("id")) id=elem.getAttribute("id");
    if (elem.hasAttribute("name")) name=elem.getAttribute("name");
    if (elem.hasAttribute("className")) className=elem.getAttribute("className");
    if (elem.hasAttribute("minValue"))
      dataMin= XMLUtil.getDouble(elem.getAttribute("minValue"));
    if (elem.hasAttribute("maxValue"))
      dataMax= XMLUtil.getDouble(elem.getAttribute("maxValue"));
    if (elem.hasAttribute("q1Value"))
      dataQ1= XMLUtil.getDouble(elem.getAttribute("q1Value"));
    if (elem.hasAttribute("medianValue"))
      dataMedian= XMLUtil.getDouble(elem.getAttribute("medianValue"));
    if (elem.hasAttribute("q3Value"))
      dataQ3= XMLUtil.getDouble(elem.getAttribute("q3Value"));
    if (elem.hasAttribute("minResidual"))
      resMin= XMLUtil.getDouble(elem.getAttribute("minResidual"));
    if (elem.hasAttribute("maxResidual"))
      resMax= XMLUtil.getDouble(elem.getAttribute("maxResidual"));
    if (elem.hasAttribute("meanResidual"))
      resMean= XMLUtil.getDouble(elem.getAttribute("meanResidual"));
    if (elem.hasAttribute("stDevResidual"))
      resStDev= XMLUtil.getDouble(elem.getAttribute("stDevResidual"));
    return id!=null;
  }
}
