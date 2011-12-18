package spade.analysis.tools.moves.simulation;

import spade.lib.util.IntArray;
import spade.lib.util.DoubleArray;
import spade.time.TimeMoment;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: natascha
 * Date: Nov 15, 2011
 * Time: 3:28:12 PM
 * Represents a link between two places in movement simulation
 */
public class Link {
  /**
   * The identifier of the link (same as in the geo layer with the links)
   */
  public String id=null;
  /**
   * The identifier of the link origin
   */
  public String origId=null;
  /**
   * The identifier of the link destination
   */
  public String destId=null;
  /**
   * The place of link origin
   */
  public Place origin=null;
  /**
   * The place of link destination
   */
  public Place destination=null;
  /**
   * The path length of the link (in km in case of geographical coordinates)
   */
  public double pathLength=0;
  /**
   * The maximum number of moves through this links per model time step
   * taken from the historical data
   */
  public int maxNMoves=0;
  /**
   * The current model time step (may differ from the simulation time step)
   */
  public int modTStepN=0;
  /**
   * The model-predicted load for the time step of the model (which may differ
   * from the time step of the simulation).
   */
  public double currModLoad =0;
  /**
   * The loads of this link by model time steps
   */
  public IntArray modLoads=null;
  /**
   * The numbers of trajectories sent to this link on each model time step
   */
  public IntArray nTrajDemand=null;
  /**
   * The minimal speeds of the movement by the model steps
   */
  public DoubleArray minSpeeds=null;
  /**
   * The maximal speeds of the movement by the model steps
   */
  public DoubleArray maxSpeeds=null;
  /**
   * Capacities by the model time steps
   */
  public IntArray capacities=null;
  /**
   * The number of vehicles that actually moved to the destination place, by model steps
   */
  public IntArray nTrMoved=null;
  /**
   * The number of vehicles that were sent back to the origin place, by model steps
   */
  public IntArray nTrBack=null;
  /**
   * The base (standard) load of this link on the current simulation time step
   */
  public int baseLoad =0;
  /**
   * The pending routes on this link
   */
  public Vector<Route> routes=null;

  public void setBaseLoad(int load) {
    baseLoad=load;
  }

  public void setModLoad(int load) {
    if (modLoads ==null)
      modLoads =new IntArray(10,10);
    for (int i=modLoads.size(); i<=modTStepN; i++)
      modLoads.addElement(0);
    modLoads.setElementAt(load,modTStepN);
  }

  public void addRoute(Route r) {
    if (r==null)
      return;
    if (routes==null)
      routes=new Vector<Route>(10,10);
    routes.addElement(r);
    if (nTrajDemand==null)
      nTrajDemand=new IntArray(10,10);
    for (int i=nTrajDemand.size(); i<=modTStepN; i++)
      nTrajDemand.addElement(0);
    nTrajDemand.setElementAt(nTrajDemand.elementAt(modTStepN)+1,modTStepN);
  }

  public int getNRoutes() {
    if (routes==null)
      return 0;
    return routes.size();
  }

  public int getBaseLoad() {
    return baseLoad;
  }

  public int getTotalLoad() {
    return baseLoad+getNRoutes();
  }

  public void setPossibleSpeed (double speed) {
    if (minSpeeds==null)
      minSpeeds=new DoubleArray(10,10);
    for (int i=minSpeeds.size(); i<=modTStepN; i++)
      minSpeeds.addElement(Double.NaN);
    if (maxSpeeds==null)
      maxSpeeds=new DoubleArray(10,10);
    for (int i=maxSpeeds.size(); i<=modTStepN; i++)
      maxSpeeds.addElement(0);
    if (Double.isNaN(minSpeeds.elementAt(modTStepN)) || speed<minSpeeds.elementAt(modTStepN))
      minSpeeds.setElementAt(speed,modTStepN);
    if (speed>maxSpeeds.elementAt(modTStepN))
      maxSpeeds.setElementAt(speed,modTStepN);
  }

  public void addCapacity(int cap) {
    if (capacities ==null)
      capacities =new IntArray(10,10);
    for (int i=capacities.size(); i<=modTStepN; i++)
      capacities.addElement(0);
    capacities.setElementAt(cap+capacities.elementAt(modTStepN),modTStepN);
  }

  public void moveRoutesToNextPlace (int maxNRoutes,
                                     double speed) {
    if (maxNRoutes<1 || getNRoutes()<1)
      return;

    double timeToGet=pathLength/speed; //in hours
    long secToGet=Math.round(timeToGet*3600);
    int nToMove=Math.min(routes.size(),maxNRoutes);
    int nSendBack=routes.size()-nToMove;
    for (int i=0; i<nToMove; i++) {
      Route r=routes.elementAt(0);
      routes.removeElementAt(0);
      if (r.nCurr+1>=r.arriveTimes.length) {
        System.out.println(r.toString());
        continue;
      }
      TimeMoment t=r.leaveTimes[r.nCurr].getCopy();
      t.setPrecision('s');
      t.add(secToGet);
      ++r.nCurr;
      r.arriveTimes[r.nCurr]=t;
      destination.addRoute(r);
    }
    for (int i=0; i<nSendBack; i++) {
      Route r=routes.elementAt(routes.size()-1);
      routes.removeElementAt(routes.size()-1);
      origin.receiveRouteBack(r);
    }
    if (nTrMoved==null)
      nTrMoved=new IntArray(10,10);
    for (int i=nTrMoved.size(); i<=modTStepN; i++)
      nTrMoved.addElement(0);
    nTrMoved.setElementAt(nTrMoved.elementAt(modTStepN)+nToMove,modTStepN);
    if (nTrBack==null)
      nTrBack=new IntArray(10,10);
    for (int i=nTrBack.size(); i<=modTStepN; i++)
      nTrBack.addElement(0);
    nTrBack.setElementAt(nTrBack.elementAt(modTStepN)+nSendBack,modTStepN);
  }
}
