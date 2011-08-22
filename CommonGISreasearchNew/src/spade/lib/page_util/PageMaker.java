package spade.lib.page_util;

import java.awt.FileDialog;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.lib.help.Helper;
import spade.lib.util.StringUtil;
import ui.ImagePrinter;
import core.ActionDescr;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 11, 2009
 * Time: 11:19:18 AM
 * Used for making HTML pages
 */
public class PageMaker {

	protected ESDACore core = null;
	/**
	 * The path to the current application
	 */
	public String applPath = null;

	protected String sLFpath = null, // folder for saving
			sLFdt = null, // date/time of last saving
			sLFdt_fmt = null; // ... - file name prefix

	public String getLFpath() {
		return sLFpath;
	}

	public String getDTstring() {
		return sLFdt_fmt;
	}

	public String getDTfname() {
		return sLFdt;
	}

	public PageMaker(ESDACore core) {
		this.core = core;
	}

	/**
	 * Produces HTML pages according to the structure defined.
	 */
	private void createFolder() {
		sLFpath = core.getDataKeeper().getApplicationPath();
		if (sLFpath == null) {
			FileDialog fd = new FileDialog(core.getUI().getMainFrame(), "Where to store the pages?", FileDialog.SAVE);
			fd.setVisible(true);
			sLFpath = fd.getDirectory();
			String fname = fd.getFile();
			if (fname != null && fname.length() > 0)
				if (sLFpath != null) {
					sLFpath += /*"\\"*/System.getProperty("file.separator") + fname;
				} else {
					sLFpath = fname;
				}
			fd.dispose();
			if (sLFpath == null) {
				core.getUI().showMessage("Could not get a directory for storing the pages!", true);
				return;
			}
		}
		if (sLFdt == null) {
			composeDTstr();
		}
		sLFpath = sLFpath.substring(0, sLFpath.lastIndexOf(System.getProperty("file.separator")) + 1) + sLFdt;
		new File(sLFpath).mkdir();
		sLFpath += System.getProperty("file.separator");
	}

	public void makePages(PageCollection pc) {
		//create folder for HTML etc.
		if (sLFpath == null) {
			createFolder();
		}
		// print the pages to files
		for (int i = 0; i < pc.getPageCount(); i++) {
			makeOnePage(pc.pages.elementAt(i));
		}
		// add printing event to the logged actions list
		Vector<ActionDescr> loggedActions = core.getLoggedActions();
		try {
			FileOutputStream out = new FileOutputStream(sLFpath + "index.html");
			DataOutputStream dos = new DataOutputStream(out);
			dos.writeBytes("<HTML>\r\n<HEAD><TITLE>" + sLFdt + "</TITLE></HEAD>\r\n<BODY>\r\n");
			for (int i = 0; i < loggedActions.size(); i++) {
				dos.writeBytes("<P>" + addBRtoString(loggedActions.elementAt(i).getDescription()) + "\r\n");
			}
			dos.writeBytes("</BODY></HTML>\r\n");
			out.close();
		} catch (IOException ioe) {
			System.err.println("File index.html writing error " + ioe.toString());
		}
		// load index.html to browser window
		Helper.showPage(sLFpath + "index.html", sLFpath + "index.html");
	}

	private void addImagesToVectors(PageElementMultiple pem, Vector vImages, Vector vFnames) {
		for (int i = 0; i < pem.getItemCount(); i++)
			if (pem.getItem(i) instanceof PageElementImage && ((PageElementImage) pem.getItem(i)).image != null) {
				PageElementImage pei = (PageElementImage) pem.getItem(i);
				vImages.addElement(pei.image);
				vFnames.addElement(pei.fname);
			} else if (pem.getItem(i) instanceof PageElementMultiple) {
				addImagesToVectors((PageElementMultiple) pem.getItem(i), vImages, vFnames);
			}
	}

	protected String addBRtoString(String in) {
		String out = in;
		out = out.replace("\r\n", "<BR>\r\n");
		return out;
	}

	protected void makeOnePage(PageStructure ps) {
		// create all needed images, if any
		ImagePrinter impr = new ImagePrinter(core.getSupervisor());
		Vector vImages = new Vector(10, 10), vFnames = new Vector(10, 10);
		for (int i = 0; i < ps.getElementCount(); i++)
			if (ps.getElement(i) instanceof PageElementImage && ((PageElementImage) ps.getElement(i)).image != null) {
				PageElementImage pei = (PageElementImage) ps.getElement(i);
				vImages.addElement(pei.image);
				if (pei.fname == null) {
					pei.fname = "img" + i;
				}
				vFnames.addElement(pei.fname);
				if (!Float.isNaN(pei.x1)) {
					File f = new File(sLFpath + sLFdt + "_" + pei.fname + "_extent.txt");
					try {
						if (f.createNewFile()) {
							BufferedWriter wr = new BufferedWriter(new FileWriter(f));
							wr.write("x1=" + pei.x1 + "\r\n");
							wr.write("y1=" + pei.y1 + "\r\n");
							wr.write("x2=" + pei.x2 + "\r\n");
							wr.write("y2=" + pei.y2 + "\r\n");
							wr.close();
						}
					} catch (IOException e) {
					}
				}
			} else if (ps.getElement(i) instanceof PageElementMultiple) {
				addImagesToVectors((PageElementMultiple) ps.getElement(i), vImages, vFnames);
			}
		if (vImages.size() > 0) {
			impr.setImages(vImages, vFnames);
			impr.saveImages(false, "png", sLFpath + sLFdt + "_");
		}
		// make HTML
		try {
			FileOutputStream out = new FileOutputStream(sLFpath + sLFdt + ps.fname + ".html");
			DataOutputStream dos = new DataOutputStream(out);
			dos.writeBytes("<HTML>\r\n<HEAD><TITLE>" + ps.title + "</TITLE></HEAD>\r\n<BODY>\r\n");
			if (ps.header != null) {
				dos.writeBytes("<p>" + addBRtoString(ps.header) + "</p>\r\n");
			}
			switch (ps.layout) {
			case PageStructure.LAYOUT_TABLE:
				// ToDo
				break;
			case PageStructure.LAYOUT_2_COLUMNS:
				dos.writeBytes("<TABLE WIDTH=100%><TR>\r\n<TD ALIGN=CENTER></TD><TD></TD></TR>\r\n<TR><TD VALIGN=TOP>\r\n"); // outer table
				for (int i = 0; i < ps.getElementCount() - 1; i++) {
					dos.writeBytes("<P>\r\n");
					makeOnePageElement(dos, ps.getElement(i));
					dos.writeBytes("</P>\r\n");
				}
				dos.writeBytes("</TD>\r\n"); // outer table
				dos.writeBytes("<TD VALIGN=TOP>\r\n"); // (right column)
				makeOnePageElement(dos, ps.getElement(ps.getElementCount() - 1));
				dos.writeBytes("</TD></TR></TABLE>\r\n"); // outer table
				break;
			default: // PageStructure.LAYOUT_COLUMN
				for (int i = 0; i < ps.getElementCount(); i++) {
					dos.writeBytes("<P>\r\n");
					makeOnePageElement(dos, ps.getElement(i));
					dos.writeBytes("</P>\r\n");
				}
				break;
			}
			if (ps.footer != null) {
				dos.writeBytes("<P>" + addBRtoString(ps.footer) + "</P>\r\n");
			}
			dos.writeBytes("</BODY></HTML>\r\n");
			out.close();
		} catch (IOException ioe) {
			System.err.println("File index.html writing error " + ioe.toString());
		}
	}

	protected void makeOnePageElement(DataOutputStream dos, PageElement pe) {
		try {
			if (pe.header != null) {
				dos.writeBytes(addBRtoString(pe.header) + "<BR>\r\n");
			}
			if (pe.pageRef != null) {
				dos.writeBytes("<A HREF=\"" + sLFdt + pe.pageRef.fname + ".html\">\r\n");
			}
			if (pe instanceof PageElementText) {
				PageElementText pet = (PageElementText) pe;
				dos.writeBytes(addBRtoString(pet.text) + "<BR>\r\n");
			} else if (pe instanceof PageElementImage) {
				PageElementImage pei = (PageElementImage) pe;
				dos.writeBytes("<IMG SRC=\"" + sLFdt + "_" + pei.fname + ".png\">\r\n"); // image: specimens of all clusters
			} else if (pe instanceof PageElementTable) {
				dos.writeBytes("<TABLE BORDER=1>\r\n"); // inner table: statistics of the subclusters
				PageElementTable pet = (PageElementTable) pe;
				for (int nr = 0; nr < pet.getNRows(); nr++) {
					dos.writeBytes("<TR ALIGN=" + ((nr == 0) ? "CENTER" : "RIGHT") + ">\r\n");
					for (int nc = 0; nc < pet.getNColumns(); nc++) {
						String str = pet.texts[nr][nc];
						str = str.replace("\r\n", "<BR>\r\n");
						dos.writeBytes("<TD>" + str + "</TD>\r\n");
					}
					dos.writeBytes("</TR>\r\n");
				}
				dos.writeBytes("</TABLE>\r\n"); // inner table: statistics of the subclusters
			} else if (pe instanceof PageElementMultiple) {
				PageElementMultiple pem = (PageElementMultiple) pe;
				int n = 0;
				dos.writeBytes("<TABLE><TR>\r\n"); // inner table
				for (int i = 0; i < pem.getItemCount(); i++) {
					dos.writeBytes("<TD>\r\n");
					makeOnePageElement(dos, pem.getItem(i));
					dos.writeBytes("</TD>\r\n");
					n++;
					if (n == pem.nColumns) {
						dos.writeBytes("</TR>\r\n");
						dos.writeBytes("<TR>\r\n");
						n = 0;
					}
				}
				dos.writeBytes("</TR></TABLE>\r\n"); // inner table
			}
			if (pe.pageRef != null) {
				dos.writeBytes("</A>\r\n");
			}
			if (pe.footer != null) {
				dos.writeBytes("<BR>" + addBRtoString(pe.footer) + "\r\n");
			}
		} catch (IOException ioe) {
			System.err.println("File index.html writing error " + ioe.toString());
		}
	}

	/**
	 * used for composing filenames (sLFdt) from current date & time
	 * and human-readable timestamps (sLFdt_fmt)
	 */
	public void composeDTstr() {
		if (sLFpath == null) {
			createFolder();
		}
		GregorianCalendar date = new GregorianCalendar();
		sLFdt = StringUtil.padString(String.valueOf(date.get(Calendar.YEAR)), '0', 4, true) + StringUtil.padString(String.valueOf(date.get(Calendar.MONTH) + 1), '0', 2, true)
				+ StringUtil.padString(String.valueOf(date.get(Calendar.DAY_OF_MONTH)), '0', 2, true) + "_" + StringUtil.padString(String.valueOf(date.get(Calendar.HOUR_OF_DAY)), '0', 2, true)
				+ StringUtil.padString(String.valueOf(date.get(Calendar.MINUTE)), '0', 2, true) + StringUtil.padString(String.valueOf(date.get(Calendar.SECOND)), '0', 2, true);
		sLFdt_fmt = StringUtil.dateTimeToString(date);
	}

}
