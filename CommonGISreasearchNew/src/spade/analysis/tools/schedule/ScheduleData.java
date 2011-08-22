package spade.analysis.tools.schedule;

import spade.lib.util.Matrix;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoLayer;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.LinkDataDescription;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 01-Mar-2007
 * Time: 14:51:33
 * A structure containing all data relevannt to one transportation schedule:
 * 1) the source table with transportation orders;
 * 2) the layer with the source and destination locations occurring in the table;
 * 3) the layer with the non-aggregated links (movements) between the sources
 *    and destinations, which is built on the basis of the table with the orders;
 * 4) the MovementAggregator, which aggregates the transportation orders from
 *    the table by pairs (source, destination);
 * 5) the secondary table with the aggregated data, which is generated by the
 *    MovementAggregator;
 * 6) the layer with the links between the sources and destination built on the
 *    basis of the aggregated data.
 */
public class ScheduleData {
	/**
	 * The source table with transportation orders
	 */
	public DataTable souTbl = null;
	/**
	 * The specification (metadata) of the schedule
	 */
	public DataSourceSpec spec = null;
	/**
	 * A description (metadata) of the links (movements) specified in the
	 * schedule. This description is, in fact, a part of the DataSourceSpec,
	 * but it is convenient to have a special reference to it.
	 */
	public LinkDataDescription ldd = null;
	/**
	 * Index of the table column with the names of the source locations
	 */
	public int souNameColIdx = -1;
	/**
	 * Index of the table column with the names of the destination locations
	 */
	public int destNameColIdx = -1;
	/**
	 * Index of the table column with the number of transported items
	 */
	public int itemNumColIdx = -1;
	/**
	 * Index of the table column with the category of the transported items
	 */
	public int itemCatColIdx = -1;
	/**
	 * Index of the table column with the vehicle identifier
	 */
	public int vehicleIdColIdx = -1;
	/**
	 * Index of the table column with the vehicle type
	 */
	public int vehicleTypeColIdx = -1;
	/**
	 * Index of the table column with the identifier of the vehicle home base
	 * (initial location)
	 */
	public int vehicleHomeIdColIdx = -1;
	/**
	 * Index of the table column with the name of the vehicle home base
	 * (initial location)
	 */
	public int vehicleHomeNameColIdx = -1;
	/**
	 * The layer with the source and destination locations occurring in the table
	 * with the transportation orders
	 */
	public DGeoLayer locLayer = null;
	/**
	 * The layer with the non-aggregated links (movements) between the sources
	 * and destinations, which is built on the basis of the table with the orders
	 */
	public DGeoLayer linkLayer = null;
	/**
	 * The MovementAggregator, which aggregates the transportation orders from
	 * the table by pairs (source, destination)
	 */
	public MovementAggregator moveAggregator = null;
	/**
	 * The secondary table with the aggregated data, which is generated by the
	 * MovementAggregator
	 */
	public DataTable aggTbl = null;
	/**
	 * The number of the first column with updatable statistics in aggTbl
	 */
	public int firstUpdatableColIdx = -1;
	/**
	 * The layer with the links between the sources and destination built on the
	 * basis of the aggregated data
	 */
	public DGeoLayer aggLinkLayer = null;
	/**
	 * The matrix with the distances and/or travel times between the source and
	 * destination locations
	 */
	public Matrix distancesMatrix = null;
	/**
	 * A table with data about the groups of items transported according to the
	 * schedule
	 */
	public DataTable itemData = null;
	/**
	 * A table with data about the numbers of items in the sources by time intervals
	 */
	public DataTable souItemNumTable = null;
	/**
	 * A layer corresponding to souItemNumTable
	 */
	public DGeoLayer souItemNumLayer = null;
	/**
	 * A table with data about the use of the destinations by time intervals
	 */
	public DataTable destUseTable = null;
	/**
	 * A layer corresponding to destUseTable
	 */
	public DGeoLayer destUseLayer = null;
	/**
	 * A table with static data about the vehicles including type, identifier and
	 * name of the home base (initial location) and, possibly, something else.
	 */
	public DataTable vehicleInfo = null;
	/**
	 * A table with aggregated data about the presence of the vehicles in
	 * different sites
	 */
	public DataTable aggrDataVehicleSites = null;
	/**
	 * A layer with the sites visited by the vehicles
	 */
	public DGeoLayer vehicleSites = null;
}
