package export;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 30-Aug-2007
 * Time: 13:46:40
 * Used when it is important to put record numbers in the exported data.
 */
public interface RecordNumberSaver {
	/**
	 * Indicates whether the exporter saves record numbers
	 */
	public boolean getSaveRecordNumbers();

	/**
	 * Requires the exporter to save or not to save record numbers
	 */
	public void setSaveRecordNumbers(boolean toSave);

	/**
	 * Returns the name of the attribute (column) in which the record
	 * numbers are put
	 */
	public String getRecNumColName();

	/**
	 * Sets the name of the attribute (column) in which the record
	 * numbers will be put
	 */
	public void setRecNumColName(String colName);
}
