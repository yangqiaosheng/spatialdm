package core;

import java.applet.AppletContext;
import java.awt.Component;
import java.awt.List;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.analysis.system.URLOpener;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.help.Helper;
import spade.lib.lang.Language;
import spade.vis.action.ObjectEvent;
import spade.vis.database.AttributeDataPortion;
import spade.vis.event.DEvent;
import spade.vis.event.EventReceiver;

/**
* A URLOpener is linked to a table havind an attribute with the name "URL".
* It listens to double-click events related to objects of this table and
* opens the URLs corresponding to these objects, if they are specified
* in the table. A URLOpener should be created only in an applet.
*/
public class URLOpenerImpl implements URLOpener, EventReceiver, ActionListener {
	static ResourceBundle res = Language.getTextResource("core.Res");
	/**
	* The table from which to take the URLs
	*/
	protected AttributeDataPortion dTable = null;
	/**
	* The Supervisor from which the URLOpener can receive object events
	*/
	protected Supervisor supervisor = null;
	/**
	* The index of the attribute containing URLs. The name of this attribute
	* in the table should be URL.
	*/
	protected int urlIdx = -1;
	/**
	* The AppletContext is used to open URLs
	*/
	protected AppletContext applContext = null;

	/**
	* Checks if everything that is needed for opening URLs is available:
	* 1) supervisor;
	* 2) applet context;
	* 3) table;
	* 4) index of the attribute containing URLs
	*/
	protected boolean canOpenURLs() {
		return supervisor != null && dTable != null && urlIdx >= 0 && (applContext != null || Helper.getPathToBrowser() != null);
	}

	/**
	* Sets the table from which to take the URLs
	*/
	@Override
	public void setTable(AttributeDataPortion table) {
		dTable = table;
		urlIdx = dTable.findAttrByName("URL");
		if (canOpenURLs()) {
			supervisor.registerObjectEventReceiver(this);
		}
	}

	/**
	* Sets the Supervisor from which the URLOpener can receive object events
	*/
	@Override
	public void setSupervisor(Supervisor sup) {
		supervisor = sup;
		if (supervisor != null) {
			applContext = (AppletContext) supervisor.getSystemSettings().getParameter("AppletContext");
		}
		if (canOpenURLs()) {
			supervisor.registerObjectEventReceiver(this);
		}
	}

//------------------ EventReceiver interface ------------------------------------
	/**
	* The EventReceiver answers whether it is interested in getting the specified
	* kind of events.
	*/
	@Override
	public boolean doesListenToEvent(String eventId) {
		return eventId != null && eventId.equals(ObjectEvent.select);
	}

	/**
	* This method is used for delivering events to the Event Receiver.
	*/
	@Override
	public void eventOccurred(DEvent evt) {
		if ((evt instanceof ObjectEvent) && evt.getSourceMouseEvent() != null && evt.getId().equals(ObjectEvent.select)) {
			ObjectEvent oe = (ObjectEvent) evt;
			Vector obj = oe.getAffectedObjects();
			if (obj == null || obj.size() < 1)
				return;
			if (oe.getSetIdentifier() == null || !oe.getSetIdentifier().equals(dTable.getEntitySetIdentifier()))
				return;
			Vector urls = new Vector(10, 5);
			for (int i = 0; i < obj.size(); i++) {
				int recN = dTable.indexOf((String) obj.elementAt(i));
				if (recN < 0) {
					continue;
				}
				String urlStr = dTable.getAttrValueAsString(urlIdx, recN);
				if (urlStr == null || urlStr.length() < 1)
					return;
				StringTokenizer st = new StringTokenizer(urlStr, ",;|\n\r");
				while (st.hasMoreTokens()) {
					urls.addElement(st.nextToken());
				}
			}
			if (urls.size() < 1)
				return;
			System.out.println("URL = " + urls.toString());
			if (urls.size() == 1) {
				openURL((String) urls.elementAt(0));
				return;
			}
			/*
			PopupMenu pmenu=new PopupMenu();
			pmenu.addActionListener(this);
			for (int i=0; i<urls.size(); i++) {
			  MenuItem item=pmenu.add(new MenuItem((String)urls.elementAt(i)));
			  item.addActionListener(this);
			}
			MouseEvent me=evt.getSourceMouseEvent();
			Component c=me.getComponent();
			c.add(pmenu);
			pmenu.show(c,me.getX(),me.getY());
			c.remove(pmenu);
			*/
			List lst = new List((urls.size() < 8) ? urls.size() + 2 : 10, false);
			for (int i = 0; i < urls.size(); i++) {
				lst.add((String) urls.elementAt(i));
			}
			MouseEvent me = evt.getSourceMouseEvent();
			Component c = me.getComponent();
			OKDialog dia = new OKDialog(CManager.getAnyFrame(c), res.getString("select_url"), true);
			dia.addContent(lst);
			Point p = c.getLocationOnScreen();
			p.x += me.getX();
			p.y += me.getY();
			dia.show(p);
			if (dia.wasCancelled())
				return;
			if (lst.getSelectedIndex() >= 0) {
				openURL(lst.getSelectedItem());
			}
		}
	}

	/**
	* Returns a unique identifier of the event receiver (may be produced
	* automatically, used only internally, not shown to the user).
	*/
	@Override
	public String getIdentifier() {
		if (dTable != null)
			return "URLOpener_" + dTable.getContainerIdentifier();
		return "URLOpener";
	}

	protected void showMessage(String msg, boolean error) {
		if (supervisor != null && supervisor.getUI() != null) {
			supervisor.getUI().showMessage(msg, error);
		}
	}

	/**
	* Opens the specified URL. In an applet the applet context is used.
	* In a local variant of the system the URLOpener cannot use the applet context
	* for opening URLs. Instead it starts the browser on the local computer.
	*/
	protected void openURL(String path) {
		if (path == null || (applContext == null && Helper.getPathToBrowser() == null))
			return;
		if (applContext != null) {
			URL url = null;
			try {
				url = new URL(path);
			} catch (MalformedURLException e) {
				showMessage(e.toString(), true);
				return;
			}
			if (url == null) {
				showMessage(res.getString("Cannot_open_the_URL") + path, true);
				return;
			}
			applContext.showDocument(url, "_blank");
		} else {
			try {
				String cmd[] = new String[2];
				cmd[0] = Helper.getPathToBrowser();
				cmd[1] = path;
				Runtime.getRuntime().exec(cmd);
			} catch (IOException e) {
				supervisor.getUI().showMessage(e.toString(), true);
				System.out.println(e.toString());
			}
		}
	}

	/**
	* Reacts to selection in the list of URLs
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd != null && !cmd.equalsIgnoreCase("cancel")) {
			openURL(cmd);
		}
	}
}