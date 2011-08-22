package db_work.ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.PopupManager;
import spade.lib.basicwin.TabbedPanel;
import db_work.database.JDBCConnector;
import db_work.database.OracleConnector;
import db_work.database.PostgresConnector;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 24-Jan-2006
 * Time: 16:50:23
 * To change this template use File | Settings | File Templates.
 */
public class MainFrame extends Frame implements WindowListener {

	JDBCConnector reader = null;

	TabbedPanel tp = null;

	public MainFrame() {
		super("Intelligent Aggregator");
		addWindowListener(this);
		setVisible(true);
		setLayout(new BorderLayout());

		List list = new List();
		list.add("Oracle");
		list.select(0);
		list.add("Postgres");
		list.add("JDBC");
		OKDialog ok = new OKDialog(this, "Database format?", false);
		ok.addContent(list);
		ok.show();

		switch (list.getSelectedIndex()) {
		case 0:
			reader = new OracleConnector();
			break;
		case 1:
			reader = new PostgresConnector();
			break;
		default:
			reader = new JDBCConnector();
		}
		reader.setFrame(this);

		if (!reader.openConnection(true)) {
			System.exit(0);
		}
		if (!reader.loadTableDescriptor(0, null)) {
			System.exit(0);
		}

		Panel p = new Panel();
		p.setLayout(new ColumnLayout());
		Label l = new Label(reader.getURL() + "/" + reader.getUserName());
		new PopupManager(l, "Database = " + reader.getURL() + "\n" + "User name = " + reader.getUserName(), true);
		p.add(l);
		p.add(new Line(false));
		add(p, BorderLayout.NORTH);
		tp = new TabbedPanel();
		add(tp, BorderLayout.CENTER);
		tp.addComponent(reader.getTableName(0), new TableInfoPanel(reader, reader.getTableDescriptor(0)));
		tp.makeLayout(false);
		pack();
	}

	@Override
	public void windowClosing(WindowEvent e) {
		if (e.getSource().equals(this)) {
			dispose();
			//any other cleenup...
			if (reader != null) {
				reader.closeConnection();
			}
			System.out.println("Good bye!");
			System.exit(0);
		}
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}
}
