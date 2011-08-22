package spade.vis.dmap;

public class Res extends java.util.ListResourceBundle {
	static final Object[][] contents = new String[][] { { "Confirm", "Confirm" }, { "Remove_the_layer_", "Remove the layer?" }, { "Parameters", "Parameters" }, { "Choose_background", "Choose background colour for project" },
			{ "Background", "Background" }, { "Territory_", "Territory: " }, { "Click_to_change", "Click to change" }, { "Name_", "Name:" }, { "Change_line_color", "Change line colour" }, { "Change_fill_color", "Change fill colour" },
			{ "draw_lines", "Draw lines" }, { "fill_contours", "Fill contours" }, { "Select_line_thickness", "Select line thickness" }, { "put_labels_on_the_map", "Put labels on the map" }, { "Change_color_of", "Change colour of labels" },
			{ "Change_font_for", "Change font for labels" }, { "Select_color", "Select colour" }, { "Select_font", "Select font" }, { "lth_highlighting", "Line thickness for highlighting:" },
			{ "circles_highlighting", "Mark highlighted objects with circles" }, { "circle_size", "Circle size:" }, { "circle_color", "Change circle colour" }, { "distorted_color", "the colour may be distorted" },
			{ "lth_selection", "Line thickness for selection:" }, { "OK", "OK" }, { "Cancel", "Cancel" }, { "Total_", "Total: " }, { "object", "object" }, { "objects", "objects" }, { "switch_layer", "Click here to switch the layer on or off" },
			{ "change_layer_drawing", "Click here to change the appearance or name of the layer, switch on/off label drawing" }, { "activate_layer", "Click here to make the layer active (sensitive to mouse events)" },
			{ "change_visualization", "Click here to change parameters of the data visualisation" }, { "draw_condition", "Draw depends on scale" }, { "min_scale", "Min scale:" }, { "max_scale", "Max scale:" }, { "Attention", "Attention!" },
			{ "layer_conditional_draw", "This layer is drawn conditionally depending on " + "the map scale. It will not be shown at the " + "current map scale. You may change layer properties " + "after clicking on the layer\'s icon." },
			{ "transparency_", "Transparency: " }, { "Drawing", "Drawing" }, { "Names", "Names" }, { "Scale", "Scale" }, { "Highlighting", "Highlighting" } };

	@Override
	public Object[][] getContents() {
		return contents;
	}
}
