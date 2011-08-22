package spade.lib.basicwin;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import spade.lib.lang.Language;
import spade.lib.util.CopyFile;

public class GetPathDlg extends Dialog implements ActionListener {
	//static ResourceBundle res = ResourceBundle.getBundle("spade.lib.basicwin.Res", Language.lang);
	static ResourceBundle res = Language.getTextResource("spade.lib.basicwin.Res");
	protected TextField pathField = null;
	protected String path = null, fileMask = null, dir = null;
	protected Frame ownerFrame = null;
	/**
	* Load or save file?
	*/
	protected boolean load = true;

	public GetPathDlg(Frame owner, String title, boolean toLoad) {
		super(owner, title, true);
		load = toLoad;
		ownerFrame = owner;
		initialize();
	}

	public GetPathDlg(Frame owner, String title) {
		this(owner, title, true);
	}

	public void setFileMask(String mask) {
		fileMask = mask;
	}

	public void setDirectory(String directory) {
		dir = directory;
	}

	public String getDirectory() {
		return dir;
	}

	protected void initialize() {
		Panel mp = new Panel(new ColumnLayout());
		// following text: "Enter path or URL:"
		mp.add(new Label(res.getString("Enter_path_or_URL_")));
		pathField = new TextField(40);
		pathField.addActionListener(this);
		// following text: "Browse"
		Button b = new Button(res.getString("Browse"));
		b.setActionCommand("browse");
		b.addActionListener(this);
		Panel p = new Panel(new BorderLayout());
		p.add(pathField, "Center");
		p.add(b, "East");
		mp.add(p);
		mp.add(new Label(" "));
		p = new Panel(new FlowLayout(FlowLayout.CENTER, 10, 3));
		b = new Button(res.getString("OK"));
		b.setActionCommand("OK");
		b.addActionListener(this);
		p.add(b);
		// following text: "Cancel"
		b = new Button(res.getString("Cancel"));
		b.setActionCommand("cancel");
		b.addActionListener(this);
		p.add(b);
		mp.add(p);
		add(mp, "Center");
		pack();
		Dimension d = getSize(), ss = getToolkit().getScreenSize();
		setLocation((ss.width - d.width) / 2, (ss.height - d.height) / 2);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == pathField) {
			if (checkPathInField()) {
				dispose();
			}
			return;
		}
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("cancel")) {
			path = null;
			dispose();
		} else if (cmd.equals("OK")) {
			if (checkPathInField()) {
				dispose();
			}
		} else if (cmd.equals("browse")) {
			try {
				FileDialog fd = new FileDialog(ownerFrame, getTitle(), (load) ? FileDialog.LOAD : FileDialog.SAVE);
				if (fileMask != null) {
					fd.setFile(fileMask);
				}
				if (dir != null) {
					fd.setDirectory(dir);
				}
				fd.show();
				String fname = fd.getFile();
				dir = fd.getDirectory();
				fd.dispose();
				if (fname != null) {
					path = CopyFile.makeCanonic(dir + fname);
					dispose();
				}
			} catch (Exception ex) {
			}
		}
	}

	protected boolean checkPathInField() {
		path = pathField.getText();
		if (path == null)
			return false;
		path = path.trim();
		if (path.length() < 1)
			return false;
		return true;
	}

	public String getPath() {
		return path;
	}
}
