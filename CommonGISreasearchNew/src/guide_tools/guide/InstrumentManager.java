package guide_tools.guide;

import java.awt.Checkbox;
import java.awt.Component;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import spade.kbase.scenarios.Common;
import spade.kbase.scenarios.Input;
import spade.kbase.scenarios.Instrument;
import spade.kbase.scenarios.Restriction;
import spade.kbase.scenarios.TreeNode;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.FoldablePanel;
import spade.lib.basicwin.PopupManager;
import spade.lib.basicwin.TextCanvas;

/**
* Manages GUI elements relevant to an instrument
*/

public class InstrumentManager implements ItemListener {
	/**
	* The GuideCore contains the knowledge base, the context currently defined,
	* references to the system manager, current scenario etc. as well as
	* some constants used in different components of the Guide
	*/
	protected GuideCore core = null;

	protected Instrument instr = null;
	/**
	* The task the instrument belongs to
	*/
	protected TreeNode task = null;
	protected String layerId = null, layerIdInContext = null;
	/**
	* Currently selected attributes
	*/
	protected Vector attr = null;

	protected Panel instrPanel = null;
	protected Checkbox onoffCB = null;

	public InstrumentManager(Instrument instr, // the instrument to manage
			TreeNode task, // the task the instrument belongs to
			GuideCore core) {
		this.instr = instr;
		this.task = task;
		this.core = core;
	}

	public Instrument getInstrument() {
		return instr;
	}

	public TreeNode getTask() {
		return task;
	}

	public Component constructControls() {
		if (instrPanel != null)
			return instrPanel;
		instrPanel = new Panel(new ColumnLayout());
		if (!instr.isDefault) {
			onoffCB = new Checkbox(core.context.fillNameSlots(instr.getName()), false);
			onoffCB.addItemListener(this);
			if (instr.getExplanation() != null) {
				new PopupManager(onoffCB, core.context.fillNameSlots(instr.getExplanation()), true);
			}
			instrPanel.add(onoffCB);
		}
		if (instr.isDefault && instr.getInstruction() != null) {
			TextCanvas tc = new TextCanvas();
			tc.setBackground(GuideCore.bkgInstructionColor);
			tc.setText(core.context.fillNameSlots(instr.getInstruction()));
			instrPanel.add(tc);
		}
		return instrPanel;
	}

	public Component getControls() {
		if (instrPanel == null) {
			constructControls();
		}
		return instrPanel;
	}

	protected void determineLayer() {
		//on which layer the visualization is done?
		if (layerId == null) {
			for (int i = 0; i < instr.inputs.size() && layerId == null; i++) {
				Input input = (Input) instr.inputs.elementAt(i);
				if (input == null || input.arg == null) {
					continue;
				}
				ContextItem cit = core.context.getContextItem(input.arg);
				if (cit == null || !cit.getType().equals("layer") || cit.getContent() == null) {
					continue;
				}
				layerIdInContext = input.arg;
				layerId = (String) cit.getContent();
			}
		}
	}

	protected Vector selectAttributes(String purpose) {
		Vector subset = selectAppropriateAttributes();
		if (subset != null) {
			int mm[] = getMinAndMaxAttrNumber();
			return letUserSelectAttributes(purpose, subset, mm[0], mm[1]);
		}
		return null;
	}

	protected int[] getMinAndMaxAttrNumber() {
		int minN = -1, maxN = -1;
		if (instr.restrictions != null) {
			for (int i = 0; i < instr.restrictions.size(); i++) {
				Restriction r = (Restriction) instr.restrictions.elementAt(i);
				if (r.type == null || r.values == null || r.values.size() < 1) {
					continue;
				}
				if (r.type.equals("min_attr_number") || r.type.equals("max_attr_number")) {
					int n = -1;
					String str = (String) r.values.elementAt(0);
					try {
						n = Integer.valueOf(str).intValue();
					} catch (NumberFormatException nfe) {
					}
					if (n < 0) {
						continue;
					}
					if (r.type.equals("min_attr_number")) {
						minN = n;
					} else {
						maxN = n;
					}
				}
			}
		}
		int mm[] = new int[2];
		mm[0] = minN;
		mm[1] = maxN;
		return mm;
	}

	protected Vector selectAppropriateAttributes() {
		if (core.context == null || instr == null || instr.inputs == null || instr.inputs.size() < 1)
			return null;
		determineLayer();
		if (layerId == null)
			return null;
		//select attributes to be visualized on the map
		//if the user selected a subset of attributes to consider, restrict
		//the set to select from
		Vector attrSubset = null;
		for (int i = 0; i < core.context.getItemCount() && attrSubset == null; i++) {
			ContextItem cit = core.context.getContextItem(i);
			if (cit != null && cit.getType().equals("attributes") && cit.getRefersTo() != null && cit.getRefersTo().equals(layerIdInContext)) {
				Object content = cit.getContent();
				if (content != null) {
					if (content instanceof Vector) {
						attrSubset = (Vector) ((Vector) content).clone();
					} else {
						attrSubset = new Vector(1, 5);
						attrSubset.addElement(content);
					}
				}
			}
		}
		if (instr.restrictions != null) {
			for (int i = 0; i < instr.restrictions.size(); i++) {
				Restriction restr = (Restriction) instr.restrictions.elementAt(i);
				if (!restr.isValid()) {
					continue;
				}
				if (restr.type.equals("attr_type")) {
					char types[] = new char[restr.getValuesCount()];
					for (int j = 0; j < types.length; j++) {
						types[j] = Common.encodeAttrType(restr.getValue(j));
					}
					for (int j = attrSubset.size() - 1; j >= 0; j--) {
						char t = core.sysMan.getAttributeType((String) attrSubset.elementAt(j), core.mapN, layerId);
						boolean found = false;
						for (int k = 0; k < types.length && !found; k++) {
							found = types[k] == t;
						}
						if (!found) {
							attrSubset.removeElementAt(j);
						}
					}
				}
			}
		}
		return attrSubset;
	}

	protected Vector letUserSelectAttributes(String purpose, Vector attrSubset, int minN, int maxN) {
		if (attrSubset == null)
			return null; //no attributes to select from
		System.out.println("minN=" + minN + ", maxN=" + maxN);
		String prompt = "Select " + ((maxN == 1) ? "an attribute" : "attributes");
		if (purpose != null) {
			prompt = prompt + " " + purpose;
		}
		attr = core.sysMan.selectAttributes(core.mapN, layerId, attrSubset, attr, minN, maxN, prompt);
		return attr;
	}

	@Override
	public void itemStateChanged(ItemEvent evt) {
		if (evt.getSource() == onoffCB)
			if (onoffCB.getState()) {
				//display the instruction or/and instrument-dependent controls
				Component cmp = makeActiveStateControls();
				if (cmp != null) {
					instrPanel.add(cmp);
					CManager.validateAll(instrPanel);
					CManager.scrollToExpose(cmp);
				} else {
					onoffCB.setState(false);
				}
			} else {
				if (instrPanel.getComponentCount() > 1) {
					for (int i = instrPanel.getComponentCount() - 1; i > 0; i--) {
						instrPanel.remove(i);
					}
					CManager.validateAll(instrPanel);
				}
				closeInstrument();
			}
	}

	protected Component makeActiveStateControls() {
		String txt = instr.getInstruction();
		if (txt == null) {
			txt = instr.getExplanation();
		}
		if (txt != null) {
			TextCanvas tc = new TextCanvas();
			tc.setBackground(GuideCore.bkgInstructionColor);
			tc.setText(core.context.fillNameSlots(txt));
			return new FoldablePanel(tc);
		}
		return null; //temporarily
	}

	public void closeInstrument() {
	}

	public void foldInstrumentControls() {
		foldWhateverPossible(instrPanel);
	}

	protected void foldWhateverPossible(Panel p) {
		if (p == null)
			return;
		if (p instanceof FoldablePanel) {
			((FoldablePanel) p).close();
		}
		for (int i = 0; i < p.getComponentCount(); i++)
			if (p.getComponent(i) instanceof Panel) {
				foldWhateverPossible((Panel) p.getComponent(i));
			}
	}
}