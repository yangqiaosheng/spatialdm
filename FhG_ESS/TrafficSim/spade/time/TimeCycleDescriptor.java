package spade.time;

import org.w3c.dom.Element;
import spade.lib.util.XMLUtil;

/**
 * Created by IntelliJ IDEA.
 * User: natascha
 * Date: Jun 29, 2011
 * Time: 3:58:05 PM
 * Defines existing temporal cycles: daily, weekly, etc.
 */
public class TimeCycleDescriptor {
  public static String cycleNames[]={
      "daily","weekly","yearly"
  };
  public static char units[]={'y','m','d','h','t','s','w'};
  public static char unitsCap[]={'Y','M','D','H','T','S','W'};
  public static String unitNames[]={"year","month","day","hour","minute","second","week"};
  public static String unitNamesPlural[]={"years","months","days","hours","minutes","seconds","weeks"};

  /**
   * The name of the time cycle, one of the cycleNames
   */
  public String cycleName=null;
  /**
   * The unit in which the cycle length and step length is measured;
   *   one of Date.time_symbols ('y','m','d','h','t','s') or 'w' for "week".
   */
  public char timeUnit=0;
  /**
   * The length of one time cycle, in the specified time units.
   * E.g., the length of the yearly cycle in months is 12, in weeks 52,
   * and in days 365.
   */
  public int cycleLength=0;
  /**
   * The length of the time intervals (steps) in which the cycle is divided
   */
  public int stepLength=0;
  /**
   * The number of time steps in the cycle.
   */
  public int nStepsInCycle =0;
  /**
   * The position in the cycle from which the data start.
   * E.g., there may be a yearly (seasonal) cycle but the chosen start
   * positon of the cycle may be not necessarily 1st of January but
   * 1st of September or another month.
   * The position is specified w.r.t. the time unit.
   * 0 means that the start position coincides with the cycle start.
   * The maximal possible value is cycleLength-1.
   */
  public int startShift =0;

  public Object clone() {
    TimeCycleDescriptor tcd=new TimeCycleDescriptor();
    tcd.cycleName=cycleName;
    tcd.timeUnit=timeUnit;
    tcd.cycleLength=cycleLength;
    tcd.stepLength=stepLength;
    tcd.nStepsInCycle=nStepsInCycle;
    tcd.startShift=startShift;
    return tcd;
  }

  /**
   * Stores the information about the temporal cycle in XML format
   */
  public String toXML() {
    String str="<TimeCycle character=\"" +cycleName+
        "\" unit=\""+getTimeUnitName(timeUnit)+
        "\" cycleLength=\""+cycleLength+
        "\" stepLength=\""+stepLength+
        "\" nStepsInCycle=\""+nStepsInCycle+
        "\" startShift=\""+startShift+"\" />";
    return str;
  }

  public String toString () {
    return toXML();
  }

  /**
   * Restores itself (i.e. all necessary internal settings) from
   * the given element of an XML document. Returns true if successful.
   */
  public boolean restoreFromXML(Element elem) {
    if (elem==null) return false;
    if (!elem.getTagName().equalsIgnoreCase("TimeCycle")) return false;
    if (elem.hasAttribute("character")) {
      cycleName=elem.getAttribute("character");
      if (!isValidCycleName(cycleName))
        return false;
    }
    if (elem.hasAttribute("unit")) {
      timeUnit=getTimeUnit(elem.getAttribute("unit"));
      if (timeUnit==0 || !isValidTimeUnit(timeUnit))
        return false;
    }
    if (elem.hasAttribute("cycleLength"))
      cycleLength=XMLUtil.getInt(elem.getAttribute("cycleLength"));
    if (elem.hasAttribute("stepLength"))
      stepLength=XMLUtil.getInt(elem.getAttribute("stepLength"));
    if (elem.hasAttribute("nStepsInCycle"))
      nStepsInCycle=XMLUtil.getInt(elem.getAttribute("nStepsInCycle"));
    if (elem.hasAttribute("startShift"))
      startShift=XMLUtil.getInt(elem.getAttribute("startShift"));
    return cycleName!=null && cycleLength>0;
  }

  public static boolean isValidCycleName (String cName) {
    if (cName==null) return false;
    for (int i=0; i<cycleNames.length; i++)
      if (cName.equalsIgnoreCase(cycleNames[i]))
        return true;
    return false;
  }

  public static boolean isValidTimeUnit (char timeUnit) {
    return getTimeUnitIdx(timeUnit)>=0;
  }

  public static int getTimeUnitIdx(char timeUnit) {
    for (int i=0; i<units.length; i++)
      if (timeUnit==units[i] || timeUnit==unitsCap[i])
        return i;
    return -1;
  }

  public static String getTimeUnitName (char timeUnit) {
    for (int i=0; i<units.length; i++)
      if (timeUnit==units[i] || timeUnit==unitsCap[i])
        return unitNames[i];
    return null;
  }
  public static String getTimeUnitNamePlural (char timeUnit) {
    for (int i=0; i<units.length; i++)
      if (timeUnit==units[i] || timeUnit==unitsCap[i])
        return unitNamesPlural[i];
    return null;
  }
  public static char getTimeUnit(String unitName) {
    if (unitName==null) return 0;
    for (int i=0; i<units.length; i++)
      if (unitName.equalsIgnoreCase(unitNames[i]) ||
          unitName.equalsIgnoreCase(unitNamesPlural[i]))
        return units[i];
    return 0;
  }

  /**
   * Returns the length of the specified cycle measured in the specified time units
   */
  public static int getCycleLength (String cycleName, char timeUnit) {
    if (!isValidTimeUnit(timeUnit))
      return 0;
    int unitIdx=Date.getTimeUnitIdx(timeUnit);
    int cycleLen=0;
    if (cycleName.equalsIgnoreCase("daily")) {
      if (unitIdx<3) return 0;
      cycleLen=24;
      if (unitIdx>3) //minutes or seconds
        cycleLen*=60;
      if (unitIdx>4) //seconds
        cycleLen*=60;
    }
    else
    if (cycleName.equalsIgnoreCase("weekly")) {
      if (unitIdx<2) return 0;
      cycleLen=7;
      if (unitIdx>2) //hours or minutes or seconds
        cycleLen*=24;
      if (unitIdx>3) //minutes or seconds
        cycleLen*=60;
      if (unitIdx>4) //seconds
        cycleLen*=60;
    }
    else
    if (cycleName.equalsIgnoreCase("yearly")) {
      if (unitIdx<0)  //it should be 'w' - for "week"
        cycleLen=52;
      else {
        if (unitIdx<1) return 0;
        if (unitIdx==1) //month
          cycleLen=12;
        else {
          cycleLen=365; //days in a year
          if (unitIdx>2) //hours or minutes or seconds
            cycleLen*=24;
          if (unitIdx>3) //minutes or seconds
            cycleLen*=60;
          if (unitIdx>4) //seconds
            cycleLen*=60;
        }
      }
    }
    return cycleLen;
  }

  /**
   * Computes the number of time intervals (steps) of the given length in the given time cycle
   * @param cycleName - the name of the cycle, one of "daily","weekly","yearly"
   * @param stepLength - the length of the time intervals (steps) in which the cycle is divided
   * @param timeUnit - the unit in which the step length is measured;
   *   one of Date.time_symbols ('y','m','d','h','t','s') or 'w' for "week".
   * @return the number of intervals
   */
  public static int getNIntervals (String cycleName, int stepLength, char timeUnit) {
    return getCycleLength(cycleName,timeUnit)/stepLength;
  }

  /**
   * Computes the position of the given date in the specified cycle
   * @param d - the date
   * @param cycleName - the name of the cycle, one of "daily","weekly","yearly"
   * @param stepLength - the length of the time intervals (steps) in which the cycle is divided
   * @param timeUnit - the unit in which the step length is measured;
   *   one of Date.time_symbols ('y','m','d','h','t','s') or 'w' for "week".
   * @param startPos - the chosen start positon of the cycle
   * @return the position of the given date in the cycle, starting from 0.
   *   If invalid, returns -1.
   */
  public static int getPosInCycle (Date d, String cycleName,
                                   int stepLength, char timeUnit, int startPos) {
    if (d==null) return -1;
    if (!isValidTimeUnit(timeUnit))
      return -1;
    if (!isValidCycleName(cycleName))
      return -1;
    if (stepLength <1)
      return -1;
    Date d0=(Date)d.getCopy();
    d0.setElementValue('s',0);
    d0.setElementValue('t',0);
    d0.setElementValue('h',0);
    int unitIdx=Date.getTimeUnitIdx(timeUnit);
    if (cycleName.equalsIgnoreCase("daily")) {
      if (unitIdx<3) return -1;
    }
    else
    if (cycleName.equalsIgnoreCase("weekly")) {
      if (unitIdx<2) return -1;
      int dow=d0.getDayOfWeek();
      if (dow>1) {
        char p=d0.getPrecision();
        d0.setPrecision('d');
        d0.add(-dow+1);
        d0.setPrecision(p);
      }
    }
    else
    if (cycleName.equalsIgnoreCase("yearly")) {
      if (unitIdx<1 && timeUnit!='w') return -1;
      d0.setElementValue('d',1);
      d0.setElementValue('m',1);
      if (timeUnit=='w') {
        int dow=d0.getDayOfWeek();
        if (dow>1) {
          char p=d0.getPrecision();
          d0.setPrecision('d');
          d0.add(-dow+1);
          d0.setPrecision(p);
        }
      }
    }
    long diff=(unitIdx>0)?d.subtract(d0,timeUnit):d.subtract(d0,'d');
    if (timeUnit=='w')
      diff/=7;
    int pos=(int)(diff/stepLength);
    if (startPos>0) {
      pos-=startPos;
      if (pos<0)
        pos+=getCycleLength(cycleName,timeUnit);
    }
    return pos;
  }
  /**
   * Tries to determine if the given time range contains at least one full cycle
   * with the specified name. If so, returns the description of the cycle
   */
  public static TimeCycleDescriptor getCycle (String cycleName, Date d0, Date dLast, int nSteps) {
    if (cycleName.equalsIgnoreCase("daily"))
      return getDailyCycle(d0,dLast,nSteps);
    if (cycleName.equalsIgnoreCase("weekly"))
      return getWeeklyCycle(d0,dLast,nSteps);
    if (cycleName.equalsIgnoreCase("yearly"))
      return getYearlyCycle(d0,dLast,nSteps);
    return null;
  }
  /**
   * Tries to determine if the given time range contains at least one full yearly cycle.
   * If so, returns the description of the cycle
   */
  public static TimeCycleDescriptor getYearlyCycle (Date d0, Date dLast, int nSteps) {
    if (d0==null || dLast==null || nSteps<2)
      return null;
    if (!d0.hasElement('y') || !dLast.hasElement('y'))
      return null;
    if (!d0.hasElement('m') || !dLast.hasElement('m'))
      return null;
    long diff=dLast.subtract(d0);
    if (diff+1<nSteps)
      return null;
    long stepLen=diff/(nSteps-1);
    Date dNext=(Date)dLast.getCopy();
    dNext.add(stepLen);

    long nMonths=dNext.subtract(d0,'m');
    if (nMonths<12)
      return null;

    TimeCycleDescriptor tcd=new TimeCycleDescriptor();
    tcd.cycleName="yearly";
    tcd.cycleLength=getCycleLength("yearly",'m');
    tcd.timeUnit=d0.getPrecision();
    Date d1=(Date)d0.getCopy();
    d1.setPrecision('y');
    d1.add(1);
    d1.setPrecision(tcd.timeUnit);
    long yearLen=d1.subtract(d0);
    tcd.nStepsInCycle =(int)Math.round(1.0*yearLen/stepLen);
    if (!d0.hasElement('d') || d0.getElementValue('d')==1) { //cycle of months in year?
      tcd.stepLength=tcd.cycleLength/tcd.nStepsInCycle;
      if (tcd.stepLength*tcd.nStepsInCycle ==tcd.cycleLength) {
        tcd.timeUnit='m';
        tcd.startShift=d0.getElementValue('m')-1;
        if (tcd.startShift>0)
          tcd.startShift=tcd.cycleLength-tcd.startShift;
        return tcd;
      }
    }
    if (!d0.hasElement('d'))
      return null;
    tcd.stepLength=(int)dNext.subtract(dLast,'d');
    if (tcd.stepLength%7==0) {
      //cycle of weeks in year
      tcd.timeUnit='w';
      tcd.cycleLength=getCycleLength("yearly",'w');
      tcd.startShift=Math.max(0,d0.getWeekOfYear()-1);
      if (tcd.startShift>=tcd.cycleLength)
        tcd.startShift=0;
      if (tcd.startShift>0)
        tcd.startShift=tcd.cycleLength-tcd.startShift;
    }
    else {
      //cycle of days in year
      tcd.timeUnit='d';
      tcd.cycleLength=getCycleLength("yearly",'d');
      tcd.startShift=Math.max(0,d0.getDayOfYear()-1);
      if (tcd.startShift>0)
        tcd.startShift=tcd.cycleLength-tcd.startShift;
    }
    return tcd;
  }
  /**
   * Tries to determine if the given time range contains at least one full weekly cycle.
   * If so, returns the description of the cycle
   */
  public static TimeCycleDescriptor getWeeklyCycle (Date d0, Date dLast, int nSteps) {
    if (d0==null || dLast==null || nSteps<2)
      return null;
    if (!d0.hasElement('d') || !dLast.hasElement('d'))
      return null;
    if (d0.useElement('h') && d0.getElementValue('h')>0)
      return null;
    if (d0.useElement('t') && d0.getElementValue('t')>0)
      return null;
    if (d0.useElement('s') && d0.getElementValue('s')>0)
      return null;

    long diff=dLast.subtract(d0);
    if (diff+1<nSteps)
      return null;
    long stepLen=diff/(nSteps-1);
    Date dNext=(Date)dLast.getCopy();
    dNext.add(stepLen);
    long nDays=dNext.subtract(d0,'d');
    if (nDays<7)
      return null;
    if (nDays==nSteps) {
      //cycle of days in week
      TimeCycleDescriptor tcd=new TimeCycleDescriptor();
      tcd.cycleName="weekly";
      tcd.timeUnit='d';
      tcd.cycleLength=7;
      tcd.nStepsInCycle=7;
      tcd.stepLength=1;
      tcd.startShift=Math.max(0,d0.getDayOfWeek()-1);
      if (tcd.startShift>0)
        tcd.startShift=tcd.cycleLength-tcd.startShift;
      return tcd;
    }
    TimeCycleDescriptor tcd=getDailyCycle(d0,dLast,nSteps);
    if (tcd==null)
      return null;
    tcd.cycleName="weekly";
    int shift=Math.max(0,d0.getDayOfWeek()-1);
    if (shift>0)
      shift=7-shift;
    tcd.startShift+=shift*tcd.cycleLength;
    tcd.cycleLength*=7;
    tcd.nStepsInCycle*=7;
    return tcd;
  }
  /**
   * Tries to determine if the given time range contains at least one full daily cycle.
   * If so, returns the description of the cycle
   */
  public static TimeCycleDescriptor getDailyCycle (Date d0, Date dLast, int nSteps) {
    if (d0==null || dLast==null || nSteps<2)
      return null;
    if (!d0.hasElement('d') || !dLast.hasElement('d'))
      return null;
    if (!d0.hasElement('h') || !dLast.hasElement('h'))
      return null;
    long diff=dLast.subtract(d0);
    if (diff+1<nSteps)
      return null;
    long stepLen=diff/(nSteps-1);
    Date dNext=(Date)dLast.getCopy();
    dNext.add(stepLen);
    long nDays=dNext.subtract(d0,'d');
    if (nDays<1)
      return null;
    long nHours=dNext.subtract(d0,'h');
    if (nHours<24)
      return null;
    TimeCycleDescriptor tcd=new TimeCycleDescriptor();
    tcd.cycleName="daily";
    tcd.timeUnit=d0.getPrecision();
    Date d1=(Date)d0.getCopy();
    d1.setPrecision('d');
    d1.add(1);
    d1.setPrecision(tcd.timeUnit);
    long dayLen=d1.subtract(d0);
    long nStepsPerDay=Math.round(1.0*dayLen/stepLen);
    if (nStepsPerDay*stepLen!=dayLen)
      return null;

    tcd.cycleLength=getCycleLength("daily",tcd.timeUnit);
    tcd.nStepsInCycle=(int)nStepsPerDay;
    tcd.stepLength=tcd.cycleLength/tcd.nStepsInCycle;
    tcd.startShift=0;
    if (d0.getElementValue('h')>0 ||
        (d0.hasElement('t') && d0.getElementValue('t')>0) ||
        (d0.hasElement('s') && d0.getElementValue('s')>0)) {
      Date d00=(Date)d0.getCopy();
      d00.setElementValue('h',0);
      if (d0.hasElement('t'))
        d00.setElementValue('t',0);
      if (d0.hasElement('s'))
        d00.setElementValue('s',0);
      tcd.startShift=(int)d0.subtract(d00,tcd.timeUnit);
      if (tcd.startShift>0)
        tcd.startShift=tcd.cycleLength-tcd.startShift;
    }
    return tcd;
  }
}
