package help;

//This is an automatically produced file. Please, do not edit manually!

import spade.lib.help.HelpIndex;
import spade.lib.lang.Language;

public class DescartesHelpIndex implements HelpIndex {
	protected String pathToHelp = "";
	protected static String langList[] = { "english", "german" };
	protected static int currLangN = -1;
	protected String index[][] = { { "index", "english/Contents.htm", "german/Inhalt.htm" }, { "legend", "legend.html", "german/Erscheinung.htm" }, { "layer_appearance", "legend.html", "german/Erscheinung.htm" },
			{ "toggle_layers", "legend.html", "german/Erscheinung.htm" }, { "lookup_values", "english/Popup.htm", "german/Popup.htm" }, { "zoom", "zoom.html", null },
			{ "decision_support", "english/DecisionSupport.html", "german/Entscheidungshilfe.htm" }, { "dynamic_link", "english/Dynlinking.htm", "german/DynamischeVerknuepfung.htm" },
			{ "mark_entities", "english/Dynlinking.htm", "german/DynamischeVerknuepfung.htm" }, { "color_scale", "color_scale.html", null } };

	public static void setLanguage(String language) {
		if (language == null)
			return;
		currLangN = -1;
		for (int i = 0; i < langList.length && currLangN < 0; i++)
			if (langList[i].equalsIgnoreCase(language)) {
				currLangN = i;
			}
	}

	@Override
	public String getHelpFileName(String topicId) {
		if (topicId == null)
			return null;
		if (currLangN < 0) {
			setLanguage(Language.currLanguage);
		}
		for (String[] element : index)
			if (element[0].equalsIgnoreCase(topicId))
				if (element[currLangN + 1] != null)
					return pathToHelp + element[currLangN + 1];
				else
					return pathToHelp + element[1];
		return null;
	}

}
