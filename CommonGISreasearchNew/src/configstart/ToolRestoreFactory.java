package configstart;

import spade.analysis.system.ToolReCreator;

/**
* This is a register of components that can re-create various analysis tools the
* states of which were previously stored externally. For tool re-creation,
* the components use tool specifications retrieved from the external storage.
* The components must implement the interface spade.analysis.system.ToolReCreator
*/
public class ToolRestoreFactory {
	/**
	* The list of known tool specification types (class names) and the
	* corresponding classes for tool re-creation. The tool re-creating classes must
	* implement the interface spade.analysis.system.ToolReCreator.
	* It is possible to include a specification class more than once in this
	* register. Then, if one of the restoring classes is missing in the system
	* configuration, another class will be used. This allows us to have simpler
	* and more advanced variants of tool restorers, if necessary.
	*/
	protected static String restorers[][] = { { "spade.vis.spec.ToolSpec", "configstart.VisInitiator" }, { "spade.vis.spec.QuerySpec", "configstart.VisInitiator" }, { "spade.vis.spec.ToolSpec", "configstart.MapVisInitiator" },
			{ "spade.vis.spec.MapWindowSpec", "configstart.MapWindowInitiator" }, { "spade.vis.spec.WinSpec", "configstart.ChartWindowInitiator" }, { "spade.vis.spec.AnimatedVisSpec", "spade.time.vis.TimeVisInitiator" },
			{ "spade.vis.spec.TemporalToolSpec", "spade.time.vis.TimeVisInitiator" }, { "spade.vis.spec.WinSpec", "spade.time.ui.TimePanelRestorer" } };
	/**
	* Used for avoiding construction of many instances of the same restorer class.
	*/
	protected static ToolReCreator restorerInstances[] = null;
	/**
	* Used for avoiding repeated attempts to construct instances of non-existing
	* classes.
	*/
	protected static boolean noClass[] = null;;

	/**
	* Returns a tool re-creator able to re-create the tool according to the
	* given specification. May return null if there is no apropriate class.
	*/
	public static ToolReCreator getToolRestorer(Object spec) {
		if (spec == null)
			return null;
		if (noClass == null) {
			noClass = new boolean[restorers.length];
			for (int i = 0; i < restorers.length; i++) {
				noClass[i] = false;
			}
		}
		String specClassName = spec.getClass().getName();
		for (int i = 0; i < restorers.length; i++)
			if (!noClass[i] && restorers[i][0].equalsIgnoreCase(specClassName)) {
				ToolReCreator restorer = null;
				if (restorerInstances != null && restorerInstances[i] != null) {
					restorer = restorerInstances[i];
				} else {
					try {
						restorer = (ToolReCreator) Class.forName(restorers[i][1]).newInstance();
					} catch (Exception e) {
					}
					if (restorer != null) {
						if (restorerInstances == null) {
							restorerInstances = new ToolReCreator[restorers.length];
							for (int j = 0; j < restorerInstances.length; j++) {
								restorerInstances[j] = null;
							}
						}
						restorerInstances[i] = restorer;
					} else {
						noClass[i] = true;
					}
				}
				if (restorer != null && restorer.canFulfillSpecification(spec))
					return restorer;
			}
		return null;
	}

	/**
	* Returns an instance of a particular tool re-creator class. May return null
	* if the class is not available in the current system configuration.
	*/
	public static ToolReCreator getToolRestorer(String className) {
		if (className == null)
			return null;
		int idx = -1;
		for (int i = 0; i < restorers.length && idx < 0; i++)
			if (restorers[i][1].equalsIgnoreCase(className)) {
				idx = i;
			}
		if (idx >= 0) {
			if (restorerInstances != null && restorerInstances[idx] != null)
				return restorerInstances[idx];
			if (noClass != null && noClass[idx]) {
				System.out.println("Class " + className + " is not available!");
				return null;
			}
			if (noClass == null) {
				noClass = new boolean[restorers.length];
				for (int i = 0; i < restorers.length; i++) {
					noClass[i] = false;
				}
			}
			if (restorerInstances == null) {
				restorerInstances = new ToolReCreator[restorers.length];
				for (int j = 0; j < restorerInstances.length; j++) {
					restorerInstances[j] = null;
				}
			}
		}
		Object obj = null;
		try {
			obj = Class.forName(className).newInstance();
		} catch (Exception e) {
			System.out.println("Class " + className + " not found!");
			e.printStackTrace();
		}
		if (obj == null) {
			if (idx >= 0) {
				noClass[idx] = true;
			}
			return null;
		}
		if (!(obj instanceof ToolReCreator)) {
			System.out.println("ERROR: the class " + className + " does not implement the interface ToolReCreator!");
			if (idx >= 0) {
				noClass[idx] = true;
			}
			return null;
		}
		if (idx >= 0) {
			restorerInstances[idx] = (ToolReCreator) obj;
		}
		return (ToolReCreator) obj;
	}
}
