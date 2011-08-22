package spade.time.vis;

import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.classification.ClassBroadcastPanel;
import spade.analysis.classification.ObjectColorer;
import spade.analysis.classification.QualitativeClassifier;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Line;
import spade.lib.color.CS;
import spade.lib.util.BubbleSort;
import spade.lib.util.IntArray;
import spade.lib.util.ObjectWithIndex;
import spade.time.TimeMoment;
import spade.vis.database.Attribute;
import spade.vis.database.DataTable;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Oct 27, 2009
 * Time: 2:09:12 PM
 * An interactive component allowing the user to divide time into periods
 * and look for periodic patterns. It is assumed thet time moments or
 * intervals are colored.
 */
public class TimeArranger extends Panel implements ActionListener, ItemListener, PropertyChangeListener, Destroyable {
	/**
	 * The ordered sequence of time moments. These may be starting moments of
	 * time intervals; pay attention to the distance (difference) between the consecutive
	 * moments!
	 * If these are instances of spade.time.Date, pay also attention to the precision!
	 * E.g. if the precision is 'm', these are months.
	 */
	protected TimeMoment times[] = null;
	/**
	 * The indexes of the respective records in the table
	 */
	protected int tIdxs[] = null;
	/**
	 * The list of classes of time moments
	 */
	protected String allClassLabels[] = null;
	/**
	 * The colors assigned to the classes time moments
	 */
	protected Color allClassColors[] = null;
	/**
	 * The indexes of the classes of the time moments in the list of the classes
	 */
	protected int timeClassIdxs[] = null;
	/**
	 * The table in which the rows correspond to the time moments.
	 * The table is needed for constructing the classifier and broadcasting
	 * the colors to other components.
	 */
	protected DataTable timeTable = null;
	/**
	 * The index of the column of the table which contains the classes of the time moments
	 */
	protected int tblColIdx = -1;
	/**
	 * The system's supervisor
	 */
	protected Supervisor supervisor = null;
	/**
	 * The canvas used for the visualization
	 */
	protected TimeArrangerCanvas tCanvas = null;
	/**
	 * Used to choose the attribute (table column) defining the classification
	 */
	protected Choice chAttr = null;
	/**
	 * The indexes of the table columns corresponding to the items of the
	 * choice control
	 */
	protected IntArray clColNs = null;
	/**
	 * The classifier is needed for broadcasting the colors
	 */
	protected QualitativeClassifier classifier = null;
	/**
	 * Broadcasts class colors
	 */
	protected ClassBroadcastPanel cbp = null;
	/**
	 * Used to generate unique names
	 */
	public static int instanceN = 0;

	protected TextField tfNColumns = null, tfOffset = null, tfSizeX = null, tfSizeY = null;

	/**
	 * @param timeTable - The table in which the rows correspond to the time moments
	 * @param tblColIdx - The index of the column of the table which contains the classes of the time moments
	 * @param supervisor - The system's supervisor
	 */
	public TimeArranger(DataTable timeTable, int tblColIdx, Supervisor supervisor) {
		this.timeTable = timeTable;
		this.tblColIdx = tblColIdx;
		this.supervisor = supervisor;

		setName("Time Arranger " + (++instanceN));

		if (!getDataFromTable(timeTable, tblColIdx, true))
			return;
		timeTable.addPropertyChangeListener(this);
		supervisor.addPropertyChangeListener(this);

		setLayout(new BorderLayout());
		tCanvas = new TimeArrangerCanvas(12, times, allClassLabels, allClassColors, timeClassIdxs);
		add(tCanvas, BorderLayout.CENTER);

		Panel cp = new Panel(new ColumnLayout());
		add(cp, BorderLayout.NORTH);

		clColNs = new IntArray(20, 20);
		clColNs.addElement(tblColIdx);
		chAttr = new Choice();
		chAttr.addItemListener(this);
		cp.add(new Label("Classification attribute:"));
		cp.add(chAttr);
		String aName = timeTable.getAttributeName(tblColIdx);
		if (aName.length() > 50) {
			aName = aName.substring(0, 51) + "...";
		}
		chAttr.addItem(aName);
		for (int i = 0; i < timeTable.getAttrCount(); i++)
			if (i != tblColIdx && timeTable.getAttribute(i).isClassification()) {
				clColNs.addElement(i);
				aName = timeTable.getAttributeName(i);
				if (aName.length() > 50) {
					aName = aName.substring(0, 51) + "...";
				}
				chAttr.addItem(aName);
			}
		chAttr.select(0);
		cp.add(new Line(false));

		classifier = new QualitativeClassifier(timeTable, timeTable.getAttributeId(tblColIdx));
		classifier.setup();
		cbp = new ClassBroadcastPanel();
		cbp.construct(classifier, supervisor);
		cp.add(cbp);
		cp.add(new Line(false));

		Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		p.add(new Label("N columns:"));
		p.add(tfNColumns = new TextField("12", 3));
		p.add(new Label("Offset:"));
		p.add(tfOffset = new TextField("0", 3));
		cp.add(p);

		cp.add(new Line(false));
		p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		p.add(new Label("Cell size, x:"));
		p.add(tfSizeX = new TextField("20", 3));
		p.add(new Label("y:"));
		p.add(tfSizeY = new TextField("20", 3));
		cp.add(p);

		tfNColumns.addActionListener(this);
		tfOffset.addActionListener(this);
		tfSizeX.addActionListener(this);
		tfSizeY.addActionListener(this);
	}

	/**
	 * Retrieves time moments and their classes from the table
	 * @param table - The table in which the rows correspond to the time moments
	 * @param colIdx - the index of the table column with the class assignments
	 * @param takeDataForUse - if true, the retrieved data will be assigned to the
	 *   respective fields times, timeClassIdxs, allClassLabels, and allClassColors.
	 *   If false, the method only checks if the necessary data can be retrieved.
	 * @return true if successful
	 */
	protected boolean getDataFromTable(DataTable table, int colIdx, boolean takeDataForUse) {
		if (table == null || colIdx < 0)
			return false;

		//retrieve times from the table
		Vector<ObjectWithIndex> vt = new Vector<ObjectWithIndex>(table.getDataItemCount(), 1);
		for (int i = 0; i < table.getDataItemCount(); i++) {
			Object obj = table.getDescribedObject(i);
			if (obj != null && (obj instanceof TimeMoment)) {
				vt.addElement(new ObjectWithIndex(obj, i));
			}
		}
		if (vt.size() < 2) {
			System.out.println("Failed to retrieve times from the table!");
			return false;
		}
		if (!checkCreateClassLabelsAndColors(table, colIdx))
			return false;

		if (takeDataForUse) {
			timeTable = table;
			tblColIdx = colIdx;
			Attribute clAttr = table.getAttribute(colIdx);
			allClassLabels = clAttr.getValueList();
			allClassColors = clAttr.getValueColors();
			BubbleSort.sort(vt);
			times = new TimeMoment[vt.size()];
			tIdxs = new int[vt.size()];
			timeClassIdxs = new int[vt.size()];
			for (int i = 0; i < vt.size(); i++) {
				times[i] = (TimeMoment) vt.elementAt(i).obj;
				tIdxs[i] = vt.elementAt(i).index;
				String cLab = table.getAttrValueAsString(colIdx, tIdxs[i]);
				timeClassIdxs[i] = (cLab == null) ? -1 : clAttr.getValueN(cLab);
			}
		}
		return true;
	}

	protected boolean checkCreateClassLabelsAndColors(DataTable table, int colIdx) {
		Attribute clAttr = table.getAttribute(colIdx);
		String classLabels[] = clAttr.getValueList();
		Color classColors[] = clAttr.getValueColors();
		if (classLabels == null) {
			IntArray cN = new IntArray(1, 1);
			cN.addElement(colIdx);
			Vector values = table.getAllValuesInColumnsAsStrings(cN);
			if (values == null || values.size() < 1) {
				System.out.println("Failed to retrieve classes from the table column!");
				return false;
			}
			BubbleSort.sort(values);
			classLabels = new String[values.size()];
			for (int i = 0; i < values.size(); i++) {
				classLabels[i] = (String) values.elementAt(i);
			}
		}
		if (classColors == null || classColors.length != classLabels.length) {
			classColors = new Color[classLabels.length];
			for (int i = 0; i < classColors.length; i++)
				if (i < CS.niceColors.length) {
					classColors[i] = CS.getNiceColor(i);
				} else if (i < CS.niceColors.length * 3) {
					classColors[i] = classColors[i - CS.niceColors.length].darker();
				} else {
					classColors[i] = Color.getHSBColor((float) Math.random(), (float) Math.max(Math.random(), 0.5), (float) Math.max(Math.random(), 0.5));
				}
		}
		if (clAttr.getValueList() == null || clAttr.getValueColors() == null) {
			clAttr.setValueListAndColors(classLabels, classColors);
		}
		return true;
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource().equals(tfNColumns) || ae.getSource().equals(tfOffset)) {
			tCanvas.setNColumns(readIntFromTF(tfNColumns, tCanvas.getNColumns(), 2, 200), readIntFromTF(tfOffset, tCanvas.getOffset(), 0, tCanvas.getNColumns() - 1));
		} else if (ae.getSource().equals(tfSizeX)) {
			tCanvas.setCellSizeX(readIntFromTF(tfSizeX, tCanvas.getCellSizeX(), 1, 100));
		} else if (ae.getSource().equals(tfSizeY)) {
			tCanvas.setCellSizeY(readIntFromTF(tfSizeY, tCanvas.getCellSizeX(), 1, 100));
		}
	}

	/**
	 * Selection of another classification attribute
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(chAttr)) {
			int idx = chAttr.getSelectedIndex();
			int cIdx = clColNs.elementAt(idx);
			if (cIdx == tblColIdx)
				return;
			takeClassesFromColumn(cIdx);
		}
	}

	/**
	 * Takes classes from the specified table column
	 */
	protected void takeClassesFromColumn(int colIdx) {
		tblColIdx = colIdx;
		Attribute clAttr = timeTable.getAttribute(tblColIdx);
		allClassLabels = clAttr.getValueList();
		allClassColors = clAttr.getValueColors();
		for (int i = 0; i < times.length; i++) {
			String cLab = timeTable.getAttrValueAsString(tblColIdx, tIdxs[i]);
			timeClassIdxs[i] = (cLab == null) ? -1 : clAttr.getValueN(cLab);
		}
		tCanvas.setClasses(allClassLabels, allClassColors, timeClassIdxs);
		classifier = new QualitativeClassifier(timeTable, timeTable.getAttributeId(tblColIdx));
		classifier.setup();
		cbp.replaceClassifier(classifier);
	}

	/**
	 * Reacts to adding new attributes to the table
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(timeTable)) {
			if (e.getPropertyName().equals("new_attributes")) {
				Vector<String> aIds = (Vector<String>) e.getNewValue();
				if (aIds == null)
					return;
				for (int i = 0; i < aIds.size(); i++) {
					int cN = timeTable.getAttrIndex(aIds.elementAt(i));
					if (cN < 0 || clColNs.indexOf(cN) >= 0) {
						continue;
					}
					if (timeTable.getAttribute(cN).isClassification()) {
						clColNs.addElement(cN);
						String aName = timeTable.getAttributeName(cN);
						if (aName.length() > 50) {
							aName = aName.substring(0, 51) + "...";
						}
						chAttr.addItem(aName);
					}
				}
			} else if (e.getPropertyName().equals("values") || e.getPropertyName().equals("value_colors")) {
				Vector<String> aIds = (Vector<String>) e.getNewValue();
				if (aIds == null)
					return;
				String currId = timeTable.getAttributeId(tblColIdx);
				if (aIds.contains(currId)) {
					takeClassesFromColumn(tblColIdx);
				}
			}
		} else if (e.getSource().equals(supervisor)) {
			if (e.getPropertyName().equals(Supervisor.eventTimeColors)) {
				ObjectColorer oCol = supervisor.getObjectColorer();
				if (oCol.getObjectContainer().equals(timeTable) && (oCol instanceof QualitativeClassifier)) {
					QualitativeClassifier cl = (QualitativeClassifier) oCol;
					int cN = cl.getAttrColumnN();
					if (cN < 0 || cN == tblColIdx)
						return;
					int idx = clColNs.indexOf(cN);
					if (idx < 0)
						return;
					takeClassesFromColumn(cN);
					chAttr.select(idx);
				}
			}
		}
	}

	protected int readIntFromTF(TextField tf, int defaultValue, int min, int max) {
		int n = defaultValue;
		try {
			n = Integer.valueOf(tf.getText()).intValue();
		} catch (NumberFormatException nfe) {
			n = defaultValue;
			tf.setText("" + n);
		}
		if (n < min || n > max) {
			n = defaultValue;
			tf.setText("" + n);
		}
		return n;
	}

	// ------------- Destroyable ---------------------------

	protected boolean destroyed = false;

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public void destroy() {
		if (classifier != null) {
			classifier.destroy();
		}
	}
}
