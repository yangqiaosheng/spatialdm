package spade.analysis.tools.moves.simulation;

import spade.lib.util.IntArray;
import spade.time.TimeMoment;

import java.util.Vector;
import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: natascha
 * Date: Nov 15, 2011
 * Time: 3:27:45 PM
 * Represents a place in the movement simulation
 */
public class Place {
  /**
   * The identifier of the place (same as in the geo layer with the places)
   */
  public String id=null;
  /**
   * The links originating from this place.
   */
  public Vector<Link> links=null;
  /**
   * The load of this place on the current simulation time step
   */
  public int currLoad =0;
  /**
   * The base loads of this place on all simulation steps
   */
  public IntArray baseLoads =null;
  /**
   * The pending routes from this place. For each outgoing link, there is
   * a vector of routes that will go through this link.
   */
  public Vector<Route>[] routesOut =null;
  /**
   * The routes that end in this place
   */
  public Vector<Route> routesEnd=null;

  public int findLinkByDestId(String destId) {
    if (links==null || destId==null)
      return -1;
    for (int i=0; i<links.size(); i++)
      if (destId.equals(links.elementAt(i).destId))
        return i;
    return -1;
  }

  public void addLink (Link lnk) {
    if (lnk==null || lnk.origId==null || !lnk.origId.equals(id) ||
        lnk.destId==null)
      return;
    if (findLinkByDestId(lnk.destId)>=0)
      return;
    if (links==null)
      links=new Vector<Link>(10,10);
    links.addElement(lnk);
  }

  public void setCurrLoad(int currLoad) {
    this.currLoad =currLoad;
  }
  /**
   * Adds the given load to the current load
   */
  public void addLoad(int load) {
    this.currLoad +=load;
  }
  /**
   * Stores the current load in the array of base loads for all simulation steps
   */
  public void storeCurrLoad (int simStep) {
    if (baseLoads ==null)
      baseLoads =new IntArray(300,100);
    for (int i=baseLoads.size(); i<simStep; i++)
      baseLoads.addElement(0);
    if (simStep<baseLoads.size())
      baseLoads.setElementAt(currLoad,simStep);
    else
      baseLoads.addElement(currLoad);
  }

  /**
   * Creates "routes", i.e., structures from which trajectories will be later generated.
   * @param places - the sequence of places to be visited
   * @param nTimes - the number of times this route will be followed,
   *   i.e., the number of trajectories that need to be generated
   * @param tStart - the start time moment of the simulation.
   */
  public void addRoutePlan(RouteNode[] places, int nTimes, TimeMoment tStart) {
    if (places==null || places.length<2 || nTimes<1)
      return;
    if (!places[0].placeId.equals(id))
      return; //not a route from this place!
    int lIdx=findLinkByDestId(places[1].placeId);
    if (lIdx<0)
      return; //no suitable link for this route!
    if (routesOut ==null) {
      routesOut =new Vector[links.size()];
      for (int i=0; i<routesOut.length; i++)
        routesOut[i]=null;
    }
    if (routesOut[lIdx]==null)
      routesOut[lIdx]=new Vector<Route>(Math.max(1000,nTimes*100),1000);
    for (int i=0; i<nTimes; i++) {
      Route r=new Route();
      r.places=places;
      r.arriveTimes =new TimeMoment[places.length];
      r.leaveTimes =new TimeMoment[places.length];
      for (int j=0; j<r.arriveTimes.length; j++) {
        r.arriveTimes[j]=null;
        r.leaveTimes[j]=null;
      }
      r.arriveTimes[0]=tStart.getCopy();
      r.arriveTimes[0].setPrecision('s');
      routesOut[lIdx].addElement(r);
    }
    //randomize the order of the routes
    Random rnd=new Random();
    for (int i=routesOut[lIdx].size()-1; i>=0; i--) {
      int k=rnd.nextInt(routesOut[lIdx].size());
      if (k!=i) {
        //swap i-th and k-th route
        Route r=routesOut[lIdx].elementAt(i);
        routesOut[lIdx].setElementAt(routesOut[lIdx].elementAt(k),i);
        routesOut[lIdx].setElementAt(r,k);
      }
    }
  }
  /**
   * Adds a given route to the list of routes
   */
  public void addRoute(Route r) {
    if (r==null || r.places==null || r.places.length<=r.nCurr)
      return;
    if (!r.places[r.nCurr].placeId.equals(id))
      return; //not a route from this place!
    if (r.nCurr>=r.places.length-1) {
      //this is the last place on the route
      if (routesEnd==null)
        routesEnd=new Vector(50,50);
      routesEnd.addElement(r);
      return;
    }
    int lIdx=findLinkByDestId(r.places[r.nCurr+1].placeId);
    if (lIdx<0) {
      //no suitable link for this route!
      if (routesEnd==null)
        routesEnd=new Vector(50,50);
      routesEnd.addElement(r);  //this route will not be lost and will be analyzed later
      return;
    }
    if (routesOut ==null) {
      routesOut =new Vector[links.size()];
      for (int i=0; i<routesOut.length; i++)
        routesOut[i]=null;
    }
    if (routesOut[lIdx]==null)
      routesOut[lIdx]=new Vector<Route>(1000,1000);
    insertRoute(r,routesOut[lIdx]);
  }
  /**
   * Inserts the given route in the given vector taking into account the
   * time of the last move
   */
  protected void insertRoute(Route r, Vector<Route> routes) {
    if (r==null || routes==null)
      return;
    int idx=-1;
    if (r.arriveTimes !=null && r.nCurr<r.arriveTimes.length && r.arriveTimes[r.nCurr]!=null) {
      for (int i=0; i<routes.size() && idx<0; i++) {
        Route r2=routes.elementAt(i);
        if (r2.arriveTimes ==null || r.nCurr>=r2.arriveTimes.length || r2.arriveTimes[r.nCurr]==null)
          idx=i;
        else
          if (r.arriveTimes[r.nCurr].compareTo(r2.arriveTimes[r.nCurr])<0)
            idx=i;
      }
    }
    if (idx>=0)
      routes.insertElementAt(r,idx);
    else
      routes.addElement(r);
  }
  /**
   * Receives a route that was sent back by one of the links, which is
   * overloaded and cannot propagate this route to the next place.
   */
  public void receiveRouteBack (Route r) {
    if (routesOut==null)
      return;
    if (r==null || r.places==null || r.places.length<=r.nCurr)
      return;
    if (!r.places[r.nCurr].placeId.equals(id))
      return; //not a route from this place!
    int lIdx=findLinkByDestId(r.places[r.nCurr+1].placeId);
    if (lIdx>=0)
      insertRoute(r,routesOut[lIdx]);
  }
  /**
   * Distributes the pending routes from this place among the links
   * originating from this place, depending on the next place in the route
   * @param t0 - the start time of the first trajectory
   * @param tLen - the length of the time interval within which the trajectories are distributed
   * @param tUnit - the unit in which the tBetween and tLen are measured
   */
  public void distributeRoutes(TimeMoment t0, long tLen, char tUnit) {
    if (routesOut ==null || routesOut.length<1)
      return;
    if (links==null || links.isEmpty())
      return;
    for (int i=0; i<routesOut.length; i++)
      sendRoutesToLink(i,t0,tLen,tUnit);
  }

  public void sendRoutesToLink(int lIdx, TimeMoment t0, long tLen, char tUnit) {
    if (routesOut ==null || lIdx<0 || lIdx>=routesOut.length)
      return;
    if (routesOut[lIdx]==null || routesOut[lIdx].size()<1)
      return;
    Link link=links.elementAt(lIdx);
    int nToMove=link.maxNMoves;
    if (nToMove<10) nToMove=(int)tLen;
    if (nToMove>routesOut[lIdx].size())
      nToMove=routesOut[lIdx].size();
    IntArray idxToRemove=new IntArray(nToMove,10);
    double tBetween=(1.0*tLen)/nToMove;
    double dt=0;
    int nPassed=0;
    for (int j=0; j<routesOut[lIdx].size() && nPassed<nToMove; j++) {
      Route r=routesOut[lIdx].elementAt(j);
      if (r.arriveTimes[r.nCurr]!=null) {
        //check if this route will already be in this place
        long tDiff=r.arriveTimes[r.nCurr].subtract(t0,tUnit);
        if (tDiff>=tLen)
          continue; //not here yet on this step
        r.leaveTimes[r.nCurr]=r.arriveTimes[r.nCurr].getCopy();
        if (tDiff<dt)
          r.leaveTimes[r.nCurr].add(Math.round(dt-tDiff));
      }
      else  {
        r.arriveTimes[r.nCurr]=t0.getCopy();
        r.leaveTimes[r.nCurr]=t0.getCopy();
        r.leaveTimes[r.nCurr].add(Math.round(dt));
      }
      link.addRoute(r);
      idxToRemove.addElement(j);
      dt+=tBetween;
      ++nPassed;
    }
    for (int j=idxToRemove.size()-1; j>=0; j--)
      routesOut[lIdx].removeElementAt(idxToRemove.elementAt(j));
  }

  public int getLinkIdx (String linkId) {
    if (linkId==null || links==null)
      return -1;
    for (int i=0; i<links.size(); i++)
      if (linkId.equals(links.elementAt(i).id))
        return i;
    return -1;
  }

  public void sendRoutesToLink (Link link, TimeMoment t0, long tLen, char tUnit) {
    if (link==null)
      return;
    sendRoutesToLink(getLinkIdx(link.id),t0,tLen,tUnit);
  }

  public boolean hasRoutes () {
    if (routesOut ==null || routesOut.length<1)
      return false;
    for (int i=0; i<routesOut.length; i++)
      if (routesOut[i]!=null && routesOut[i].size()>0)
        return true;
    return false;
  }

  public int getRoutesCount() {
    if (routesOut ==null || routesOut.length<1)
      return 0;
    int count=0;
    for (int i=0; i<routesOut.length; i++)
      if (routesOut[i]!=null)
        count+=routesOut[i].size();
    return count;
  }
}
