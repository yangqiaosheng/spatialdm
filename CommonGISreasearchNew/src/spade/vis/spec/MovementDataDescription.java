package spade.vis.spec;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 17-Apr-2007
 * Time: 18:08:35
 * Describes a table specifying trajectories of moving objects.
 */
public class MovementDataDescription {
	/**
	* The symbols used to specify elements of date/time strings:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years, for example, dd.mm.yyyy.
	*/
	public static final char TIME_SYMBOLS[] = { 's', 't', 'h', 'd', 'm', 'y' };
	/**
	 * The name of the column with the trajectory identifiers (this column is
	 * mandatory)
	 */
	public String trajectIdColName = null;
	/**
	 * The index of the column with the trajectory identifiers (this column is
	 * mandatory)
	 */
	public int trajectIdColIdx = -1;
	/**
	 * The name of the column with the time references (this column is
	 * mandatory)
	 */
	public String timeColName = null;
	/**
	 * The index of the column with the time references (this column is
	 * mandatory)
	 */
	public int timeColIdx = -1;
	/**
	 * The scheme (template) for the specification of the time references
	 */
	public String timeScheme = null;
	/**
	* The name of the field with X-coordinates (if the table contains coordinates
	* of object positions)
	*/
	public String xColName = null;
	/**
	* The index of the field with X-coordinates (if the table contains coordinates
	* of object positions)
	*/
	public int xColIdx = -1;
	/**
	* The name of the field with Y-coordinates (if the table contains coordinates
	* of object positions)
	*/
	public String yColName = null;
	/**
	* The index of the field with Y-coordinates (if the table contains coordinates
	* of object positions)
	*/
	public int yColIdx = -1;
	/**
	 * An optional identifier of the layer containing the positions (not
	 * necessarily points).
	 */
	public String posLayerRef = null;

	/**
	* Checks if the given symbol is a right time/date symbol, i.e. one of the
	* symbols 's','t','h','d','m','y', or 'a'.
	*/
	public static boolean isTimeSymbol(char symbol) {
		if (symbol == 'a')
			return true;
		for (char element : TIME_SYMBOLS)
			if (symbol == element)
				return true;
		return false;
	}
}
