package spade.vis.spec;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;

/**
* This is an interface to be implemented by classes able to read and write
* certain specifications specified as "tags", i.e. sequences of lines starting
* with <keyword> and ending with </keyword>. Unlike the interface TagReader,
* it is foreseen that a SpecSaver returns a specification from its
* "read..." method and accepts a specification as an argument in its "write..."
* method.
*/
public interface SpecSaver {
	/**
	* Reads a single specification, i.e. a sequence of lines starting
	* with <tagName> and ending with </tagName>. The first argument is
	* the tag name (e.g. "tool"), the second argument is the BufferedReader used
	* to read the data. Returns the specification read.
	*/
	public Object readSpecification(String tagName, BufferedReader br) throws IOException;

	/**
	* Stores the given specification to a file as a sequence of
	* lines starting with <keyword ...> and ending with </keyword>.
	*/
	public void writeSpecification(Object spec, DataOutputStream writer) throws IOException;
}