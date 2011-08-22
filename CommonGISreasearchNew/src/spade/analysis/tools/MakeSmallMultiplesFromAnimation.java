package spade.analysis.tools;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.system.Supervisor;
import spade.analysis.tools.clustering.ClusterImage;
import spade.lib.basicwin.FlexibleGridLayout;
import spade.lib.basicwin.ImageCanvas;
import spade.time.FocusInterval;
import spade.time.TimeMoment;
import spade.time.vis.DataVisAnimator;
import spade.vis.database.Parameter;
import ui.ImagePrinter;
import ui.SimpleMapView;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Sep 28, 2009
 * Time: 11:13:02 AM
 * This tool builds a panel with multiple map images each representing
 * data for one value of a temporal parameter.
 */
public class MakeSmallMultiplesFromAnimation extends BaseAnalyser implements ActionListener {

	protected Vector<ClusterImage> images = null;

	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	@Override
	public void run(ESDACore core) {
		this.core = core;
		if (core == null || core.getUI() == null || core.getUI().getCurrentMapViewer() == null) {
			showMessage("No map exists!", true);
			return;
		}
		Supervisor sup = core.getSupervisor();
		if (sup == null) {
			showMessage("No supervisor!", true);
			return;
		}
		if (sup.getSaveableToolCount() < 1) {
			showMessage("No animation found!", true);
			return;
		}
		Vector<DataVisAnimator> animators = new Vector(5, 5);
		for (int i = 0; i < sup.getSaveableToolCount(); i++)
			if (sup.getSaveableTool(i) instanceof DataVisAnimator) {
				animators.addElement((DataVisAnimator) sup.getSaveableTool(i));
			}
		if (animators.size() < 1) {
			showMessage("No animation found!", true);
			return;
		}
		Parameter param = animators.elementAt(0).getParameter();
		for (int j = 1; j < animators.size() && param.getValueCount() < 2; j++) {
			param = animators.elementAt(j).getParameter();
		}

		FocusInterval fint = animators.elementAt(0).getFocusInterval();
		TimeMoment start0 = fint.getCurrIntervalStart().getCopy();
		long iLen = ((TimeMoment) param.getValue(1)).subtract((TimeMoment) param.getValue(0));
		if (iLen < 1) {
			iLen = 1;
		}
		long nSteps = Math.round(Math.ceil(1.0 * fint.getDataIntervalLength() / iLen)) + 1;

		images = new Vector<ClusterImage>((int) nSteps, 1);
		SimpleMapView mw = (SimpleMapView) core.getUI().getCurrentMapViewer();

		fint.setCurrIntervalStart(fint.getDataIntervalStart());
		for (long i = 0; i < nSteps; i++) {
			TimeMoment parVal = animators.elementAt(0).getCurrParamValue();
			for (int j = 1; j < animators.size() && parVal == null; j++) {
				parVal = animators.elementAt(j).getCurrParamValue();
			}
			Image image = mw.getMapAsImage();
			if (image != null) {
				ClusterImage cim = new ClusterImage();
				cim.clusterIdx = (int) i + 1;
				cim.clusterLabel = (parVal == null) ? fint.getCurrIntervalStart().toString() : parVal.toString();
				if (cim.clusterLabel == null) {
					cim.clusterLabel = fint.getCurrIntervalStart().toString();
				}
				cim.image = image;
				cim.size = 0;
				images.addElement(cim);
			}
			if (i < nSteps - 1) {
				fint.moveIntervalBy(iLen);
			}
		}
		fint.setCurrIntervalStart(start0);

		if (images != null && images.size() > 0) {
			Panel mainP = new Panel(new FlexibleGridLayout(2, 2));
			for (int i = 0; i < images.size(); i++) {
				ClusterImage cim = images.elementAt(i);
				ImageCanvas ic = new ImageCanvas(cim.image);
				Panel p = new Panel(new BorderLayout());
				p.add(new Label(cim.clusterLabel), BorderLayout.NORTH);
				p.add(ic, BorderLayout.CENTER);
				mainP.add(p);
			}
			ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			scp.add(mainP);
			Panel imP = new Panel(new BorderLayout());
			imP.add(scp, BorderLayout.CENTER);
			Panel p = new Panel(new FlowLayout(FlowLayout.RIGHT, 0, 2));
			Button b = new Button("Save the images");
			b.setActionCommand("save");
			b.addActionListener(this);
			p.add(b);
			imP.add(p, BorderLayout.NORTH);

			ClusterImage cim = images.elementAt(0);
			int w = cim.image.getWidth(null), h = cim.image.getHeight(null);
			core.getDisplayProducer().makeWindow(imP, param.getName(), w * 3 + 30 + scp.getVScrollbarWidth(), h * 3 + 50);
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		FileDialog fd = new FileDialog(core.getUI().getMainFrame(), "Where to store the images?", FileDialog.SAVE);
		fd.setVisible(true);
		String dir = fd.getDirectory();
		fd.dispose();
		if (dir == null) {
			core.getUI().showMessage("Could not get a directory for storing the images!", true);
			return;
		}
		ImagePrinter impr = new ImagePrinter(core.getSupervisor());
		Vector vImages = new Vector(images.size(), 10), vFnames = new Vector(images.size(), 10);
		for (int i = 0; i < images.size(); i++) {
			ClusterImage cim = images.elementAt(i);
			vImages.addElement(cim.image);
			vFnames.addElement(String.valueOf(cim.clusterIdx));
		}
		impr.setImages(vImages, vFnames);
		impr.saveImages(false, "png", dir);
	}
}
