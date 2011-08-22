package spade.vis.spec;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;

/**
* This is an interface to be implemented by classes able to read and write
* certain descriptions specified as "tags", i.e. sequences of lines starting
* with <keyword ...> and ending with </keyword>.
*/
public interface TagReader {
	/**
	* Reads a corresponding "tag", i.e. a sequence of lines starting
	* with <keyword ...> and ending with </keyword>. The first argument is
	* the first string of the tag (i.e. any string starting with "<"), the
	* second argument is the BufferedReader used to read the data.
	* Returns true if the description has been successfully read, or at least
	* the end line has been encountered.
	*/
	public boolean readDescription(String str, BufferedReader br) throws IOException;

	/**
	* Stores the description to a file as a sequence of
	* lines starting with <keyword ...> and ending with </keyword>.
	*/
	public void writeDescription(DataOutputStream writer) throws IOException;
}