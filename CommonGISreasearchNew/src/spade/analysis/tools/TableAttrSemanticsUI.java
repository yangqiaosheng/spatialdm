package spade.analysis.tools;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import spade.lib.basicwin.DialogContent;
import spade.vis.database.AttributeDataPortion;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 03-Jan-2007
 * Time: 15:16:19
 * Asks the user to provide meta-information about the meanings of the
 * attributes a table in a table
 */
public class TableAttrSemanticsUI extends Panel implements ActionListener, ItemListener, DialogContent {
	/**
	* Types of attributes (same as in spade.vis.database.AttributeTypes)
	*/
	public static final char types[] = { 'I', 'R', 'C', 'L', 'T', 'G' };
	/**
	 * Texts corresponding to the types of attributes
	 */
	public static final String typeNames[] = { "integer", "real", "character", "logical", "time", "geometry" };
	/**
	 * The table for which semantic information is required
	 */
	protected AttributeDataPortion table = null;
	/**
	 * Text strings specifying mandatory and optional table contents, for example,
	 * "x-coordinate",  "y-coordinate", "time", etc.
	 */
	protected String allMeanings[] = null;
	/**
	 * Number of mandatory meanings, which must be present in the data (the
	 * remaining meanings are treated as optional). The mandatory meanings
	 * come at the beginning of the array allMeanings.
	 */
	protected int nMandMeanings = 0;
	/**
	 * The list with column names and types
	 */
	protected List colList = null;
	/**
	 * For each meaning, contains the number (index) of the corresponding column
	 * or -1 if there is no corresponding column
	 */
	protected int colIdxMeaning[] = null;
	/**
	 * Other UI controls
	 */
	protected Checkbox meanCB[] = null, noMeanCB = null;
	protected TextField meanTF[] = null;
	/**
	 * Explains why cannot be closed
	 */
	protected String errMsg = null;
	/**
	 * True if successfully constructed
	 */
	protected boolean setupOK = false;

	/**
	 * Constructs the UI
	 * @param table - the table
	 * @param meanings - the possible meanings (text strings) of table columns
	 * @param nMandMeanings - number of mandatory meanings
	 */
	public TableAttrSemanticsUI(AttributeDataPortion table, String meanings[], int nMandMeanings) {
		this.table = table;
		if (meanings == null || meanings.length < 1)
			return;
		if (table == null)
			return;
		if (table.getAttrCount() < nMandMeanings) {
			errMsg = "Too few attributes in the table!";
			return;
		}
		this.allMeanings = meanings;
		this.nMandMeanings = nMandMeanings;
		int nMean = meanings.length;
		colIdxMeaning = new int[nMean];
		meanCB = new Checkbox[nMean];
		meanTF = new TextField[nMean];
		CheckboxGroup cbg = new CheckboxGroup();
		noMeanCB = new Checkbox("no meaning", true, cbg);
		Panel meanP = new Panel(new GridLayout(nMean, 2));
		for (int i = 0; i < nMean; i++) {
			colIdxMeaning[i] = -1;
			meanCB[i] = new Checkbox(allMeanings[i], false, cbg);
			meanCB[i].addItemListener(this);
			meanTF[i] = new TextField(20);
			meanTF[i].setEditable(false);
			meanP.add(meanCB[i]);
			meanP.add(meanTF[i]);
			if (i < nMandMeanings) {
				meanCB[i].setForeground(Color.red.darker());
				meanTF[i].setForeground(Color.red.darker());
			}
		}

		colList = new List(Math.max(Math.min(10, table.getAttrCount()), nMean + 2));
		colList.addItemListener(this);
		for (int i = 0; i < table.getAttrCount(); i++) {
			colList.add(table.getAttributeName(i) + " (" + getTypeName(table.getAttributeType(i)) + ")");
		}

		Panel p = new Panel(new BorderLayout());
		p.add(meanP, BorderLayout.CENTER);
		Panel pp = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 5));
		Button b = new Button("Erase");
		b.setActionCommand("erase");
		b.addActionListener(this);
		pp.add(b);
		p.add(pp, BorderLayout.SOUTH);
		setLayout(new BorderLayout(5, 0));
		add(colList, BorderLayout.CENTER);
		add(p, BorderLayout.EAST);
		setupOK = true;
	}

	/**
	 * Returns true if successfully constructed
	 */
	public boolean isOK() {
		return setupOK;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("erase")) {
			int colN = colList.getSelectedIndex();
			if (colN >= 0) {
				//check if any meaning has been earlier assigned to this column
				int meanIdx = -1;
				for (int i = 0; i < colIdxMeaning.length && meanIdx < 0; i++)
					if (colIdxMeaning[i] == colN) {
						meanIdx = i;
					}
				if (meanIdx >= 0) {
					colIdxMeaning[meanIdx] = -1;
					meanTF[meanIdx].setText("");
				}
			}
			noMeanCB.setState(true);
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		int colN = colList.getSelectedIndex();
		if (colN < 0) {
			noMeanCB.setState(true);
			return;
		}
		//check if any meaning has been earlier assigned to this column
		int meanIdx = -1;
		for (int i = 0; i < colIdxMeaning.length && meanIdx < 0; i++)
			if (colIdxMeaning[i] == colN) {
				meanIdx = i;
			}
		if (e.getSource().equals(colList)) { //selection of a column
			if (meanIdx < 0) {
				noMeanCB.setState(true);
			} else {
				meanCB[meanIdx].setState(true);
			}
			return;
		}
		if (e.getSource() instanceof Checkbox) { //selection of a checkbox for a column
			int cbIdx = -1;
			for (int i = 0; i < meanCB.length && cbIdx < 0; i++)
				if (meanCB[i].getState()) {
					cbIdx = i;
				}
			if (cbIdx == meanIdx)
				return;
			if (meanIdx >= 0) {
				colIdxMeaning[meanIdx] = -1;
				meanTF[meanIdx].setText("");
			}
			if (cbIdx >= 0) {
				colIdxMeaning[cbIdx] = colN;
				String str = colList.getItem(colN);
				int k = str.lastIndexOf('(');
				str = str.substring(0, k - 1);
				meanTF[cbIdx].setText(str);
			}
			return;
		}
	}

	@Override
	public String getErrorMessage() {
		return errMsg;
	}

	@Override
	public boolean canClose() {
		errMsg = null;
		//checks if all mandatory meanings have been assigned to appropriate columns
		if (nMandMeanings < 1)
			return true;
		for (int i = 0; i < nMandMeanings; i++)
			if (colIdxMeaning[i] < 0) {
				errMsg = "No column specified for " + allMeanings[i] + "!";
				return false;
			}
		return true;
	}

	/**
	 * Returns the column numbers assigned to the specified meanings.
	 */
	public int[] getColumnNumbers() {
		return colIdxMeaning;
	}

	/**
	 * Returns the text corresponding to the given type of attribute
	 */
	public static String getTypeName(char type) {
		for (int i = 0; i < types.length; i++)
			if (type == types[i])
				return typeNames[i];
		return null;
	}
}
