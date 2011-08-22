package data_load.intelligence;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 12-Jul-2004
 * Time: 16:53:03
 */
public class Res extends java.util.ListResourceBundle {
	static final Object[][] contents = new String[][] {
			{ "index_table", "Index a parameter-dependent table" },
			{ "No_table_found", "No table found!" },
			{ "No_tables_without_params", "There is nothing to index: no tables without parameters!" },
			{ "Select_table_to_index", "Select the table to index" },
			{ "No_data_in_table", "No data in the table" },
			{ "Time_ref_in_col", "Time references in columns" },
			{ "Table_indexing_stage", "Table indexing; stage" },
			{ "of", "of" },
			{ "Extr_time_ref", "Finding and extracting time references specified in one or more columns" },
			{ "Task", "Task" },
			{ "Explanations", "Explanations" },
			{ "Examples", "Examples" },
			{ "Has_columns_with_time_ref", "Does the table contain one or more columns that " + "specify the time moments the data in rows refer to?" },
			{ "Columns", "Columns" },
			{ "Values", "Values" },
			{ "Show_values", "Show values" },
			{ "Select", "Select" },
			{ "Sort_alpha", "Sort alphabetically" },
			{ "Time_refs_are_in_col", "Time references are in columns:" },
			{ "Col_name", "Column name" },
			{ "Format", "Format" },
			{ "Simple_val", "Simple value" },
			{ "Comp_val", "Compound value" },
			{ "Meaning_or_template", "Meaning or template" },
			{ "No_selection_yet", "No columns have been selected yet" },
			{ "Remove", "Remove" },
			{ "second", "second" },
			{ "minute", "minute" },
			{ "hour", "hour" },
			{ "day", "day" },
			{ "month", "month" },
			{ "year", "year" },
			{ "abstract", "abstract time count" },
			{ "enter_template", "<enter the template>" },
			{ "Result_col_name", "Resulting column or parameter name:" },
			{ "Result_template", "Template for displaying resulting dates/times:" },
			{ "No_templ_for_abstract", "No template is used for displaying abstract time counts!" },
			{ "This_is_param", "This is a parameter" },
			{ "Keep_orig_columns", "Keep original columns in the resulting table" },
			{ "Retrieve_times", "Retrieve times" },
			{
					"Time_ref_expl1",
					"Data in a table may refer to different time moments " + "such as days or years. For example, a table may contain values of " + "population number and gross domestic product in various countries in "
							+ "a series of years. One of the possible ways to specify the temporal references " + "is that the table has a column (or sometimes a group of columns) with " + "values indicating these time moments." },
			{
					"Time_ref_expl2",
					"Specification of time moments may be simple or compound. " + "Simple are, for example, years: 2001, 2002, 2003, etc. Compound specifications " + "include several components, as, for example, dates consisting of day, month, and"
							+ "year: 25.09.1998 or 31/12/2004. There are two possible ways to specify " + "compound time moments. First, the components can come in separate columns. "
							+ "Thus, there may be a column with days, a column with months, and a column " + "with years. The system is able to retrieve the components from the separate "
							+ "columns and unite them. Second, the components may be specified in a single " + "column according to a certain template, for example, dd.mm.yyyy, where d "
							+ "means day, m means month, and y means year. Each symbol in a template must " + "be placed in the positions from which the corresponding component of a "
							+ "compound time moment need to be retrieved. In templates, besides d, m, " + "and y, it is possible to use the symbols h (hour), t (minute), and s (second). "
							+ "The system regards any other symbols as delimiters and ignores them. Hence, " + "the templates dd.mm.yyyy and dd/mm/yyyy are equivalent." },
			{
					"Time_ref_expl3",
					"Time references in a table must be treated as parameters " + "of the dataset if for each (geographical) object there are several data " + "records (table rows) with values of the same attributes referring to "
							+ "different time moments. For example, for each country, there are values " + "of population number and gross domestic product in years 2000, 2001, 2002," + "and 2003. In this case, year is a parameter of this dataset." },
			{
					"Time_ref_expl4",
					"However, time references are not always parameters. For " + "example, in data about some events (earthquakes, traffic incidents, " + "observations of a rare bird species, etc.) the date of an event is not "
							+ "a parameter but an attribute of this event." },
			{
					"Time_ref_example1",
					"Example 1. A table has the following columns: country, " + "year, population number, gross domestic product, and so on. Time references " + "are in the column year. The format of the time references is simple, "
							+ "their meaning is year. Year is a parameter of this dataset." },
			{
					"Time_ref_example2",
					"Example 2. A table has the following columns: country, " + "year, month, total export, total import, and so on. Time references are in " + "columns year and month. The format of the values in both columns is simple. "
							+ "The meaning of the column year is year, the meaning of the column month is month. " + "The system will unite components retrieved from these columns into compound "
							+ "time references consisting of months and years. The resulting time references " + "must be treated as a parameter of this dataset." },
			{
					"Time_ref_example3",
					"Example 3. A table has the following columns: observation " + "identifier, latitude, longitude, observation date, who observed, what observed, " + "etc. The column \"observation date\" contains time references specified as "
							+ "compound values consisting of day, month, and year. The values correspond " + "to the template dd.mm.yyyy. The observation date is not a parameter in "
							+ "this dataset but one of the attributes describing an observation." },
			{ "No_values_found", "No values found" },
			{ "no_time_ref_column_selected", "No column with time references selected!" },
			{ "no_template_for_column", "No template specified for the column" },
			{ "no_time_symbols", "There are no time symbols" },
			{ "in_template_for_column", "in the template specified for the column" },
			{ "Symbols", "Symbols" },
			{ "must_come_in_sequence", "must come in uninterrupted sequence" },
			{ "and", "and" },
			{ "but_no", "but no" },
			{ "found", "found" },
			{ "Repeated_occurrence_of", "Repeated occurrence of" },
			{ "in_more_than_one_columns", "in more than one columns" },
			{ "Abstract_only_in_one_column", "Abstract time references can only be " + "specified in a single column and cannot be combined with other reference types." },
			{ "Elements", "Elements" },
			{ "Element", "Element" },
			{ "absent_in_table_but_occurs_in_template", "is absent in the table but occurs in the resulting template" },
			{ "Resulting_template_has_no_positions_for", "Resulting template has no positions for element" },
			{ "No_valid_description", "No valid description of time references is specified!" },
			{ "Failed_retrieve_times", "Failed to retrieve any time moments from the specified columns!" },
			{ "Retrieved", "Retrieved" },
			{ "time_reference", "time reference" },
			{ "different_time_references", "different time references" },
			{ "Time_range", "Time range" },
			{ "from", "from" },
			{ "to", "to" },
			{ "Failed_to_retrieve_in", "Failed to transform data into time references in" },
			{ "cases_of", "cases of" },
			{ "All_orig_values_processed", "All original values successfully transformed into time references!" },
			{ "Diff_times", "Different time references retrieved:" },
			{ "Transformation_results", "Transformation results:" },
			{ "Which_attributes_depend_on", "Which attributes depend on the " },
			{ "parameter", "parameter" },
			{ "parameters", "parameters" },
			{
					"Param_expl_1",
					"A dataset typically consists of several components differing " + "in their role. Some of the components are results of measurements, observations, " + "calculations, etc., while other reflect the context of obtaining these "
							+ "measurements, observations, or calculations. The context may include the " + "time moments when the measurements etc. were done, locations in space, "
							+ "objects or groups of objects whose properties were measured or observed, " + "and so on." },
			{ "Param_expl_2",
					"Components of the first kind are called attributes and " + "components of the second kind - parameters. Each value of an attribute " + "present in a dataset refers to a particular combination of values of the " + "parameters." },
			{
					"Param_expl_3",
					"For example, data about daily sales of some company include such " + "attributes as the volume of sales and the income. Values of these attributes " + "refer to different stores where products of this company are sold, dates, "
							+ "and articles: store S on date D sold X units of the article A for the total " + "price Z euro. Here, S, D, and A are values of the parameters store, date, and "
							+ "article while X and Z are values of the attributes volume and income." },
			{
					"Col_param_expl_1",
					"Values of parameters are often specified in columns of " + "a table. Thus, a table with the sales data may have a column with the " + "names or locations of the stores, a column with the dates, a column with "
							+ "the names of the articles, and, of course, a column with the volume figures " + "and a column with the total prices. The first three columns specify the "
							+ "context for the numbers in the remaining two columns, or, in other words, " + "define the meaning of these numbers." },
			{ "Col_param_expl_2", "On this stage of the indexing process, your task is to " + "specify which columns of the table (if any) contain parameter values. " + "Temporal parameters such as dates are processed separately from non-temporal." },
			{
					"Col_param_example1",
					"Data about daily sales of some company consists of the " + "items (records) of the following structure: store S on date D sold X units " + "of the article A for the total price Z euro. The data are organised in a "
							+ "table with 5 columns: store name, date, article, amount sold, total price." },
			{ "Col_param_example2",
					"The columns \"store name\", \"date\", and \"article\" " + "are parameters of this table and the remaining two columns are attributes " + "whose values refer to particular combinations of values of the parameters." },
			{ "Col_param_example3", "The parameter \"date\" is a temporal parameter, which " + "is processed in a separate step of the indexing process. In this step, " + "only non-temporal parameters need to be indicated." },
			{ "Extr_col_params", "Finding and extracting non-temporal parameters specified in table columns" },
			{ "Has_columns_with_params", "Does the table contain one or more columns with " + "values of non-temporal  parameters the data in rows refer to?" }, { "no_param_column_selected", "No column with parameter values selected!" },
			{ "Param_in_col", "Non-temporal references in columns" }, { "Sorted", "Sorted" }, { "Reorder", "Reorder" }, { "Non_time_refs_are_in_col", "Non-temporal references are in columns:" }, { "Sorting", "Sorting" },
			{ "Order_param_values", "Order parameter values" }, { "Wait_table_restructuring", "Wait... Parameter retrieval and table restructuring in progress (may be long!)" }, { "Table_restructuring_complete", "Table restructuring complete!" },
			{ "protract_values", "Protract known values forward in time" }, { "explain_protract", "where values are missing, insert the values from previous time moments" }, { "", "" }, { "", "" } };

	public Object[][] getContents() {
		return contents;
	}
}
