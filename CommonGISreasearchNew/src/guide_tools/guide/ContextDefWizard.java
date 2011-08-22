package guide_tools.guide;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import spade.kbase.scenarios.ContextElement;
import spade.kbase.scenarios.TreeNode;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.OKFrame;
import spade.lib.basicwin.TextCanvas;
import spade.lib.util.IntArray;

/**
* A wisard for definition of task context (selection of layers, attributes etc.)
*/

public class ContextDefWizard extends Thread {
	/**
	* The object which activated the wizard and waits for its results
	*/
	protected ActionListener owner = null;

	protected GuideCore core = null;
	/**
	* The sequence of tasks for which the context should be defined.
	* Elements of the vector are instances of TreeNode
	*/
	protected Vector taskList = null;
	/**
	* Indicates whether the wizard should try to acquire the full context or
	* only the obligatory context elements
	*/
	protected boolean getFullContext = true;
	/**
	* The element last defined through this wizard
	*/
	protected ContextElement lastDefined = null;
	/**
	* Indicates whether the thread is currently running
	*/
	protected boolean running = false;
	/**
	* The result of context specification: true == OK, false == cancelled or error
	*/
	protected boolean result = false;
	/**
	* Shows whether the process of context specification was cancelled
	*/
	protected boolean cancelled = false;
	/**
	* The error message that can be generated in the course of context definition
	*/
	protected String errorMsg = null;

	protected OKFrame dialog = null;

	/**
	* Returns the error message or null if no error occurred
	*/
	public String getErrorMessage() {
		return errorMsg;
	}

	/**
	* Starts the wizard (thread) to specify the context of the given task.
	*/
	public void specifyContext(ActionListener owner, GuideCore core, TreeNode task, boolean full) {
		this.owner = owner;
		this.core = core;
		taskList = new Vector(1, 5);
		taskList.addElement(task);
		getFullContext = full;
		start();
	}

	/**
	* Starts the wizard (thread) to specify the context for the given task
	* sequence.
	*/
	public void specifyContext(ActionListener owner, GuideCore core, Vector tasks, boolean full) {
		this.owner = owner;
		this.core = core;
		taskList = tasks;
		getFullContext = full;
		start();
	}

	/**
	* Notifies the owner (the object that started the wizard) about finishing
	*/
	public void finish() {
		if (dialog != null) {
			dialog.dispose();
			dialog = null;
		}
		running = false;
		if (owner != null) {
			owner.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "finished"));
		}
	}

	/**
	* Replies whether the thread is currently running
	*/
	public boolean isRunning() {
		return running;
	}

	/**
	* The result of context specification: true == OK, false == cancelled or error
	*/
	public boolean getResult() {
		return result;
	}

	/**
	* Replies whether the process of context specification was cancelled
	*/
	public boolean wasCancelled() {
		return cancelled;
	}

	@Override
	public void run() {
		running = true;
		result = false;
		cancelled = false;
		errorMsg = null;
		if (taskList == null || taskList.size() < 1) {
			result = true;
			finish();
			return;
		}
		Vector contextSpec = new Vector(10, 5), tasks = new Vector(10, 5);
		for (int i = 0; i < taskList.size(); i++) {
			TreeNode task = (TreeNode) taskList.elementAt(i);
			Vector tc = (getFullContext) ? core.kb.tasks.getTaskContext(task) : core.kb.tasks.getObligatoryTaskContext(task);
			if (tc != null) {
				for (int j = 0; j < tc.size(); j++) {
					contextSpec.addElement(tc.elementAt(j));
					tasks.addElement(task);
				}
			}
		}
		if (contextSpec == null || contextSpec.size() < 1) {
			result = true;
			finish();
			return;
		}
		Vector wizardSteps = new Vector(10, 10);
		IntArray elNumbers = new IntArray(10, 10);
		int currStepN = 0, elN = 0;
		if (lastDefined != null) {
			elN = contextSpec.indexOf(lastDefined) + 1;
		}
		if (elN < 0) {
			elN = 0;
		}
		boolean stepBack = false;
		while (elN < contextSpec.size()) {
			ContextElement cel = (ContextElement) contextSpec.elementAt(elN);
			boolean error = false;
			Panel mainP = null;
			if (currStepN < wizardSteps.size() && elN == elNumbers.elementAt(currStepN)) {
				//this context element was defined earlier
				cel = (ContextElement) contextSpec.elementAt(elN);
				ContextDefPanel pan = (ContextDefPanel) wizardSteps.elementAt(currStepN);
				mainP = pan;
				if (!stepBack) {
					SelectPanel sp = pan.getSelectPanel();
					if (sp != null) {
						sp.setup();
						if (!sp.isValid()) {
							mainP = null;
							error = true;
							errorMsg = sp.getErrorMessage();
							//remove all dialog panels starting from the panel which produced
							//the error
							for (int i = wizardSteps.size() - 1; i >= currStepN; i--) {
								wizardSteps.removeElementAt(i);
								elNumbers.removeElementAt(i);
							}
						}
					}
				}
			} else {
				//remove further steps that were defined for some old context elements
				//that are no more valid
				for (int i = wizardSteps.size() - 1; i >= currStepN; i--) {
					wizardSteps.removeElementAt(i);
					elNumbers.removeElementAt(i);
				}
				//check if this context element is already defined
				if (core.context.isContextElementDefined(cel.localId)) {
					++elN;
					continue;
				}
				//check if the system supports specification of this type of context element
				if (cel.type == null || !core.context.isContextTypeSupported(cel.type)) {
					//skip the unsupported or unknown element of a context
					++elN;
					continue;
				}
				error = !core.context.canBeDefined(cel, core.mapN);
				if (error) {
					errorMsg = core.context.getErrorMessage();
				} else {
					//construct a panel for specification of the context element
					mainP = makeContextDefinitionPanel(core, (TreeNode) tasks.elementAt(elN), cel);
					wizardSteps.addElement(mainP);
					elNumbers.addElement(elN);
				}
			}
			if (error) {
				if (!core.kb.tasks.isObligatory(cel, (TreeNode) tasks.elementAt(elN))) {
					++elN;
					continue;
				}
				mainP = new Panel();
				mainP.setLayout(new BorderLayout());
				mainP.add(new Label(cel.getName(), Label.CENTER), "North");
				TextCanvas errorText = new TextCanvas();
				errorText.setText(errorMsg);
				errorText.setPreferredSize(Metrics.mm() * 100, Metrics.mm() * 10);
				errorText.setForeground(Color.red.darker());
				errorText.drawFrame = true;
				errorText.toBeCentered = true;
				mainP.add(errorText, "Center");
			}
			/*
			OKDialog dialog=new OKDialog(
			  (core.getCurrentFrame()==null)?CManager.getDummyFrame():core.getCurrentFrame(),
			  cel.getName(),!error,currStepN>0);
			dialog.addContent(mainP);
			dialog.show();
			*/
			dialog = new OKFrame(null, cel.getName(), !error, currStepN > 0);
			dialog.addContent(mainP);
			dialog.start();
			while (!dialog.isFinished()) {
				try {
					sleep(500);
				} catch (InterruptedException ie) {
				}
			}

			if (dialog.wasBackPressed()) {
				--currStepN;
				elN = elNumbers.elementAt(currStepN);
			} else if (error) {
				finish();
				return;
			} else if (dialog.wasCancelled()) {
				core.context.addContextItem(new ContextItem(cel, core.mapN, null));
				cancelled = true;
				finish();
				return;
			} else {
				ContextItem cit = null;
				if (mainP instanceof ContextDefPanel) {
					ContextDefPanel cdp = (ContextDefPanel) mainP;
					cit = cdp.getContextItemDefinition();
				}
				if (cit == null) {
					cit = new ContextItem(cel, core.mapN, new Boolean(true));
				}
				core.context.addContextItem(cit);
				lastDefined = cel;
				++currStepN;
				++elN;
			}
			dialog = null;
		}
		result = true;
		finish();
	}

	/**
	* Constructs a panel for definition of the given context element
	*/
	public ContextDefPanel makeContextDefinitionPanel(GuideCore core, TreeNode task, ContextElement cel) {
		if (cel == null)
			return null;
		this.core = core;
		if (!core.context.canBeDefined(cel, core.mapN))
			return null;
		ContextDefPanel mainP = new ContextDefPanel();
		mainP.setLayout(new BorderLayout());
		//add the name of the task or scenario
		if (task != null) {
			TextCanvas txtc = new TextCanvas();
			if (task.getNodeType().equals("scenario")) {
				txtc.addTextLine("Scenario: " + task.getName());
			} else {
				txtc.addTextLine("Task: " + task.getName());
			}
			txtc.setPreferredSize(Metrics.mm() * 100, Metrics.mm() * 10);
			mainP.add(txtc, "North");
		}

		ColumnLayout colLayout = new ColumnLayout();
		Panel cp = new Panel(colLayout);
		cp.setBackground(GuideCore.bkgPanelColor);
		cp.add(new Line(false));
		//add the prompt
		TextCanvas txtc = new TextCanvas();
		txtc.setBackground(GuideCore.bkgPromptColor);
		txtc.setPreferredSize(Metrics.mm() * 100, Metrics.mm() * 10);
		txtc.addTextLine(core.context.fillNameSlots(cel.getPrompt()));
		txtc.toBeCentered = true;
		cp.add(txtc);
		if (cel.getInstruction() != null) { //add the instruction
			cp.add(new Line(false));
			Label instrL = new Label("Instruction", Label.CENTER);
			if (cel.getHelpTopicId() == null || !core.sysMan.canHelp(cel.getHelpTopicId())) {
				cp.add(instrL);
			} else {
				Panel pp = new Panel(new BorderLayout());
				pp.add(instrL, "Center");
				Button b = new Button("?");
				b.addActionListener(core);
				b.setActionCommand("help_" + cel.getHelpTopicId());
				pp.add(b, "East");
				cp.add(pp);
			}
			txtc = new TextCanvas();
			txtc.setBackground(GuideCore.bkgInstructionColor);
			txtc.setPreferredSize(Metrics.mm() * 100, Metrics.mm() * 10);
			txtc.addTextLine(core.context.fillNameSlots(cel.getInstruction()));
			cp.add(txtc);
		}
		if (cel.method.equals("select_one") || cel.method.equals("select_many")) {
			SelectPanel sp = new SelectPanel(cel, !core.kb.tasks.isObligatory(cel, task), task.getId(), core.mapN, core.sysMan, core.context);
			cp.add(sp);
			colLayout.setStretchLast(true);
		}
		mainP.add(cp, "Center");
		return mainP;
	}

	public ContextItem defineContextElement(GuideCore core, ContextElement cel, TreeNode task) {
		this.core = core;
		errorMsg = null;
		//check if the system supports specification of this type of context element
		if (cel.type == null || !core.context.isContextTypeSupported(cel.type))
			return null;
		boolean error = !core.context.canBeDefined(cel, core.mapN);
		Panel mainP = null;
		if (error) {
			if (core.kb.tasks.isObligatory(cel, task))
				return null;
			errorMsg = core.context.getErrorMessage();
			mainP = new Panel();
			mainP.setLayout(new BorderLayout());
			mainP.add(new Label(cel.getName(), Label.CENTER), "North");
			TextCanvas errorText = new TextCanvas();
			errorText.setText(errorMsg);
			errorText.setPreferredSize(Metrics.mm() * 100, Metrics.mm() * 10);
			errorText.setForeground(Color.red.darker());
			errorText.drawFrame = true;
			errorText.toBeCentered = true;
			mainP.add(errorText, "Center");
		} else {
			//construct a panel for specification of the context element
			mainP = makeContextDefinitionPanel(core, task, cel);
		}
		OKDialog dialog = new OKDialog((core.getCurrentFrame() == null) ? CManager.getAnyFrame() : core.getCurrentFrame(), cel.getName(), !error, false);
		dialog.addContent(mainP);
		dialog.show();
		if (error)
			return null;
		if (dialog.wasCancelled()) {
			errorMsg = "Specification of the context element was cancelled";
			return null;
		}
		ContextItem cit = null;
		if (mainP instanceof ContextDefPanel) {
			ContextDefPanel cdp = (ContextDefPanel) mainP;
			cit = cdp.getContextItemDefinition();
		}
		if (cit == null) {
			cit = new ContextItem(cel, core.mapN, new Boolean(true));
		}
		lastDefined = cel;
		return cit;
	}

}
