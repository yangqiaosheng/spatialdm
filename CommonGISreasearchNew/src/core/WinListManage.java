package core;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;

public class WinListManage extends Panel implements ActionListener {
	static ResourceBundle res = Language.getTextResource("core.Res");

	protected Vector frames = null;
	protected List lst = null;
	protected TextField tf = null;
	protected OKDialog okd = null;

	public WinListManage(Vector windows) {
		if (windows == null || windows.size() < 1)
			return;
		frames = new Vector(windows.size(), 1);
		lst = new List(10);
		for (int i = 0; i < windows.size(); i++)
			if (windows.elementAt(i) instanceof Frame) {
				Frame fr = (Frame) windows.elementAt(i);
				frames.addElement(fr);
				lst.add(fr.getTitle());
			}
		if (frames.size() < 1) {
			frames = null;
			lst = null;
			return;
		}
		lst.addActionListener(this);
		setLayout(new BorderLayout());
		add(new Label(res.getString("Available_windows") + ":", Label.CENTER), BorderLayout.NORTH);
		add(lst, BorderLayout.CENTER);
		Panel p = new Panel(new FlowLayout(FlowLayout.CENTER, 30, 3));
		Button b = new Button(res.getString("To_front"));
		p.add(b);
		b.setActionCommand("To_front");
		b.addActionListener(this);
		b = new Button(res.getString("Rename"));
		p.add(b);
		b.setActionCommand("Rename");
		b.addActionListener(this);
		add(p, BorderLayout.SOUTH);
	}

	public boolean isOK() {
		return frames != null && frames.size() > 0;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(lst) || e.getActionCommand().equals("To_front")) {
			int idx = lst.getSelectedIndex();
			if (idx < 0)
				return;
			Frame fr = (Frame) frames.elementAt(idx);
			// if (fr.getState()==Frame.ICONIFIED) fr.setState(Frame.NORMAL);
			// for Java1.1 ~MO
			java2d.Drawing2D.setNormalFrameState(fr);

			Window win = CManager.getWindow(this);
			if (win != null) {
				win.dispose();
			}
			fr.toFront();
		} else if (e.getActionCommand().equals("Rename")) {
			int idx = lst.getSelectedIndex();
			if (idx < 0)
				return;
			Frame fr = (Frame) frames.elementAt(idx);
			Panel p = new Panel(new GridLayout(2, 1));
			p.add(new Label(res.getString("New_name") + ":"));
			tf = new TextField(fr.getTitle(), 60);
			p.add(tf);
			tf.addActionListener(this);
			okd = new OKDialog(CManager.getAnyFrame(this), res.getString("Rename_window"), true);
			okd.addContent(p);
			okd.show();
			if (okd == null || okd.wasCancelled()) {
				okd = null;
				tf = null;
				return;
			}
			String name = tf.getText();
			if (name != null) {
				name = name.trim();
				if (name.length() > 0) {
					fr.setName(name);
					fr.setTitle(name);
					lst.replaceItem(name, idx);
					lst.select(idx);
				}
			}
			okd = null;
			tf = null;
		} else if (tf != null && e.getSource().equals(tf)) {
			okd.dispose();
			String name = tf.getText();
			if (name != null) {
				name = name.trim();
				if (name.length() > 0) {
					int idx = lst.getSelectedIndex();
					if (idx >= 0) {
						Frame fr = (Frame) frames.elementAt(idx);
						fr.setName(name);
						fr.setTitle(name);
						lst.replaceItem(name, idx);
						lst.select(idx);
					}
				}
			}
			okd = null;
			tf = null;
		}
	}
}
