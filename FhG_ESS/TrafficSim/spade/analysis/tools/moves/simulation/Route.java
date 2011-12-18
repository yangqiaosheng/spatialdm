package spade.analysis.tools.moves.simulation;

import spade.time.TimeMoment;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: natascha
 * Date: Nov 15, 2011
 * Time: 3:16:01 PM
 * Contains the list of the route nodes in the chronological order
 */
public class Route {
  /**
   * The list of the route nodes (visited places) in the chronological order
   */
  public RouteNode[] places=null;
  /**
   * The times of arriving in each place
   */
  public TimeMoment[] arriveTimes =null;
  /**
   * The times of leaving each place
   */
  public TimeMoment[] leaveTimes =null;
  /**
   * The index of the current place (in simulation)
   */
  public int nCurr=0;

  public String toString() {
    if (places==null || places.length<0)
      return "null";
    String str="";
    for (int i=0; i<places.length; i++) {
      if (i>0) str+=";";
      if (places[i]==null)
        str+="null";
      else
        str+=places[i].placeId;
    }
    return str;
  }

}
