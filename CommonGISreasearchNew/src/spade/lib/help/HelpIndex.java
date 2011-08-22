package spade.lib.help;

public interface HelpIndex {
	/**
	* Returns the name of the help file explainint the specified topic. Topics
	* are specified through identifiers.
	*/
	public String getHelpFileName(String identifier);
}