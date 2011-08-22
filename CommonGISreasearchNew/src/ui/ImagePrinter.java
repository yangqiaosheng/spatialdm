package ui;

import java.applet.AppletContext;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.PrintJob;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.plot.PrintableImage;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.GetPathDlg;
import spade.lib.basicwin.NotificationLine;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;

/**
 * Created by IntelliJ IDEA.
 * User: DIvan
 * Date: Mar 25, 2004
 * Time: 12:45:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class ImagePrinter {
	static ResourceBundle res = Language.getTextResource("ui.Res");

	public final static int margin = 15; // mm

	protected Supervisor supervisor;
	protected Vector images = new Vector();
	protected Vector names = new Vector();
	protected Vector ids = new Vector();
	protected List ilist = new List(10, true);
	protected PrintJob pj;
	protected Dimension pageSize;
	protected Dimension targetSize;
	protected int pixMargin;

	public ImagePrinter(Supervisor sup) {
		this.supervisor = sup;
	}

	public void setImages(Vector images, Vector ids) {
		this.images = images;
		this.ids = ids;
	}

	public void chooseAndSaveOrPrint(boolean print) {
		if (supervisor == null)
			return;
		if (!isPrintSaveAvailable(print)) {
			if (supervisor.getUI() != null) {
				/**/supervisor.getUI().showMessage("Cannot export image", true);
			}
			return;
		}

		for (int i = 0; i < supervisor.getSaveableToolCount(); i++)
			if (supervisor.getSaveableTool(i) instanceof PrintableImage) {
				String name = ((PrintableImage) supervisor.getSaveableTool(i)).getName();
				if (name.length() > 50) {
					name = name.substring(0, 50) + " ...";
				}
				names.addElement(name);
				ids.addElement(StringUtil.makeID((String) names.lastElement()));
				ilist.add((String) names.lastElement());
			} else if (supervisor.getSaveableTool(i) instanceof SimpleMapView) {
				SimpleMapView map = (SimpleMapView) supervisor.getSaveableTool(i);
				names.addElement("Map " + map.getInstanceN());
				ids.addElement(StringUtil.makeID((String) names.lastElement()));
				ilist.add((String) names.lastElement());
				names.addElement("Legend " + map.getInstanceN());
				ids.addElement(StringUtil.makeID((String) names.lastElement()));
				ilist.add((String) names.lastElement());
				names.addElement("Map with legend " + map.getInstanceN());
				ids.addElement(StringUtil.makeID((String) names.lastElement()));
				ilist.add((String) names.lastElement());
			}

/*
    { "Clear_all", " Clear all" },
    { "Select_all", "Select all" },
*/
		Button all = new Button(res.getString("Select_all"));
		Button none = new Button(res.getString("Clear_all"));
		all.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				for (int i = 0; i < ilist.getItemCount(); i++) {
					ilist.select(i);
				}
			}
		});
		none.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				for (int i = 0; i < ilist.getItemCount(); i++) {
					ilist.deselect(i);
				}
			}
		});
		Panel buttons = new Panel(new GridLayout(1, 2));
		buttons.add(all);
		buttons.add(none);

		Panel content = new Panel(new BorderLayout());
		content.add(ilist, "Center");
		content.add(buttons, "South");

		OKDialog okd = new OKDialog(supervisor.getUI().getMainFrame(), "Select what to save/print", true);
		okd.addContent(content);
		okd.show();
		if (okd.wasCancelled())
			return;

		if (print) {
			openPrintJob();
			if (pj == null)
				return;
		} else {
			targetSize = new Dimension(600, 800);
		}

		for (int i = 0; i < supervisor.getSaveableToolCount(); i++)
			if (supervisor.getSaveableTool(i) instanceof PrintableImage) {
				images.addElement(((PrintableImage) supervisor.getSaveableTool(i)).getImage());
			} else if (supervisor.getSaveableTool(i) instanceof SimpleMapView) {
				SimpleMapView map = (SimpleMapView) supervisor.getSaveableTool(i);
				Image img = map.getMapAsImage(targetSize);
				images.addElement(img);
				img = map.getLegendAsImage(targetSize);
				images.addElement(img);
				img = map.getMapAndLegendAsImage(targetSize);
				images.addElement(img);
			}

		if (print) {
			printImages(true);
			pj.end();
		} else {
			saveImages(true);
		}
	}

	protected void openPrintJob() {
/**/	pj = Toolkit.getDefaultToolkit().getPrintJob(supervisor.getUI().getMainFrame(), "Printing", null);
		if (pj == null) {
/**/		System.err.println("Cannot create print job");
			return;
		}
		pageSize = pj.getPageDimension();
		pixMargin = (int) (pj.getPageResolution() / 25.4 * margin);
		targetSize = new Dimension(pageSize.width - 2 * pixMargin, pageSize.height - 2 * pixMargin);
	}

	protected void printImages(boolean fromList) {
		if (fromList && (ilist.getSelectedItems() == null || ilist.getSelectedItems().length == 0))
			return;
		for (int i = 0; i < images.size(); i++) {
			if (fromList && !ilist.isIndexSelected(i)) {
				continue;
			}
			Graphics page = pj.getGraphics();
			Image img = (Image) images.elementAt(i);
			if (img.getWidth(null) > targetSize.width || img.getHeight(null) > targetSize.height) {
				float scX = (float) img.getWidth(null) / targetSize.width;
				float scY = (float) img.getHeight(null) / targetSize.height;
				float scale = 1 / Math.max(scX, scY);
				if (Float.isNaN(scale) || Float.isInfinite(scale)) {
					scale = 1;
				}
				img = img.getScaledInstance((int) (img.getWidth(null) * scale), (int) (img.getHeight(null) * scale), Image.SCALE_SMOOTH);
			}
			page.drawImage(img, pageSize.width / 2 - img.getWidth(null) / 2, pixMargin, null);
			page.dispose();
		}
	}

	public void saveImages(boolean fromList) {
		saveImages(fromList, null, null);
	}

	public boolean saveImages(boolean fromList, String sFmt, String sPath) {
		if (fromList && (ilist.getSelectedItems() == null || ilist.getSelectedItems().length == 0))
			return false;
		Class classSaveImageDlg = null, classSaveJPEGImage = null, classSavePNGImage = null, classSaveBMPImage = null, classImageSerializer = null;
		Frame fr = null;
		NotificationLine nlStatus = null;
		Exception cnfEx = null;
		String servletURL = null;

		// Check first of all is the package available at all...
		try {
			classSaveImageDlg = Class.forName("ui.bitmap.SaveImageDlg");
			classSaveJPEGImage = Class.forName("ui.bitmap.SaveJPEGImage");
			classSavePNGImage = Class.forName("ui.bitmap.SavePNGImage");
			classSaveBMPImage = Class.forName("ui.bitmap.SaveBMPImage");
		} catch (Exception e) {
			cnfEx = e;
		}
		try {
			classImageSerializer = Class.forName("ui.bitmap.ImageSerializer");
		} catch (Exception e) {
			System.out.println("WARNING: No image serializer available in the system.");
		}

		ImageSaverProperties imgSavProps = null;
		ImageSaver imgSaver = null;

		String fmt = sFmt;
		if (fmt == null) {
			if (supervisor != null) {
				servletURL = supervisor.getSystemSettings().getParameterAsString("ImageServlet");
				if (supervisor.getUI() != null) {
					fr = supervisor.getUI().getMainFrame();
					nlStatus = supervisor.getUI().getStatusLine();
				}
			}
			if (cnfEx != null || classSaveImageDlg == null) { // hmmm... it seems that it's not available
				System.err.println(cnfEx.toString());
				System.err.println(res.getString("Saving_graphics_into"));
				if (nlStatus != null) {
					// following string: "Saving graphics into a file is not included in this version of the system"
					nlStatus.showMessage(res.getString("Saving_graphics_into"), false);
				}
				return false;
			}

			if (fr == null) {
				fr = CManager.getAnyFrame();
			}
			OKDialog okd = new OKDialog(fr, res.getString("Saving"), true);
			Object objSIDlg = null;
			if (classSaveImageDlg != null) {
				try {
					objSIDlg = classSaveImageDlg.newInstance();
				} catch (Exception ex) {
				}
			}
			if (objSIDlg != null) {
				okd.addContent((Component) objSIDlg);
			}
			okd.show();
			if (okd.wasCancelled())
				return false;
			imgSavProps = (ImageSaverProperties) objSIDlg;
			fmt = imgSavProps.getSelectedFormat();
		}
		if (fmt.startsWith(".")) {
			fmt = fmt.substring(1);
		}

		if (!(fmt.equals("jpg") || fmt.equals("png") || fmt.equals("bmp"))) {
			// following string: "This format is not supported!"
			if (nlStatus != null) {
				nlStatus.showMessage(res.getString("This_format_is_not"), true);
			}
			System.err.println("Format is not supported");
			return false;
		}
		// If NOT Java 2 or higher (JPEG is not supported)
		if (fmt.equals("jpg")) {
			try {
				if (classSaveJPEGImage != null) {
					imgSaver = (ImageSaver) (classSaveJPEGImage.newInstance());
				}
			} catch (Exception ex) {
			}
			if (imgSaver != null && !imgSaver.isAvailable()) {
				if (nlStatus != null) {
					// following string: "JPEG image encoding is not supported by Java 1.1.x ! Use Java 2 or higher instead"
					nlStatus.showMessage(res.getString("JPEG_image_encoding"), false);
				}
				return false;
			}
		}

		String path = "";
		if (sPath == null) {
			if (supervisor.getSystemSettings().checkParameterValue("isLocalSystem", "true")) {
				GetPathDlg fd = new GetPathDlg(fr, res.getString("Select_the_file_to"));
				fd.setFileMask("Filename will be ignored");
				fd.show();
				path = fd.getPath();
				if (path == null) {
					if (nlStatus != null) {
						nlStatus.showDefaultMessage();
					}
					return false;
				}
				path = path.substring(0, path.lastIndexOf(System.getProperty("file.separator")) + 1);
			}
		} else {
			path = sPath;
		}

		for (int i = 0; i < images.size(); i++) {
			if (fromList && !ilist.isIndexSelected(i)) {
				continue;
			}
			Image img = (Image) images.elementAt(i);

			if (supervisor.getSystemSettings().checkParameterValue("isLocalSystem", "true")) {
				// following string: "Select the file to save map"
//        if (path.indexOf("."+fmt)<0) path+="."+fmt;
				FileOutputStream fosMap = null;
				String fname = path + ids.elementAt(i) + "." + fmt;
				try {
					fosMap = new FileOutputStream(fname);
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (fosMap == null) {
					System.out.println("ERROR: Cannot save image: file <" + fname + ">could not be opened");
					if (nlStatus != null) {
						nlStatus.showDefaultMessage();
					}
					return false;
				}

				if (fmt.equals("jpg")) {
					try {
						if (classSaveJPEGImage != null) {
							imgSaver = (ImageSaver) (classSaveJPEGImage.newInstance());
						}
					} catch (Exception ex) {
					}
					if (imgSaver != null && imgSaver.isAvailable()) {
						if (imgSavProps != null) {
							imgSaver.setProperties(imgSavProps);
						}
						imgSaver.saveImage(fosMap, img, img.getWidth(null), img.getHeight(null));
					} else {
						System.err.println("JPEG file format support is not included in this version of the system");
					}
				} else // png-format
				if (fmt.equals("png")) {
					try {
						if (classSavePNGImage != null) {
							imgSaver = (ImageSaver) (classSavePNGImage.newInstance());
						}
					} catch (Exception ex) {
					}
					if (imgSaver != null && imgSaver.isAvailable()) {
						if (imgSavProps != null) {
							imgSaver.setProperties(imgSavProps);
						}
						imgSaver.saveImage(fosMap, img, img.getWidth(null), img.getHeight(null));
					} else {
						System.err.println("PNG file format support is not included in this version of the system");
					}
				} else // bmp-format
				if (fmt.equals("bmp")) {
					try {
						if (classSaveBMPImage != null) {
							imgSaver = (ImageSaver) (classSaveBMPImage.newInstance());
						}
					} catch (Exception ex) {
					}
					if (imgSaver != null && imgSaver.isAvailable()) {
						imgSaver.saveImage(fosMap, img, img.getWidth(null), img.getHeight(null));
					} else {
						System.err.println("BMP file format support is not included in this version of the system");
					}
				}
			} else {
				ImgSerializer imgSer = null;
				if (classImageSerializer != null) {
					try {
						imgSer = (ImgSerializer) classImageSerializer.newInstance();
						imgSer.setServletURL(servletURL);
					} catch (Exception ex) {
						System.out.println(ex);
					}
					if (imgSer != null) {
						URL imageURL = imgSer.serializeImage(img, imgSavProps, StringUtil.SQLid((String) ids.elementAt(i), 20));
						if (imageURL != null) {
							((AppletContext) supervisor.getSystemSettings().getParameter("AppletContext")).showDocument(imageURL, "_blank");
						}
					} else {
						System.err.println("Cannot receive image from server");
					}

				}
			}
		}
		if (nlStatus != null) {
			nlStatus.showDefaultMessage();
		}
		return true;
	}

	public boolean isPrintSaveAvailable(boolean print) {
		if (supervisor == null)
			return false;
		if (print)
			return supervisor.getSystemSettings().checkParameterValue("isLocalSystem", "true");
		String servletURL = null;
		boolean OK = false;
		if (supervisor != null) {
			servletURL = supervisor.getSystemSettings().getParameterAsString("ImageServlet");
			OK = (servletURL != null || supervisor.getSystemSettings().checkParameterValue("isLocalSystem", "true"));
		}
		return OK;
	}

	public void saveOrPrintMap(SimpleMapView map, boolean print) {
		if (supervisor == null || map == null)
			return;
		if (!isPrintSaveAvailable(print)) {
			if (supervisor.getUI() != null) {
				supervisor.getUI().showMessage("Cannot export image", true);
			}
			return;
		}

		Checkbox cbSaveMap = new Checkbox(res.getString("Map"), true);
		Checkbox cbSaveLegend = new Checkbox(res.getString("Legend"), true);
		Checkbox cbTogether = new Checkbox(res.getString("together"), false);
		Panel pSaveWhat = new Panel(new GridLayout(5, 1, 0, 0));
		pSaveWhat.add(new Label(res.getString("Select_what_to_save_"), Label.CENTER));
		pSaveWhat.add(cbSaveMap);
		pSaveWhat.add(cbSaveLegend);
		Panel ptg = new Panel();
		ptg.setLayout(new BorderLayout());
		Panel tab = new Panel();
		ptg.add(tab, "West");
		ptg.add(cbTogether, "Center");
		pSaveWhat.add(ptg);

		OKDialog okd = new OKDialog(supervisor.getUI().getMainFrame(), "Export", true);
		okd.addContent(pSaveWhat);
		okd.show();
		if (okd.wasCancelled())
			return;

		if (!cbSaveMap.getState() && !cbSaveLegend.getState() && !cbTogether.getState())
			return;

		if (print) {
			openPrintJob();
			if (pj == null)
				return;
		} else {
			targetSize = new Dimension(600, 800);
		}

		Image img;
		if (cbSaveMap.getState() && cbSaveLegend.getState() && cbTogether.getState()) {
			img = map.getMapAndLegendAsImage(targetSize);
			images.addElement(img);
			names.addElement("Map with legend " + map.getInstanceN());
			ids.addElement(StringUtil.makeID((String) names.lastElement()));
		} else {
			if (cbSaveMap.getState()) {
				img = map.getMapAsImage(targetSize);
				images.addElement(img);
				names.addElement("Map " + map.getInstanceN());
				ids.addElement(StringUtil.makeID((String) names.lastElement()));
			}
			if (cbSaveLegend.getState()) {
				img = map.getLegendAsImage(targetSize);
				images.addElement(img);
				names.addElement("Legend " + map.getInstanceN());
				ids.addElement(StringUtil.makeID((String) names.lastElement()));
			}
		}

		if (print) {
			printImages(false);
			pj.end();
		} else {
			saveImages(false);
		}
	}
}
