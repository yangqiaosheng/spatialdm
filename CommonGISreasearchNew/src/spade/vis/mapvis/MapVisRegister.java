package spade.vis.mapvis;

import java.util.ResourceBundle;

import spade.lib.lang.Language;

/**
* Contains information about implementation of visualization methods necessary
* for construction of cartographic visualizers and manupulators.
*/
public class MapVisRegister {
	static ResourceBundle res = Language.getTextResource("spade.vis.mapvis.Res");
	/**
	* Information about implementation of visualization methods necessary for
	* construction of visualizers and manupulators. For each implemented
	* visualization method there is a line specifying:
	* 1) method identifier (internal, not visible for users)
	* 2) method name (to be shown to users)
	* 3) name of the class implementing the method for area objects
	* 4) name of the class implementing the method for sign objects (if different
	*    from (2), otherwise null)
	* 5) name of the class implementing the manipulator
	* Full class names must be given!
	* If there is no implementation for a method yet, null stands for the class
	* name.
	* The methods are listed in the preferred order as they should be shown to users
	*/
	static protected final String mimpl[][] = {
			// following string: "Unclassified choropleth map"
			{ "value_paint", res.getString("Unclassified"), "spade.vis.mapvis.NumValuePainter", "spade.vis.mapvis.NumValueSignPainter", "spade.analysis.manipulation.VisComparison" },
			// following string: "Classified choropleth map"
			{ "class1D", res.getString("Classified_choropleth"), "spade.analysis.classification.NumAttr1Classifier", null, "spade.analysis.classification.NumAttr1ClassManipulator" },
			// following string: "Cross-classification"
			{ "class2D", res.getString("Cross_classification"), "spade.analysis.classification.NumAttr2Classifier", null, "spade.analysis.classification.NumAttr2ClassManipulator" },
			// following string: "Standalone bars"
			{ "standalone_bars", res.getString("Standalone_bars"), "spade.vis.mapvis.SingleBarDrawer", null, "spade.analysis.manipulation.VisComparison" },
			// following string: "Graduated circles"
			{ "circles", res.getString("Graduated_circles"), "spade.vis.mapvis.CirclePresenter", null, "spade.analysis.manipulation.CircleManipulator" },
			// following string: "Parallel bars"
			{ "parallel_bars", res.getString("Parallel_bars"), "spade.vis.mapvis.MultiBarDrawer", null, "spade.analysis.manipulation.MultiVisComparison" },
			// following string: "Pies"
			{ "pies", res.getString("Pies"), "spade.vis.mapvis.PieChartDrawer", null, "spade.analysis.manipulation.PieChartManipulator" },
			// following string: "Segmented bars"
			//{"segmented_bars",res.getString("Segmented_bars"),null,null,null},
			// following string: "double-sided bars"
			//{"doublesided_bars",res.getString("double_sided_bars"),null,null,null},
			// following string: "inclusion signs"
			//{"inclusion_signs",res.getString("inclusion_signs1"),null,null,null},
			// following string: "radial bars"
			//{"radial_bars",res.getString("radial_bars1"),null,null,null},
			// following string: "Triangles"
			{ "triangles", res.getString("Triangles"), "spade.vis.mapvis.TwoNumberTriangleDrawer", null, null },
			// following string: "Parallel bars"
			{ "class_bars", res.getString("Class_bars"), "spade.vis.mapvis.MultiNumAttr1ClassDrawer", null, "spade.analysis.classification.MultiNumAttr1ClassManipulator" },
			// following string: "Dominance"
			{ "class_dominant", res.getString("Dominance"), "spade.analysis.classification.ArgMaxClassifier", null, "spade.analysis.classification.ArgMaxClassManipulator" },
			// following string: "multiple maps"
			{ "multiples", res.getString("multiple_choro_maps"), "spade.vis.mapvis.MultiMapNumPainter", "spade.vis.mapvis.MultiMapNumSignPainter", "spade.analysis.manipulation.MultiMapVisComparison" },
			{ "multiple_class_maps", res.getString("multiple_class_maps"), "spade.vis.mapvis.MultiMapNumAttr1ClassDrawer", "spade.vis.mapvis.MultiMapNumAttr1ClassSignDrawer", "spade.analysis.classification.MultiNumAttr1ClassManipulator" },
			{ "multi_bars", res.getString("multiple_bar_maps"), "spade.vis.mapvis.MultiMapBarDrawer", null, "spade.analysis.manipulation.MultiMapVisComparison" },
			{ "multi_circles", res.getString("multiple_circle_maps"), "spade.vis.mapvis.MultiMapCircleDrawer", null, "spade.analysis.manipulation.MultiMapCircleManipulator" },
			// following string: "Qualitative colouring"
			{ "qualitative_colour", res.getString("Qualitative_colouring"), "spade.analysis.classification.QualitativeClassifier", null, "spade.analysis.classification.AnyClassManipulator" },
			{ "shapes", "Shapes of symbols", "spade.vis.mapvis.SimpleSignPresenter", null, "spade.analysis.manipulation.SimpleSignManipulator" },
			{ "colors+shapes", "Colors + shapes of symbols", "spade.vis.mapvis.ColorAndShapeVisualizer", null, "spade.analysis.manipulation.ColorAndShapeManipulator" },
			// following string: "Icons"
			{ "icons", res.getString("Icons"), "spade.vis.mapvis.IconPresenter", null, null },
			// following string: ...
			{ "qualitative_cross_class", res.getString("Qualitative_cross_classification"), "spade.analysis.classification.QualitativeCrossClassifier", null, "spade.analysis.classification.AnyClassManipulator" },
			// following string: "Stacks"
			{ "stacks", res.getString("Stacks"), "spade.vis.mapvis.StackDrawer", null, "spade.analysis.manipulation.StackManipulator" },
			// following string: "Utility bars"
			{ "utility_bars", res.getString("Utility_bars"), "spade.vis.mapvis.UtilitySignDrawer", null, "spade.analysis.manipulation.UtilitySignsManipulator" },
			// following string: "Utility wheels"
			{ "utility_wheels", res.getString("Utility_wheels"), "spade.vis.mapvis.UtilityWheelDrawer", null, "spade.analysis.manipulation.UtilitySignsManipulator" },
			{ "line_thickness", res.getString("line_thickness"), "spade.vis.mapvis.LineThicknessVisualiser", null, "spade.analysis.manipulation.SizeFocuser" },
			{ "line_thickness_color", res.getString("line_thickness_color"), "spade.vis.mapvis.LineThicknessAndColorVisualiser", null, "spade.analysis.manipulation.SizeAndColorManipulator" },
			{ "direction&speed", "Direction and speed charts", "spade.analysis.tools.moves.DirectionAndSpeedVisualizer", null, "spade.analysis.tools.moves.DirectionAndSpeedManipulator" }, };
	/**
	* Special visualization methods used only for time-series data. Typically these
	* methods are not proposed for selection together with the other methods.
	*/
	static protected final String timeVisMethods[][] = {
	// following string: "Unclassified choropleth map"
	{ "value_flow", res.getString("Value_flow_diagrams"), //0
			"spade.time.vis.ValueFlowVisualizer", null, "spade.time.vis.ValueFlowManipulator" } };

	/**
	 * The methods suitable for area objects. For each method there is a line
	 * specifying:
	 * 1) method identifier (internal, not visible to users)
	 * 2) method name (to be shown to users)
	 * Note that the same methods may have different names depending on the type
	 * of objects they are aplies to
	 */
	static protected final String methodsForAreas[][] = {
			// following string: "Unclassified choropleth map"
			{ "value_paint", res.getString("Unclassified") },
			// following string: "Classified choropleth map"
			{ "class1D", res.getString("Classified_choropleth") },
			// following string: "Cross-classification"
			{ "class2D", res.getString("Cross_classification") },
			// following string: "Standalone bars"
			{ "standalone_bars", res.getString("Standalone_bars") },
			// following string: "Graduated circles"
			{ "circles", res.getString("Graduated_circles") },
			// following string: "Parallel bars"
			{ "parallel_bars", res.getString("Parallel_bars") },
			// following string: "Pies"
			{ "pies", res.getString("Pies") },
			// following string: "Triangles"
			{ "triangles", res.getString("Triangles") },
			// following string: "Class mosaics"
			{ "class_bars", res.getString("Class_bars") },
			// following string: "Dominance"
			{ "class_dominant", res.getString("Dominance") },
			// following string: "multiple maps"
			{ "multiples", res.getString("multiple_choro_maps") }, { "multiple_class_maps", res.getString("multiple_class_maps") }, { "multi_bars", res.getString("multiple_bar_maps") }, { "multi_circles", res.getString("multiple_circle_maps") },
			// following string: "Qualitative colouring"
			{ "qualitative_colour", res.getString("Qualitative_colouring") },
			// following string: "Icons"
			{ "shapes", "Shapes of symbols" }, { "colors+shapes", "Colors + shapes of symbols" }, { "icons", res.getString("Icons") }, { "qualitative_cross_class", res.getString("Qualitative_cross_classification") },
			// following string: "Stacks"
			{ "stacks", res.getString("Stacks") },
			// following string: "Utility bars"
			{ "utility_bars", res.getString("Utility_bars") },
			// following string: "Utility wheels"
			{ "utility_wheels", res.getString("Utility_wheels") } };

	/**
	 * The methods suitable for point objects. For each method there is a line
	 * specifying:
	 * 1) method identifier (internal, not visible to users)
	 * 2) method name (to be shown to users)
	 * Note that the same methods may have different names depending on the type
	 * of objects they are aplies to
	 */
	static protected final String methodsForPoints[][] = {
			// following string: "Unclassified shading"
			{ "value_paint", res.getString("Unclassified_shading") },
			// following string: "Classified shading"
			{ "class1D", res.getString("Classified_shading") },
			// following string: "Cross-classification"
			{ "class2D", res.getString("Cross_classification") },
			// following string: "Standalone bars"
			{ "standalone_bars", res.getString("Standalone_bars") },
			// following string: "Graduated circles"
			{ "circles", res.getString("Graduated_circles") },
			// following string: "Parallel bars"
			{ "parallel_bars", res.getString("Parallel_bars") },
			// following string: "Pies"
			{ "pies", res.getString("Pies") },
			// following string: "Triangles"
			{ "triangles", res.getString("Triangles") },
			// following string: "Class mosaics"
			{ "class_bars", res.getString("Class_bars") },
			// following string: "Dominance"
			{ "class_dominant", res.getString("Dominance") },
			// following string: "multiple maps"
			{ "multiples", res.getString("multiple_maps_shading") }, { "multiple_class_maps", res.getString("multiple_maps_class_shading") }, { "multi_bars", res.getString("multiple_bar_maps") },
			{ "multi_circles", res.getString("multiple_circle_maps") },
			// following string: "Qualitative colouring"
			{ "qualitative_colour", res.getString("Qualitative_colouring") },
			// following string: "Icons"
			{ "shapes", "Shapes of symbols" }, { "colors+shapes", "Colors + shapes of symbols" }, { "icons", res.getString("Icons") }, { "qualitative_cross_class", res.getString("Qualitative_cross_classification") },
			// following string: "Stacks"
			{ "stacks", res.getString("Stacks") },
			// following string: "Utility bars"
			{ "utility_bars", res.getString("Utility_bars") },
			// following string: "Utility wheels"
			{ "utility_wheels", res.getString("Utility_wheels") } };

	/**
	 * The methods suitable for line objects. For each method there is a line
	 * specifying:
	 * 1) method identifier (internal, not visible to users)
	 * 2) method name (to be shown to users)
	 * Note that the same methods may have different names depending on the type
	 * of objects they are aplies to
	 */
	static protected final String methodsForLines[][] = {
			// following string: "Unclassified shading"
			{ "value_paint", res.getString("Unclassified_shading") },
			// following string: "Classified shading"
			{ "class1D", res.getString("Classified_shading") },
			// following string: "Cross-classification"
			{ "class2D", res.getString("Cross_classification") },
			// following string: "Dominance"
			{ "class_dominant", res.getString("Dominance") },
			// following string: "multiple maps"
			{ "multiples", res.getString("multiple_maps_shading") }, { "multiple_class_maps", res.getString("multiple_maps_class_shading") },
			// following string: "Qualitative colouring"
			{ "qualitative_colour", res.getString("Qualitative_colouring") }, { "qualitative_cross_class", res.getString("Qualitative_cross_classification") }, { "line_thickness", res.getString("line_thickness") },
			{ "line_thickness_color", res.getString("line_thickness_color") } };

	static public String[][] getVisMethodsInfo() {
		return mimpl;
	}

	static public String[][] getTimeVisMethodsInfo() {
		return timeVisMethods;
	}

	static public String[][] getVisMethodsForAreas() {
		return methodsForAreas;
	}

	static public String[][] getVisMethodsForPoints() {
		return methodsForPoints;
	}

	static public String[][] getVisMethodsForLines() {
		return methodsForLines;
	}
}