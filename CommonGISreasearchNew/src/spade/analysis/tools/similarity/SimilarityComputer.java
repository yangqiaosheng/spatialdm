package spade.analysis.tools.similarity;

import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;

import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.distances.DistanceComputer;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.OKDialog;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 24-Apr-2007
 * Time: 11:27:30
 * Computes the "distances" from a specified geographical object to the other
 * objects of the same layer using various distance measures.
 * Produces a table column with the distances.
 */
public abstract class SimilarityComputer {
	protected ESDACore core = null;
	/**
	 * The layer with point objects that need to be clustered
	 */
	protected DGeoLayer layer = null;
	/**
	 * The table with thematic data attached to the layer. If there is no such
	 * table, it will be produced. The column with the distances will be added to
	 * this table.
	 */
	protected DataTable table = null;
	/**
	 * The geographical object to which the distances are computed.
	 */
	protected DGeoObject selObj = null;
	/**
	 * The DistanceComputer is used to compute distances
	 */
	protected DistanceComputer distComp = null;
	/**
	 * Generated description of the distance function used
	 */
	protected String description = null;

	public void setSystemCore(ESDACore core) {
		this.core = core;
	}

	public void setLayer(DGeoLayer layer) {
		this.layer = layer;
		AttributeDataPortion tbl = layer.getThematicData();
		if (tbl != null && (tbl instanceof DataTable)) {
			table = (DataTable) tbl;
		}
	}

	public void setSelectedObject(DGeoObject selObj) {
		this.selObj = selObj;
	}

	protected static String getObjectName(DGeoObject gobj) {
		if (gobj == null)
			return null;
		String name = gobj.getLabel();
		if (name == null) {
			name = gobj.getSpatialData().getName();
		}
		return name;
	}

	/**
	 * Generates an appropriate distance computer depending on the type
	 * of the objects in the layer. May ask the user for parameters of the
	 * similarity computation. Returns true if successful.
	 */
	abstract protected boolean getDistanceComputer();

	/**
	 * Returns the generated description of the distance function used
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Can do some preparation of the data for the computing if necessary
	 */
	abstract protected void prepareData();

	/**
	 * Computes the distance for the object with the given index in the layer.
	 */
	abstract protected double getDistanceForObject(int idx);

	/**
	 * Computes the distance between the objects with the given indexes in the layer.
	 */
	abstract protected double getDistanceBetweenObjects(int idx1, int idx2);

	/**
	 * Performs the computation and produces a column with the distances to the
	 * given object. Return true if successful.
	 */
	public boolean computeDistances() {
		if (layer == null || selObj == null || layer.getObjectCount() < 1)
			return false;
		if (!getDistanceComputer()) {
			showMessage("Failed to produce a Distance Computer!", true);
			return false;
		}
		distComp.setCoordinatesAreGeographic(layer.isGeographic());
		prepareData();
		int nObj = layer.getObjectCount();
		double distances[] = new double[nObj];
		for (int i = 0; i < layer.getObjectCount(); i++) {
			distances[i] = (layer.getObject(i).equals(selObj)) ? 0.0 : getDistanceForObject(i);
		}
		String name = getObjectName(selObj);
		if (name == null) {
			name = selObj.getIdentifier();
		}
		String attrName = "Distance to " + name;
		if (description != null) {
			attrName += " (" + description + ")";
		}
		return putDistancesToTable(layer, name, distances, attrName, core);
	}

	public static boolean putDistancesToTable(DGeoLayer layer, String objName, double distances[], String attrName, ESDACore core) {
		if (layer == null || distances == null || distances.length < 1)
			return false;
		Panel p = new Panel(new BorderLayout());
		p.add(new Label("Name of the new table column?"), BorderLayout.WEST);
		TextField tf = new TextField(attrName, 30);
		p.add(tf, BorderLayout.CENTER);
		Panel pp = new Panel(new ColumnLayout());
		pp.add(p);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Resulting column name?", true);
		dia.addContent(pp);
		dia.show();
		if (dia == null || dia.wasCancelled()) {
			dia = null;
			return false;
		}
		String str = tf.getText();
		if (str != null) {
			str = str.trim();
			if (str.length() > 0) {
				attrName = str;
			}
		}
		DataTable table = null;
		AttributeDataPortion tbl = layer.getThematicData();
		if (tbl != null && (tbl instanceof DataTable)) {
			table = (DataTable) tbl;
		}
		boolean newTable = table == null;
		if (newTable) {
			table = new DataTable();
			table.setEntitySetIdentifier(layer.getEntitySetIdentifier());
			table.setName(layer.getName());
		}
		table.addAttribute(attrName, "attr_" + (table.getAttrCount() + 1), AttributeTypes.real);
		int cn = table.getAttrCount() - 1;
		for (int i = 0; i < Math.min(layer.getObjectCount(), distances.length); i++)
			if (!Double.isNaN(distances[i])) {
				DGeoObject gobj = layer.getObject(i);
				DataRecord rec = null;
				if (newTable || gobj.getData() == null || !(gobj.getData() instanceof DataRecord)) {
					rec = new DataRecord(gobj.getIdentifier(), getObjectName(gobj));
					table.addDataRecord(rec);
					gobj.setThematicData(rec);
				} else {
					rec = (DataRecord) gobj.getData();
				}
				rec.setNumericAttrValue(distances[i], String.valueOf(distances[i]), cn);
			}
		if (newTable) {
			DataLoader dataLoader = core.getDataLoader();
			int tN = dataLoader.addTable(table);
			dataLoader.linkTableToMapLayer(tN, layer);
		}
		core.getUI().showMessage("The distances have been put in table column " + attrName, false);
		return true;
	}

	/**
	 * Computes a full matrix of pair-wise distances among the objects.
	 * The order corresponds to the order of the table records
	 */
	public float[][] computeDistanceMatrix() {
		if (layer == null || layer.getObjectCount() < 1)
			return null;
		DataTable table = null;
		AttributeDataPortion tbl = layer.getThematicData();
		if (tbl != null && (tbl instanceof DataTable)) {
			table = (DataTable) tbl;
		}
		if (table == null) {
			showMessage("The layer has no DataTable!", true);
			return null;
		}
		float distMatrix[][] = table.getDistanceMatrix();
		int nObj = tbl.getDataItemCount();
		if (distMatrix != null && distMatrix.length != nObj) {
			distMatrix = null;
		}
		if (distMatrix == null) {
			long freeMem = Runtime.getRuntime().freeMemory(), needMem = ((long) nObj) * nObj * Float.SIZE / 8;
			if (needMem >= freeMem / 3) {
				System.out.println("Garbage collector started, free memory before: " + freeMem);
				Runtime.getRuntime().gc();
				freeMem = Runtime.getRuntime().freeMemory();
				System.out.println("Garbage collector finished, free memory after: " + freeMem);
			}
			try {
				distMatrix = new float[nObj][nObj];
			} catch (OutOfMemoryError out) {
				System.out.println("Similarity computing: not enough memory for distance matrix, need: " + needMem);
				showMessage("Similarity computing: not enough memory for distance matrix, need: " + needMem, true);
				return null;
			}
			System.out.println("Similarity computing: distance matrix constructed!");
			showMessage("Similarity computing: distance matrix constructed!", false);
		} else {
			System.out.println("Similarity computing: reusing previously created distance matrix");
			showMessage("Similarity computing: reusing previously created distance matrix", false);
		}
		if (!getDistanceComputer()) {
			showMessage("Failed to produce a Distance Computer!", true);
			return null;
		}
		for (int i = 0; i < nObj; i++) {
			for (int j = 0; j < nObj; j++)
				if (i == j) {
					distMatrix[i][j] = 0;
				} else {
					distMatrix[i][j] = Float.NaN;
				}
		}
		distComp.setCoordinatesAreGeographic(layer.isGeographic());
		prepareData();
		int nPairs = 0;
		for (int i = 0; i < nObj - 1; i++) {
			int idx1 = layer.getObjectIndex(table.getDataItemId(i));
			if (idx1 < 0) {
				continue;
			}
			for (int j = i + 1; j < nObj; j++) {
				int idx2 = layer.getObjectIndex(table.getDataItemId(j));
				if (idx2 < 0) {
					continue;
				}
				distMatrix[i][j] = distMatrix[j][i] = (float) getDistanceBetweenObjects(idx1, idx2);
				++nPairs;
				if (nPairs % 100 == 0) {
					showMessage(+nPairs + " distances computed", false);
				}
			}
		}
		table.setDistanceMatrix(distMatrix);
		String title = distComp.getMethodName(), parStr = distComp.getParameterDescription();
		if (parStr != null) {
			title += "; " + parStr;
		}
		title = Dialogs.askForStringValue(CManager.getAnyFrame(), "Give an explanatory title for the distance matrix?", title, null, "Title", false);
		table.setDistMatrixTitle(title);
		return distMatrix;
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}
}
