package configstart;

import spade.vis.spec.SpecSaver;

/**
* This class is able to return for a given keyword a class able to read
* a sequence of lines starting with <keyword> and ending with </keyword>
* and get some description from it. The "factory" contains a list of known
* keywords and the corresponding classes. The classes must implement the
* interface spade.vis.spec.SpecSaver.  The "factory" is used for saving and
* restoring "snapshots".
*/
public class StateSaverFactory {
	/**
	* The list of known keywords and the corresponding classes. The classes must
	* implement the interface spade.vis.spec.SpecSaver.
	*/
	protected static String savers[][] = { { "chart", "configstart.ToolStateSaver" }, { "tool", "configstart.ToolStateSaver" }, { "map", "configstart.ToolStateSaver" }, { "temporal_vis", "configstart.TemporalToolStateSaver" },
			{ "animated_tool", "configstart.AnimatedVisSaver" }, { "query", "configstart.QuerySaver" }, { "map_window", "configstart.MapWindowStateSaver" }, { "time_controls", "configstart.WinStateSaver" },
			{ "time_filter_controls", "configstart.WinStateSaver" }, { "chart_window", "configstart.WinStateSaver" }, { "query_window", "configstart.WinStateSaver" }, { "transformation", "configstart.TransformStateSaver" } };
	/**
	* Used for avoiding construction of many instances of the same saver class.
	*/
	protected static SpecSaver saverInstances[] = null;

	/**
	* Returns a tag reader able to read a tag starting with the given keyword.
	* May return null if there is no apropriate reader.
	*/
	public static SpecSaver getSpecSaver(String keyword) {
		if (keyword == null)
			return null;
		for (int i = 0; i < savers.length; i++)
			if (savers[i][0].equalsIgnoreCase(keyword)) {
				if (saverInstances != null && saverInstances[i] != null)
					return saverInstances[i];
				SpecSaver reader = null;
				try {
					reader = (SpecSaver) Class.forName(savers[i][1]).newInstance();
				} catch (Exception e) {
					return null;
				}
				if (reader != null) {
					if (saverInstances == null) {
						saverInstances = new SpecSaver[savers.length];
						for (int j = 0; j < saverInstances.length; j++) {
							saverInstances[j] = null;
						}
					}
					saverInstances[i] = reader;
				}
				return reader;
			}
		return null;
	}
}
