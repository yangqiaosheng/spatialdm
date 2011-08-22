package spade.vis.spec;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.lib.util.StringUtil;

/**
* This structure describes references to values of a parameter in a table with
* thematic data. Values of a parameter must be contained in some table column.
*/
public class ParamDescription extends ProtoParam implements TagReader, java.io.Serializable {
	/**
	* The name of the column in which values of the parameter are contained. The
	* parameter will have the same name as the column.
	*/
	public String columnName = null;
	/**
	* The index of the column in which values of the parameter are contained.
	*/
	public int columnIdx = -1;
	/**
	* The list of names of columns depending on this parameter. If the list is
	* empty, this means that all columns in the table depend on the parameter.
	*/
	public Vector dependentCols = null;
	/**
	* This variable indicates whether this parameter description must be stored.
	* A parameter description may be created only for internal purposes.
	* In this case it is not stored in an application file.
	*/
	public boolean internal = false;
	/**
	* Identifiers of the columns that must not be moved to the resulting table.
	* These may be, for example, initial columns containing dates in string
	* format (the system constructs from them temporal attributes, and the source
	* columns are not actually used)
	*/
	public Vector colsToRemove = null;

	/**
	* Reads a description of a parameter. A description must be a sequence of
	* lines starting with <parameter> and ending with </parameter>. The
	* first argument is the first string of the tag (i.e. the string starting with
	* "<parameter"), the second argument is the BufferedReader used to read the
	* description. Returns true if the description has been successfully read,
	* or at least the end line has been encountered.
	*/
	@Override
	public boolean readDescription(String str, BufferedReader br) throws IOException {
		if (str == null || br == null || !str.startsWith("<") || !str.substring(1).toLowerCase().startsWith("parameter"))
			return false;
		boolean endFound = false;
		while (true) {
			str = br.readLine();
			if (str == null) {
				break;
			}
			str = str.trim();
			if (str.length() < 1) {
				continue;
			}
			if (str.startsWith("*")) {
				continue; //this is a comment
			}
			if (str.equalsIgnoreCase("</parameter>")) {
				endFound = true;
				break;
			}
			StringTokenizer st = new StringTokenizer(str, "=");
			if (st.countTokens() < 2) {
				continue;
			}
			String w1 = StringUtil.removeQuotes(st.nextToken().trim()), w2 = st.nextToken().trim();
			if (!w1.equalsIgnoreCase("dependent_columns") && !w1.equalsIgnoreCase("order")) {
				w2 = StringUtil.removeQuotes(w2);
			}
			if (w1 == null || w2 == null || w1.length() < 1 || w2.length() < 1) {
				continue;
			}
			if (w1.equalsIgnoreCase("column_name")) {
				columnName = w2;
			} else if (w1.equalsIgnoreCase("dependent_columns")) {
				dependentCols = StringUtil.getNames(w2, ";,", false);
			} else if (w1.equalsIgnoreCase("order")) {
				if (w2.equalsIgnoreCase("SORTED")) {
					ordered = true;
				} else {
					order = StringUtil.getNames(w2, ";,", false);
					ordered = order != null && order.size() > 1;
					if (!ordered) {
						order = null;
					}
				}
			} else if (w1.equalsIgnoreCase("protract_known_values")) {
				protractKnownValues = w2.equalsIgnoreCase("yes");
			}
		}
		if (!endFound)
			return false;
		return true;
	}

	/**
	* Stores the description of a parameter to a file as a sequence of
	* lines starting with <parameter> and ending with </parameter>.
	*/
	@Override
	public void writeDescription(DataOutputStream writer) throws IOException {
		if (writer == null || internal)
			return;
		writer.writeBytes("<Parameter>\n");
		if (columnName != null) {
			writer.writeBytes("column_name=\"" + columnName + "\"\n");
		}
		if (dependentCols != null && dependentCols.size() > 0) {
			writer.writeBytes("dependent_columns=");
			for (int i = 0; i < dependentCols.size(); i++) {
				if (i > 0) {
					writer.writeBytes(";");
				}
				writer.writeBytes("\"" + (String) dependentCols.elementAt(i) + "\"");
			}
			writer.writeBytes("\n");
		}
		if (ordered)
			if (order == null || order.size() < 2) {
				writer.writeBytes("order=SORTED\n");
			} else {
				writer.writeBytes("order=");
				for (int i = 0; i < order.size(); i++) {
					if (i > 0) {
						writer.writeBytes(";");
					}
					writer.writeBytes("\"" + (String) order.elementAt(i) + "\"");
				}
				writer.writeBytes("\n");
			}
		if (protractKnownValues) {
			writer.writeBytes("protract_known_values=yes\n");
		}
		writer.writeBytes("</Parameter>\n");
	}
}