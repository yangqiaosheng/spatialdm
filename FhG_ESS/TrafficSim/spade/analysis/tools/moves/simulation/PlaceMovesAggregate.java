package spade.analysis.tools.moves.simulation;

/**
 * Created by IntelliJ IDEA.
 * User: natascha
 * Date: Nov 29, 2011
 * Time: 1:27:52 PM
 * Contains aggregated information about the number of moving objects
 * that visited (passed through) a place
 */
public class PlaceMovesAggregate {
  /**
   * The identifier of the place (same as in the geo layer with the places)
   */
  public String id=null;
  /**
   * The base (standard) loads by time intervals
   */
  public int baseLoads[]=null;
  /**
   * The counts of the additional (simulated) trajectories by time intervals
   */
  public int addCounts[]=null;
}
