package spade.analysis.tools.somlink;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Vector;

import spade.vis.database.Attribute;
import spade.vis.database.DataTable;
import spade.vis.database.Parameter;
import spade.vis.dmap.DGeoLayer;
import spade.vis.geometry.Computing;
import spade.vis.geometry.RealPoint;
import useSOM.SOMCellInfo;
import useSOM.SOMResult;
import external.gunther_foidl.SammonsProjection;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Oct 30, 2009
 * Time: 4:07:28 PM
 * Describes an application of SOM to some data
 */
public class SOMApplInfo {
	/**
	 * Constants to indicate the nature of the objects to which the SOM has been applied
	 */
	public static final char Places = 'p', Times = 't', Other_Objects = 0;
	/**
	 * Constants to indicate the nature of the attributes (features of the objects)
	 * to which the SOM has been applied
	 */
	public static final char Time_Series = 't', Spatial_Distributions = 's', Parametric_Attribute = 'p', Multiattribute_Profiles = 'm', Other_Features = 0;
	/**
	 * The table with the original data to which the SOM has been applied
	 */
	public DataTable tblSOM = null;
	/**
	 * The map layer corresponding to the table (may be absent)
	 */
	public DGeoLayer tblSOMlayer = null;
	/**
	 * The parameter to the values of which the SOM has been applied
	 */
	public Parameter paramSOM = null;
	/**
	 * The top-level attributes used in SOM (i.e. there may be attributes with children)
	 */
	public Vector<Attribute> selAttrs = null;
	/**
	 * Indicates whether the SOM has been applied to the values of the parameter
	 * as the objects
	 */
	public boolean applySOMtoParam = false;
	/**
	 * The nature of the objects to which the SOM has been applied
	 */
	public char objType = Other_Objects;
	/**
	 * The nature of the attributes (features of the objects)
	 * to which the SOM has been applied
	 */
	public char featuresType = Other_Features;
	/**
	 * Images representing the objects to which the SOM has been applied
	 * (may be absent)
	 */
	public HashMap<Integer, BufferedImage> images = null;
	/**
	 * The table containing the SOM result (identifiers of the SOM cells) in its column.
	 * This may be the same table as tblSOM if the SOM has been applied to the objects
	 * described in the table or this may be a new table if the SOM has been applied
	 * to the values of the parameter paramSOM.
	 */
	public DataTable tblSOMResult = null;
	/**
	 * The index of the column in the table tblSOMResult containing the identifiers
	 * of the SOM cells.
	 */
	public int colIdxSOMCells = -1;
	/**
	 * Describes the result obtained from SOM
	 */
	public SOMResult somRes = null;
	/**
	 * A table with the SOM neurons and their features.
	 * The first 2 columns in the table contain the x and y positions
	 * of the respective cell:
	 * tbl.addAttribute("x","x", AttributeTypes.integer);
	 * tbl.addAttribute("y","y", AttributeTypes.integer);
	 */
	public DataTable tblSOMneuro = null;
	/**
	 * Projection of the SOM neurons to 2-dimensional space obtained
	 * e.g. by means of Sammon's projection algorithm
	 */
	public RealPoint projection[][] = null;

	/**
	 * Returns a copy of itself
	 */
	public SOMApplInfo getCopy() {
		SOMApplInfo somapi = new SOMApplInfo();
		somapi.tblSOM = tblSOM;
		somapi.tblSOMlayer = tblSOMlayer;
		somapi.paramSOM = paramSOM;
		somapi.selAttrs = selAttrs;
		somapi.applySOMtoParam = applySOMtoParam;
		somapi.objType = objType;
		somapi.featuresType = featuresType;
		somapi.images = images;
		somapi.tblSOMResult = tblSOMResult;
		somapi.colIdxSOMCells = colIdxSOMCells;
		somapi.somRes = somRes;
		somapi.tblSOMneuro = tblSOMneuro;
		return somapi;
	}

	/**
	 * Finds the minimum and maximum distances between neighbouring SOM cells
	 * @return an array with 2 numbers: 0) minimum; 1) maximum
	 */
	public double[] getMinMaxDistanceBtwNbs() {
		if (somRes == null || somRes.cellInfos == null)
			return null;
		double minmax[] = { Double.POSITIVE_INFINITY, 0. };
		for (int x = 0; x < somRes.xdim; x++) {
			for (int y = 0; y < somRes.ydim; y++)
				if (somRes.cellInfos[x][y].distances != null) {
					for (double d : somRes.cellInfos[x][y].distances) {
						if (d < minmax[0]) {
							minmax[0] = d;
						}
						if (d > minmax[1]) {
							minmax[1] = d;
						}
					}
				}
		}
		if (Double.isInfinite(minmax[0]) || minmax[1] <= minmax[0])
			return null;
		return minmax;
	}

	/**
	 * An instance of SammonsProjection is created once and then can
	 * be reused, i.e. the algorithm can be resumed to run some more
	 * iterations
	 */
	protected SammonsProjection samPr = null;

	/**
	 * Builds a projection of the SOM neurons to 2D space so that similar neurond are
	 * close and dissimilar neurons are distant. Uses the Sammon's projection algorithm
	 * with the positions of the neurons in the matrix as an initial projection.
	 * Assigns the results to the internal variable "projection".
	 */
	public void getProjection(int nIterations) {
		if (somRes == null || somRes.cellInfos == null)
			return;
		int count = somRes.xdim * somRes.ydim;
		double iniProj[][] = new double[count][2];
		int idx = -1;
		for (int y = 0; y < somRes.ydim; y++) {
			for (int x = 0; x < somRes.xdim; x++) {
				++idx;
				iniProj[idx][0] = x;
				iniProj[idx][1] = y;
			}
		}
		double distMatr[][] = null;
		if (samPr == null) {
			SOMCellInfo ci[] = new SOMCellInfo[count];
			idx = -1;
			for (int y = 0; y < somRes.ydim; y++) {
				for (int x = 0; x < somRes.xdim; x++) {
					++idx;
					ci[idx] = somRes.cellInfos[x][y];
				}
			}
			distMatr = new double[count][count];
			double maxD = 0;
			for (int i = 0; i < count - 1; i++) {
				distMatr[i][i] = 0;
				for (int j = i + 1; j < count; j++) {
					distMatr[i][j] = distMatr[j][i] = Computing.getMinkowskiDistance(ci[i].neuronFV, ci[j].neuronFV, 2);
					if (maxD < distMatr[i][j]) {
						maxD = distMatr[i][j];
					}
				}
			}
			//correction of the distances in the distance matrix so that they are comparable
			//with the euclidean distances in the SOM matrix
			double maxDistSOM = Math.sqrt(somRes.ydim * somRes.ydim + somRes.xdim * somRes.xdim);
			double factor = maxDistSOM / maxD;
			for (int i = 0; i < count - 1; i++) {
				for (int j = i + 1; j < count; j++) {
					distMatr[i][j] *= factor;
					distMatr[j][i] *= factor;
				}
			}
		} else {
			distMatr = samPr.getDistanceMatrix();
		}

		samPr = new SammonsProjection(distMatr, iniProj, 2, nIterations, true);
		samPr.CreateMapping();
		double proj[][] = samPr.getProjection();
		if (proj == null)
			return;
		projection = new RealPoint[somRes.xdim][somRes.ydim];
		idx = -1;
		for (int y = 0; y < somRes.ydim; y++) {
			for (int x = 0; x < somRes.xdim; x++) {
				++idx;
				projection[x][y] = new RealPoint((float) proj[idx][0], (float) proj[idx][1]);
				System.out.println("(" + x + "," + y + ") >> (" + projection[x][y].x + "," + projection[x][y].y + ")");
			}
		}
	}

	public void refineProjection(int nIterations) {
		if (samPr == null) {
			getProjection(nIterations);
			return;
		}
		samPr.runMore(nIterations);
		double proj[][] = samPr.getProjection();
		if (proj == null)
			return;
		projection = new RealPoint[somRes.xdim][somRes.ydim];
		int idx = -1;
		for (int y = 0; y < somRes.ydim; y++) {
			for (int x = 0; x < somRes.xdim; x++) {
				++idx;
				projection[x][y] = new RealPoint((float) proj[idx][0], (float) proj[idx][1]);
				System.out.println("(" + x + "," + y + ") >> (" + projection[x][y].x + "," + projection[x][y].y + ")");
			}
		}
	}

}
