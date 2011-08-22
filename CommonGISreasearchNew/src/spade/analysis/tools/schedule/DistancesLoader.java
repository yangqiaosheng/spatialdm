package spade.analysis.tools.schedule;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Vector;

import spade.lib.util.CopyFile;
import spade.lib.util.Matrix;
import spade.lib.util.StringUtil;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 02-Mar-2007
 * Time: 15:53:47
 * Loads distances and/or times from an ASCII file and puts them in a matrix.
 */
public class DistancesLoader {
	/**
	 * Loads distances and/or times from an ASCII file with the given name (path).
	 * If the path is relative and the path to the application directory is
	 * specified, looks for the file in the application directory or its
	 * subdirectory. Puts the data into a matrix and returns the matrix.
	 * The file with the distances is supposed to have the following structure:
	 * <source_ID>,<destination_ID>,<distance>{,<time>}
	 */
	public Matrix loadDistances(String fileName, String applPath) {
		if (fileName == null)
			return null;
		if (applPath != null && !CopyFile.isAbsolutePath(fileName)) {
			applPath = CopyFile.getDir(applPath);
			if (applPath != null) {
				fileName = applPath + fileName;
			}
		}
		InputStream stream = openStream(fileName);
		if (stream == null)
			return null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		Matrix matrix = null;
		Vector fieldNames = null;
		while (true) {
			String str = null;
			try {
				str = reader.readLine();
			} catch (EOFException eof) {
				break;
			} catch (IOException ioe) {
				System.out.println("Error reading distances: " + ioe);
				return null;
			}
			if (str == null) {
				break;
			}
			Vector v = StringUtil.getNames(str, ",;\t", true);
			if (v == null || v.size() < 3) {
				continue;
			}
			boolean hasNumbers = false;
			Float val = null;
			try {
				val = Float.valueOf((String) v.elementAt(2));
				hasNumbers = val != null;
			} catch (NumberFormatException e) {
			}
			if (!hasNumbers) {
				if (fieldNames == null) {
					fieldNames = v;
				}
				continue;
			}
			String rowId = (String) v.elementAt(0), columnId = (String) v.elementAt(1);
			if (rowId == null || rowId.length() < 1 || columnId == null || columnId.length() < 1) {
				continue;
			}
			if (matrix == null) {
				matrix = new Matrix(50, 50, 10);
				if (fieldNames != null && fieldNames.size() > 2) {
					for (int i = 2; i < fieldNames.size(); i++) {
						matrix.addAttribute((String) fieldNames.elementAt(i));
					}
				}
			}
			matrix.put(val, 0, rowId, columnId);
			matrix.put(val, 0, columnId, rowId);
			for (int i = 3; i < v.size(); i++) {
				val = null;
				try {
					val = Float.valueOf((String) v.elementAt(i));
				} catch (NumberFormatException e) {
				}
				if (val != null) {
					matrix.put(val, i - 2, rowId, columnId);
					matrix.put(val, i - 2, columnId, rowId);
				}
			}
		}
		closeStream(stream);
		if (matrix == null)
			return null;
		for (int i = 0; i < matrix.getNRows(); i++) {
			String rowId = matrix.getRowIdentifier(i);
			int colIdx = matrix.getColumnIndex(rowId);
			if (colIdx >= 0) {
				for (int n = 0; n < matrix.getNAttributes(); n++) {
					matrix.put(new Float(0), n, i, colIdx);
				}
			}
		}
		for (int j = 0; j < matrix.getNColumns(); j++) {
			String colId = matrix.getColumnIdentifier(j);
			int rowIdx = matrix.getRowIndex(colId);
			if (rowIdx >= 0) {
				for (int n = 0; n < matrix.getNAttributes(); n++) {
					matrix.put(new Float(0), n, rowIdx, j);
				}
			}
		}
		return matrix;
	}

	/**
	* Opens the stream on the specified data source. If the source
	* string starts with HTTP or FILE, accesses the source as a URL, otherwise
	* opens it as a local file
	*/
	protected InputStream openStream(String dataSource) {
		if (dataSource == null)
			return null;
		int idx = dataSource.indexOf(':');
		boolean isURL = false;
		InputStream stream = null;
		if (idx > 0) {
			String pref = dataSource.substring(0, idx);
			if (pref.equalsIgnoreCase("HTTP") || pref.equalsIgnoreCase("FILE")) {
				isURL = true;
			}
		}
		try {
			if (isURL) { //try to access the source as a URL
				URL url = new URL(dataSource);
				stream = url.openStream();
			} else {
				stream = new FileInputStream(dataSource);
			}
		} catch (IOException ioe) {
			System.out.println("Error accessing " + dataSource + ": " + ioe);
			return null;
		}
		return stream;
	}

	protected void closeStream(InputStream stream) {
		if (stream == null)
			return;
		try {
			stream.close();
		} catch (IOException ioe) {
		}
	}
}
