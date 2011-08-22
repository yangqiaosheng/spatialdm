package data_load.readers;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.GeoDataReader;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.lib.util.CopyFile;
import spade.vis.database.DataPortion;
import spade.vis.database.DataSupplier;
import spade.vis.database.LayerData;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.geometry.ImageGeometry;
import spade.vis.geometry.RealRectangle;
import spade.vis.spec.DataSourceSpec;

/**
* Gets an image from a GIF or JPG file
* Support of TIFF images added by P.G.
*/
public class ImageReader extends BaseDataReader implements GeoDataReader, DataSupplier {
	static ResourceBundle res = Language.getTextResource("data_load.readers.Res");
	/**
	* The spatial data loaded
	*/
	protected LayerData data = null;
	boolean JIMILibraryAvailable = false;

	/**
	* Loads the data. When allowed (the argument mayAskUser is true) and necessary,
	* starts a dialog or a wizard for the user
	* to specify the data source, constraints, etc. Returns true if the data
	* have been successfully loaded.
	*/
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

		// Support of reading of JIMI-MIME-Types images  (P.G.)
		Class classJIMI = null;
		try {
			classJIMI = Class.forName("com.sun.jimi.core.Jimi");
		} catch (Exception e) {
			System.out.println("JIMI Pro library not found in the CLASSPATH!");
			//e.printStackTrace();
		}
		JIMILibraryAvailable = classJIMI != null;
		// System.out.println("JIMI Library "+(!JIMILibraryAvailable? "not":"")+" found!");
		// System.out.println("TIFF encoder "+(!tiffReaderAvailable? "not":"")+" found!");
		//System.out.println("Available image readers:");
		String availableFileTypes = "";
		String[] availableJIMIFileTypes = null;
		Vector availableJIMIFileTypes2DOS = null;

		if (JIMILibraryAvailable) {
			availableJIMIFileTypes = com.sun.jimi.core.Jimi.getDecoderTypes();
			if (availableJIMIFileTypes != null) {
				int MIMETypeSeparatorIdx = -1;
				int MIMETypeXSeparatorIdx = -1;
				String currentMIMEType = null;
				for (String availableJIMIFileType : availableJIMIFileTypes) {
					currentMIMEType = availableJIMIFileType;
					if (currentMIMEType != null) {
						// System.out.println(currentMIMEType);
						MIMETypeSeparatorIdx = currentMIMEType.indexOf('/');
						MIMETypeXSeparatorIdx = currentMIMEType.lastIndexOf('-');
						if (MIMETypeXSeparatorIdx > 0) {
							MIMETypeSeparatorIdx = MIMETypeXSeparatorIdx;
						}
					}
					if (availableJIMIFileTypes2DOS == null) {
						availableJIMIFileTypes2DOS = new Vector(availableJIMIFileTypes.length);
					}
					String extensionCurrent = currentMIMEType.substring(MIMETypeSeparatorIdx + 1);
					if (extensionCurrent != null && !availableJIMIFileTypes2DOS.contains(extensionCurrent)) {
						availableJIMIFileTypes2DOS.addElement(extensionCurrent);
					}
				}
				if (!availableJIMIFileTypes2DOS.isEmpty()) {
					if (!availableJIMIFileTypes2DOS.contains("tif") && availableJIMIFileTypes2DOS.contains("tiff")) {
						availableJIMIFileTypes2DOS.addElement("tif");
					}
					if (!availableJIMIFileTypes2DOS.contains("tga") && availableJIMIFileTypes2DOS.contains("targa")) {
						availableJIMIFileTypes2DOS.addElement("tga");
					}
					if (!availableJIMIFileTypes2DOS.contains("ras") && availableJIMIFileTypes2DOS.contains("raster")) {
						availableJIMIFileTypes2DOS.addElement("ras");
					}
					if (!availableJIMIFileTypes2DOS.contains("pct") && availableJIMIFileTypes2DOS.contains("pict")) {
						availableJIMIFileTypes2DOS.addElement("pct");
					}
					StringBuffer sbFileTypes = new StringBuffer("*." + (String) availableJIMIFileTypes2DOS.elementAt(0));
					for (int i = 1; i < availableJIMIFileTypes2DOS.size(); i++) {
						sbFileTypes.append((";*." + (String) availableJIMIFileTypes2DOS.elementAt(i)));
					}
					availableFileTypes = sbFileTypes.toString();
				}
				// System.out.println(availableFileTypes);
			}
		} else {
			availableFileTypes = "*.jpg;*.jpeg;*.gif";
		}
		// ~P.G.
		if (spec == null || spec.source == null)
			if (mayAskUser) {
				//following text:"Select the file with geographical data"
				String path = browseForFile(res.getString("Select_the_file_with3"), availableFileTypes);
				if (path == null) {
					setDataReadingInProgress(false);
					return false;
				}
				if (spec == null) {
					spec = new DataSourceSpec();
				}
				spec.source = path;
			} else {
				//following text:"The data source for layer is not specified!"
				showMessage(res.getString("The_data_source_for"), true);
				setDataReadingInProgress(false);
				return false;
			}
		if (spec.name == null) {
			spec.name = CopyFile.getName(spec.source);
		}
		if (spec.bounds == null || spec.bounds.size() < 1) {
			//need the bounding rectangle
			if (!mayAskUser) {
				//following text:"The bounds of layer "
				//following text:" are not specified!"
				showMessage(res.getString("The_bounds_of_layer") + spec.name + res.getString("are_not_specified_"), true);
				setDataReadingInProgress(false);
				return false;
			}
			//run a dialog for specifying the bounds
			GetBoundsPanel gbp = new GetBoundsPanel();
			//following text:"Bounds of "
			OKDialog okd = new OKDialog(getFrame(), res.getString("Bounds_of") + spec.name, true);
			okd.addContent(gbp);
			okd.show();
			if (okd.wasCancelled()) {
				setDataReadingInProgress(false);
				return false;
			}
			RealRectangle rr = new RealRectangle();
			rr.rx1 = gbp.x1;
			rr.rx2 = gbp.x2;
			rr.ry1 = gbp.y1;
			rr.ry2 = gbp.y2;
			if (spec.bounds == null) {
				spec.bounds = new Vector(1, 1);
			}
			spec.bounds.addElement(rr);
		}
		//following text:"Start loading image from
		showMessage(res.getString("Start_loading_image") + spec.source, false);
		data = readSpecific();
		dataError = data == null;
		setDataReadingInProgress(false);
		return !dataError;
	}

	protected LayerData readSpecific() {
		if (spec == null || spec.source == null || spec.bounds == null || spec.bounds.size() < 1)
			return null;
		RealRectangle bounds = (RealRectangle) spec.bounds.elementAt(0);
		if (bounds == null)
			return null;
		int idx = spec.source.indexOf(':');
		boolean isURL = false;
		if (idx > 0) {
			String pref = spec.source.substring(0, idx);
			if (pref.equalsIgnoreCase("HTTP") || pref.equalsIgnoreCase("FILE")) {
				isURL = true;
			}
		}
		Image img = null;
		try {
			if (isURL) { //try to access the source as a URL
				URL url = new URL(spec.source);
				// P.G.
				if (JIMILibraryAvailable) {
					img = com.sun.jimi.core.Jimi.getImage(url);
				} else {
					img = Toolkit.getDefaultToolkit().getImage(url);
				}
			} else {
				if (JIMILibraryAvailable) {
					img = com.sun.jimi.core.Jimi.getImage(spec.source);
				} else {
					img = Toolkit.getDefaultToolkit().getImage(spec.source);
				}
			}
			// ~P.G.
		} catch (IOException ioe) {
			//following text:"Error accessing "
			showMessage(res.getString("Error_accessing") + spec.source + ": " + ioe, true);
			return null;
		}
		if (img == null) {
			//following text:"Cannot load an image from "
			showMessage(res.getString("Cannot_load_an_image") + spec.source, true);
			return null;
		}

		LayerData data = new LayerData();
		data.setBoundingRectangle(bounds.rx1, bounds.ry1, bounds.rx2, bounds.ry2);
		ImageGeometry geom = new ImageGeometry();
		geom.img = img;
		geom.rx1 = bounds.rx1;
		geom.ry1 = bounds.ry1;
		geom.rx2 = bounds.rx2;
		geom.ry2 = bounds.ry2;
		//following text:"image"
		SpatialEntity spe = new SpatialEntity("image");
		spe.setGeometry(geom);
		data.addDataItem(spe);
		//following text:"The image has been loaded from "
		showMessage(res.getString("The_image_has_been") + spec.source, false);
		data.setHasAllData(true);
		return data;
	}

	/**
	* Returns the map layer constructed from the geographical data loaded (if any).
	* If the data have not been loaded yet, the reader constructs a layer without
	* objects and sets itself as the  data supplier of the layer. Then the data
	* will be loaded when the layer is first drawn.
	*/
	@Override
	public DGeoLayer getMapLayer() {
		DGeoLayer layer = new DGeoLayer();
		layer.setDataSource(spec);
		if (spec.id != null) {
			layer.setContainerIdentifier(spec.id);
		}
		if (spec.name != null) {
			layer.setName(spec.name);
		} else if (spec.source != null) {
			layer.setName(CopyFile.getName(spec.source));
		}
		if (data != null) {
			layer.receiveSpatialData(data);
		} else {
			layer.setDataSupplier(this);
		}
		return layer;
	}

//----------------- DataSupplier interface -----------------------------------
	/**
	* Returns the SpatialDataPortion containing all DataItems available
	*/
	@Override
	public DataPortion getData() {
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
		return getData();
	}

	/**
	* When no more data from the DataSupplier are needed, this method is
	* called. Here the DataSupplier can clear its internal structures
	*/
	@Override
	public void clearAll() {
		data = null;
	}
}
