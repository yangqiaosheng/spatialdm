package spade.analysis.tools.processors;

import java.awt.Button;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.system.Processor;
import spade.analysis.tools.DataAnalyser;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.NotificationLine;
import spade.lib.basicwin.OKDialog;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoLayer;
import core.ActionDescr;
import core.ResultDescr;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 26, 2009
 * Time: 2:37:43 PM
 * Allows the user to store and retrieve generated processors.
 */
public class ProcessorsManager implements DataAnalyser, ActionListener {
	/**
	 * The list of known processor classes (i.e. classes implementing the interface
	 * spade.analysis.system.Processor. For each class, the list specifies the
	 * full class name and the corresponding XML tag name.
	 */
	protected static final String processorClasses[][] = { { "spade.analysis.tools.clustering.ObjectsToClustersAssigner", "Classifier" } };

	protected static String getProcessorClassNameByTag(String tagName) {
		if (tagName == null)
			return null;
		for (String[] processorClasse : processorClasses)
			if (tagName.equalsIgnoreCase(processorClasse[1]))
				return processorClasse[0];
		return null;
	}

	protected ESDACore core = null;

	/**
	 * Returns true when the tool has everything necessary for its operation.
	 * For example, if the tool manages a number of analysis methods, it should
	 * check whether the class for at least one method is available.
	 * Here, always returns true.
	 */
	@Override
	public boolean isValid(ESDACore core) {
		return true;
	}

	/**
	 * Keeps a list of processors
	 */
	protected List list = null;
	/**
	* Remembers the last directory where data were saved
	*/
	protected static String lastDir = null;

	protected Panel mainP = null;
	protected NotificationLine lStatus = null;

	protected void constructMainP() {
		if (mainP == null) {
			mainP = new Panel(new ColumnLayout());
		} else {
			mainP.removeAll();
		}
		if (lStatus == null) {
			lStatus = new NotificationLine(null);
		}
		mainP.add(lStatus);
		Panel p = new Panel(new FlowLayout(FlowLayout.CENTER, 20, 2));
		Vector vp = core.getAvailableProcessors();
		if (vp != null && vp.size() > 0) {
			mainP.add(new Label("Registered processors:"));
			list = new List(Math.max(vp.size() + 1, 5), true);
			for (int i = 0; i < vp.size(); i++) {
				list.add(((Processor) vp.elementAt(i)).getName());
			}
			list.select(list.getItemCount() - 1);
			mainP.add(list);
			Button b = new Button("Save");
			b.setActionCommand("save");
			b.addActionListener(this);
			p.add(b);
			if (vp.size() > 1) {
				b = new Button("Join");
				b.setActionCommand("join");
				b.addActionListener(this);
				p.add(b);
			}
		} else {
			mainP.add(new Label(""));
			mainP.add(new Label("No processors have been created or loaded yet!"));
			mainP.add(new Label(""));
		}
		Button b = new Button("Load");
		b.setActionCommand("load");
		b.addActionListener(this);
		p.add(b);
		mainP.add(p);
		mainP.add(new Line(false));
	}

	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	@Override
	public void run(ESDACore core) {
		if (core == null || core.getUI() == null)
			return;
		this.core = core;
		constructMainP();
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Processors", false);
		dia.addContent(mainP);
		dia.show();
		/*
		if (dia.wasCancelled())
		  return;
		*/
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		showMessage(null, false);
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("save")) {
			if (list == null)
				return;
			int idxs[] = list.getSelectedIndexes();
			if (idxs == null || idxs.length != 1) {
				showMessage("Select a single element of the list!", true);
				return;
			}
			int idx = idxs[0];
			Vector vp = core.getAvailableProcessors();
			if (vp == null || idx >= vp.size())
				return;
			Processor pr = (Processor) vp.elementAt(idx);
			String xmlStr = pr.toXML();
			if (xmlStr == null) {
				showMessage("The processor have not produced an XML description!", true);
				return;
			}
			FileDialog fd = new FileDialog(core.getUI().getMainFrame(), "File to save the information?");
			if (lastDir != null) {
				fd.setDirectory(lastDir);
			}
			fd.setFile("*.xml");
			fd.setMode(FileDialog.SAVE);
			fd.show();
			if (fd.getDirectory() == null)
				return;
			lastDir = fd.getDirectory();
			String fname = fd.getFile();
			ActionDescr aDescr = new ActionDescr();
			aDescr.startTime = System.currentTimeMillis();
			if (lastDir != null && lastDir.length() > 0) {
				File file = new File(lastDir);
				if (!file.exists()) {
					file.mkdir();
				}
				if (!file.exists()) {
					showMessage("Cannot create directory [" + lastDir + "]!", true);
					return;
				}
				lastDir = file.getAbsolutePath().replace('\\', '/');
				if (!lastDir.endsWith("/")) {
					lastDir += "/";
				}
				fname = lastDir + fname;
			}
			try {
				FileOutputStream out = new FileOutputStream(fname);
				DataOutputStream dos = new DataOutputStream(out);
				dos.writeBytes(xmlStr);
				out.close();
			} catch (IOException ioe) {
				showMessage("File writing error " + ioe.toString(), true);
				return;
			}
			aDescr.endTime = System.currentTimeMillis();
			aDescr.aName = "Store processor \"" + pr.getName() + "\" in file";
			aDescr.addParamValue("Path", fname);
			core.logAction(aDescr);
		} else if (cmd.equals("join")) {
			if (list == null)
				return;
			int idxs[] = list.getSelectedIndexes();
			if (idxs == null || idxs.length != 2) {
				showMessage("Select two elements of the list!", true);
				return;
			}
			Vector vp = core.getAvailableProcessors();
			if (vp == null || idxs[0] >= vp.size() || idxs[1] >= vp.size())
				return;
			Processor pr1 = (Processor) vp.elementAt(idxs[0]);
			Processor pr2 = (Processor) vp.elementAt(idxs[1]);
			if (!pr1.canJoin() || !pr2.canJoin()) {
				showMessage("A Processor of this kind cannot be joined with another one!", true);
				return;
			}
			ActionDescr aDescr = new ActionDescr();
			aDescr.startTime = System.currentTimeMillis();
			Processor pr = pr1.join(pr2);
			if (pr == null) {
				String err = pr1.getErrorMessage();
				if (err != null) {
					showMessage(err, true);
				} else {
					showMessage("Failed to join the selected processors!", true);
				}
				return;
			}
			showMessage("Join successful!", false);
			String name = Dialogs.askForStringValue(core.getUI().getMainFrame(), "Name of the joint processor?", "Joint " + pr1.getName() + " and " + pr2.getName(), null, "Name of the joint processor?", false);
			pr.setName(name);
			core.registerProcessor(pr);
			aDescr.endTime = System.currentTimeMillis();
			aDescr.aName = "Join processors \"" + pr1.getName() + "\" and \"" + pr2.getName() + "\"";
			aDescr.addParamValue("Processor 1", pr1.getName());
			aDescr.addParamValue("Processor 2", pr2.getName());
			aDescr.addParamValue("Resulting processor name", pr.getName());
			ResultDescr rd = new ResultDescr();
			rd.product = pr;
			rd.comment = "Obtained by joining processors \"" + pr1.getName() + "\" and \"" + pr2.getName() + "\"";
			aDescr.addResultDescr(rd);
			core.logAction(aDescr);
			constructMainP();
			CManager.validateAll(mainP);
			Window win = CManager.getWindow(mainP);
			if (win != null) {
				win.pack();
			}
			showMessage("Joint processor: " + pr.getName(), false);
		} else if (cmd.equals("load")) {
			FileDialog fd = new FileDialog(core.getUI().getMainFrame(), "File with processor description?");
			if (lastDir != null) {
				fd.setDirectory(lastDir);
			}
			fd.setFile("*.xml");
			fd.setMode(FileDialog.LOAD);
			fd.show();
			if (fd.getDirectory() == null)
				return;
			lastDir = fd.getDirectory();
			String fname = fd.getFile();
			if (lastDir != null && lastDir.length() > 0) {
				File file = new File(lastDir);
				if (file.exists()) {
					lastDir = file.getAbsolutePath().replace('\\', '/');
					if (!lastDir.endsWith("/")) {
						lastDir += "/";
					}
					fname = lastDir + fname;
				}
			}
			ActionDescr aDescr = new ActionDescr();
			aDescr.startTime = System.currentTimeMillis();
			Document doc = loadXML(fname);
			if (doc == null) {
				showMessage("Failed to load an XML document!", true);
				return;
			}
			String tagName = getMainTagName(doc);
			if (tagName == null) {
				showMessage("The XML document does not contain a global element describing some processor!", true);
				return;
			}
			String className = getProcessorClassNameByTag(tagName);
			if (className == null) {
				showMessage("Unknown tag: [" + tagName + "]!", true);
				return;
			}
			Processor proc = null;
			try {
				Object pr = Class.forName(className).newInstance();
				if (pr instanceof Processor) {
					proc = (Processor) pr;
				} else {
					showMessage("The class " + className + " is not a Processor!", true);
					return;
				}
			} catch (Exception cle) {
				showMessage("Could not construct an instance of " + className + ": " + cle.toString(), true);
				return;
			}
			if (proc == null) {
				showMessage("Could not generate a processor for the tag [" + tagName + "]!", true);
				return;
			}
			if (proc.canRestoreFromXML(doc) && proc.restoreFromXML(doc)) {
				core.registerProcessor(proc);
				aDescr.endTime = System.currentTimeMillis();
				aDescr.aName = "Load processor from file " + fname;
				aDescr.addParamValue("File name", fname);
				aDescr.addParamValue("Processor name", proc.getName());
				ResultDescr rd = new ResultDescr();
				rd.product = proc;
				rd.comment = "Loaded from file \"" + fname + "\"";
				aDescr.addResultDescr(rd);
				core.logAction(aDescr);
				constructMainP();
				CManager.validateAll(mainP);
				Window win = CManager.getWindow(mainP);
				if (win != null) {
					win.pack();
				}
				showMessage("Restored " + proc.getName(), false);
				if (proc.canMakeMapLayer() && Dialogs.askYesOrNo(core.getUI().getMainFrame(), "Build a map layer with the cluster prototypes used in the classifier?", "Build a layer?")) {
					String name = "Prototypes of " + proc.getName();
/*
          name=Dialogs.askForStringValue(core.getUI().getMainFrame(),
            "Layer name?",name,null,"Layer name",false);
*/
					aDescr = new ActionDescr();
					aDescr.startTime = System.currentTimeMillis();
					DGeoLayer spLayer = proc.makeMapLayer(name);
					if (spLayer != null) {
						DataTable spTable = (DataTable) spLayer.getThematicData();
						DataLoader dLoader = core.getDataLoader();
						int tblN = dLoader.addTable(spTable);
						dLoader.addMapLayer(spLayer, -1);
						dLoader.setLink(spLayer, tblN);
						spLayer.setLinkedToTable(true);
						spLayer.setThematicFilter(spTable.getObjectFilter());
						spLayer.setLinkedToTable(true);
						dLoader.processTimeReferencedObjectSet(spLayer);
						dLoader.processTimeReferencedObjectSet(spTable);
						aDescr.endTime = System.currentTimeMillis();
						aDescr.aName = "Generate map layer with cluster prototypes from the classifier \"" + proc.getName() + "\"";
						aDescr.addParamValue("Processor", proc.getName());
						rd = new ResultDescr();
						rd.product = spLayer;
						rd.comment = "The map layer contains the cluster prototypes " + "from the classifier \"" + proc.getName() + "\"";
						spLayer.setMadeByAction(aDescr);
						aDescr.addResultDescr(rd);
						rd = new ResultDescr();
						rd.product = spTable;
						rd.comment = "The table describes the objects of the layer \"" + spLayer.getName() + "\"";
						spTable.setMadeByAction(aDescr);
						aDescr.addResultDescr(rd);
						core.logAction(aDescr);
					}
				}
			} else {
				String err = proc.getErrorMessage();
				if (err != null) {
					showMessage(err, true);
				} else {
					showMessage("Failed to restore the processor from an XML document!", true);
				}
				return;
			}
		}
	}

	protected Document loadXML(String fname) {
		if (fname == null) {
			showMessage("File not found!", true);
			return null;
		}
		File f = new File(fname);
		if (!f.exists()) {
			showMessage("File not found!", true);
			return null;
		}
		Document doc = null;
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			doc = docBuilder.parse(new File(fname));
			// normalize text representation
			doc.getDocumentElement().normalize();
		} catch (SAXParseException err) {
			System.out.println("** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId());
			System.out.println("   " + err.getMessage());
			// print stack trace as below
		} catch (SAXException e) {
			Exception x = e.getException();
			((x == null) ? e : x).printStackTrace();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return doc;
	}

	protected String getMainTagName(Document doc) {
		if (doc != null && doc.getDocumentElement() != null)
			return doc.getDocumentElement().getTagName();
		return null;
	}

	protected void showMessage(String msg, boolean error) {
		if (lStatus != null && lStatus.isShowing()) {
			lStatus.showMessage(msg, error);
		} else if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		}
		if (error) {
			System.out.println("!--> " + msg);
		}
	}
}
