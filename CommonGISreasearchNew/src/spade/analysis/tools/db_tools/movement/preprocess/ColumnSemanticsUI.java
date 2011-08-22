package spade.analysis.tools.db_tools.movement.preprocess;

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
import db_work.data_descr.ColumnDescriptor;
import db_work.data_descr.TableDescriptor;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 08-Dec-2006
 * Time: 17:20:10
 * Asks the user to provide meta-information about the structure of a table
 * containing movement data: which of the columns contain coordinates, time,
 * entity identifier, trajectory identifier (if any), etc.
 */
public class ColumnSemanticsUI extends Panel implements ActionListener, ItemListener, DialogContent {
	/**
	 * Text strings specifying mandatory table contents,for example, "x-coordinate",
	 * "y-coordinate", "time", etc. The table must contain fields corresponding to
	 * these meanings.
	 */
	protected String meanings[] = null;
	/**
	 * Text strings specifying optional table contents,for example, "speed",
	 * "distance from start", etc. The table is not required to contain fields
	 * corresponding to these meanings.
	 */
	protected String optMeanings[] = null;
	/**
	 * Contains all meanings together, both mandatory and optional (for processing
	 * convenience)
	 */
	protected String allMeanings[] = null;
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
	 * Constructs the UI
	 * @param td - the descriptor of the table (must be previously obtained)
	 * @param meanings - the meanings (text strings) for which there must be
	 *                   corresponding table columns
	 * @param optMeanings - optional meanings (text strings):
	 *                      corresponding columns do not necessarily exist
	 */
	public ColumnSemanticsUI(TableDescriptor td, String meanings[], String optMeanings[]) {
		if (td == null)
			return;
		if ((meanings == null || meanings.length < 1) && (optMeanings == null || optMeanings.length < 1))
			return;
		if (meanings != null && td.getNColumns() < meanings.length)
			return;
		this.meanings = meanings;
		this.optMeanings = optMeanings;
		int nMean = 0;
		if (meanings != null) {
			nMean += meanings.length;
		}
		int nObligMean = nMean;
		if (optMeanings != null) {
			nMean += optMeanings.length;
		}
		allMeanings = new String[nMean];
		int k = 0;
		if (meanings != null) {
			for (String meaning : meanings) {
				allMeanings[k++] = meaning;
			}
		}
		if (optMeanings != null) {
			for (String optMeaning : optMeanings) {
				allMeanings[k++] = optMeaning;
			}
		}
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
			if (i < nObligMean) {
				meanCB[i].setForeground(Color.red.darker());
				meanTF[i].setForeground(Color.red.darker());
			}
		}

		colList = new List(Math.max(Math.min(10, td.getNColumns()), nMean + 2));
		colList.addItemListener(this);
		for (int i = 0; i < td.getNColumns(); i++) {
			ColumnDescriptor cd = td.getColumnDescriptor(i);
			colList.add(cd.name + " (" + cd.type + ")");
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
	public boolean canClose() {
		//checks if all mandatory meanings have been assigned to appropriate columns
		if (meanings == null || meanings.length < 1)
			return true;
		for (int i = 0; i < meanings.length; i++)
			if (colIdxMeaning[i] < 0) {
				errMsg = "No column specified for " + meanings[i] + "!";
				return false;
			}
		return true;
	}

	@Override
	public String getErrorMessage() {
		return errMsg;
	}

	/**
	 * Returns the column numbers assigned to obligatory meanings.
	 * Returns null if no obligatory meanings were provided.
	 */
	public int[] getMandatoryColumnNumbers() {
		if (meanings == null || meanings.length < 1)
			return null;
		int columns[] = new int[meanings.length];
		for (int i = 0; i < meanings.length; i++) {
			columns[i] = colIdxMeaning[i];
		}
		return columns;
	}

	/**
	 * Returns the column numbers assigned to optional meanings.
	 * Returns null if no obligatory meanings were provided.
	 */
	public int[] getOptionalColumnNumbers() {
		if (optMeanings == null || optMeanings.length < 1)
			return null;
		int nObligMean = 0;
		if (meanings != null) {
			nObligMean = meanings.length;
		}
		int columns[] = new int[optMeanings.length];
		for (int i = 0; i < optMeanings.length; i++) {
			columns[i] = colIdxMeaning[nObligMean + i];
		}
		return columns;
	}
}
