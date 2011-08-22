package spade.analysis.tools.clustering;

import java.util.Vector;

import spade.analysis.tools.distances.DistanceMatrixUser;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 30-Aug-2007
 * Time: 15:52:31
 */
public class DistanceMatrixBasedClusterer extends LayerClusterer {

	protected float distMatrix[][] = null;
	public String matrixName = null;

	/**
	 * Makes a copy of this clusterer, ignoring any results obtained so far
	 */
	@Override
	public LayerClusterer getCopy() {
		DistanceMatrixBasedClusterer dmCl = new DistanceMatrixBasedClusterer();
		copyFields(dmCl);
		dmCl.distMatrix = distMatrix;
		dmCl.matrixName = matrixName;
		return dmCl;
	}

	/**
	 * Returns a list (vector) of names of available distance functions
	 */
	@Override
	public Vector getDistanceComputerNames() {
		Vector names = new Vector(1, 1);
		names.addElement("Pre-computed distances");
		return names;
	}

	public void setDistanceMatrix(float[][] distMatrix) {
		this.distMatrix = distMatrix;
	}

	public void setMatrixName(String matrixName) {
		this.matrixName = matrixName;
	}

	/**
	 * Returns true if it has all necessary internal settings
	 */
	@Override
	public boolean hasValidSettings() {
		return distMatrix != null;
	}

	/**
	 * Generates an appropriate distance computer depending on the type
	 * of the objects in the layer.If several distance computers exist,
	 * creates a computer with the given index
	 * @param methodN - the index of the distance computing method in the
	 *                  register of the available methods.
	 *                  This parameter is ignored.
	 * @return true if successful
	 */
	@Override
	public boolean generateDistanceComputer(int methodN) {
		distComp = new DistanceMatrixUser(distMatrix);
		if (matrixName != null) {
			description = "Pre-computed distances from " + matrixName;
		} else {
			description = "Pre-computed distances";
		}
		return true;
	}

	/**
	 * Forms cluster objects with indexes of the objects in the layer
	 */
	@Override
	protected Vector<DClusterObject> prepareData() {
		Vector<DClusterObject> objects = new Vector<DClusterObject>();
		for (int i = 0; i < layer.getObjectCount(); i++)
			if (layer.isObjectActive(i)) {
				String id = layer.getObjectId(i);
				int n = -1;
				try {
					n = Integer.parseInt(id);
				} catch (NumberFormatException e) {
				}
				if (n < 0) {
					continue;
				}
				DClusterObject clo = new DClusterObject(layer.getObject(i), id, i);
				clo.numId = n;
				objects.addElement(clo);
			}
		return objects;
	}

	/**
	 * Used for clustering
	 */
	@Override
	public double distance(DClusterObject o1, DClusterObject o2) {
		if (o1 == null || o2 == null)
			return 2 * distanceThreshold;
		return distComp.findDistance(o1, o2, true);
	}
}
