package spade.analysis.plot;

import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.classification.SingleAttributeClassifier;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Destroyable;
import spade.lib.util.Frequencies;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectFilter;
import spade.vis.dataview.TableViewer;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 18.01.2007
 * Time: 17:13:45
 * Displays frequencies of values in a table column
 */
public class ValueFrequencyViewer extends Panel implements PropertyChangeListener, ItemListener, Destroyable, HighlightListener {
	/**
	* The source of the data to be shown on the plot
	*/
	protected DataTable dataTable = null;
	/**
	 * The table column for which value frequencies are counted and displayed
	 */
	protected int colN = -1;
	/**
	 * Indicates whether the attribute is numeric
	 */
	protected boolean isNumeric = false;
	/**
	* Supervisor provides access of a plot to the Highlighter (common for
	* all data displays) and in this way links together all displays
	*/
	protected Supervisor supervisor = null;
	/**
	 * The filter of the table
	 */
	protected ObjectFilter filter = null;
	/**
	 * A generated table with values and their frequencies
	 */
	protected DataTable freqTable = null;
	/**
	 * The display of the table with the frequencies
	 */
	protected TableViewer tableViewer = null;
	/**
	 * Used for the selection of an attribute to be used for sorting
	 */
	protected Choice sortChoice = null;
	/**
	 * Used for choosing the direction of sorting
	 */
	protected Choice dirChoice = null;
	/**
	* Array of numbers of currently selected records
	*/
	protected IntArray selNumbers = null;

	public ValueFrequencyViewer(DataTable dataTable, int colN, Supervisor sup) {
		this.dataTable = dataTable;
		this.colN = colN;
		supervisor = sup;
		if (dataTable == null || colN < 0 || colN >= dataTable.getAttrCount())
			return;
		String attrName = dataTable.getAttributeName(colN);
		setName("Frequencies of values of attribute \"" + attrName + "\"");
		isNumeric = dataTable.isAttributeNumeric(colN);
		Frequencies freq = dataTable.getValueFrequencies(colN, false, true);
		if (freq == null || freq.getItemCount() < 1) {
			setLayout(new BorderLayout());
			add(new Label("No attribute values found!"));
			return;
		}
		freq.sortItems((isNumeric) ? Frequencies.order_as_numbers_ascend : Frequencies.order_alphabet);
		freqTable = new DataTable();
		freqTable.setEntitySetIdentifier("values_attr_" + dataTable.getAttributeId(colN));
		freqTable.addAttribute("Overall frequency", "overal_freq", AttributeTypes.integer);
		freqTable.addAttribute("Frequency after filtering", "filter_freq", AttributeTypes.integer);
		freqTable.addAttribute("Ratio (%)", "ratio", AttributeTypes.real);
		freqTable.addAttribute("Frequency among selected", "selected_freq", AttributeTypes.integer);
		freqTable.addAttribute("% selected among all", "selected_ratio", AttributeTypes.real);
		Frequencies filtFreq = dataTable.getValueFrequencies(colN, true, true);
		checkWhatSelected();
		Frequencies selFreq = dataTable.getValueFrequencies(colN, selNumbers, false, true);
		IntArray selValueIndexes = new IntArray(freq.getItemCount(), 1);
		for (int i = 0; i < freq.getItemCount(); i++) {
			String value = freq.getItemAsString(i);
			int f1 = freq.getFrequency(i), f2 = filtFreq.getFrequency(value);
			if (f2 < 0) {
				f2 = 0;
			}
			float ratio = Float.NaN;
			if (f1 > 0) {
				ratio = 100.0f * f2 / f1;
			}
			String ratioStr = (f1 == 0) ? null : (f2 == 0) ? "0.00" : (f1 == f2) ? "100.00" : StringUtil.floatToStr(ratio, 2);
			int f3 = 0;
			float selRatio = 0f;
			String selRatioStr = "0.00";
			if (selFreq != null) {
				f3 = selFreq.getFrequency(value);
				if (f3 < 0) {
					f3 = 0;
				}
				if (f3 > 0) {
					selRatio = 100.0f * f3 / f1;
					selRatioStr = StringUtil.floatToStr(selRatio, 2);
					if (f3 >= f2) {
						selValueIndexes.addElement(i);
					}
				}
			}
			DataRecord rec = new DataRecord(String.valueOf(i + 1), value);
			freqTable.addDataRecord(rec);
			rec.setNumericAttrValue(f1, String.valueOf(f1), 0);
			rec.setNumericAttrValue(f2, String.valueOf(f2), 1);
			rec.setNumericAttrValue(ratio, ratioStr, 2);
			rec.setNumericAttrValue(f3, String.valueOf(f3), 3);
			rec.setNumericAttrValue(selRatio, selRatioStr, 4);
		}
		tableViewer = new TableViewer(freqTable, supervisor, this);
		tableViewer.setTreatItemNamesAsNumbers(isNumeric);
		Vector attr = new Vector(freqTable.getAttrCount(), 1);
		for (int i = 0; i < freqTable.getAttrCount(); i++) {
			attr.addElement(freqTable.getAttributeId(i));
		}
		tableViewer.setVisibleAttributes(attr);
		tableViewer.setTableLens(true);
		setLayout(new BorderLayout());
		add(new Label(attrName), BorderLayout.NORTH);
		add(tableViewer, BorderLayout.CENTER);
		if (selValueIndexes.size() > 0) {
			selectRowsInTableViewer(selValueIndexes);
		}

		sortChoice = new Choice();
		sortChoice.addItemListener(this);
		sortChoice.addItem("No selection");
		sortChoice.addItem("Name (1-st column)");
		for (int i = 0; i < freqTable.getAttrCount(); i++) {
			sortChoice.addItem(freqTable.getAttributeName(i));
		}
		sortChoice.select(0);

		dirChoice = new Choice();
		dirChoice.add("Ascending");
		dirChoice.add("Descending");
		dirChoice.select(0);
		dirChoice.setEnabled(false);
		dirChoice.addItemListener(this);

		Panel p = new Panel(new FlowLayout(FlowLayout.CENTER, 5, 0));
		p.add(new Label("Sort by", Label.RIGHT));
		p.add(sortChoice);
		p.add(new Label("order:", Label.RIGHT));
		p.add(dirChoice);
		add(p, BorderLayout.SOUTH);

		filter = dataTable.getObjectFilter();
		if (filter != null) {
			filter.addPropertyChangeListener(this);
		}
		if (supervisor != null) {
			supervisor.addPropertyChangeListener(this);
			supervisor.getHighlighter(freqTable.getEntitySetIdentifier()).addHighlightListener(this);
			supervisor.getHighlighter(dataTable.getEntitySetIdentifier()).addHighlightListener(this);
		}
		dataTable.addPropertyChangeListener(this);
	}

	/**
	 * Gets from the Highlighter of the dataTable objects the list
	 * of currently selected objects.
	 */
	protected void checkWhatSelected() {
		if (supervisor == null || dataTable == null)
			return;
		Highlighter highlighter = supervisor.getHighlighter(dataTable.getEntitySetIdentifier());
		if (highlighter == null)
			return;
		if (selNumbers == null) {
			selNumbers = new IntArray(100, 100);
		} else {
			selNumbers.removeAllElements();
		}
		Vector selIds = highlighter.getSelectedObjects();
		if (selIds == null || selIds.size() < 1)
			return;
		for (int i = 0; i < dataTable.getDataItemCount() && selNumbers.size() < selIds.size(); i++)
			if (selIds.contains(dataTable.getDataItemId(i))) {
				selNumbers.addElement(i);
			}
	}

	/**
	* Checks if the record with the given number is currently selected, i.e.
	* should be drawn in "highlighted" mode
	*/
	protected boolean isRecordSelected(int recN) {
		return selNumbers != null && selNumbers.indexOf(recN) >= 0;
	}

	/**
	 * Called after filter changes
	 */
	protected void recountFrequencies() {
		Frequencies filtFreq = dataTable.getValueFrequencies(colN, true, true);
		checkWhatSelected();
		Frequencies selFreq = dataTable.getValueFrequencies(colN, selNumbers, false, true);
		IntArray selValueIndexes = new IntArray(freqTable.getDataItemCount(), 1);
		for (int i = 0; i < freqTable.getDataItemCount(); i++) {
			DataRecord rec = freqTable.getDataRecord(i);
			int f1 = (int) Math.round(rec.getNumericAttrValue(0));
			int f2 = f1;
			if (filtFreq != null) {
				f2 = filtFreq.getFrequency(rec.getName());
			}
			if (f2 < 0) {
				f2 = 0;
			}
			float ratio = Float.NaN;
			if (f1 > 0) {
				ratio = 100.0f * f2 / f1;
			}
			String ratioStr = (f1 == 0) ? null : (f2 == 0) ? "0.00" : (f1 == f2) ? "100.00" : StringUtil.floatToStr(ratio, 2);
			int f3 = 0;
			float selRatio = 0f;
			String selRatioStr = "0.00";
			if (selFreq != null) {
				f3 = selFreq.getFrequency(rec.getName());
				if (f3 < 0) {
					f3 = 0;
				}
				if (f3 > 0) {
					selRatio = 100.0f * f3 / f1;
					selRatioStr = StringUtil.floatToStr(selRatio, 2);
					if (f3 >= f2) {
						selValueIndexes.addElement(i);
					}
				}
			}
			rec.setNumericAttrValue(f2, String.valueOf(f2), 1);
			rec.setNumericAttrValue(ratio, ratioStr, 2);
			rec.setNumericAttrValue(f3, String.valueOf(f3), 3);
			rec.setNumericAttrValue(selRatio, selRatioStr, 4);
		}
		Vector attr = new Vector(freqTable.getAttrCount() - 1, 1);
		for (int i = 1; i < freqTable.getAttrCount(); i++) {
			attr.addElement(freqTable.getAttributeId(i));
		}
		freqTable.notifyPropertyChange("values", null, attr);
		selectRowsInTableViewer(selValueIndexes);
	}

	protected void selectRowsInTableViewer(IntArray selValueIndexes) {
		boolean changed = false;
		if (selValueIndexes != null && selValueIndexes.size() > 0) {
			changed = tableViewer.setSelNumbers(selValueIndexes);
		} else {
			changed = tableViewer.setSelNumbers(null);
		}
		if (changed && supervisor != null) {
			Highlighter hl = supervisor.getHighlighter(freqTable.getEntitySetIdentifier());
			if (selValueIndexes == null || selValueIndexes.size() < 1) {
				hl.clearSelection(this);
			} else {
				Vector objIds = new Vector(selValueIndexes.size());
				for (int i = 0; i < selValueIndexes.size(); i++) {
					objIds.addElement(freqTable.getDataItemId(selValueIndexes.elementAt(i)));
				}
				hl.replaceSelectedObjects(this, objIds);
			}
		}
	}

	/**
	 * Called after selection changes
	 */
	protected void recountFrequenciesAmongSelected() {
		Vector attr = new Vector(freqTable.getAttrCount() - 1, 1);
		for (int i = 3; i <= 4; i++) {
			attr.addElement(freqTable.getAttributeId(i));
		}
		checkWhatSelected();
		Frequencies selFreq = dataTable.getValueFrequencies(colN, selNumbers, false, true);
		if (selFreq == null) {
			for (int i = 0; i < freqTable.getDataItemCount(); i++) {
				DataRecord rec = freqTable.getDataRecord(i);
				rec.setNumericAttrValue(0f, "0", 3);
				rec.setNumericAttrValue(0f, "0.00", 4);
			}
			freqTable.notifyPropertyChange("values", null, attr);
			selectRowsInTableViewer(null);
			return;
		}
		IntArray selValueIndexes = new IntArray(freqTable.getDataItemCount(), 1);
		for (int i = 0; i < freqTable.getDataItemCount(); i++) {
			DataRecord rec = freqTable.getDataRecord(i);
			int f1 = (int) Math.round(rec.getNumericAttrValue(0));
			int f2 = (int) Math.round(rec.getNumericAttrValue(1));
			int f3 = 0;
			float selRatio = 0f;
			String selRatioStr = "0.00";
			if (selFreq != null) {
				f3 = selFreq.getFrequency(rec.getName());
				if (f3 < 0) {
					f3 = 0;
				}
				if (f3 > 0) {
					selRatio = 100.0f * f3 / f1;
					selRatioStr = StringUtil.floatToStr(selRatio, 2);
					if (f3 >= f2) {
						selValueIndexes.addElement(i);
					}
				}
			}
			rec.setNumericAttrValue(f3, String.valueOf(f3), 3);
			rec.setNumericAttrValue(selRatio, selRatioStr, 4);
		}
		freqTable.notifyPropertyChange("values", null, attr);
		selectRowsInTableViewer(selValueIndexes);
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(filter)) {
			if (e.getPropertyName().equals("destroyed")) {
				filter.removePropertyChangeListener(this);
				filter = null;
			} else {
				recountFrequencies();
			}
		} else if (e.getSource().equals(tableViewer) && e.getPropertyName().equals("sorting")) {
			dirChoice.select((tableViewer.getDescending()) ? 1 : 0);
			sortChoice.select(tableViewer.getSortAttrIdx() + 2);
			dirChoice.setEnabled(tableViewer.getSortAttrIdx() >= -1);
		} else if (e.getSource() == dataTable) {
			if (e.getPropertyName().equals("destroyed") || e.getPropertyName().equals("structure_complete")) {
				destroy();
			} else if (e.getPropertyName().equals("filter")) {
				if (filter != null) {
					filter.removePropertyChangeListener(this);
				}
				filter = dataTable.getObjectFilter();
				if (filter != null) {
					filter.addPropertyChangeListener(this);
				}
				recountFrequencies();
			} else if (e.getPropertyName().equals("values") || e.getPropertyName().equals("data_added") || e.getPropertyName().equals("data_removed") || e.getPropertyName().equals("data_updated")) {
				selectRowsInTableViewer(null);
				//the frequency table needs to be re-filled again
				Frequencies freq = dataTable.getValueFrequencies(colN, false, true);
				if (freq == null || freq.getItemCount() < 1) {
					//set all previous frequency values to zero
					for (int i = 0; i < freqTable.getDataItemCount(); i++) {
						DataRecord rec = freqTable.getDataRecord(i);
						for (int j = 0; j < rec.getAttrCount(); j++) {
							rec.setNumericAttrValue(0f, "0", j);
						}
					}
					Vector attr = new Vector(freqTable.getAttrCount(), 1);
					for (int i = 0; i < freqTable.getAttrCount(); i++) {
						attr.addElement(freqTable.getAttributeId(i));
					}
					freqTable.notifyPropertyChange("values", null, attr);
				} else {
					freq.sortItems((isNumeric) ? Frequencies.order_as_numbers_ascend : Frequencies.order_alphabet);
					Frequencies filtFreq = dataTable.getValueFrequencies(colN, true, true);
					freqTable.removePropertyChangeListener(tableViewer);
					freqTable.removeAllData();
					for (int i = 0; i < freq.getItemCount(); i++) {
						String value = freq.getItemAsString(i);
						int f1 = freq.getFrequency(i), f2 = filtFreq.getFrequency(value);
						if (f2 < 0) {
							f2 = 0;
						}
						float ratio = Float.NaN;
						if (f1 > 0) {
							ratio = 100.0f * f2 / f1;
						}
						String ratioStr = (f1 == 0) ? null : (f2 == 0) ? "0.00" : (f1 == f2) ? "100.00" : StringUtil.floatToStr(ratio, 2);
						DataRecord rec = new DataRecord(String.valueOf(i + 1), value);
						freqTable.addDataRecord(rec);
						rec.setNumericAttrValue(f1, String.valueOf(f1), 0);
						rec.setNumericAttrValue(f2, String.valueOf(f2), 1);
						rec.setNumericAttrValue(ratio, ratioStr, 2);
					}
					freqTable.addPropertyChangeListener(tableViewer);
					freqTable.notifyPropertyChange("data_updated", null, null);
				}
			}
		} else if (e.getPropertyName().equals(Supervisor.eventObjectColors) && dataTable.getEntitySetIdentifier().equals(e.getNewValue())) {
			//propagated classification of objects
			boolean hasItemColors = tableViewer.hasItemColors();
			Color itemColors[] = null;
			SingleAttributeClassifier cl = null;
			if (supervisor != null && supervisor.getObjectColorer() != null && (supervisor.getObjectColorer() instanceof SingleAttributeClassifier) && supervisor.getObjectColorer().getEntitySetIdentifier().equals(dataTable.getEntitySetIdentifier())) {
				cl = (SingleAttributeClassifier) supervisor.getObjectColorer();
			}
			if (cl != null && cl.getAttrColumnN() == colN) {
				itemColors = new Color[freqTable.getDataItemCount()];
				for (int i = 0; i < itemColors.length; i++) {
					itemColors[i] = cl.getColorForValue(freqTable.getDataItemName(i));
				}
			}
			if (itemColors != null || (itemColors == null && hasItemColors)) {
				tableViewer.setItemColors(itemColors);
				tableViewer.tablerepaint();
				tableViewer.repaint();
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(sortChoice) || e.getSource().equals(dirChoice)) {
			setSortingParameters();
			tableViewer.tablerepaint();
			tableViewer.repaint();
		}
	}

	private void setSortingParameters() {
		switch (sortChoice.getSelectedIndex()) {
//if no selection
		case 0:
			tableViewer.setSortAttr(null);
			dirChoice.setEnabled(false);
			break;
//if selection by name
		case 1:
			tableViewer.setSortAttr("_NAME_");
			dirChoice.setEnabled(true);
			tableViewer.setDescending(dirChoice.getSelectedIndex() == 1);
			break;
//if selection by some attribute
		default:
			int aidx = sortChoice.getSelectedIndex() - 2;
			tableViewer.setSortAttr(aidx);
			dirChoice.setEnabled(true);
			tableViewer.setDescending(dirChoice.getSelectedIndex() == 1);
//Set horizontal scrollbar to show sorting attribute if it is not visible
			if (aidx < tableViewer.getHScrollValue() || aidx > (tableViewer.getHScrollValue() + tableViewer.getHScroll().getVisibleAmount() - 1)) {
				tableViewer.getHScroll().setValue(aidx);
			}
		}
	}

//---------------------- Destroyable interface -------------------------------
	/**
	* Indicates whether the viewer was destroyed
	*/
	protected boolean destroyed = false;

	/**
	* Makes necessary operations for destroying, in particular, unregisters from
	* listening table changes.
	*/
	@Override
	public void destroy() {
		if (dataTable != null) {
			dataTable.removePropertyChangeListener(this);
		}
		if (supervisor != null) {
			supervisor.removePropertyChangeListener(this);
			supervisor.getHighlighter(freqTable.getEntitySetIdentifier()).removeHighlightListener(this);
			supervisor.getHighlighter(dataTable.getEntitySetIdentifier()).removeHighlightListener(this);
		}
		if (filter != null) {
			filter.removePropertyChangeListener(this);
		}
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

//---------------------- HighlightListener interface -------------------------
	/**
	* Notification about change of the set of objects to be transiently
	* highlighted. A ValueFrequencyViewer does not react to this event.
	*/
	@Override
	public void highlightSetChanged(Object source, String setId, Vector highlighted) {
	}

	/**
	* Notification about change of the set of objects to be selected (durably
	* highlighted).
	* In response, a ValueFrequencyViewer should
	* 1) find table records corresponding to the selected/deselected attribute values;
	* 2) send an event to highlight/dehighlight the corresponding objects
	* The argument "source" is usually a reference to a Highlighter.
	* The argument setId is the identifier of the set the highlighted objects
	* belong to (e.g. map layer or table). The argument "selected" is a vector
	* of identifiers of currently selected objects.
	*/
	@Override
	public void selectSetChanged(Object source, String setId, Vector selected) {
		if (setId.equals(freqTable.getEntitySetIdentifier())) {
			if (source.equals(this))
				return;
			IntArray selValIndexes = tableViewer.getRecordNumbers(selected);
			Highlighter hl = supervisor.getHighlighter(dataTable.getEntitySetIdentifier());
			if (selValIndexes == null || selValIndexes.size() < 1) {
				hl.clearSelection(this);
			} else {
				Vector values = new Vector(selValIndexes.size(), 1);
				int nsel = 0;
				for (int i = 0; i < selValIndexes.size(); i++) {
					DataRecord frec = freqTable.getDataRecord(selValIndexes.elementAt(i));
					values.addElement(frec.getName());
					nsel += frec.getNumericAttrValue(1);
				}
				if (nsel < 1) {
					hl.clearSelection(this);
				} else {
					Vector selObjIds = new Vector(nsel, 1);
					for (int i = 0; i < dataTable.getDataItemCount(); i++)
						if (filter.isActive(i)) {
							DataRecord rec = dataTable.getDataRecord(i);
							if (StringUtil.isStringInVectorIgnoreCase(rec.getAttrValueAsString(colN), values)) {
								selObjIds.addElement(rec.getId());
							}
						}
					hl.replaceSelectedObjects(this, selObjIds);
				}
			}
		} else if (setId.equals(dataTable.getEntitySetIdentifier())) {
			recountFrequenciesAmongSelected();
		}
	}
}
