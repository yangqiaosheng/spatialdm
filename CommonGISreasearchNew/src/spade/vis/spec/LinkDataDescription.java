package spade.vis.spec;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;

import spade.lib.util.StringUtil;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 04-Jan-2007
 * Time: 17:12:02
 * Describes a table specifying links (flows) between geographical objects in
 * some layer. The objects may be points or areas. A link may have any
 * attributes. A link is directed. The source and destination nodes of a link
 * may be time-referenced.
 * The table specifying links must contain columns with identifiers of the
 * source and destination nodes. It can also contain columns with time
 * references of the source and destination nodes.
 */
public class LinkDataDescription implements TagReader, java.io.Serializable {
	/**
	 * The identifier of the layer containing the source and destination nodes
	 * of the link.
	 */
	public String layerRef = null;
	/**
	 * The name of the column with the link source nodes
	 */
	public String souColName = null;
	/**
	 * The index of the column with the link source nodes
	 */
	public int souColIdx = -1;
	/**
	 * The name of the column with the link destination nodes
	 */
	public String destColName = null;
	/**
	 * The index of the column with the link destination nodes
	 */
	public int destColIdx = -1;
	/**
	 * The name of the column with the time references of the link source nodes
	 */
	public String souTimeColName = null;
	/**
	 * The index of the column with the time references of the link source nodes
	 */
	public int souTimeColIdx = -1;
	/**
	 * The scheme (template) for the specification of the time references of the
	 * link source nodes
	 */
	public String souTimeScheme = null;
	/**
	 * The name of the column with the time references of the link destination nodes
	 */
	public String destTimeColName = null;
	/**
	 * The index of the column with the time references of the link destination nodes
	 */
	public int destTimeColIdx = -1;
	/**
	 * The scheme (template) for the specification of the time references of the
	 * link destination nodes
	 */
	public String destTimeScheme = null;
	/**
	 * The path to the file with the distances between the locations, if exists
	 */
	public String distancesFilePath = null;

	/**
	* Stores the description of a table with link data to a file as a sequence of
	* lines starting with <LinkData> and ending with </LinkData>.
	*/
	@Override
	public void writeDescription(DataOutputStream writer) throws IOException {
		if (writer == null)
			return;
		writer.writeBytes("<LinkData>\n");
		writer.writeBytes("layer_ref=\"" + layerRef + "\"\n");
		writer.writeBytes("link_start_column=\"" + souColName + "\"\n");
		if (souColIdx >= 0) {
			writer.writeBytes("link_start_column_index=" + souColIdx + "\n");
		}
		writer.writeBytes("link_end_column=\"" + destColName + "\"\n");
		if (destColIdx >= 0) {
			writer.writeBytes("link_end_column_index=" + destColIdx + "\n");
		}
		if (souTimeColName != null) {
			writer.writeBytes("link_start_time_column=\"" + souTimeColName + "\"\n");
		}
		if (souTimeColIdx >= 0) {
			writer.writeBytes("link_start_time_column_index=" + souTimeColIdx + "\n");
		}
		if (souTimeScheme != null) {
			writer.writeBytes("link_start_time_scheme=\"" + souTimeScheme + "\"\n");
		}
		if (destTimeColName != null) {
			writer.writeBytes("link_end_time_column=\"" + destTimeColName + "\"\n");
		}
		if (destTimeColIdx >= 0) {
			writer.writeBytes("link_end_time_column_index=" + destTimeColIdx + "\n");
		}
		if (destTimeScheme != null) {
			writer.writeBytes("link_end_time_scheme=\"" + destTimeScheme + "\"\n");
		}
		if (distancesFilePath != null) {
			writer.writeBytes("distances_file=\"" + distancesFilePath + "\"\n");
		}
		writer.writeBytes("</LinkData>\n");
	}

	/**
	* Reads a description of a table with link data. A description must be a
	 * sequence of lines starting with <LinkData> and ending with </LinkData>. The
	* first argument is the first string of the tag (i.e. the string starting with
	* "<LinkData"), the second argument is the BufferedReader used to read the
	* description. Returns true if the description has been successfully read,
	* or at least the end line has been encountered.
	*/
	@Override
	public boolean readDescription(String str, BufferedReader br) throws IOException {
		if (str == null || br == null || !str.startsWith("<") || !str.substring(1).toLowerCase().startsWith("linkdata"))
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
			if (str.equalsIgnoreCase("</linkdata>")) {
				endFound = true;
				break;
			}
			StringTokenizer st = new StringTokenizer(str, "=");
			if (st.countTokens() < 2) {
				continue;
			}
			String w1 = StringUtil.removeQuotes(st.nextToken().trim()), w2 = StringUtil.removeQuotes(st.nextToken().trim());
			if (w1 == null || w2 == null || w1.length() < 1 || w2.length() < 1) {
				continue;
			}
			if (w1.equalsIgnoreCase("layer_ref")) {
				layerRef = w2;
			} else if (w1.equalsIgnoreCase("link_start_column")) {
				souColName = w2;
			} else if (w1.equalsIgnoreCase("link_end_column")) {
				destColName = w2;
			} else if (w1.equalsIgnoreCase("link_start_time_column")) {
				souTimeColName = w2;
			} else if (w1.equalsIgnoreCase("link_start_time_scheme")) {
				souTimeScheme = w2;
			} else if (w1.equalsIgnoreCase("link_end_time_column")) {
				destTimeColName = w2;
			} else if (w1.equalsIgnoreCase("link_end_time_scheme")) {
				destTimeScheme = w2;
			} else if (w1.equalsIgnoreCase("distances_file")) {
				distancesFilePath = w2;
			} else if (w1.equalsIgnoreCase("link_start_column_index") || w1.equalsIgnoreCase("link_end_column_index") || w1.equalsIgnoreCase("link_start_time_column_index") || w1.equalsIgnoreCase("link_end_time_column_index")) {
				int k = -1;
				try {
					k = Integer.valueOf(w2).intValue();
				} catch (NumberFormatException nfe) {
				}
				if (k < 0) {
					continue;
				}
				if (w1.equalsIgnoreCase("link_start_column_index")) {
					souColIdx = k;
				} else if (w1.equalsIgnoreCase("link_end_column_index")) {
					destColIdx = k;
				} else if (w1.equalsIgnoreCase("link_start_time_column_index")) {
					souTimeColIdx = k;
				} else if (w1.equalsIgnoreCase("link_end_time_column_index")) {
					destTimeColIdx = k;
				}
			}
		}
		if (!endFound)
			return false;
		return true;
	}
}
