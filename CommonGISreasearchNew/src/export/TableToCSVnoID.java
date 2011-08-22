package export;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Jan 18, 2010
 * Time: 5:47:35 PM
 * To change this template use File | Settings | File Templates.
 */
/**
* This is a class for writing the contents of a Descartes table into CSV format
 * without ID and Name columns
*/
public class TableToCSVnoID extends TableToCSV {
	/**
	* Returns the name of the output format suitable for showing to the user.
	* For example, "Shape (ArcView)", "csv (comma-separated values)"
	*/
	@Override
	public String getFormatName() {
		return "csv (comma-separated values) without ID and NAME";
	}

	/**
	 * for CommonGIS projects we always need ID and NAME
	 * for exporting to other systems, they are not needed
	 */
	@Override
	protected boolean exportID() {
		return false;
	}
}
