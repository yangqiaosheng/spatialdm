package spade.analysis.tools.db_tools;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 22-Dec-2006
 * Time: 14:52:54
 * Contains possible relevant meanings of datasets and table columns
 */
public class DataSemantics {
	/**
	 * Possible meanings of data
	 */
	public static final int UNKNOWN = 0, STATIC_POINTS = 1, EVENTS = 2, MOVEMENTS = 3, TIME_RELATED = 4, OTHER = 4;
	/**
	 * Short texts corresponding to the meanings
	 */
	public static final String dataMeaningNames[] = { "unknown", "static points", "events", "movements", "time-related data", "other" };
	/**
	 * Explanatory texts corresponding to the meanings
	 */
	public static final String dataMeaningTexts[] = { "unknown", "static discrete objects (points)", "events (time-referenced points)", "movements (discrete objects changing positions)", "data referred to time", "other" };
	/**
	 * Specifies possible relevant meanings of columns in a table containing
	 * point data
	 */
	public static final String pointSemantics[] = { "x-coordinate", "y-coordinate", "object identifier" };
	/**
	 * Number of mandatory meanings, which must be present in point data (the
	 * remaining meanings are treated as optional). The mandatory meanings
	 * come at the beginning of the array pointSemantics.
	 */
	public static final int nMandPointSem = 2;
	/**
	 * "Canonic", or "standard" names of table fields with the meanings
	 * specified in eventSemantics
	 */
	public static final String canonicFieldNamesPoints[] = { "X", "Y", "ID" };
	/**
	 * Specifies possible relevant meanings of columns in a table containing
	 * event data
	 */
	public static final String eventSemantics[] = { "x-coordinate", "y-coordinate", "time", "event identifier" };
	/**
	 * Number of mandatory meanings, which must be present in event data (the
	 * remaining meanings are treated as optional). The mandatory meanings
	 * come at the beginning of the array eventSemantics.
	 */
	public static final int nMandEventSem = 3;
	/**
	 * "Canonic", or "standard" names of table fields with the meanings
	 * specified in eventSemantics
	 */
	public static final String canonicFieldNamesEvents[] = { "X", "Y", "DT", "ID" };
	/**
	 * Specifies possible relevant meanings of columns in a table containing
	 * movement data
	 */
	public static final String movementSemantics[] = { "x-coordinate", "y-coordinate", "time", "entity identifier", "time of next measurement", "time interval to next measurement", "trajectory identifier", "speed (measured)", "direction (measured)",
			"distance to next measurement", "x-distance", "y-distance", "speed (computed)", "acceleration (computed)", "direction (computed)", "turn (computed)" };
	/**
	 * Number of mandatory meanings, which must be present in movement data (the
	 * remaining meanings are treated as optional). The mandatory meanings
	 * come at the beginning of the array movementSemantics.
	 */
	public static final int nMandMoveSem = 3;
	/**
	 * Index of trajectory identifier 
	 */
	public static final int idxOfTrajId = 6;
	/**
	 * "Canonic", or "standard" names of table fields with the meanings
	 * specified in movementSemantics
	 */
	public static final String canonicFieldNamesMovement[] = { "X_", "Y_", "DT_", "ID_", "NEXTTIME_", "DIFFTIME_", "TID_", "SPEED", "COURSE", "DISTANCE_", "DX_", "DY_", "SPEED_C", "ACCELERATION_C", "COURSE_C", "TURN_C" };

	/**
	 * Specifies possible relevant meanings of columns in a table containing
	 * event data
	 */
	public static final String timerelatedSemantics[] = { "time", "identifier" };
	/**
	 * Number of mandatory meanings, which must be present in event data (the
	 * remaining meanings are treated as optional). The mandatory meanings
	 * come at the beginning of the array eventSemantics.
	 */
	public static final int nMandTimerelatedSem = 2;
	/**
	 * "Canonic", or "standard" names of table fields with the meanings
	 * specified in eventSemantics
	 */
	public static final String canonicFieldNamesTimerelated[] = { "DT", "ID" };

}
