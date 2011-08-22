package spade.kbase.tasks;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import spade.lib.basicwin.ItemPainter;
import spade.lib.basicwin.OwnList;
import spade.lib.basicwin.TextCanvas;

public class TaskSelection implements ItemPainter, ActionListener, ItemListener {
	public static String RunTask = "Run task";

	protected Panel TSPanel = null, ParentPanel = null;
	protected String CurrID = "";
	protected ActionListener al = null;
	protected OwnList tl = null;
	protected TextCanvas tc = null;
	protected TextArea ta = null;
	protected Vector hierID = null, altID = null; // IDs of items in the lists, null for the root
	protected String defaultName;
	protected Button bRun = null;

	public void setCurrID(String CurrID) {
		this.CurrID = CurrID;
	}

	public String getCurrID() {
		return CurrID;
	}

	protected TaskTree tree = null;

	public TaskSelection(TaskTree tree, ActionListener al, Panel ParentPanel, Panel TSPanel) {
		this.tree = tree;
		this.al = al;
		this.ParentPanel = ParentPanel;
		this.TSPanel = TSPanel;
		defaultName = tree.getTaskCollectionName();
		TSPanel.setLayout(new BorderLayout());
		Panel p = new Panel();
		p.setLayout(new GridLayout(1, 2));
		TSPanel.add("Center", p);
		tl = new OwnList(this);
		p.add(tl);
		tl.addActionListener(this);
		tl.addItemListener(this);
		tc = new TextCanvas(); // Task Comment
		ScrollPane sp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		sp.add(tc);
		sp.addComponentListener(tc);
		p.add(sp);

		p = new Panel();
		p.setLayout(new BorderLayout());
		TSPanel.add("South", p);

		ta = new TextArea("");
		ta.setEditable(false);
		p.add(ta, "Center");

		Panel pp = new Panel();
		pp.setLayout(new FlowLayout());
		p.add(pp, "South");
		bRun = new Button("Run selected task");
		bRun.addActionListener(al);
		bRun.setActionCommand(TaskSelection.RunTask);
		bRun.setEnabled(false);
		pp.add(bRun);

		hierID = new Vector(1);
		hierID.addElement(null);
		tl.addItem(null);
		tl.setSelected(0);
		fillAlt(null);

		ParentPanel.doLayout();
		ParentPanel.invalidate();
		ParentPanel.validate();
		;
		ParentPanel.repaint();
	}

	public void fillAlt(String ParentID) {
		tl.setNItems(hierID.size());
		altID = new Vector(10, 10);
		for (int i = 0; i < tree.getTaskCount(); i++) {
			TreeNode tn = tree.getTask(i);
			if ((ParentID == null && tn.getParentId() == null) || (ParentID != null && ParentID.equals(tn.getParentId()))) {
				altID.addElement(new String(tn.getId()));
				tl.addItem(null);
			}
			if (ParentID != null) { // hierTA ...
			}
		}
	}

	@Override
	public int itemH() {
		return 20;
	}

	@Override
	public int maxItemW() {
		return 100;
	}

	@Override
	public void drawItem(Graphics g, int n, int x, int y, int w, boolean isActive) {
		FontMetrics fm = g.getFontMetrics();
		int fh = fm.getHeight(), asc = fm.getAscent();
		g.setColor((isActive) ? Color.gray : Color.lightGray);
		int h = itemH();
		g.fillRect(x, y, w, h);
		g.setColor(Color.black);
		String ID = null;
		if (n + 1 <= hierID.size()) {
			ID = (String) hierID.elementAt(n);
		} else {
			ID = (String) altID.elementAt(n - hierID.size());
		}
		String name = null;
		if (ID == null) {
			name = defaultName;
		} else {
			TreeNode tn = tree.findTreeNode(ID);
			if (tn != null) {
				name = tn.getName();
			}
		}
		int dx = 0;
		if (n == 0 || tree.treeNodeHasChildren(ID)) {
			dx += 20;
			int x1 = (n <= hierID.size() - 1) ? 3 + x + 10 * n : 3 + x + 10 * hierID.size(), yt = y + 3, yb = y + 16;
			g.setColor(Color.darkGray);
			g.drawRect(x1, yt + 2, 13, 10);
			g.drawLine(x1 + 1, yt + 1, x1 + 2, yt);
			g.drawLine(x1 + 2, yt, x1 + 6, yt);
			g.setColor(Color.black);
			g.drawLine(x1 + 1, yb, x1 + 14, yb);
			g.drawLine(x1 + 14, yt + 3, x1 + 14, yb);
			g.drawLine(x1 + 7, yt, x1 + 8, yt + 1);
		}
		g.drawString(name, (n <= hierID.size() - 1) ? x + dx + 10 * n : x + dx + 10 * hierID.size(), y + asc);
	}

	@Override
	public void drawEmptyList(Graphics g, int x, int y, int w, int h) {
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() == bRun)
			//System.out.println("* button pressed");
			return;
		int itemN = tl.getSelectedIndex();
		if (itemN >= hierID.size()) { // Alt
			int n = itemN - hierID.size(), m = tree.findTreeNodeIndex((String) altID.elementAt(n));
			tl.setNItems(hierID.size()); // clean list of alternatives
			TreeNode tn = tree.getTask(m);
			hierID.addElement(tn.getId());
			tc.setText(tn.getExplanation());
			fillAlt(tn.getId());
			tl.setSelected(hierID.size() - 1);
		} else { // hierl
			int n = itemN, m = tree.findTreeNodeIndex((String) hierID.elementAt(n));
			int k = hierID.size();
			if (n < k - 1) {
				for (int i = k - 1; i > n; i--) {
					hierID.removeElementAt(i);
				}
				tl.setNItems(n);
				String id = null;
				if (m > -1) {
					TreeNode tn = tree.getTask(m);
					id = tn.getId();
				}
				fillAlt(id);
				tl.setSelected(itemN);
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		int itemN = tl.getSelectedIndex(), n = -1, m = -1;
		String ID = null;
		if (itemN >= hierID.size()) { // altL
			n = itemN - hierID.size();
			m = tree.findTreeNodeIndex(ID = (String) altID.elementAt(n));
		}
		if (itemN <= hierID.size() - 1) { // hierL
			n = itemN;
			m = tree.findTreeNodeIndex(ID = (String) hierID.elementAt(n));
		}
		setCurrID(ID);
		if (m == -1) {
			tc.clear();
			ta.setText("");
			bRun.setEnabled(false);
		} else {
			bRun.setEnabled(!tree.treeNodeHasChildren(ID));
			TreeNode tn = tree.getTask(m);
			tc.setText(tn.getExplanation());
			String str = "";
			if (tn.context != null && tn.context.size() > 0) {
				for (int i = 0; i < tn.context.size(); i++) {
					if (i > 0) {
						str += "--------------\n";
					}
					ContextElement ce = (ContextElement) tn.context.elementAt(i);
					str += "Name=" + ce.getName() + ", type=" + ce.getTypeName() + "\n";
					if (ce.getExplanation() != null) {
						str += "Explanation:\n" + ce.getExplanation() + "\n";
					}
					if (ce.getInstruction() != null) {
						str += "Instruction:\n" + ce.getInstruction() + "\n";
					}
					if (ce.getRestriction() != null) {
						str += ce.getRestriction().toString() + "\n";
					}
				}
			}
			ta.setText(str);
		}
	}

}
