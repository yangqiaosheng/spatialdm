package spade.analysis.transform;

import java.awt.Component;

/**
* The interface to be fulfilled by a component that saves results of attribute
* transformation to a table. The interface is introduced in order to make the
* component itself easily removable from the system.
*/
public interface TransformedDataSaver {
	/**
	* Setups the saver. For this purpose, needs the attribute transformer, the
	* results of which need to be saved. Returns true if successfully set.
	*/
	public boolean setup(AttributeTransformer transformer);

	/**
	* Returns a UI component for accessing this saver (for example, a panel with
	* a "Save" button)
	*/
	public Component getUI();
}