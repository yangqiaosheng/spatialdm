package spade.analysis.system;

/**
* Keeps a register of available data readers (i.e. classes reading geographic
* or/and thematic data from various sources and formats). Constructs data
* readers. Can select an appropriate data reader according to a specified
* data format
*/
public interface DataReaderFactory {
	/**
	* Returns the number of available classes for data reading
	*/
	public int getAvailableReaderCount();

	/**
	* Returns the identifier of the AVAILABLE reader with the given index in the
	* list of available readers
	*/
	public String getAvailableReaderId(int idx);

	/**
	* Returns the short name of the AVAILABLE reader with the given index in the
	* list of available readers. This should be a short text characterising the
	* data source appropriate for this reader. The text must be suitable for
	* putting on a button. For example, "DBF", "Oracle database", "Clipboard", etc.
	*/
	public String getAvailableReaderName(int idx);

	/**
	* Returns the description (extended name) of the AVAILABLE reader with the
	* given index in the list of available readers. This text must be usable as
	* an explanation to a button.
	*/
	public String getAvailableReaderDescr(int idx);

	/**
	* Constructs an instance of a data reader with the given identifier.
	*/
	public DataReader constructReader(String id);

	/**
	* Constructs a data reader for data having the specified format
	*/
	public DataReader getReaderOfFormat(String format);

	/**
	* If construction of a reader failed, returns the message
	* explaining the reason of this.
	*/
	public String getErrorMessage();
}