package guide_tools.guide;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import spade.kbase.scenarios.Common;
import spade.kbase.scenarios.Input;
import spade.kbase.scenarios.Instrument;
import spade.kbase.scenarios.Restriction;
import spade.kbase.scenarios.Tool;
import spade.kbase.scenarios.ToolInput;
import spade.kbase.scenarios.TreeNode;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.FoldablePanel;
import spade.lib.basicwin.PopupManager;
import spade.lib.basicwin.TextCanvas;
import spade.lib.util.StringUtil;

/**
* Manages triggering of supplementary analysis tools available in the system
* besides map visualisation
*/
public class SupplToolManager extends InstrumentManager implements ActionListener {
	/**
	* The tool the instrument refers to
	*/
	protected Tool tool = null;
	/**
	* Minimum and maximum allowed attribute number for the tool
	*/
	protected int minN = -1, maxN = -1;

	/**
	* Checks if the given instrument can be used: 1) it is properly defined;
	* 2) the corresponding function is available; 3) the necessary context
	* is defined.
	*/
	public static boolean canBeUsed(Instrument instr, GuideCore core) {
		if (instr == null || !instr.type.equals("tool") || instr.function == null)
			return false;
		if (!core.sysMan.isToolAvailable(instr.function))
			return false;
		//are the input context items to replace the tool arguments defined?
		for (int i = 0; i < instr.getInputCount(); i++)
			if (!core.context.isContextElementDefined(instr.getInput(i).arg))
				return false;
		return true;
	}

	/**
	* Finds in the knowledge base the Tool the given instrument refers to.
	* Replaces the formal arguments of the Tool (if any) by actual
	* arguments, i.e. elements of currently defined context mentioned in the
	* Instrument specification. If the Instrument specification contains any
	* task-specific instruction texts, these texts replace the more generic
	* texts in the Tool specification. Returns a copy of the Tool with the
	* arguments and texts being replaced. If no replacements have been made,
	* the initial Tool is returned.
	*/
	public static Tool getTool(Instrument instr, GuideCore core) {
		if (instr == null || !instr.type.equals("tool") || instr.function == null)
			return null;
		Tool tool = core.kb.getTool(instr.function), copy = null;
		if (tool == null)
			return null;
		//replace name, explanation, and instruction
		if (instr.getName() != null) {
			copy = tool.makeCopy();
			copy.setName(instr.getName());
		}
		if (instr.getExplanation() != null) {
			if (copy == null) {
				copy = tool.makeCopy();
			}
			copy.setExplanation(instr.getExplanation());
		}
		if (instr.getInstruction() != null) {
			if (copy == null) {
				copy = tool.makeCopy();
			}
			copy.setGeneralInstruction(instr.getInstruction());
		}
		if (instr.getInputCount() > 0 && tool.getInputCount() > 0) {
			//replace arguments
			for (int i = 0; i < tool.getInputCount(); i++) {
				String argId = tool.getToolInput(i).arg_id;
				//which context element should replace it?
				String elId = null;
				for (int j = 0; j < instr.getInputCount() && elId == null; j++) {
					Input input = instr.getInput(j);
					if (input.standsFor != null && input.standsFor.equals(argId)) {
						elId = input.arg;
					}
				}
				if (elId != null) {
					if (copy == null) {
						copy = tool.makeCopy();
					}
					//replace in tool input
					copy.getToolInput(i).arg_id = elId;
					//replace references in texts
					if (copy.name != null) {
						copy.name = StringUtil.replace(copy.name, argId, elId);
					}
					if (copy.explanation != null) {
						copy.explanation = StringUtil.replace(copy.explanation, argId, elId);
					}
					if (copy.instructions != null) {
						for (int j = 0; j < copy.instructions.size(); j++) {
							String str = (String) copy.instructions.elementAt(j);
							str = StringUtil.replace(str, argId, elId);
							copy.instructions.setElementAt(str, j);
						}
					}
				}
			}
		}
		if (copy == null)
			return tool;
		return copy;
	}

	public SupplToolManager(Instrument instr, // the instrument to manage
			TreeNode task, // the task the instrument belongs to
			GuideCore core) {
		super(instr, task, core);
		tool = getTool(instr, core);
		//System.out.println(instr.toString());
		//System.out.println(tool.toString());
	}

	@Override
	public Component constructControls() {
		if (tool == null)
			return super.constructControls();
		if (instrPanel != null)
			return instrPanel;
		instrPanel = new Panel(new ColumnLayout());
		if (core.sysMan.isToolRunnable(tool.function)) {
			onoffCB = new Checkbox(core.context.fillNameSlots(tool.getName()), false);
			onoffCB.addItemListener(this);
			if (tool.getExplanation() != null) {
				new PopupManager(onoffCB, core.context.fillNameSlots(tool.getExplanation()), true);
			}
			if (core.sysMan.canHelp(tool.function)) {
				Button b = new Button("?");
				b.setActionCommand("help_" + tool.function);
				b.addActionListener(core);
				Panel p = new Panel(new BorderLayout());
				p.add(onoffCB, "Center");
				p.add(b, "East");
				instrPanel.add(p);
			} else {
				instrPanel.add(onoffCB);
			}
		} else {
			Label l = new Label(core.context.fillNameSlots(tool.getName()));
			if (tool.getExplanation() != null) {
				new PopupManager(l, core.context.fillNameSlots(tool.getExplanation()), true);
			}
			Component cap = l;
			if (core.sysMan.canHelp(tool.function)) {
				Button b = new Button("?");
				b.setActionCommand("help_" + tool.function);
				b.addActionListener(core);
				Panel p = new Panel(new BorderLayout());
				p.add(l, "Center");
				p.add(b, "East");
				cap = p;
			}
			Component c = retrieveRelevantInstructions();
			if (c != null) {
				FoldablePanel fp = new FoldablePanel(c, cap);
				fp.open();
				instrPanel.add(fp);
			} else {
				instrPanel.add(cap);
			}
		}
		return instrPanel;
	}

	/**
	* Returns a TextCanvas or a Panel containing the general instruction about
	* the usage of the tool as well as instructions for application of the tool
	* to the relevant primitive tasks (specified in the Instrument)
	*/
	protected Component retrieveRelevantInstructions() {
		if (tool == null || tool.instructions == null || tool.instructions.size() < 1)
			return null;
		TextCanvas tc = null;
		String txt = tool.getGeneralInstruction();
		if (txt != null) {
			tc = new TextCanvas();
			tc.setBackground(GuideCore.bkgInstructionColor);
			tc.addTextLine(core.context.fillNameSlots(txt));
		}
		if (instr.useReq == null && instr.useDesired == null)
			return tc;
		Vector primTasks = new Vector(5, 5);
		if (instr.useReq != null) {
			for (int i = 0; i < instr.useReq.size(); i++) {
				primTasks.addElement(instr.useReq.elementAt(i));
			}
		}
		if (instr.useDesired != null) {
			for (int i = 0; i < instr.useDesired.size(); i++) {
				primTasks.addElement(instr.useDesired.elementAt(i));
			}
		}
		if (primTasks.size() < 1)
			return tc;
		Panel p = new Panel(new ColumnLayout());
		if (tc != null) {
			p.add(tc);
		}
		for (int i = 0; i < primTasks.size(); i++) {
			String primTask = (String) primTasks.elementAt(i);
			;
			txt = tool.getInstruction(primTask);
			if (txt != null) {
				Label l = new Label(core.kb.getPrimTaskName(primTask));
				l.setForeground(GuideCore.txtPrimTaskColor);
				p.add(l);
				tc = new TextCanvas();
				tc.setBackground(GuideCore.bkgInstructionColor);
				tc.addTextLine(core.context.fillNameSlots(txt));
				p.add(tc);
			}
		}
		return p;
	}

	@Override
	protected Component makeActiveStateControls() {
		//to which layer the visualisation is applied?
		determineLayer();
		if (layerId == null)
			return null;
		//does the tool require selection of attributes?
		ToolInput ainp = null;
		for (int i = 0; i < tool.getInputCount() && ainp == null; i++)
			if (tool.getToolInput(i).arg_type.equals("attribute")) {
				ainp = tool.getToolInput(i);
			}
		if (ainp == null) {
			if (core.sysMan.applyTool(core.mapN, layerId, null, tool.function)) {
				Component c = retrieveRelevantInstructions();
				if (c == null)
					return null;
				FoldablePanel fp = new FoldablePanel(c);
				//fp.open();
				return fp;
			}
			TextCanvas tc = new TextCanvas();
			tc.setBackground(GuideCore.visFailBkgColor);
			tc.addTextLine(core.sysMan.getErrorMessage());
			return tc;
		}
		if (minN < 0 && maxN < 0) {
			minN = ainp.minNumber;
			maxN = ainp.maxNumber;
			int mm[] = getMinAndMaxAttrNumber(); //from the instrument specification
			//further restrict the attribute number allowed
			if (mm[0] > minN) {
				minN = mm[0];
			}
			if (mm[1] > 0 && mm[1] < maxN) {
				maxN = mm[1];
			}
		}
		//select attributes for application of the tool
		Vector attrSubset = selectAppropriateAttributes(ainp);
		if (attrSubset == null || attrSubset.size() < 1 || attrSubset.size() < minN) {
			TextCanvas tc = new TextCanvas();
			tc.setBackground(GuideCore.visFailBkgColor);
			tc.addTextLine("No appropriate attributes for the tool!");
			return tc;
		}
		attr = letUserSelectAttributes("for " + core.context.fillNameSlots(tool.getName()), attrSubset, minN, maxN);
		if (attr == null)
			return null;
		Panel pp = new Panel(new BorderLayout());
		if (attr.size() == 1) {
			pp.add(new Label("Attribute:"), "Center");
		} else {
			pp.add(new Label("Attributes:"), "Center");
		}
		Button b = new Button("Change");
		b.setActionCommand("change_attributes");
		b.addActionListener(this);
		Panel p = new Panel(new FlowLayout(FlowLayout.CENTER, 5, 0));
		p.add(b);
		pp.add(p, "East");
		TextCanvas tc = new TextCanvas();
		for (int i = 0; i < attr.size(); i++) {
			tc.addTextLine(core.sysMan.getAttributeName((String) attr.elementAt(i), core.mapN, layerId));
		}
		p = new Panel(new ColumnLayout());
		p.add(pp);
		p.add(tc);
		if (core.sysMan.applyTool(core.mapN, layerId, attr, tool.function)) {
			Component c = retrieveRelevantInstructions();
			if (c != null) {
				FoldablePanel fp = new FoldablePanel(c);
				//fp.open();
				p.add(fp);
			}
		} else {
			tc = new TextCanvas();
			tc.setBackground(GuideCore.visFailBkgColor);
			tc.addTextLine(core.sysMan.getErrorMessage());
			p.add(tc);
		}
		return p;
	}

	protected Vector selectAppropriateAttributes(ToolInput ainp) {
		if (ainp == null)
			return null;
		determineLayer();
		if (layerId == null)
			return null;
		Vector attrSubset = selectAppropriateAttributes(); //from the superclass
		if (attrSubset == null)
			return null; //no attributes to select from
		System.out.println("attributes (superclass selection): " + attrSubset.toString());
		//if the tool has restrictions on types of attributes, exclude the
		//attributes that do not satisfy them
		if (ainp.restrictions != null) {
			for (int i = 0; i < ainp.restrictions.size(); i++) {
				Restriction restr = (Restriction) ainp.restrictions.elementAt(i);
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
		System.out.println("attributes (after restriction): " + attrSubset.toString());
		return attrSubset;
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		String cmd = evt.getActionCommand();
		if (cmd.equals("change_attributes")) {
			if (instrPanel.getComponentCount() > 1) {
				for (int i = instrPanel.getComponentCount() - 1; i > 0; i--) {
					instrPanel.remove(i);
				}
			}
			Component cmp = makeActiveStateControls();
			if (cmp != null) {
				instrPanel.add(cmp);
			} else {
				onoffCB.setState(false);
			}
			CManager.validateAll(instrPanel);
			if (cmp != null) {
				CManager.scrollToExpose(cmp);
			}
		}
	}
}