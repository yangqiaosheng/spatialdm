package spade.time.query;

import java.awt.Button;
import java.awt.Choice;
import java.awt.Label;
import java.awt.Panel;
import java.util.ResourceBundle;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.RowLayout;
import spade.lib.lang.Language;

/**
* A panel with UI elements for controlling possible parameters of the
* query builder, in particular, the method of combining query conditions
* (AND or OR)
*/
public class TimeQueryControls extends Panel {
	/**
	* All texts that appear in the user interface must be defined in resource
	* classes in two languages: english and german. Typically, there is a pair
	* of such resource classes in each package. English texts are defined in
	* the class <package_name>.Res, and german texts in the class
	* <package_name>.Res_de. The class spade.lib.lang.Language cares about the
	* selection of the apropriate resources depending on the selected user
	* interface language.
	*/
	static ResourceBundle res = Language.getTextResource("spade.time.query.Res");
	/**
	* The choice element for selecting the operation for combining multiple
	* query conditions: logical AND or logical OR
	*/
	public Choice operationChoice = null;
	/**
	* The button for deleting all query conditions
	*/
	public Button eraseButton = null;

	/**
	* Constructs all the controls
	*/
	public TimeQueryControls() {
		operationChoice = new Choice();
		operationChoice.addItem(res.getString("AND"));
		operationChoice.addItem(res.getString("OR"));
		Panel p = new Panel(new RowLayout(5, 0));
		p.add(new Label(res.getString("combine_cond_with")));
		p.add(operationChoice);
		eraseButton = new Button(res.getString("erase_all"));
		eraseButton.setActionCommand("erase_all");
		p.add(eraseButton);
		setLayout(new ColumnLayout());
		add(p);
		//possibly, some more controls will be needed
		//...
	}
}