package guide_tools.guide;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.kbase.scenarios.Instrument;
import spade.kbase.scenarios.TreeNode;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.FoldablePanel;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.PopupManager;
import spade.lib.basicwin.SplitLayout;
import spade.lib.basicwin.TextCanvas;
import spade.lib.util.IntArray;

public class TaskSupportPanel extends Panel implements ActionListener, ItemListener, PropertyChangeListener, Destroyable {
	/**
	* The GuideCore contains the knowledge base, the context currently defined,
	* references to the system manager, current scenario etc. as well as
	* some constants used in different components of the Guide
	*/
	protected GuideCore core = null;

	protected TreeNode root = null, currTask = null;
	/**
	* Subtasks of the current task
	*/
	protected Vector subtasks = null;
	/**
	* Checkboxes corresponding to the subtasks
	*/
	protected Vector subtaskCB = null;
	/**
	* InstrumentManagers responsible for individual instruments
	*/
	protected Vector instrMans = null;
	/**
	* The task the context of which is currently defined
	*/
	protected TreeNode taskToWait = null;
	/**
	* Interface elements
	*/
	protected Panel leftPanel = null, rightPanel = null, taskPanel = null, instrPanel = null;
	protected TaskTreeViewer treeViewer = null;
	protected ContextView cView = null;
	/**
	* Listeners of action events (cancel)
	*/
	protected Vector aList = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	public TaskSupportPanel(GuideCore core, TreeNode root) {
		this.core = core;
		this.root = root;
		if (core == null || core.kb == null || core.kb.tasks == null || root == null)
			return;
		SplitLayout spl = new SplitLayout(this, SplitLayout.VERT);
		setLayout(spl);
		leftPanel = new Panel(new BorderLayout());
		cView = new ContextView(core);
		cView.addPropertyChangeListener(this);
		ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		scp.add(cView);
		leftPanel.add(scp, "Center");
		spl.addComponent(leftPanel, 0.3f);
		rightPanel = new Panel(new BorderLayout());
		spl.addComponent(rightPanel, 0.7f);
		startTaskSelection();
	}

	protected boolean checkDefineTaskContext(TreeNode task) {
		taskToWait = null;
		if (task == null)
			return true;
		//all parents of the task should be checked
		Vector pt = new Vector(5, 5);
		TreeNode node = task;
		while (node != null && !core.isContextDefined(node)) {
			pt.insertElementAt(node, 0);
			node = core.getTask(node.getParentId());
		}
		if (pt.size() > 0) {
			taskToWait = task;
			setEnabled(false);
			cView.setEnabled(false);
			ContextDefWizard cdw = new ContextDefWizard();
			cdw.specifyContext(this, core, pt, false);
			return false;
		}
		return true;
	}

	protected void startTaskSelection() {
		if (checkDefineTaskContext(root)) {
			selectTaskToPerform();
		}
	}

	protected void selectTaskToPerform() {
		boolean wasShowing = isShowing();
		if (wasShowing) {
			setVisible(false);
		}
		rightPanel.removeAll();
		for (int i = leftPanel.getComponentCount() - 1; i > 0; i--) {
			leftPanel.remove(i);
		}
		eraseCurrentTaskData();
		if (treeViewer == null) {
			treeViewer = new TaskTreeViewer(core.kb.tasks, root, core);
			treeViewer.addActionListener(this);
		} else {
			treeViewer.reset();
		}
		rightPanel.add(new Label("Select a task to fulfil", Label.CENTER), "North");
		rightPanel.add(treeViewer, "Center");
		if (wasShowing) {
			invalidate();
			validate();
			setVisible(true);
		}
	}

	public void addActionListener(ActionListener actL) {
		if (actL == null)
			return;
		if (aList == null) {
			aList = new Vector(5, 5);
		}
		aList.addElement(actL);
	}

	public void removeActionListener(ActionListener actL) {
		if (actL != null && aList != null) {
			aList.removeElement(actL);
		}
	}

	protected void sendActionEventToListeners(String cmd) {
		if (aList == null || aList.size() < 1)
			return;
		ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, cmd);
		for (int i = 0; i < aList.size(); i++) {
			((ActionListener) aList.elementAt(i)).actionPerformed(ae);
		}
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		String cmd = evt.getActionCommand();
		if (cmd == null)
			return;
		if ((evt.getSource() instanceof ContextDefWizard) && cmd.equals("finished")) {
			setEnabled(true);
			cView.setEnabled(true);
			TreeNode task = taskToWait;
			if (task == null)
				return;
			taskToWait = null;
			ContextDefWizard cdw = (ContextDefWizard) evt.getSource();
			if (cdw.getResult()) {
				if (task == root) {
					selectTaskToPerform();
				} else if (task == currTask) {
					startSupportCurrentTask();
				} else {
					startSupportSubtask(task);
				}
			} else {
				if (task == root) {
					cancel();
				} else if (task == currTask) {
					eraseCurrentTaskData();
				} else {
					uncheckSubtask(task);
				}
			}
		} else {
			if (cView.isContextBeingEdited())
				return;
			if (cmd.equals("Cancel")) {
				cancel();
			} else if (evt.getSource() instanceof TaskTreeViewer) {
				if (cmd.equals("Start_Task")) {
					taskSelected(treeViewer.getSelectedTask());
				}
			} else if (evt.getSource() instanceof Button) {
				if (evt.getActionCommand().equals("Task_Done")) {
					taskDone();
				} else if (evt.getActionCommand().equals("Task_Select")) {
					startTaskSelection();
				}
			}
		}
	}

	protected void taskSelected(TreeNode task) {
		if (task == null)
			return;
		currTask = task;
		if (checkDefineTaskContext(currTask)) {
			startSupportCurrentTask();
		}
	}

	protected void startSupportCurrentTask() {
		//show the instruments related to the task
		setVisible(false);
		core.context.setTaskContextState(currTask.getId(), true);
		rightPanel.removeAll();
		Button b = new Button("Done");
		b.setActionCommand("Task_Done");
		b.addActionListener(this);
		Panel p = new Panel(new FlowLayout(FlowLayout.CENTER));
		p.add(b);
		Panel bp = new Panel(new BorderLayout());
		bp.add(p, "Center");
		b = new Button("Cancel");
		b.setActionCommand("Cancel");
		b.addActionListener(this);
		p = new Panel(new FlowLayout(FlowLayout.RIGHT));
		p.add(b);
		bp.add(p, "East");
		rightPanel.add(bp, "South");
		p = new Panel(new FlowLayout(FlowLayout.CENTER));
		b = new Button("Other task");
		b.setActionCommand("Task_Select");
		b.addActionListener(this);
		p.add(b);
		leftPanel.add(p, "South");
		leftPanel.add(makeTaskPanel(), "North");
		rightPanel.add(makeInstrumentPanel(), "Center");
		invalidate();
		validate();
		setVisible(true);
	}

	protected void taskDone() {
		//select another task
		startTaskSelection();
	}

	protected void cancel() {
		sendActionEventToListeners("Cancel");
	}

	protected Panel makeTaskPanel() {
		if (subtasks == null) {
			subtasks = new Vector(10, 5);
		} else {
			subtasks.removeAllElements();
		}
		if (subtaskCB == null) {
			subtaskCB = new Vector(10, 5);
		} else {
			subtaskCB.removeAllElements();
		}

		Panel p = new Panel(new BorderLayout());
		p.add(new Label("Task and Context", Label.CENTER), "North");
		fillTaskPanel();
		p.add(taskPanel, "Center");
		return p;
	}

	protected void fillTaskPanel() {
		if (taskPanel == null) {
			taskPanel = new Panel(new ColumnLayout());
			taskPanel.setBackground(GuideCore.bkgTaskNameColor);
		} else {
			taskPanel.removeAll();
		}
		//Task name
		TextCanvas tcan = new TextCanvas();
		tcan.setText("Task: " + currTask.getName());
		taskPanel.add(tcan);
		PopupManager popman = new PopupManager(tcan, currTask.getExplanation(), true);
		if (hasExecutableSubtasks(currTask)) {
			Label l = new Label("Subtasks:", Label.CENTER);
			taskPanel.add(l);
			addExecutableSubtasksOf(currTask.getId(), 0, taskPanel);
		}
		if (taskPanel.isShowing()) {
			CManager.validateAll(taskPanel);
		}
	}

	/**
	* Returns true if the task with the given identifier has subtasks that can
	* actually be fulfilled (i.e. the restrictions are satisfied and the
	* context can be defined)
	* ! restrictions are not checked at the moment !
	*/
	protected boolean hasExecutableSubtasks(TreeNode task) {
		Vector children = core.kb.tasks.getChildrenOf(task.getId());
		if (children == null)
			return false;
		for (int i = 0; i < children.size(); i++) {
			TreeNode node = (TreeNode) children.elementAt(i);
			if (core.isTaskPerformable(node))
				if (node.getNodeType().equals("task"))
					return true;
				else if (hasExecutableSubtasks(node))
					return true;
		}
		return false;
	}

	protected void addExecutableSubtasksOf(String parentId, int level, Panel panel) {
		if (parentId == null)
			return;
		TreeNode parent = core.kb.tasks.findTreeNode(parentId);
		if (parent == null)
			return;
		Vector children = core.kb.tasks.getChildrenOf(parentId);
		if (children == null)
			return;
		CheckboxGroup cbg = null;
		if (parent.getNodeType().equals("task_group")) {
			cbg = new CheckboxGroup();
			//Checkbox dummyCB=new Checkbox("dummy",cbg,false);
		}
		for (int i = 0; i < children.size(); i++) {
			TreeNode node = (TreeNode) children.elementAt(i);
			if (!core.isTaskPerformable(node)) {
				continue;
			}
			Checkbox cb = new Checkbox(node.getShortName(), cbg, false);
			cb.addItemListener(this);
			cb.setBackground(GuideCore.bkgSubtasksColor);
			PopupManager popman = new PopupManager(cb, node.getExplanation(), true);
			if (level == 0) {
				panel.add(cb);
			} else {
				cb.setEnabled(false);
				Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 5, 0));
				for (int j = 0; j < level; j++) {
					p.add(new Label(""));
				}
				Panel pp = new Panel(new BorderLayout());
				pp.setBackground(GuideCore.bkgSubtasksColor);
				pp.add(p, "West");
				pp.add(cb, "Center");
				panel.add(pp);
			}
			subtasks.addElement(node);
			subtaskCB.addElement(cb);
			if (hasExecutableSubtasks(node)) {
				addExecutableSubtasksOf(node.getId(), level + 1, panel);
			}
		}
	}

	protected Vector createInstrumentManagers(TreeNode task) {
		if (task.instruments == null)
			return null;
		Vector mans = new Vector(10, 5);
		//first add default instruments
		for (int i = 0; i < task.instruments.size(); i++) {
			Instrument instr = (Instrument) task.instruments.elementAt(i);
			if (!instr.isDefault) {
				continue;
			}
			InstrumentManager iman = null;
			if (instr.type.equals("map_vis")) {
				iman = new MapVisManager(instr, task, core);
			} else if (instr.type.equals("tool")) {
				if (SupplToolManager.canBeUsed(instr, core)) {
					iman = new SupplToolManager(instr, task, core);
				}
			} else {
				iman = new InstrumentManager(instr, task, core);
			}
			if (iman != null) {
				mans.addElement(iman);
			}
		}
		//add optional instruments
		for (int i = 0; i < task.instruments.size(); i++) {
			Instrument instr = (Instrument) task.instruments.elementAt(i);
			if (instr.isDefault) {
				continue;
			}
			InstrumentManager iman = null;
			if (instr.type.equals("map_vis")) {
				iman = new MapVisManager(instr, task, core);
			} else if (instr.type.equals("tool")) {
				if (SupplToolManager.canBeUsed(instr, core)) {
					iman = new SupplToolManager(instr, task, core);
				}
			} else {
				iman = new InstrumentManager(instr, task, core);
			}
			if (iman != null) {
				mans.addElement(iman);
			}
		}
		if (mans.size() < 1)
			return null;
		return mans;
	}

	protected Panel makeInstrumentPanel() {
		Panel p = new Panel(new BorderLayout());
		p.add(new Label("Instruments", Label.CENTER), "North");
		instrMans = null;
		fillInstrumentPanel();
		ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		scp.add(instrPanel);
		p.add(scp, "Center");
		return p;
	}

	protected void fillInstrumentPanel() {
		if (instrPanel == null) {
			instrPanel = new Panel(new ColumnLayout());
			instrPanel.setBackground(GuideCore.bkgInstrumentsColor);
		} else {
			instrPanel.removeAll();
		}
		if (instrMans == null) {
			instrMans = createInstrumentManagers(currTask);
		}
		if (instrMans == null)
			return;
		for (int i = 0; i < instrMans.size(); i++) {
			InstrumentManager iman = (InstrumentManager) instrMans.elementAt(i);
			if (iman.getInstrument().isDefault) { //add label with name
				//check if instruments with the same name were added earlier
				String name = core.context.fillNameSlots(iman.getInstrument().getName());
				boolean sameName = false;
				for (int j = i - 1; j >= 0 && !sameName; j--) {
					InstrumentManager iman1 = (InstrumentManager) instrMans.elementAt(j);
					sameName = name.equalsIgnoreCase(iman1.getInstrument().getName());
				}
				if (sameName) {
					continue;
				}
				if (i > 0) {
					instrPanel.add(new Line(false));
				}
				//collect numbers of the instruments with the same name
				IntArray num = new IntArray(5, 5);
				for (int j = i; j < instrMans.size(); j++) {
					iman = (InstrumentManager) instrMans.elementAt(j);
					if (iman.getInstrument().isDefault && name.equalsIgnoreCase(iman.getInstrument().getName())) {
						num.addElement(j);
					}
				}
				Label namel = new Label(name);
				iman = (InstrumentManager) instrMans.elementAt(num.elementAt(0));
				Component c = null;
				if (num.size() == 1) {
					c = iman.constructControls();
				} else {
					Panel p = new Panel(new ColumnLayout());
					if (num.size() == 2) {
						p.add(new FoldablePanel(iman.constructControls()));
					} else {
						Panel pp = new Panel(new ColumnLayout());
						for (int j = 0; j < num.size() - 1; j++) {
							iman = (InstrumentManager) instrMans.elementAt(num.elementAt(j));
							pp.add(iman.constructControls());
						}
						p.add(new FoldablePanel(pp));
					}
					iman = (InstrumentManager) instrMans.elementAt(num.elementAt(num.size() - 1));
					p.add(iman.constructControls());
					c = p;
				}
				FoldablePanel fp = new FoldablePanel(c, namel);
				fp.open();
				instrPanel.add(fp);
			} else {
				if (i > 0) {
					instrPanel.add(new Line(false));
				}
				instrPanel.add(iman.constructControls());
			}
		}
		if (instrPanel.isShowing()) {
			CManager.validateAll(instrPanel);
		}
	}

	@Override
	public void itemStateChanged(ItemEvent evt) {
		if (cView.isContextBeingEdited())
			return;
		if (evt.getSource() instanceof Checkbox) {
			Checkbox cb = (Checkbox) evt.getSource();
			//which subtask does this checkbox correspond to?
			int subtaskN = -1;
			for (int i = 0; i < subtaskCB.size() && subtaskN < 0; i++)
				if (cb == subtaskCB.elementAt(i)) {
					subtaskN = i;
				}
			if (subtaskN < 0)
				return;
			boolean enabled = cb.getState();
			TreeNode task = (TreeNode) subtasks.elementAt(subtaskN);
			//if this is a task group, enable or disable group members
			if (task.getNodeType().equals("task_group")) {
				String taskId = task.getId();
				for (int i = 0; i < subtasks.size(); i++) {
					TreeNode sub = (TreeNode) subtasks.elementAt(i);
					if (taskId.equals(sub.getParentId())) {
						Checkbox tcb = (Checkbox) subtaskCB.elementAt(i);
						tcb.setEnabled(enabled);
						if (!enabled)
							if (tcb.getCheckboxGroup() != null) {
								tcb.getCheckboxGroup().setSelectedCheckbox(null);
							} else {
								tcb.setState(false);
							}
					}
				}
			} else if (enabled) {
				//deactivate contexts of the other subtasks in the same group
				String parentId = task.getParentId();
				if (parentId != null) {
					for (int i = 0; i < subtasks.size(); i++)
						if (i != subtaskN) {
							TreeNode sub = (TreeNode) subtasks.elementAt(i);
							if (parentId.equals(sub.getParentId())) {
								core.context.setTaskContextState(sub.getId(), false);
							}
						}
				}
			}
			removeInstrumentsOfInactiveTasks();
			modifyInstrumentPanel();
			core.context.setTaskContextState(task.getId(), enabled);
			if (enabled) {
				subtaskSelected(task);
			}
		}
	}

	protected void uncheckSubtask(TreeNode task) {
		if (subtasks == null || subtaskCB == null)
			return;
		int subtaskN = subtasks.indexOf(task);
		if (subtaskN < 0 || subtaskN >= subtaskCB.size())
			return;
		Checkbox cb = (Checkbox) subtaskCB.elementAt(subtaskN);
		if (cb != null)
			if (cb.getCheckboxGroup() != null) {
				cb.getCheckboxGroup().setSelectedCheckbox(null);
			} else {
				cb.setState(false);
			}
		core.context.setTaskContextState(task.getId(), false);
	}

	protected void subtaskSelected(TreeNode task) {
		if (checkDefineTaskContext(task)) {
			startSupportSubtask(task);
		}
	}

	protected void startSupportSubtask(TreeNode task) {
		core.context.setTaskContextState(task.getId(), true);
		//add instruments belonging to this subtask
		Vector tman = createInstrumentManagers(task);
		if (tman == null)
			return;
		//find where in the whole list the first optional instrument comes
		//in the same loop "wrap", or "fold" the panels of the current instruments
		int pos = -1;
		for (int i = 0; i < instrMans.size(); i++)
			if (!((InstrumentManager) instrMans.elementAt(i)).getInstrument().isDefault) {
				if (pos < 0) {
					pos = i;
				}
				((InstrumentManager) instrMans.elementAt(i)).foldInstrumentControls();
			}
		//add default insruments of the subtask at "pos", the rest - at the end
		for (int i = 0; i < tman.size(); i++) {
			InstrumentManager iman = (InstrumentManager) tman.elementAt(i);
			if (pos >= 0 && iman.getInstrument().isDefault) {
				instrMans.insertElementAt(iman, pos++);
			} else {
				instrMans.addElement(iman);
			}
		}
		modifyInstrumentPanel();
	}

	protected void removeInstrumentsOfInactiveTasks() {
		//remove instruments belonging to deselected or disabled subtasks
		for (int j = instrMans.size() - 1; j >= 0; j--) {
			InstrumentManager iman = (InstrumentManager) instrMans.elementAt(j);
			TreeNode tsk = iman.getTask();
			int tskN = subtasks.indexOf(tsk);
			if (tskN >= 0) {
				Checkbox tcb = (Checkbox) subtaskCB.elementAt(tskN);
				if (!tcb.getState() || !tcb.isEnabled()) {
					iman.closeInstrument();
					instrMans.removeElementAt(j);
				}
			}
		}
	}

	protected void modifyInstrumentPanel() {
		instrPanel.removeAll();
		fillInstrumentPanel();
		CManager.validateAll(instrPanel);
	}

	protected void eraseCurrentTaskData() {
		if (currTask != null) {
			core.context.setTaskContextState(currTask.getId(), false);
			currTask = null;
		}
		eraseSubtaskData();
	}

	protected void eraseSubtaskData() {
		if (subtasks != null) {
			for (int i = 0; i < subtasks.size(); i++) {
				TreeNode node = (TreeNode) subtasks.elementAt(i);
				core.context.setTaskContextState(node.getId(), false);
			}
			if (instrMans != null) {
				for (int j = 0; j < instrMans.size(); j++) {
					InstrumentManager iman = (InstrumentManager) instrMans.elementAt(j);
					TreeNode tsk = iman.getTask();
					if (subtasks.contains(tsk)) {
						iman.closeInstrument();
					}
				}
			}
			subtasks.removeAllElements();
		}
		if (subtaskCB != null) {
			subtaskCB.removeAllElements();
		}
	}

	/**
	* Reacts to changes of the current context
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getPropertyName().equals("context")) {
			if (treeViewer != null && treeViewer.isShowing()) {
				treeViewer.reset();
			} else
			//is the context of the current task still defined?
			if (currTask != null)
				if (!core.isContextDefined(currTask)) {
					startTaskSelection();
				} else {
					eraseSubtaskData();
					instrMans = null;
					fillTaskPanel();
					fillInstrumentPanel();
				}
		}
	}

	@Override
	public void destroy() {
		eraseCurrentTaskData();
		core.context.removePropertyChangeListener(this);
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

}
