package guide_tools.guide;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import spade.kbase.scenarios.TaskTree;
import spade.kbase.scenarios.TreeNode;
import spade.lib.basicwin.SplitLayout;
import spade.lib.basicwin.TextCanvas;
import spade.lib.basicwin.TreeView;

/**
* A UI component for viewing the task hierarchy for a specified scenario
* and selection of a task to fulfil.
* Only tasks that are possible in the currently defined context are shown.
*/

public class TaskTreeViewer extends Panel implements ItemListener, ActionListener {
	protected static Color bkgInstructionColor = new Color(192, 255, 192);

	protected TaskTree sourceTasks = null, tasks = null;
	protected TreeNode root = null, currTask = null;
	protected GuideCore core = null;
	/**
	* Interface elements
	*/
	protected TreeView taskList = null; //may be also HierList
	protected TextCanvas taskDescrField = null;
	protected Panel mainP = null;
	protected Button startBt = null;
	/**
	* Listeners of action events (start performing a task or cancel)
	*/
	protected Vector aList = null;

	public TaskTreeViewer(TaskTree taskTree, TreeNode root, GuideCore core) {
		if (taskTree == null || root == null)
			return;
		this.sourceTasks = taskTree;
		this.root = root;
		this.core = core;

		taskList = new TreeView(); //may be also HierList
		taskList.addItemListener(this);
		fillTaskList();

		taskDescrField = new TextCanvas();
		taskDescrField.setText(root.getExplanation());
		taskDescrField.setBackground(bkgInstructionColor);

		ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		scp.add(taskDescrField);

		mainP = new Panel();
		SplitLayout spl = new SplitLayout(mainP, SplitLayout.HOR);
		mainP.setLayout(spl);
		spl.addComponent(taskList, 0.7f);
		spl.addComponent(scp, 0.3f);

		setLayout(new BorderLayout());
		add(mainP, "Center");

		Panel p = new Panel(new FlowLayout(FlowLayout.CENTER));
		startBt = new Button("Start");
		startBt.setActionCommand("Start");
		startBt.addActionListener(this);
		startBt.setEnabled(false);
		p.add(startBt);
		Panel bp = new Panel(new BorderLayout());
		bp.add(p, "Center");
		Button b = new Button("Cancel");
		b.setActionCommand("Cancel");
		b.addActionListener(this);
		p = new Panel(new FlowLayout(FlowLayout.RIGHT));
		p.add(b);
		bp.add(p, "East");
		add(bp, "South");
	}

	protected void fillTaskList() {
		tasks = new TaskTree();
		//select only first-order tasks and task groups (no subtasks!)
		/**/
		for (int i = 0; i < sourceTasks.getTaskCount(); i++) {
			TreeNode task = sourceTasks.getTask(i);
			String parentId = task.getParentId();
			if (parentId != null) {
				TreeNode parent = tasks.findTreeNode(parentId);
				if (parent == null || parent.getNodeType().equals("task")) {
					continue; //skip subtasks!
				}
			}
			//check if the context of this task can be defined
			if (core.isContextDefined(task)) {
				tasks.addTask(task);
			}
		}
		taskList.clear();
		taskList.addItem(root.getId(), "Scenario: " + root.getShortName(), null);
		for (int i = 0; i < tasks.getTaskCount(); i++) {
			TreeNode task = tasks.getTask(i);
			if (task.getParentId() != null && taskList.getItemIndex(task.getParentId()) >= 0) {
				taskList.addItem(task.getId(), task.getShortName(), task.getParentId());
			}
		}
		taskList.setup();
		if (currTask != null) {
			int idx = tasks.findTreeNodeIndex(currTask.getId());
			if (idx < 0) {
				currTask = null;
			} else {
				taskList.setSelectedIndex(idx);
			}
		}
	}

	public void reset() {
		boolean wasShowing = isShowing();
		if (wasShowing) {
			setVisible(false);
		}
		fillTaskList();
		taskDescrField.setText((currTask == null) ? root.getExplanation() : currTask.getExplanation());
		startBt.setEnabled(false);
		if (wasShowing) {
			taskList.invalidate();
			validate();
			setVisible(true);
		}
	}

	public TreeView getTaskTreeView() {
		return taskList;
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

	public TreeNode getSelectedTask() {
		int idx = taskList.getSelectedIndex();
		if (idx < 0)
			return null;
		currTask = tasks.getTask(idx);
		return currTask;
	}

	@Override
	public void itemStateChanged(ItemEvent evt) {
		if (evt.getSource() == taskList && evt.getStateChange() == ItemEvent.SELECTED) {
			currTask = tasks.findTreeNode((String) evt.getItem());
			if (currTask != null) {
				taskDescrField.setText(currTask.getExplanation());
				startBt.setEnabled(currTask.getNodeType().equals("task"));
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		String cmd = evt.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("Cancel")) {
			sendActionEventToListeners("Cancel");
		} else if (cmd.equals("Start")) {
			sendActionEventToListeners("Start_Task");
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
}