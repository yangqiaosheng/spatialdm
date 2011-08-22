package data_load.intelligence;

import java.awt.event.ActionListener;

import spade.analysis.system.ESDACore;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 12-Jul-2004
 * Time: 16:36:26
 * Introduced as an intermediary between the UI of the system and the actual
 * module for table indexing in order to be able to include the table indexing
 * module only when necessary.
 */
public interface TableIndexer extends ActionListener {
	/**
	 * Returns the text of the command which can be included in a menu etc.
	 * The indexer can itself listen when the command is activated and start
	 * the process of table indexing.
	 */
	public String getCommandText();

	/**
	 * Returns the identifier of the command (which does not appear in the user
	 * interface, unlike the text of the command)
	 */
	public String getCommandId();

	/**
	 * Sets a reference to the system's core.
	 */
	public void setCore(ESDACore core);
}
