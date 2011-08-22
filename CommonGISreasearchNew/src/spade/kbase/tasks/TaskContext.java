package spade.kbase.tasks;

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.event.ActionListener;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.TextCanvas;

public class TaskContext {
	protected TaskTree tree = null;
	protected String taskID = null;
	protected ActionListener al = null;

	public TaskContext(ActionListener al, TaskTree tree, String taskID, Panel p) {
		this.al = al;
		this.tree = tree;
		this.taskID = taskID;
		p.removeAll();
		p.setLayout(new ColumnLayout());
		TreeNode task = tree.findTreeNode(taskID);
		p.add(new Label(task.getName(), Label.CENTER));
		TextCanvas tc = new TextCanvas(); // Task Comment
		ScrollPane sp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		sp.add(tc);
		sp.addComponentListener(tc);
		tc.setText(task.getExplanation());
		if (task.mapViewInstruction != null) {
			tc.addTextLine(task.mapViewInstruction);
		}
		p.add(sp);
		p.add(new Line(false));
		p.add(new Line(false));
		Panel pp = new Panel();
		pp.setLayout(new FlowLayout());
		p.add(pp);
		Button b = new Button("continue");
		b.addActionListener(al);
		pp.add(b);
		p.invalidate();
		p.setSize(p.getPreferredSize());
		if (p.getParent() != null) {
			p.getParent().validate();
		}
		p.validate();
	}

}
