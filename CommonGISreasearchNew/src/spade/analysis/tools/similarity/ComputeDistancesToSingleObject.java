package spade.analysis.tools.similarity;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.analysis.tools.DataAnalyser;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.GetPathDlg;
import spade.lib.util.CopyFile;
import spade.vis.action.Highlighter;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.geometry.Geometry;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 24-Apr-2007
 * Time: 11:04:50
 * Takes a currently selected spatial object from an active layer and computes the
 * "distances" from the other objects to this object using various distance
 * measures.
 */
public class ComputeDistancesToSingleObject implements DataAnalyser {
	protected ESDACore core = null;

	/**
	 * Returns true when the tool has everything necessary for its operation.
	 * For example, if the tool manages a number of analysis methods, it should
	 * check whether the class for at least one method is available.
	 */
	@Override
	public boolean isValid(ESDACore core) {
		return true;
	}

	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	@Override
	public void run(ESDACore core) {
		if (core == null || core.getUI() == null)
			return;
		this.core = core;
		SystemUI ui = core.getUI();
		if (ui.getCurrentMapViewer() == null || ui.getCurrentMapViewer().getLayerManager() == null) {
			showMessage("No map exists!", true);
			return;
		}
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		GeoLayer gl = lman.getActiveLayer();
		if (!(gl instanceof DGeoLayer)) {
			showMessage("The active layer is not a DGeoLayer!", true);
			return;
		}
		DGeoLayer layer = (DGeoLayer) gl;
		boolean typeIsSuitable = (layer.getType() == Geometry.point) || (layer.getType() == Geometry.line && layer.getSubtype() == Geometry.movement);
		if (!typeIsSuitable) {
			showMessage("The type of the active layer is not suitable for the similarity analysis!", true);
			return;
		}
		DGeoObject geoObj = null;
		Highlighter hl = core.getHighlighterForSet(layer.getEntitySetIdentifier());
		Vector selObj = hl.getSelectedObjects();
		if (selObj == null || selObj.size() < 1) {
			showMessage("None of the objects is currently selected!", true);
		} else if (selObj.size() == 1) {
			//use the currently selected object
			geoObj = (DGeoObject) layer.findObjectById((String) selObj.elementAt(0));
		} else {
			showMessage("More than one objects are currently selected!", true);
		}
		if (geoObj == null) {
			String txt = "Select a SINGLE object of the layer \"" + layer.getName() + "\" and try once again.";
			Dialogs.showMessage(core.getUI().getMainFrame(), txt, "Single object required");
			return;
		}
		if (Dialogs.askYesOrNo(ui.getMainFrame(), "Would you like to use " + "pre-computed distances between the objects of the layer?", "Pre-computed distances?")) {
			int objIdx = layer.getObjectIndex(geoObj.getIdentifier());
			//load the file with distances
			GetPathDlg fd = new GetPathDlg(ui.getMainFrame(), "File with the distances?");
			fd.setFileMask("*.csv;*.txt");
			fd.show();
			String path = fd.getPath();
			if (path == null)
				return;
			InputStream stream = openStream(path);
			if (stream == null)
				return;
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			int nObj = layer.getObjectCount();
			double distances[] = new double[nObj];
			for (int i = 0; i < nObj; i++) {
				distances[i] = Double.NaN;
			}
			distances[objIdx] = 0;
			boolean error = false;
			int count = 0;
			while (!error) {
				try {
					String s = reader.readLine();
					if (s == null) {
						break;
					}
					s = s.trim();
					if (s.length() < 1) {
						continue;
					}
					StringTokenizer st = new StringTokenizer(s, " ,;\t\r\n");
					if (st.countTokens() < 3) {
						continue;
					}
					int i1 = -1, i2 = -1;
					Double d = Double.NaN;
					try {
						i1 = Integer.parseInt(st.nextToken()) - 1;
						i2 = Integer.parseInt(st.nextToken()) - 1;
						d = Double.parseDouble(st.nextToken());
					} catch (NumberFormatException nfe) {
						continue;
					}
					if (i1 != i2 && !Double.isNaN(d) && d >= 0 && (i1 == objIdx || i2 == objIdx)) {
						int idx = (i1 == objIdx) ? i2 : i1;
						if (idx >= 0 && idx < nObj && Double.isNaN(distances[idx])) {
							distances[idx] = d;
							++count;
						}
					}
				} catch (EOFException eofe) {
					break;
				} catch (IOException ioe) {
					showMessage("Error reading data: " + ioe, true);
					error = true;
				}
			}
			try {
				stream.close();
			} catch (IOException e) {
			}
			if (error)
				return;
			if (count < 1) {
				showMessage("No distances loaded from " + path + "!", true);
				return;
			}
			showMessage("Got " + count + " distances", false);
			if (count < nObj - 1) {
				String str = "Only " + count + " out of the expected " + (nObj - 1) + " distances have been found.\n" + "Would you like to proceed anyway?";
				if (!Dialogs.askYesOrNo(ui.getMainFrame(), str, "Not all distances got!"))
					return;
			}
			String name = SimilarityComputer.getObjectName(geoObj);
			if (name == null) {
				name = geoObj.getIdentifier();
			}
			String attrName = "Distance to " + name + " by distance matrix " + CopyFile.getName(path);
			SimilarityComputer.putDistancesToTable(layer, name, distances, attrName, core);
			return;
		}
		SimilarityComputer simComp = null;
		if (layer.getType() == Geometry.point) {
			simComp = new PointSimilarityComputer();
		} else if (layer.getType() == Geometry.line && layer.getSubtype() == Geometry.movement) {
			simComp = new TrajectorySimilarityComputer();
		}
		if (simComp == null) {
			showMessage("Sorry... Not implemented yet!", true);
			return;
		}
		simComp.setLayer(layer);
		simComp.setSelectedObject(geoObj);
		simComp.setSystemCore(core);
		simComp.computeDistances();
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}

	/**
	* Opens the stream on the earlier specified data source. If the source
	* string starts with HTTP or FILE, accesses the source as a URL, otherwise
	* opens it as a local file
	*/
	protected InputStream openStream(String path) {
		if (path == null)
			return null;
		int idx = path.indexOf(':');
		boolean isURL = false;
		if (idx > 0) {
			String pref = path.substring(0, idx);
			if (pref.equalsIgnoreCase("HTTP") || pref.equalsIgnoreCase("FILE")) {
				isURL = true;
			}
		}
		try {
			if (isURL) { //try to access the source as a URL
				showMessage("Trying to open the URL " + path, false);
				System.out.println("Trying to open the URL " + path);
				URL url = new URL(path);
				System.out.println("URL=" + url);
				URLConnection urlc = url.openConnection();
				return urlc.getInputStream();
			} else
				return new FileInputStream(path);
		} catch (IOException ioe) {
			showMessage("Error accessing " + path + ": " + ioe, true);
		} catch (Throwable thr) {
			showMessage("Error accessing " + path + ": " + thr.toString(), true);
		}
		return null;
	}
}
