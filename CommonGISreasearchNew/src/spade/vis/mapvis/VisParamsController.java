package spade.vis.mapvis;

/**
* Constructs and starts dialogs for changing parameters of a visualization
* method (but not for interactive analytical manipulation)
*/

public interface VisParamsController {
	/**
	* Constructs and displays a dialog for changing parameters. If some
	* other dialog was opened earlier and is currently running, closes
	* the old dialog.
	*/
	public void startChangeParameters(Object vis);
}
