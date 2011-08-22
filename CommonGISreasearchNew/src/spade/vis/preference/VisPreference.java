package spade.vis.preference;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;

/**
* A general interface for all classes supporting various visualization
* preference, for example, icons corresponding to values of a qualitative
* attribute or combinations of values of several attributes.
* A preference must be able to read itself or its settings from
* a BufferedReader and write to a DataOutputStream.
*/
public interface VisPreference {
	/**
	* Reads itself or its settings from the given BufferedReader. Returns true
	* if read successfully. The signal to stop reading is a line containing the
	* tag </preference>.
	*/
	public boolean read(BufferedReader br) throws IOException;

	/**
	* Writes itself or its settings to the given DataOutputStream.
	*/
	public void write(DataOutputStream out) throws IOException;
}