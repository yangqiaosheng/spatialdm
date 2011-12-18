package spade.analysis.tools.time_series;

import net.sourceforge.openforecast.ForecastingModel;

import java.io.StringWriter;
import java.util.Vector;
import java.util.StringTokenizer;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import spade.lib.util.XMLUtil;

/**
 * Created by IntelliJ IDEA.
 * User: natascha
 * Date: Jun 30, 2011
 * Time: 3:03:39 PM
 * Describes a Holt-Winters model with single time cycle or
 * two time cycles, one including another.
 */
public class HWModelDescriptor extends TSModelDescriptor {
  /**
   * In case of two temporal cycles, there are two possibilities:
   * 1) A HW model is created separately for each position of the inner cycle;
   *    the modelling is applied to the values in this position over the outer cycle.
   *    E.g. 0 o'clock of each day, 1 o'clock of each day, and so on.
   * 2) A HW model is created separately for each inner cycle within the outer cycle.
   *    To create the model, several inner cycles standing in the same position
   *    in the outer cycle are concatenated.
   *    E.g., all Mondays, all Tuesdays, etc.
   */
  public static final int
      HW_single_cycle=0,
      HW_for_inner_cycle_positions=1,
      HW_for_inner_cycles=2;

  public static final int modWays[] = {
      HW_single_cycle,HW_for_inner_cycle_positions,HW_for_inner_cycles
  };

  public static final String modWayTexts[] = {
    "single cycle","models for inner cycle positions","models for inner cycles"
  };

  public static String getModelingWayAsString (int wayToModel) {
    for (int i=0; i<modWays.length; i++)
      if (wayToModel==modWays[i])
        return modWayTexts[i];
    return "none";
  }

  public static int getModellingWay (String text) {
    if (text==null)
      return -1;
    for (int i=0; i<modWays.length; i++)
      if (text.equalsIgnoreCase(modWayTexts[i]))
        return modWays[i];
    return -1;
  }

  /**
   * Number of cycles: one or two
   */
  public int nCycles=1;
  /**
   * In case of two temporal cycles, the length of the inner cycle, in elementary time steps;
   * or the length of the cycle in case of a single cycle
   */
  public int innerCycleLength=0;
  /**
   * The length of the outer cycle, i.e., the number of inner cycles it consists of
   */
  public int nInnerCyclesInOuter=0;
  /**
   * One of the HW_single_cycle, HW_for_inner_cycle_positions, or HW_for_inner_cycles
   */
  public int wayToModel=HW_single_cycle;
  /**
   * Describe one (in case of a single cycle) or more (in case of two cycles)
   * Holt-Winters models
   */
  public TSModelDescriptor hwModelDescr[]=null;
  /**
   * Copies all internal fields in the provided structure.
   * If the argument is null, the structure is created.
   * Returns the reference to the original or created structure.
   */
  public TSModelDescriptor getCopy (TSModelDescriptor md) {
    if (md==null)
      md=new HWModelDescriptor();
    super.getCopy(md);
    if (!(md instanceof HWModelDescriptor))
      return md;
    HWModelDescriptor hwd=(HWModelDescriptor)md;
    /*
    hwd.startTimeValue=startTimeValue;
    hwd.cycleStart=cycleStart;
    hwd.nValSinceCycleStart=nValSinceCycleStart;
    hwd.dataStart=dataStart;
    */
    if (nCycles>0) hwd.nCycles=nCycles;
    if (innerCycleLength>0) hwd.innerCycleLength=innerCycleLength;
    if (nInnerCyclesInOuter>0) hwd.nInnerCyclesInOuter=nInnerCyclesInOuter;
    hwd.wayToModel=wayToModel;
    if (hwModelDescr!=null && hwModelDescr.length>0) {
      hwd.hwModelDescr=new TSModelDescriptor[hwModelDescr.length];
      for (int i=0; i<hwModelDescr.length; i++)
        hwd.hwModelDescr[i]=
            (hwModelDescr[i]==null)?null:hwModelDescr[i].getCopy(hwd.hwModelDescr[i]);
    }
    return hwd;
  }
  /**
   * Stores the information about the model in XML format
   */
  public String toXML() {
    if (hwModelDescr==null)
      return null;
    StringWriter writer = new StringWriter();
    writer.write("<CyclicVariationModel nTimeCycles=\""+nCycles+"\"");
    if (nCycles>1)
      writer.write(" approach=\""+getModelingWayAsString(wayToModel)+"\"");
    if (className!=null) {
      writer.write(" className=\""+className+"\"");
      if (classColorRed>0 || classColorGreen>0 || classColorBlue>0)
        writer.write(" classColorRGB=\"("+classColorRed+","+classColorGreen+","+
            classColorBlue+")\"");
    }
    writer.write(" >\r\n");
    if (nCycles==1)
      writer.write("  <NStepsInCycle>"+nStepsInCycle+"</NStepsInCycle>\r\n");
    else {
      writer.write("  <InnerCycleLength>"+innerCycleLength+"</InnerCycleLength>\r\n");
      writer.write("  <NInnerCyclesInOuter>"+nInnerCyclesInOuter+"</NInnerCyclesInOuter>\r\n");
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
    for (int i=0; i<hwModelDescr.length; i++) {
      String str=hwModelDescr[i].toXML();
      if (str!=null)
        writer.write(str+"\r\n");
    }
    writer.write("</CyclicVariationModel>");
    return writer.toString();
  }
  /**
   * Restores itself (i.e. all necessary internal settings) from
   * the given element of an XML document. Returns true if successful.
   */
  public boolean restoreFromXML(Element elem) {
    if (elem==null) return false;
    if (!elem.getTagName().equalsIgnoreCase("CyclicVariationModel")) return false;
    modelIdx=4;    
    if (elem.hasAttribute("nTimeCycles"))
      nCycles= XMLUtil.getInt(elem.getAttribute("nTimeCycles"));
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
    if (elem.hasAttribute("approach")) {
      String appTxt =elem.getAttribute("approach");
      if (appTxt ==null)
        return false;
      wayToModel=getModellingWay(appTxt);
      if (wayToModel<0)
        return false;
    }
    NodeList children=elem.getChildNodes();
    Vector<TSModelDescriptor> vModels=new Vector<TSModelDescriptor>(10,10);
    for (int i =0; i <children.getLength(); i++)
      if (children.item(i)!=null &&
          children.item(i).getNodeType()== Node.ELEMENT_NODE) {
        Element child=(Element)children.item(i);
        if (child.getTagName().equalsIgnoreCase("NStepsInCycle"))
          nStepsInCycle=XMLUtil.getInt(XMLUtil.getTextFromNode(child));
        else
        if (child.getTagName().equalsIgnoreCase("InnerCycleLength"))
          innerCycleLength=XMLUtil.getInt(XMLUtil.getTextFromNode(child));
        else
        if (child.getTagName().equalsIgnoreCase("NInnerCyclesInOuter"))
          nInnerCyclesInOuter=XMLUtil.getInt(XMLUtil.getTextFromNode(child));
        else
        if (child.getTagName().equalsIgnoreCase("Statistics")) {
          if (child.hasAttribute("minValue"))
            modMin=XMLUtil.getDouble(child.getAttribute("minValue"));
          if (child.hasAttribute("maxValue"))
            modMax=XMLUtil.getDouble(child.getAttribute("maxValue"));
          if (child.hasAttribute("q1Value"))
            modQ1=XMLUtil.getDouble(child.getAttribute("q1Value"));
          if (child.hasAttribute("medianValue"))
            modMedian=XMLUtil.getDouble(child.getAttribute("medianValue"));
          if (child.hasAttribute("q3Value"))
            modQ3=XMLUtil.getDouble(child.getAttribute("q3Value"));
        }
        else
        if (child.getTagName().equalsIgnoreCase("NumTSModel")) {
          TSModelDescriptor md=new TSModelDescriptor();
          if (md.restoreFromXML(child))
            vModels.addElement(md);
        }
      }
    if (vModels.size()<1)
      return false;
    hwModelDescr=new TSModelDescriptor[vModels.size()];
    for (int i=0; i<vModels.size(); i++)
      hwModelDescr[i]=vModels.elementAt(i);
    return nCycles>0 && hwModelDescr!=null;
  }
  /**
   * Reconstructs the model using the original data and model parameters
   */
  public ForecastingModel getModel() {
    ForecastingModel model=HWModel.retrieveHWModel(this);
    if (model!=null)
      return model;
    return super.getModel();
  }
  /**
   * Stores model parameters, if any
   */
  public void setModel(ForecastingModel model) {
    if (model==null)
      return;
    if (!(model instanceof HWModel)) {
      super.setModel(model);
      return;
    }
    HWModel hwModel=(HWModel)model;
    if (hwModel.hwd !=null)
      hwModel.hwd.getCopy(this);
    modelIdx=4;
  }
}
