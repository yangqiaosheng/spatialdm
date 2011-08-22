package spade.analysis.transform;


public class Res extends java.util.ListResourceBundle {
	static final Object[][] contents = new String[][] { { "comparison", "Comparison" }, { "change", "Change" }, { "difference_to", "difference to" }, { "ratio_to", "ratio to" }, { "mean", "mean" }, { "median", "median" }, { "value", "value" },
			{ "object", "object" }, { "sel_object", "selected object" }, { "sel_value", "selected value" }, { "compute", "Compute" }, { "math_trans", "Arithmetic transformation" }, { "trans_ind", "transform individually" }, { "off", "off" },
			{ "log", "logarithm" }, { "log10", "logarithm 10" }, { "asc_order", "ascending order" }, { "desc_order", "descending order" }, { "Z_score", "Z-score" }, { "of", "of" }, { "save_transformed", "Store transformed data" },
			{ "transformed", "transformed" }, { "new_attr", "New attributes" }, { "new_attr_added", "New attributes will be added to the table" }, { "edit_attr_names", "You may edit the names of the attributes:" },
			{ "n_new_columns", "The number of the new table columns will be" }, { "where_to_store", "Where to store the transformed data?" }, { "use_previous", "use the columns created earlier" }, { "create_new", "create new columns" },
			{ "columns_required", "columns are required for the transformed data" }, { "data_stored", "The transformed data have been stored in the table" } };

	@Override
	public Object[][] getContents() {
		return contents;
	}
}