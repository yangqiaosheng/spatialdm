package data_load.readers;

import java.awt.Image;
import java.awt.Label;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.analysis.system.GeoDataReader;
import spade.lib.basicwin.GetPathDlg;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.CopyFile;
import spade.vis.database.DataPortion;
import spade.vis.database.DataSupplier;
import spade.vis.database.LayerData;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.ImageGeometry;
import spade.vis.geometry.RealRectangle;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Fraunhofer AIS.SPADE</p>
 * @author not attributable
 * @version 1.0
 */

public class MultiImageReader extends DataStreamReader implements GeoDataReader, DataSupplier {

	protected LayerData data = null;
	protected DGeoLayer layer = null;
	protected Vector index = null;
	protected RealRectangle extent = null;

	public static String imelist = "gif,jpg,jpeg";

	protected static Hashtable cash;
	protected static Vector history;
	protected static long cashCurrSize = 0L;
	public static long cashMaxSize = 21; // in millionen pixels
	protected static long instanceCounter = 0L;
	protected String setID = "";

	public MultiImageReader() {
		setID = "SET" + (++instanceCounter) + "_";
	}

	@Override
	public boolean loadData(boolean mayAskUser) {
		if (dataError)
			return false;
		if (dataReadingInProgress) {
			waitDataReadingFinish();
			return !dataError;
		}
		if (!mayAskUser) {
			setDataReadingInProgress(true);
		}
		if (mayAskUser) {
			//following text:"Select the file with the table"
			OKDialog dlg = new OKDialog(getFrame(), "", OKDialog.YES_NO_MODE, true);
			dlg.addContent(new Label(res.getString("make_new_index_file")));
			dlg.show();
			boolean toLoad = dlg.wasCancelled();

			String path = browseForFile(res.getString("Select_the_file_with1"), "*.mim", toLoad);
			System.out.println("Path=" + path);
			if (path == null) {
				setDataReadingInProgress(false);
				return false;
			}
			spec = new spade.vis.spec.DataSourceSpec();
			spec.source = path;
			if (!toLoad) {
				//make or update mim-file with tfw
				if (!makeIndexFile(path)) {
					dataError = true;
					setDataReadingInProgress(false);
					return false;
				}
				index = null;
			}
		}

		if (spec.name == null) {
			spec.name = CopyFile.getName(spec.source);
		}

		showMessage(res.getString("Start_loading_image") + spec.source, false);
		data = readSpecific(null);
		dataError = data == null;
		setDataReadingInProgress(false);
		return !dataError;
	}

	protected void readIndex() {
		index = null;
		extent = null;
		openStream();
		if (stream == null)
			return;
		index = new Vector();
		extent = new RealRectangle();
		BufferedReader rd = new BufferedReader(new InputStreamReader(stream));
		String str = null;
		try {
			while ((str = rd.readLine()) != null) {
				StringTokenizer tz = new StringTokenizer(str + ",", ",");

				String sa[] = new String[6];
				for (int i = 0; i < sa.length && tz.hasMoreTokens(); i++) {
					sa[i] = tz.nextToken().trim();
				}
				RealRectangle rr = null;
				Long so = null;
				try {
					rr = new RealRectangle();
					rr.rx1 = Float.valueOf(sa[0]).floatValue();
					rr.ry1 = Float.valueOf(sa[1]).floatValue();
					rr.rx2 = Float.valueOf(sa[2]).floatValue();
					rr.ry2 = Float.valueOf(sa[3]).floatValue();
					if (sa[5] != null) {
						so = Long.valueOf(sa[5]);
					}

				} catch (NumberFormatException nfe) {
					System.out.println("can not read (nfe) in file: " + spec.source);
					throw new Exception("nfe");
				}

				if (rr != null) {

					Object o[] = new Object[3];
					o[0] = rr;
					o[1] = sa[4];
					o[2] = so;
					index.addElement(o);

					if (Float.isNaN(extent.rx1) || extent.rx1 > rr.rx1) {
						extent.rx1 = rr.rx1;
					}
					if (Float.isNaN(extent.ry1) || extent.ry1 > rr.ry1) {
						extent.ry1 = rr.ry1;
					}
					if (Float.isNaN(extent.rx2) || extent.rx2 < rr.rx2) {
						extent.rx2 = rr.rx2;
					}
					if (Float.isNaN(extent.ry2) || extent.ry2 < rr.ry2) {
						extent.ry2 = rr.ry2;
					}

				}

			}
			System.out.println(index.size() + " records are recieved for multi-image index.");
		} catch (Exception ex) {
			index = null;
			dataError = true;

		}

	}

	protected LayerData readSpecific(Vector bounds) {
		if (spec == null || spec.source == null)
			return null;
		if (index == null) {
			readIndex();
		}
		if (index == null)
			return null;

		data = new LayerData();
		data.setBoundingRectangle(extent.rx1, extent.ry1, extent.rx2, extent.ry2);

		showMessage(res.getString("The_image_has_been") + spec.source, false);
		data.addDataItem(null); // no thematic data !
		data.setHasAllData(false);

		if (bounds == null && extent != null) {
			bounds = new Vector(1, 1);
			bounds.addElement(extent);
		}
		if (bounds == null)
			return data;
		if (bounds.size() == 0)
			return data;
		RealRectangle bn = (RealRectangle) bounds.elementAt(0);

		/// loading
		int cnt = 0;
		for (int i = 0; i < index.size(); i++) {
			Object o[] = (Object[]) index.elementAt(i);
			RealRectangle rr = (RealRectangle) o[0];
			String id = (String) o[1];

			if (bn.intersect(rr) != null && cnt < 10) {
				cnt++;
				ImageGeometry geom = new ImageGeometry();
				geom.img = getFromCash(id);
				//spade.vis.space.GeoObject go = layer.findObjectById(id);
				//if (go == null) {
				if (geom.img == null) {
					String dir = CopyFile.getDir(spec.source);
					String tk = dir.substring(0, dir.length() - 1) + "/" + id;
					//System.out.println("-!-!-!- Get image from "+tk);
					if (tk.startsWith("http:") || tk.startsWith("HTTP:") || tk.startsWith("file:") || tk.startsWith("FILE:")) {
						try {
							URL url = new URL(tk);
							geom.img = Toolkit.getDefaultToolkit().getImage(url);
							addInCash(id, geom.img, (Long) o[2]);
						} catch (MalformedURLException mfe) {
						}
					}
					if (geom.img == null) {
						try {
							geom.img = Toolkit.getDefaultToolkit().getImage(tk);
							addInCash(id, geom.img, (Long) o[2]);

						} catch (Throwable thr) {
							thr.printStackTrace();
						}
						//System.out.println("-!-!-! after Toolkit.getImage(" + tk + ")");
					}
				} else {
					//history.remove(id);
					//history.addElement(id);
				}

				//  geom = (ImageGeometry) ( (DGeoObject) go).getGeometry();
				//}
				if (geom == null) {
					continue;
				}

				geom.rx1 = rr.rx1;
				geom.ry1 = rr.ry1;
				geom.rx2 = rr.rx2;
				geom.ry2 = rr.ry2;
				SpatialEntity spe = new SpatialEntity(id);
				spe.setGeometry(geom);
				data.addItemSimple(spe);

			}

		}

		if (data.getDataItemCount() == 0)
			return null;
		return data;

	}

	@Override
	public DGeoLayer getMapLayer() {
		if (layer != null)
			return layer;
		if (index == null) {
			readIndex();
		}
		if (index == null)
			return null;

		layer = new DGeoLayer();
		layer.setType(Geometry.image);
		layer.setDataSource(spec);

		if (spec.name != null) {
			layer.setName(spec.name);
		} else {
			layer.setName(spec.source);
		}

		DataSupplier ds = this;
		/*
		data_load.DataAgent dataBroker=null;
		try {
		      dataBroker=(data_load.DataAgent)Class.forName("data_load.cache.DataBroker").newInstance();
		} catch (Exception e) {}
		if (dataBroker!=null && index!=null && index.size() >0 && extent != null ){
		  dataBroker.setDataReader(this);
		        dataBroker.setUI(ui);
		        dataBroker.init(extent,index.size());
		        ds=dataBroker;

		}
		*/
		layer.setDataSupplier(ds);
		return layer;
	}

	//----------------- DataSupplier interface -----------------------------------
	/**
	 * Returns the SpatialDataPortion containing all DataItems available
	 */
	@Override
	public DataPortion getData() {
		System.out.println("DataSuppl:getData()");
		if (data != null)
			return data;
		if (dataError)
			return null;
		if (loadData(false))
			return data;
		return null;
	}

	/**
	 * Constructs and returns a DataPortion containing DataItems fitting in at
	 * least one of the specified bounding rectangles (listed in the vector).
	 * The elements of the vector are instances of
	 * @see spade.vis.geometry.RealRectangle
	 * Readers from files do not filter data according to any query,
	 * therefore the method getData() without arguments is called
	 */
	@Override
	public DataPortion getData(Vector bounds) {
		System.out.println("DataSuppl:getData(bounds)");
		if (bounds == null)
			return getData();
		//if(data != null) return data;
		if (dataError)
			return null;
		return readSpecific(bounds);
	}

	/**
	 * When no more data from the DataSupplier are needed, this method is
	 * called. Here the DataSupplier can clear its internal structures
	 */
	@Override
	public void clearAll() {
		System.out.println("DataSuppl:clearAll():" + setID);
		data = null;

		if (cash != null && history != null) {
			Vector ids = new Vector();
			for (int i = 0; i < history.size(); i++) {
				String id = (String) history.elementAt(i);
				int p = id.indexOf("_");
				if (p > 0) {
					if (id.substring(0, p + 1).equals(setID)) {
						ids.addElement(id);
					}
				}

			}

			for (int i = 0; i < ids.size(); i++) {
				String id = (String) ids.elementAt(i);
				cash.remove(id);
				history.removeElement(id);
			}

			System.out.println(ids.size());
		}
	}

	/**
	 * create oder replace index file
	 * use files in directory of path with tfw-extension und
	 * image files with this name
	 * @param path String
	 */
	public boolean makeIndexFile(String path) {
		String pathdir = CopyFile.getDir(path);
		try {
			FileWriter wr = new FileWriter(new File(pathdir, "temp.txt"));
			makeIndexFile(wr, pathdir, "");
			wr.close();
		} catch (Exception ex) {
			System.out.println("IOException:" + ex.getMessage());
			showMessage(res.getString("Error_accessing"), true);
			return false;
		}

		boolean b = true;
		File fm = new File(path);
		if (fm.exists()) {
			b = fm.delete();
		}
		if (!b) {
			System.out.println("can't delete file: " + fm.getAbsolutePath());
			showMessage(res.getString("Error_accessing"), true);
			return false;
		}
		fm = new File(pathdir, "temp.txt");
		b = fm.renameTo(new File(path));
		if (!b) {
			System.out.println("can't rename file: " + fm.getAbsolutePath() + " to: " + path);
			showMessage(res.getString("Error_accessing"), true);
			return false;
		}

		return true;
	}

	protected void makeIndexFile(FileWriter wr, String base, String dirpath) throws Exception {

		System.out.println("base:" + base + " dirpath:" + dirpath);
		File dir = new File(base + dirpath);
		String[] list = dir.list();
		String slash = dirpath.equals("") ? "" : "/";
		for (String element : list) {
			dir = new File(base + dirpath + element);
			if (dir.isDirectory()) {
				makeIndexFile(wr, base, dirpath + slash + element);
				continue;
			}

			String ext = CopyFile.getExtension(element);
			if (ext != null && ext.equalsIgnoreCase("tfw")) {
				File tfw = new File(base + dirpath, element);
				File img = null;
				StringTokenizer tz = new StringTokenizer(imelist + ",", ",");
				String imgext = null;
				while (tz.hasMoreTokens()) {
					imgext = tz.nextToken();
					img = new File(base + dirpath, CopyFile.getNameWithoutExt(element) + "." + imgext);

					if (img.exists()) {
						//System.out.println(img.getAbsolutePath());
						break;
					} else {
						img = null;
					}
				}
				if (img == null) {
					System.out.println("image with name is not found:" + dirpath + slash + CopyFile.getNameWithoutExt(element));
					continue;
				}

				Image image = Toolkit.getDefaultToolkit().getImage(img.getAbsolutePath());

				int tm = 0;
				try {
					while (image.getHeight(null) < 0 || image.getHeight(null) < 0) {
						Thread.sleep(5);
						tm++;
						if (tm >= 1000) {
							System.out.println("time out to create image and width and height are unknown:\n    " + img.getAbsolutePath());
							tm = -1;
							break;

						}
					}
				} catch (InterruptedException iex) {
				}

				if (tm < 0) {
					continue;
				}

				double tfwdata[] = readTFW(tfw.getAbsolutePath());

				if (tfwdata != null) {
					double md = tfwdata[0];
					double mx = tfwdata[4];
					double my = tfwdata[5];

					int wd = image.getWidth(null);
					int hg = image.getHeight(null);
					img.getName();

					wr.write((mx) + "," + (my - md * hg) + "," + (mx + md * wd) + "," + (my) + "," + dirpath + slash + img.getName() + "," + ((long) wd * (long) hg) +

					"\n");
					showMessage(dirpath + slash + img.getName(), false);

				}
			}
		}
	}

	public double[] readTFW(String path) {
		double tfw[] = new double[6];
		char cb[] = new char[1024];
		try {

			FileReader rd = new FileReader(path);
			int n = rd.read(cb);
			StringTokenizer tz = new StringTokenizer(new String(cb, 0, n), "\n");
			for (int i = 0; i < 6; i++) {
				if (tz.hasMoreTokens()) {
					try {
						tfw[i] = Double.valueOf(tz.nextToken().trim()).doubleValue();
					} catch (NumberFormatException nfe) {
						tfw[i] = Double.NaN;
					}
				} else {
					tfw[i] = Double.NaN;
				}
			}

			rd.close();

		} catch (Exception e) {
			System.out.println(e.getMessage());
			return null;
		}
		return tfw;
	}

	/**
	* Runs a dialog in which the user can specify the file or URL with data
	* to be loaded
	*/
	protected String browseForFile(String dialogTitle, String fileMask, boolean toLoad) {
		GetPathDlg fd = new GetPathDlg(getFrame(), dialogTitle, toLoad);
		if (fileMask != null) {
			fd.setFileMask(fileMask);
		}
		if (dir != null) {
			fd.setDirectory(dir);
		}
		fd.show();
		dir = fd.getDirectory();
		return fd.getPath();
	}

	protected void addInCash(String id, Image img, Long sizeObj) {
		if (sizeObj == null) {
			sizeObj = new Long(1000000L);
		}
		if (cash == null) {
			cash = new Hashtable();
		}
		if (history == null) {
			history = new Vector();
		}
		System.out.println("add-in-cash(" + cash.size() + ") " + setID + id);
		//System.out.println("!-!-!-! cash:" + cash.size());
		while (true) {
			if (history.size() > 2 && cashCurrSize > cashMaxSize * 1000000L) {
				String s = (String) history.elementAt(0);
				history.removeElementAt(0);
				Object o[] = (Object[]) cash.get(s);
				cash.remove(s);
				cashCurrSize -= ((Long) o[1]).longValue();
			} else {
				break;
			}
		}
		cash.put(setID + id, new Object[] { img, sizeObj });
		cashCurrSize += sizeObj.longValue();
		history.addElement(setID + id);
	}

	protected Image getFromCash(String id) {
		if (cash == null || history == null)
			return null;
		System.out.println("get-from-cash(" + cash.size() + ") " + setID + id);
		Object o[] = (Object[]) cash.get(setID + id);
		if (o == null)
			return null;
		history.removeElement(setID + id);
		history.addElement(setID + id);
		return (Image) o[0];
	}

}
