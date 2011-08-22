package ui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.DisplayProducer;
import spade.analysis.system.Supervisor;
import spade.analysis.system.SystemUI;
import spade.analysis.system.ToolReCreator;
import spade.analysis.system.WindowManager;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.GetPathDlg;
import spade.lib.basicwin.OKDialog;
import spade.vis.map.MapViewer;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.spec.SaveableTool;
import spade.vis.spec.SpecSaver;
import configstart.Snapshot;
import configstart.SnapshotList;
import configstart.StateSaverFactory;
import configstart.ToolRestoreFactory;
import configstart.VisSpecReader;
import fr.dyade.koala.xml.kbml.KBMLDeserializer;
import fr.dyade.koala.xml.kbml.KBMLSerializer;

/**
* Saves and restores CommonGIS session states: visializations on maps,
* graphs, filters, animations, and other tools.
*/
public class SnapshotManager {
	/**
	* Specifies the right order for processing specifications in the course of
	* state restoring. It may be important that some specifications are processed
	* before others. The order is specified according to the class names of
	* potential specifications.
	*/
	protected static final String specOrder[] = { "spade.vis.spec.ToolSpec", "spade.vis.spec.TemporalToolSpec", "spade.vis.spec.AnimatedVisSpec", "spade.vis.spec.QuerySpec", "spade.vis.spec.MapWindowSpec", "spade.vis.spec.WinSpec" };

	/**
	* Puts the given list of specification in the right order: it may be important
	* that some specifications are processed before others. The right order
	* (according to class names) is specified in the array @see specOrder.
	*/
	protected static void sortSpecifications(Vector specs) {
		if (specs == null || specs.size() < 2)
			return;
		for (int i = 0; i < specs.size() - 1; i++)
			if (getSpecOrder(specs.elementAt(i), specs.elementAt(i + 1)) > 0) {
				Object sp = specs.elementAt(i + 1);
				int idx = i;
				for (int j = i - 1; j >= 0 && getSpecOrder(specs.elementAt(j), sp) > 0; j--) {
					idx = j;
				}
				specs.removeElementAt(i + 1);
				specs.insertElementAt(sp, idx);
			}
	}

	/**
	* For two given specifications, determines which of them precedes the other
	* according to the order specified in the array @see specOrder. The
	* result -1 means that the first component precedes the second, 1 means
	* the opposite, and 0 means that the order is undefined.
	*/
	public static int getSpecOrder(Object spec1, Object spec2) {
		if (spec1 == null || spec2 == null)
			return 0;
		String name1 = spec1.getClass().getName(), name2 = spec2.getClass().getName();
		if (name1.equals(name2))
			return 0;
		for (String element : specOrder) {
			if (name1.equals(element))
				return -1;
			if (name2.equals(element))
				return 1;
		}
		return 0;
	}

	/**
	 * Displays a dialog to modify the name and description of a snapshot. Returns false if cancelled
	 */
	public boolean editSnapshot(Snapshot snapshot) {
		if (snapshot == null)
			return true;
		Frame fr = null;
		SystemUI ui = supervisor.getUI();
		if (ui != null) {
			fr = ui.getMainFrame();
		}
		if (fr == null) {
			fr = CManager.getAnyFrame();
		}
		OKDialog okd = new OKDialog(fr, "Describe your findings", true);
		Panel p = new Panel(new BorderLayout());
		Panel pn = new Panel(new FlowLayout(FlowLayout.RIGHT));
		Panel pd = new Panel(new FlowLayout(FlowLayout.RIGHT));
		pn.add(new Label("Name"));
		pd.add(new Label("Description"));
		TextField tn = new TextField(55);
		tn.setText(snapshot.getName());
		TextArea td = new TextArea(5, 50);
		td.setText(snapshot.getDescription());
		pn.add(tn);
		pd.add(td);
		p.add(pn, "North");
		p.add(pd, "Center");
		okd.addContent(p);
		okd.show();
		if (okd.wasCancelled())
			return false;

		snapshot.setName(tn.getText());
		snapshot.setDescription(td.getText());
		return true;
	}

	/**
	* Saves the current CommonGIS session states: visializations on maps,
	* graphs, filters, animations, etc. (to be extended)
	*/
	public void saveProjectSnapshot() {
		Snapshot snapshot = new Snapshot();
		if (!editSnapshot(snapshot))
			return;

		snapshot.setFileName(System.getProperty("user.name") + "_" + System.currentTimeMillis() + "." + fmt);
		saveSnapshot(getApplicationDir() + snapshot.getFileName());

		loadSnapshotList();
// create if needed
		if (slist == null) {
			slist = new SnapshotList();
		}
// add snapshsot
		slist.snapshots.addElement(snapshot);
		saveSnapshotList();
	}

	public boolean saveSnapshot(String path) {
		if (supervisor == null || supervisor.getSaveableToolCount() < 1)
			return false;
		if (!supervisor.getSystemSettings().checkParameterValue("isLocalSystem", "true"))
			return false;

		SystemUI ui = supervisor.getUI();

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(path);
		} catch (IOException e) {
			if (ui != null) {
				ui.showMessage("Error creating snapshot file", true);
			}
			return false;
		}
		if (fos == null) {
			if (ui != null) {
				ui.showMessage("Cannot save map: file could not be opened", true);
			}
			return false;
		}

		DataOutputStream dos = new DataOutputStream(fos);
		boolean error = false;

		for (int i = 0; i < supervisor.getSaveableToolCount() && !error; i++) {
			SaveableTool tool = supervisor.getSaveableTool(i);
			if (tool == null) {
				continue;
			}
			Object spec = tool.getSpecification();
			if (spec == null) {
				continue;
			}
			SpecSaver saver = StateSaverFactory.getSpecSaver(tool.getTagName());
			if (saver == null) {
				if (ui != null) {
					ui.showMessage("Unknown tool type: " + tool.getTagName(), true);
				}
				continue;
			}
			try {
				saver.writeSpecification(spec, dos);
			} catch (IOException ioe) {
				if (ui != null) {
					ui.showMessage("ERROR: " + ioe.toString());
				}
				ioe.printStackTrace();
				error = true;
				break;
			}
		}

		try {
			fos.close();
		} catch (IOException ex) {
		}
		if (!error && ui != null) {
			ui.showMessage("OK", false);
		}
		return true;
	}

	private void saveSnapshotList() {
		if (dataKeeper == null)
			return;
		String applPath = dataKeeper.getApplicationPath();
		String listFile = applPath.substring(0, applPath.length() - fmt.length() - 1) + ".snapshots.xml";

// save xml
		try {
			FileOutputStream ostream = new FileOutputStream(listFile);
			KBMLSerializer bxo = new KBMLSerializer(ostream);
			bxo.setSerializationOptions(KBMLSerializer.WRITE_DEFAULT_VALUES);
			bxo.writeXMLDeclaration();
			bxo.writeDocumentTypeDefinition();
			bxo.writeKBMLStartTag();
			bxo.writeBean(slist);
			bxo.writeKBMLEndTag();
			bxo.flush();
			bxo.close();
		} catch (Exception ex) {
		}
	}

	protected static void clearAllVis(DisplayProducer dprod, LayerManager lm, MapViewer mapView) {
		if (dprod == null)
			return;
		dprod.closeAllTools();
		if (lm != null && mapView != null) {
			for (int i = 0; i < lm.getLayerCount(); i++) {
				GeoLayer gl = lm.getGeoLayer(i);
				if ((gl.getVisualizer() != null && gl.getVisualizer().getVisualizationName() != null) || gl.getBackgroundVisualizer() != null) {
					dprod.eraseDataFromMap(gl, mapView);
				}
			}
		}
	}

	/**
	* Restores a CommonGIS session state on the basis of specifications previously
	* stored in the given file. This concerns visializations on maps, graphs,
	* filters, animations, etc. (to be extended)
	* The argument @arg visManager is a component used for creating visual data
	* displays and cartographic visualizers. This may be either a DisplayProducer
	* or a SimpleDataMapper, depending on the configuration.
	*/
	public void loadSnapshot(String path, //path to the file
/*                           DataKeeper dKeeper,
                           Supervisor supervisor,
                           Object visManager,*/
			boolean makeMapManipulators) {
		if (supervisor == null || dataKeeper == null || visManager == null || path == null)
			return;

		VisSpecReader vspr = new VisSpecReader();
		if (path != null) {
			vspr.setDataSource(path);
		}
		SystemUI ui = supervisor.getUI();
		if (ui != null) {
			vspr.addProcessListener(ui.getStatusLine());
		}
		Vector specs = vspr.read();
		if (specs == null || specs.size() < 1)
			return;
		if ((visManager instanceof DisplayProducer) && ui != null) {
			clearAllVis((DisplayProducer) visManager, dataKeeper.getMap(0), ui.getMapViewer(0));
		}
		fulfillSpecifications(specs, makeMapManipulators);
	}

	/**
	* Allows the user to select a snapshot file for loading.
	*/
	public void loadSnapshot(
/*      DataKeeper dKeeper,
                           Supervisor supervisor,
                           Object visManager,*/
	boolean makeMapManipulators) {
		if (supervisor == null || dataKeeper == null || visManager == null)
			return;
		if (!supervisor.getSystemSettings().checkParameterValue("isLocalSystem", "true"))
			return;

		Frame fr = null;
		SystemUI ui = supervisor.getUI();
		if (ui != null) {
			fr = ui.getMainFrame();
		}

		if (fr == null) {
			fr = CManager.getAnyFrame();
		}
		String fmt = "str";
		// following string: "Select the file to save map"
		GetPathDlg fd = new GetPathDlg(fr, "Select snapshot file");
		fd.setFileMask("*." + fmt);
		fd.show();
		String path = fd.getPath();
		if (path == null) {
			if (ui != null) {
				ui.clearStatusLine();
			}
			return;
		}
		if (path.indexOf("." + fmt) < 0) {
			path += "." + fmt;
		}
		loadSnapshot(path, makeMapManipulators);
	}

	public void fulfillSpecifications(Vector specs,
/*                                     DataKeeper dKeeper,
                                     Supervisor supervisor,
                                     Object visManager,*/
	boolean makeMapManipulators) {
		if (specs == null || dataKeeper == null || visManager == null)
			return;
		sortSpecifications(specs);

		Window alert = new Window(new Frame());
		alert.add(new Label("Restoring system state..."));
		alert.setBackground(Color.yellow);
		alert.pack();
		Dimension scr = Toolkit.getDefaultToolkit().getScreenSize(), aldim = alert.getSize();
		alert.setLocation(scr.width / 2 - aldim.width / 2, scr.height / 2 - aldim.height / 2);
		winManager.setAllWindowsVisible(false);
		alert.setVisible(true);

		try {
			for (int i = 0; i < specs.size(); i++) {
				ToolReCreator restorer = ToolRestoreFactory.getToolRestorer(specs.elementAt(i));
				if (restorer != null) {
					restorer.fulfillSpecification(specs.elementAt(i), dataKeeper, supervisor, visManager, makeMapManipulators);
				}
			}
		} catch (Exception ex) {
			System.out.println("Unable to restore system state: " + ex);
			ex.printStackTrace();
		}

		alert.setVisible(false);
		alert.dispose();
		winManager.setAllWindowsVisible(true);
	}

// and now -  the weirdest part of the file...
	private OKDialog okd;
	private List sChooser = new List(20);
	private TextArea tDescr = new TextArea(5, 50);
	private Label lFile = new Label();
	private Button delete = new Button("Delete");
	private Button select = new Button("Load from file");
	private Button edit = new Button("Edit description");
	private String listFile;
	private SnapshotList slist = null;
	private DataKeeper dataKeeper;
	private Supervisor supervisor;
	private Object visManager;
	private WindowManager winManager;
//  private boolean makeMapManipulators;
	private String fmt = "str";

	public SnapshotManager(Supervisor supervisor, DataKeeper dataKeeper, Object visManager, WindowManager winManager) {
		if (supervisor == null)
			return;
		this.supervisor = supervisor;
		this.dataKeeper = dataKeeper;
		this.visManager = visManager;
		this.winManager = winManager;

		tDescr.setEditable(false);

		delete.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				deleteSelectedSnapshot();
			}
		});

		select.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				loadSnapshot(true);
				okd.dispose();
				sChooser.select(-1);
			}
		});

		edit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (sChooser.getSelectedIndex() >= 0) {
					int idx = sChooser.getSelectedIndex();
					editSnapshot(slist.getList()[idx]);
					sChooser.remove(idx);
					sChooser.add(slist.getList()[idx].getName(), idx);
					sChooser.select(idx);
					updateSummary(slist.getList()[idx]);
					saveSnapshotList();
				}
			}
		});

		sChooser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				if (sChooser.getSelectedIndex() >= 0) {
					loadSnapshot(getApplicationDir() + slist.getList()[sChooser.getSelectedIndex()].getFileName(), true);
					okd.dispose();
				}
			}
		});

		sChooser.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (sChooser.getSelectedIndex() >= 0) {
					updateSummary(slist.getList()[sChooser.getSelectedIndex()]);
				}
			}
		});

		sChooser.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_DELETE) {
					deleteSelectedSnapshot();
				}
			}
		});
	}

	private void deleteSelectedSnapshot() {
		int idx = sChooser.getSelectedIndex();
		if (idx >= 0) {
			File toDelete = new File(getApplicationDir() + slist.getList()[idx].getFileName());
			toDelete.delete();
			slist.snapshots.removeElementAt(idx);
			saveSnapshotList();

			sChooser.remove(sChooser.getSelectedItem());
			if (idx == sChooser.getItemCount()) {
				idx--;
			}
			if (idx >= 0) {
				sChooser.select(idx);
				updateSummary(slist.getList()[idx]);
			}
		}
	}

	private void updateSummary(Snapshot snapshot) {
		tDescr.setText(snapshot.getDescription());
		lFile.setText("File " + snapshot.getFileName() + " created by " + snapshot.getCreator() + " at " + snapshot.getCreationDate());
	}

	public void loadProjectSnapshot() {
		if (supervisor == null)
			return;
//    Vector slist = null;
		loadSnapshotList();

		if (slist == null || slist.snapshots == null || slist.snapshots.size() == 0) {
			loadSnapshot(true);
			return;
		}

		Panel p = new Panel(new BorderLayout());
		Panel bp = new Panel(new GridLayout(1, 2));
		bp.add(delete);
		bp.add(select);
		bp.add(edit);
		Panel bottom = new Panel(new BorderLayout());
		tDescr.setText("");
		lFile.setText("");
		bottom.add(tDescr, "Center");
		bottom.add(lFile, "North");
		if (supervisor.getSystemSettings().checkParameterValue("isLocalSystem", "true")) {
			bottom.add(bp, "South");
		}
		p.add(bottom, "South");
		p.add(sChooser, "Center");
		okd = new OKDialog(CManager.getAnyFrame(), "Available snapshots", true);
		okd.addContent(p);

		sChooser.removeAll();
		for (int i = 0; i < slist.snapshots.size(); i++) {
			sChooser.add(((Snapshot) slist.snapshots.elementAt(i)).getName());
		}

		okd.pack();
		okd.setVisible(true);
		if (!okd.isEnabled() || !okd.wasCancelled() && sChooser.getSelectedIndex() >= 0) {
			loadSnapshot(getApplicationDir() + slist.getList()[sChooser.getSelectedIndex()].getFileName(), true);
		}
	}

	private void loadSnapshotList() {
		if (dataKeeper != null) {
			try {
				String applPath = dataKeeper.getApplicationPath();
				listFile = applPath.substring(0, applPath.length() - fmt.length() - 1) + ".snapshots.xml";

				int idx = listFile.indexOf(':');
				boolean isURL = false;
				if (idx > 0) {
					String pref = listFile.substring(0, idx);
					if (pref.equalsIgnoreCase("HTTP") || pref.equalsIgnoreCase("FILE")) {
						isURL = true;
					}
				}

				// load xml
				KBMLDeserializer bxi;
				if (isURL) {
					bxi = new KBMLDeserializer(new URL(listFile).openStream());
				} else {
					bxi = new KBMLDeserializer(new FileInputStream(listFile));
				}
				slist = (SnapshotList) bxi.readBean();
				bxi.close();
			} catch (Exception ex) {
			}
		}
	}

	/**
	 * Returns the path to the directory of the current application. Ends with file separator
	 */
	public String getApplicationDir() {
		if (dataKeeper == null)
			return "";
		String applPath = dataKeeper.getApplicationPath();
		return applPath.substring(0, applPath.lastIndexOf(System.getProperty("file.separator")) + 1);
	}
}
