package guide_tools.guide;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.util.Vector;

import spade.kbase.scenarios.Instrument;
import spade.kbase.scenarios.Manipulator;
import spade.kbase.scenarios.TreeNode;
import spade.kbase.scenarios.Visualization;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.FoldablePanel;
import spade.lib.basicwin.PopupManager;
import spade.lib.basicwin.TextCanvas;

/**
* Manages GUI elements relevant to selection of map visualization methods
*/

public class MapVisManager extends InstrumentManager implements ActionListener {
	protected Vector suitableVisMethods = null;
	protected int currMethodN = -1;

	protected Checkbox visMethodsCB[] = null;
	protected Panel selectedMethodPanel = null;
	protected FoldablePanel attrAndVisFP = null;

	public MapVisManager(Instrument instr, // the instrument to manage
			TreeNode task, // the task the instrument belongs to
			GuideCore core) //contains visualization methods and primitive tasks
	{
		super(instr, task, core);
	}

	@Override
	protected Component makeActiveStateControls() {
		attrAndVisFP = null;
		suitableVisMethods = null;
		visMethodsCB = null;
		currMethodN = -1;
		if (core.kb.visMethods == null || core.kb.visMethods.getMethodCount() < 1 || instr == null || !instr.type.equals("map_vis"))
			return null;
		if (attr == null) {
			selectAttributes("to be presented on the map");
		}
		if (attr == null)
			return null;
		//which visualization methods are appropriate for the selected attributes?
		int nAttrs = attr.size();
		char types[] = new char[nAttrs];
		for (int i = 0; i < nAttrs; i++) {
			types[i] = core.sysMan.getAttributeType((String) attr.elementAt(i), core.mapN, layerId);
		}
		//relationships between attributes are currently unknown
		String relation = null;
		if (nAttrs > 1)
			if (core.sysMan.arePartsInWhole(attr, core.mapN, layerId)) {
				relation = "parts_of_whole";
			} else if (core.sysMan.areComparable(attr, core.mapN, layerId)) {
				relation = "comparable";
			}
		suitableVisMethods = core.kb.visMethods.selectVisMethods(nAttrs, types, relation, instr.useReq, instr.priorities);
		//remove methods that are not available in the system
		boolean notAvailable = false;
		if (suitableVisMethods != null) {
			for (int i = suitableVisMethods.size() - 1; i >= 0; i--) {
				Visualization vis = (Visualization) suitableVisMethods.elementAt(i);
				if (!core.sysMan.isMapVisMethodAvailable(vis.method)) {
					suitableVisMethods.removeElementAt(i);
				}
			}
			if (suitableVisMethods.size() < 1) {
				suitableVisMethods = null;
				notAvailable = true;
			}
		}
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

		if (suitableVisMethods == null) {
			Label l = new Label((notAvailable) ? "Sorry... The appropriate visualization methods are not available" : "Sorry... No appropriate visualization methods found");
			l.setBackground(GuideCore.visFailBkgColor);
			p.add(l);
			return p;
		}

		CheckboxGroup cbg = new CheckboxGroup();
		Label l = null;
		visMethodsCB = new Checkbox[suitableVisMethods.size()];
		if (suitableVisMethods.size() > 1) {
			l = new Label("Recommended visualisation methods:");
		} else {
			l = new Label("Recommended visualisation method:");
		}
		l.setBackground(GuideCore.visMethodsBkgColor);
		p.add(l);
		for (int i = 0; i < suitableVisMethods.size(); i++) {
			Visualization vis = (Visualization) suitableVisMethods.elementAt(i);
			String txt = "(" + vis.complexity + ") " + core.context.fillNameSlots(vis.getName());
			visMethodsCB[i] = new Checkbox(txt, cbg, false);
			visMethodsCB[i].addItemListener(this);
			visMethodsCB[i].setBackground(GuideCore.visMethodsBkgColor);
			/*
			if (!core.sysMan.isMapVisMethodAvailable(vis.method))
			  visMethodsCB[i].setEnabled(false);
			else
			*/
			if (vis.getExplanation() != null) {
				new PopupManager(visMethodsCB[i], core.context.fillNameSlots(vis.getExplanation()), true);
			}
			p.add(visMethodsCB[i]);
		}
		attrAndVisFP = new FoldablePanel(p);
		attrAndVisFP.open();
		return attrAndVisFP;
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		String cmd = evt.getActionCommand();
		if (cmd.equals("change_attributes")) {
			Vector prevAttr = attr;
			selectAttributes("to be presented on the map");
			if (attr == null) {
				attr = prevAttr;
				return;
			}
			if (prevAttr != null && prevAttr.size() == attr.size()) {
				boolean same = true;
				for (int i = 0; i < attr.size() && same; i++) {
					same = attr.elementAt(i).equals(prevAttr.elementAt(i));
				}
				if (same)
					return;
			}
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
			CManager.scrollToExpose(cmp);
		}
	}

	@Override
	public void itemStateChanged(ItemEvent evt) {
		if (evt.getSource() == onoffCB) {
			super.itemStateChanged(evt);
			return;
		}
		if (visMethodsCB != null) {
			int selMethodN = -1;
			for (int i = 0; i < visMethodsCB.length && selMethodN < 0; i++)
				if (visMethodsCB[i].getState()) {
					selMethodN = i;
				}
			if (selMethodN >= 0 && selMethodN != currMethodN) {
				if (instr.specReq != null && instr.specReq.contains("additional_map")) {
					core.sysMan.duplicateMapView(core.mapN);
				}
				Visualization vis = (Visualization) suitableVisMethods.elementAt(selMethodN);
				core.sysMan.showDataOnMap(core.mapN, layerId, attr, vis.method);
				currMethodN = selMethodN;
				if (selectedMethodPanel != null) {
					instrPanel.remove(selectedMethodPanel);
				}
				selectedMethodPanel = new Panel(new ColumnLayout());
				Label visl = new Label(vis.getName(), Label.CENTER);
				Panel ip = new Panel(new ColumnLayout());
				TextCanvas tc = new TextCanvas();
				tc.setBackground(GuideCore.bkgInstructionColor);
				if (vis.getGeneralInstruction() != null) {
					tc.addTextLine(core.context.fillNameSlots(vis.getGeneralInstruction()));
				} else if (vis.getExplanation() != null) {
					tc.addTextLine(vis.getExplanation());
				}
				if (tc.hasText()) {
					ip.add(tc);
				}
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
				for (int i = 0; i < primTasks.size(); i++) {
					String primTask = (String) primTasks.elementAt(i);
					;
					String txt = vis.getInstruction(primTask);
					if (txt != null) {
						Label l = new Label(core.kb.getPrimTaskName(primTask));
						l.setForeground(GuideCore.txtPrimTaskColor);
						ip.add(l);
						tc = new TextCanvas();
						tc.setBackground(GuideCore.bkgInstructionColor);
						tc.addTextLine(core.context.fillNameSlots(txt));
						ip.add(tc);
					}
				}
				FoldablePanel fp = new FoldablePanel(ip, visl);
				//fp.open();
				selectedMethodPanel.add(fp);
				for (int j = 0; j < vis.getManipulatorCount(); j++) {
					Manipulator man = vis.getManipulator(j);
					boolean supportsSomeTask = false;
					for (int i = 0; i < primTasks.size() && !supportsSomeTask; i++) {
						supportsSomeTask = man.isTaskSupported((String) primTasks.elementAt(i));
					}
					if (!supportsSomeTask) {
						continue;
					}
					Label manl = new Label(man.getName(), Label.CENTER);
					manl.setForeground(GuideCore.txtManipulatorNameColor);
					manl.setBackground(GuideCore.bkgManipulatorColor);
					//selectedMethodPanel.add(manl);
					tc = new TextCanvas();
					tc.setBackground(GuideCore.bkgManipulatorInstrColor);
					if (man.getGeneralInstruction() != null) {
						tc.addTextLine(core.context.fillNameSlots(man.getGeneralInstruction()));
					} else if (man.getExplanation() != null) {
						tc.addTextLine(man.getExplanation());
					}
					ip = new Panel(new ColumnLayout());
					ip.add(tc);
					for (int i = 0; i < primTasks.size(); i++) {
						String primTask = (String) primTasks.elementAt(i);
						;
						String txt = man.getInstruction(primTask);
						if (txt != null) {
							Label l = new Label(core.kb.getPrimTaskName(primTask));
							l.setForeground(GuideCore.txtPrimTaskColor);
							l.setBackground(GuideCore.bkgManipulatorInstrColor);
							ip.add(l);
							tc = new TextCanvas();
							tc.setBackground(GuideCore.bkgManipulatorInstrColor);
							tc.addTextLine(core.context.fillNameSlots(txt));
							ip.add(tc);
						}
					}
					fp = new FoldablePanel(ip, manl);
					fp.setBackground(GuideCore.bkgManipulatorColor);
					selectedMethodPanel.add(fp);
				}
				if (attrAndVisFP != null) {
					attrAndVisFP.close();
				}
				instrPanel.add(selectedMethodPanel, instrPanel.getComponentCount());
				CManager.validateAll(instrPanel);
				CManager.scrollToExpose(selectedMethodPanel);
			}
		}
	}

	@Override
	public void closeInstrument() {
		//if (layerId!=null) core.sysMan.eraseDataFromMap(core.mapN,layerId);
	}
}