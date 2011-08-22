package data_load;

import spade.analysis.system.DataKeeper;
import spade.vis.database.AttributeDataPortion;
import spade.vis.dmap.DGeoLayer;
import spade.vis.spec.DataSourceSpec;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 05-Jan-2007
 * Time: 12:07:37
 * Builds a map layer on the basis of an appropriate table.
 */
public interface LayerFromTableGenerator {
	/**
	 * Checks if this generator is relevant according to the given metadata
	 * (table destription).
	 */
	public boolean isRelevant(DataSourceSpec spec);

	/**
	 * Builds a map layer on the basis of an appropriate table. It is assumed that
	 * the Data Source Specification of the table contains all necessary metadata.
	 * @param table - the table with source data and metadata for layer generation
	 * @param dKeeper - the keeper of all data currently loaded in the system
	 *                  (the generator may need additional data)
	 * @param currMapN - the index of the current map (Layer Manager) among all
	 *                   maps loaded in the system (currently, only one map exists)
	 * @return the layer built or null.
	 */
	public DGeoLayer buildLayer(AttributeDataPortion table, DataKeeper dKeeper, int currMapN);

	/**
	 * Returns the generated error message or null if successfully generated a layer
	 */
	public String getErrorMessage();
}
