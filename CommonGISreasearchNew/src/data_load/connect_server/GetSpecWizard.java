package data_load.connect_server;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.analysis.system.AttrDataReader;
import spade.analysis.system.DataReader;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.lib.util.CopyFile;
import spade.lib.util.StringUtil;
import spade.vis.geometry.RealRectangle;
import spade.vis.spec.DataSourceSpec;
import data_load.DataReaderRegister;

/**
* To be used for acquiring the specification of data to be loaded
* using a data server
*/
public class GetSpecWizard implements ActionListener {
	static ResourceBundle res = Language.getTextResource("data_load.connect_server.Res");
	/**
	* Store the Data Server URL, the database URL, the user name, and the
	* password (if appropriate) from the previous call of the wizard
	*/
	protected static String dataServerURL = null, dbURL = null, userName = null, dbPassword = null;;
	/**
	* The dialog used for asking the user
	*/
	protected OKDialog dia = null;

	/**
	* Returns true if the given string is "JDBC" or "ODBC" (irrespective of the case)
	*/
	public static boolean isJDBC(String format) {
		if (format == null)
			return false;
		return format.equalsIgnoreCase("JDBC") || format.equalsIgnoreCase("ODBC");
	}

	/**
	* Returns true if the given string is "JDBC", "ODBC", or Oracle (irrespective
	* of the case)
	*/
	public static boolean isDB(String format) {
		if (format == null)
			return false;
		return format.equalsIgnoreCase("Oracle") || isJDBC(format);
	}

	/**
	* Returns true if the given string is "ASCII", "CSV", or "TXT" (irrespective
	* of the case)
	*/
	public static boolean isASCII(String format) {
		if (format == null)
			return false;
		return format.equalsIgnoreCase("ASCII") || format.equalsIgnoreCase("CSV") || format.equalsIgnoreCase("TXT");
	}

	/**
	* Returns true if the given string is "DBF" (irrespective of the case)
	*/
	public static boolean isDBF(String format) {
		if (format == null)
			return false;
		return format.equalsIgnoreCase("DBF");
	}

	/**
	* Returns true if the given format string corresponds to a format used for
	* vector geographical objects
	*/
	public static boolean isVectorGeography(String format) {
		if (format == null)
			return false;
		return format.equalsIgnoreCase("OVL") || format.equalsIgnoreCase("OVR") || format.equalsIgnoreCase("SHP") || format.equalsIgnoreCase("SHAPE") || format.equalsIgnoreCase("GML") || format.equalsIgnoreCase("WKB")
				|| format.equalsIgnoreCase("MIF") || format.equalsIgnoreCase("MIF/MID") || format.equalsIgnoreCase("VECTOR");
	}

	/**
	* Returns true if the given format string corresponds to a format used for
	* raster (grid) geographical objects
	*/
	public static boolean isRasterGeography(String format) {
		if (format == null)
			return false;
		return format.equalsIgnoreCase("FLT") || format.equalsIgnoreCase("BIL") || format.equalsIgnoreCase("ADF") || format.equalsIgnoreCase("GRID") || format.equalsIgnoreCase("ESR");
	}

	/**
	* Returns true if the given format string corresponds to a format used for
	* images, e.g. GIF or JPEG
	*/
	public static boolean isImage(String format) {
		if (format == null)
			return false;
		return format.equalsIgnoreCase("IMAGE") || format.equalsIgnoreCase("GIF") || format.equalsIgnoreCase("JPG")
				|| format.equalsIgnoreCase("JPEG")
				||
				// P.G. adding more formats
				format.equalsIgnoreCase("TIF") || format.equalsIgnoreCase("TIFF") || format.equalsIgnoreCase("PNG") || format.equalsIgnoreCase("BMP") || format.equalsIgnoreCase("PCX") || format.equalsIgnoreCase("PSD")
				|| format.equalsIgnoreCase("XBM") || format.equalsIgnoreCase("ICO") || format.equalsIgnoreCase("PCT") || format.equalsIgnoreCase("PICT") || format.equalsIgnoreCase("XPM") || format.equalsIgnoreCase("CUR")
				|| format.equalsIgnoreCase("RASTER") || format.equalsIgnoreCase("RAS") || format.equalsIgnoreCase("TGA") || format.equalsIgnoreCase("TARGA");
		// ~P.G.
	}

	/**
	* Fills the fields of the given structure with the information needed
	* for loading data using a data server.
	*/
	public boolean getSpecification(DataSourceSpec spec) {
		// Ask the user about the URL of the data server and data format (file or
		// database)
		Panel p = new Panel(new ColumnLayout());
		Panel pp = new Panel(new BorderLayout());
		//following text:"Data Server URL:"
		pp.add(new Label(res.getString("Data_Server_URL_")), "West");
		TextField urlTF = new TextField(50);
		urlTF.addActionListener(this);
		pp.add(urlTF, "Center");
		p.add(pp);
		pp = new Panel(new BorderLayout());
		//following text:"Load data from "
		pp.add(new Label(res.getString("Load_data_from")), "West");
		Choice c = new Choice();
		//following text:"file"
		c.addItem(res.getString("file"));
		//following text:"Oracle database"
		c.addItem(res.getString("Oracle_database"));
		//following text:"JDBC/ODBC database"
		c.addItem(res.getString("JDBC_ODBC_database"));
		pp.add(c, "Center");
		p.add(pp);
		Label errLabel = new Label("");
		p.add(errLabel);
		boolean repeat = false;
		do {
			if (spec.server != null) {
				urlTF.setText(spec.server);
			} else if (dataServerURL != null) {
				urlTF.setText(dataServerURL);
			}
			c.select(0);
			if (spec.format != null)
				if (spec.format.equalsIgnoreCase("oracle")) {
					c.select(1);
				} else if (isJDBC(spec.format)) {
					c.select(2);
				}
			eraseErrorMessage(errLabel);
			URL serverURL = null;
			//following text: "Specify data server and source"
			dia = new OKDialog(CManager.getAnyFrame(), res.getString("Specify_data_server"), true, false);
			dia.addContent(p);
			do {
				dia.show();
				if (dia.wasCancelled())
					return false;
				eraseErrorMessage(errLabel);
				String str = urlTF.getText();
				if (str != null) {
					str = str.trim();
				}
				if (str == null || str.length() < 1) {
					//following text:"The Data Server URL is not specified!"
					showErrorMessage(res.getString("The_Data_Server_URL"), errLabel);
				} else {
					try {
						serverURL = new URL(str);
					} catch (MalformedURLException mfe) {
						//following text:"Illegal URL: "
						showErrorMessage(res.getString("Illegal_URL_") + mfe.toString(), errLabel);
					}
				}
			} while (serverURL == null);
			spec.server = serverURL.toString();
			dataServerURL = spec.server;
			boolean changedFormat = spec.format == null;
			if (!changedFormat) {
				switch (c.getSelectedIndex()) {
				case 0:
					changedFormat = isDB(spec.format);
					break;
				case 1:
					changedFormat = !spec.format.equalsIgnoreCase("oracle");
					break;
				case 2:
					changedFormat = !isJDBC(spec.format);
					break;
				}
			}
			if (changedFormat) {
				switch (c.getSelectedIndex()) {
				case 0:
					spec.format = "ASCII"; //by default
					break;
				case 1:
					spec.format = "Oracle";
					break;
				case 2:
					spec.format = "JDBC";
					break;
				}
			}
			repeat = false;
			if (c.getSelectedIndex() == 0) {
				int result = getFileSpecification(spec);
				if (result == 0)
					return false;
				if (result == -1) {
					repeat = true;
				}
			} else {
				int result = getDBInfo(spec);
				if (result == 0)
					return false;
				if (result == -1) {
					repeat = true;
				}
			}
		} while (repeat);
		return true;
	}

	/**
	* Gets specification for data stored in a file. Returns
	*  1 if successfully got;
	*  0 if cancelled;
	* -1 if the button "Back" has been pressed.
	*/
	protected int getFileSpecification(DataSourceSpec spec) {
		Panel p = new Panel(new ColumnLayout());
		//following text:"Specify information for getting data from a FILE:"
		p.add(new Label(res.getString("Specify_information1"), Label.CENTER));
		Panel pp = new Panel(new BorderLayout());
		//following text:"Path or URL:"
		pp.add(new Label(res.getString("Path_or_URL_")), "West");
		TextField pathTF = new TextField(50);
		pathTF.addActionListener(this);
		pp.add(pathTF, "Center");
		p.add(pp);
		//following text:"Format:"
		p.add(new Label(res.getString("Format_"), Label.CENTER));
		CheckboxGroup cbg = new CheckboxGroup();
		//following text:"ASCII with delimiters (comma, TAB, ...)"
		Checkbox asciiCB = new Checkbox(res.getString("ASCII_with_delimiters"), true, cbg);
		p.add(asciiCB);
		//following text:"binary table format (DBF, ...)"
		Checkbox dbfCB = new Checkbox(res.getString("binary_table_format"), false, cbg);
		p.add(dbfCB);
		//following text:"specific format for VECTOR geographical "
		Checkbox vectorCB = new Checkbox(res.getString("specific_format_for") +
		//following text:"objects (SHAPE, MIF/MID, GML, WKB, ...)"
				res.getString("objects_SHAPE_MIF_MID"), false, cbg);
		p.add(vectorCB);
		//following text:"specific format for GRID (RASTER) geographical "
		Checkbox gridCB = new Checkbox(res.getString("specific_format_for1") +
		//following text: "data (BIL, ADF, ...)"
				res.getString("data_BIL_ADF_"), false, cbg);
		p.add(gridCB);
		//following text:"image (GIF, JPEG, ...)
		Checkbox imageCB = new Checkbox(res.getString("image_GIF_JPEG_"), false, cbg);
		p.add(imageCB);
		Label errLabel = new Label("");
		p.add(errLabel);
		boolean repeat = false;
		do {
			if (spec.source != null) {
				pathTF.setText(spec.source);
			}
			if (isASCII(spec.format)) {
				asciiCB.setState(true);
			} else if (isDBF(spec.format)) {
				dbfCB.setState(true);
			} else if (isVectorGeography(spec.format)) {
				vectorCB.setState(true);
			} else if (isRasterGeography(spec.format)) {
				gridCB.setState(true);
			} else if (isImage(spec.format)) {
				imageCB.setState(true);
			}
			//following text:"Specify file path and format"
			dia = new OKDialog(CManager.getAnyFrame(), res.getString("Specify_file_path_and"), true, true);
			dia.addContent(p);
			String path = null;
			do {
				dia.show();
				if (dia.wasCancelled())
					return 0;
				eraseErrorMessage(errLabel);
				String str = pathTF.getText();
				if (str != null) {
					str = str.trim();
				}
				if (str == null || str.length() < 1) {
					if (dia.wasBackPressed())
						return -1;
					//following text:"The file path is not specified!
					showErrorMessage(res.getString("The_file_path_is_not"), errLabel);
				} else if (str.indexOf(':') > 1) { //this may be a URL; check its correctness
					try {
						URL url = new URL(str);
						path = url.toString();
					} catch (MalformedURLException mfe) {
						if (dia.wasBackPressed())
							return -1;
						//following text:"Illegal URL: "
						showErrorMessage(res.getString("Illegal_URL_") + mfe.toString(), errLabel);
					}
				} else {
					path = str;
				}
				if (path != null) {
					spec.source = path;
					String format = CopyFile.getExtension(path);
					if (DataReaderRegister.isValidFormat(format)) {
						spec.format = format;
					} else if (asciiCB.getState())
						if (isASCII(spec.format)) {
							;
						} else {
							spec.format = "ASCII";
						}
					else if (dbfCB.getState()) {
						spec.format = "DBF";
					} else if (vectorCB.getState())
						if (isVectorGeography(spec.format)) {
							;
						} else {
							spec.format = "VECTOR";
						}
					else if (gridCB.getState())
						if (isRasterGeography(spec.format)) {
							;
						} else {
							spec.format = "GRID";
						}
					else if (imageCB.getState())
						if (isImage(spec.format)) {
							;
						} else {
							spec.format = "IMAGE";
						}
				}
				if (dia.wasBackPressed())
					return -1;
			} while (path == null);
			repeat = false;
			if (isASCII(spec.format)) {
				int result = getDelimiter(spec);
				if (result == 0)
					return 0;
				if (result < 0) {
					repeat = true;
				}
			} else if (isDBF(spec.format)) {
				int result = getIdAndNameFields(spec, true);
				if (result == 0)
					return 0;
				if (result < 0) {
					repeat = true;
				}
			} else if (isVectorGeography(spec.format)) {
				//check if for this format there may be a table with additional info
				DataReaderRegister drr = new DataReaderRegister();
				DataReader reader = drr.getReaderOfFormat(spec.format);
				if (reader != null && (reader instanceof AttrDataReader)) {
					int result = getIdAndNameFields(spec, false);
					if (result == 0)
						return 0;
					if (result < 0) {
						repeat = true;
					}
				}
			} else if (isImage(spec.format)) {
				int result = getBounds(spec);
				if (result == 0)
					return 0;
				if (result < 0) {
					repeat = true;
				}
			}
		} while (repeat);
		return 1;
	}

	/**
	* For an ASCII file, asks the user about the delimiter and the numbers of the
	* rows with field names and types. Returns
	*  1 if successfully got;
	*  0 if cancelled;
	* -1 if the button "Back" has been pressed.
	*/
	protected int getDelimiter(DataSourceSpec spec) {
		ASCIISpecPanel asp = new ASCIISpecPanel(spec);
		boolean repeat = false;
		do {
			//following text:"Specify delimiter"
			dia = new OKDialog(CManager.getAnyFrame(), res.getString("Specify_delimiter"), true, true);
			dia.addContent(asp);
			dia.show();
			if (dia.wasCancelled())
				return 0;
			if (dia.wasBackPressed())
				return -1;
			repeat = false;
			int result = getIdAndNameFields(spec, true);
			if (result == 0)
				return 0;
			if (result == -1) {
				repeat = true;
			}
		} while (repeat);
		return 1;
	}

	/**
	* For a table, asks the user about the fields which contain identifiers,
	* names, and, probably, x- and y-coordinates. Returns
	*  1 if successfully got;
	*  0 if cancelled;
	* -1 if the button "Back" has been pressed.
	*/
	protected int getIdAndNameFields(DataSourceSpec spec, boolean mayHaveCoordinates) {
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		Panel p = new Panel(gridbag);
		//following text:"Specify information about fields for the file"
		Label l = new Label(res.getString("Specify_information2"));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(spec.source, Label.CENTER);
		gridbag.setConstraints(l, c);
		p.add(l);
		//following text:"Column with object identifiers:"
		l = new Label(res.getString("Column_with_object"));
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField idTF = new TextField(15);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(idTF, c);
		p.add(idTF);
		//following text:"enter name or number or leave empty"
		l = new Label(res.getString("enter_name_or_number"), Label.RIGHT);
		gridbag.setConstraints(l, c);
		p.add(l);
		//following text:"Column with object names:"
		l = new Label(res.getString("Column_with_object1"));
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField nameTF = new TextField(15);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(nameTF, c);
		p.add(nameTF);
		//following text:"enter name or number or leave empty"
		l = new Label(res.getString("enter_name_or_number"), Label.RIGHT);
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField xTF = null, yTF = null;
		if (mayHaveCoordinates) {
			//following text:"Column with X-coordinates:"
			l = new Label(res.getString("Column_with_X"));
			c.gridwidth = 1;
			gridbag.setConstraints(l, c);
			p.add(l);
			xTF = new TextField(15);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(xTF, c);
			p.add(xTF);
			//following text:"enter name or leave empty"
			l = new Label(res.getString("enter_name_or_leave"), Label.RIGHT);
			gridbag.setConstraints(l, c);
			p.add(l);
			//following text:"Column with Y-coordinates:"
			l = new Label(res.getString("Column_with_Y"));
			c.gridwidth = 1;
			gridbag.setConstraints(l, c);
			p.add(l);
			yTF = new TextField(15);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(yTF, c);
			p.add(yTF);
			//following text:"enter name or leave empty"
			l = new Label(res.getString("enter_name_or_leave"), Label.RIGHT);
			gridbag.setConstraints(l, c);
			p.add(l);
		} else {
			spec.xCoordFieldName = spec.yCoordFieldName = null;
		}
		Label errLabel = new Label("");
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(errLabel, c);
		p.add(errLabel);
		boolean error = false;
		do {
			if (spec.idFieldN >= 0) {
				idTF.setText(String.valueOf(spec.idFieldN));
			} else if (spec.idFieldName != null) {
				idTF.setText(spec.idFieldName);
			}
			if (spec.nameFieldN >= 0) {
				nameTF.setText(String.valueOf(spec.nameFieldN));
			} else if (spec.nameFieldName != null) {
				nameTF.setText(spec.nameFieldName);
			}
			if (mayHaveCoordinates) {
				if (spec.xCoordFieldName != null) {
					xTF.setText(spec.xCoordFieldName);
				}
				if (spec.yCoordFieldName != null) {
					yTF.setText(spec.yCoordFieldName);
				}
			}
			//following text:"Specify column information"
			dia = new OKDialog(CManager.getAnyFrame(), res.getString("Specify_column"), true, true);
			dia.addContent(p);
			dia.show();
			if (dia.wasCancelled())
				return 0;
			if (dia.wasBackPressed())
				return -1;
			eraseErrorMessage(errLabel);
			error = false;
			String str = CManager.getTextFromField(idTF);
			if (str == null) {
				spec.idFieldN = -1;
				spec.idFieldName = null;
			} else {
				try {
					int n = Integer.parseInt(str);
					if (n < 1) {
						//following text:"Illegal number of column with identifiers!"
						showErrorMessage(res.getString("Illegal_number_of"), errLabel);
						error = true;
					} else {
						spec.idFieldN = n;
						spec.idFieldName = null;
					}
				} catch (NumberFormatException nfe) {
					spec.idFieldN = -1;
					spec.idFieldName = str;
				}
			}
			str = CManager.getTextFromField(nameTF);
			if (str == null) {
				spec.nameFieldN = -1;
				spec.nameFieldName = null;
			} else {
				try {
					int n = Integer.parseInt(str);
					if (n < 1) {
						//following text:"Illegal number of column with names!"
						showErrorMessage(res.getString("Illegal_number_of1"), errLabel);
						error = true;
					} else {
						spec.nameFieldN = n;
						spec.nameFieldName = null;
					}
				} catch (NumberFormatException nfe) {
					spec.nameFieldN = -1;
					spec.nameFieldName = str;
				}
			}
			if (mayHaveCoordinates) {
				spec.xCoordFieldName = CManager.getTextFromField(xTF);
				spec.yCoordFieldName = CManager.getTextFromField(yTF);
				if (spec.xCoordFieldName != null)
					if (spec.yCoordFieldName == null) {
						//following text: "The column with Y-coordinates is not specified!"
						showErrorMessage(res.getString("The_column_with_Y"), errLabel);
						error = true;
					} else {
						;
					}
				else if (spec.yCoordFieldName != null) {
					//following text:"The column with X-coordinates is not specified!"
					showErrorMessage(res.getString("The_column_with_X"), errLabel);
					error = true;
				}
			}
		} while (error);
		return 1;
	}

	/**
	* Gets boundaries for a geographical layer, in particular, image. Returns
	*  1 if successfully got;
	*  0 if cancelled;
	* -1 if the button "Back" has been pressed.
	*/
	protected int getBounds(DataSourceSpec spec) {
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		Panel p = new Panel(gridbag);
		//following text:"Specify geographical boundaries (georeference) for the file"
		Label l = new Label(res.getString("Specify_geographical"));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(spec.source, Label.CENTER);
		gridbag.setConstraints(l, c);
		p.add(l);
		//following text:"start X:"
		l = new Label(res.getString("start_X_"));
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField tf[] = new TextField[4];
		tf[0] = new TextField(15);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(tf[0], c);
		p.add(tf[0]);
		//following text:"start Y:"
		l = new Label(res.getString("start_Y_"));
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		p.add(l);
		tf[1] = new TextField(15);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(tf[1], c);
		p.add(tf[1]);
		//following text:"end X:"
		l = new Label(res.getString("end_X_"));
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		p.add(l);
		tf[2] = new TextField(15);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(tf[2], c);
		p.add(tf[2]);
		//following text:"end Y:"
		l = new Label(res.getString("end_Y_"));
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		p.add(l);
		tf[3] = new TextField(15);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(tf[3], c);
		p.add(tf[3]);
		Label errLabel = new Label("");
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(errLabel, c);
		p.add(errLabel);
		boolean error = false;
		RealRectangle r = null;
		if (spec.bounds != null && spec.bounds.size() > 0) {
			r = (RealRectangle) spec.bounds.elementAt(0);
		}
		do {
			if (r != null) {
				float min = r.rx1, max = r.rx2;
				if (min > r.ry1) {
					min = r.ry1;
				}
				if (max < r.ry2) {
					max = r.ry2;
				}
				int prec = StringUtil.getPreferredPrecision(min, min, max);
				tf[0].setText(StringUtil.floatToStr(r.rx1, prec));
				tf[1].setText(StringUtil.floatToStr(r.ry1, prec));
				tf[2].setText(StringUtil.floatToStr(r.rx2, prec));
				tf[3].setText(StringUtil.floatToStr(r.ry2, prec));
			}
			//following text:"Specify boundaries"
			dia = new OKDialog(CManager.getAnyFrame(), res.getString("Specify_boundaries"), true, true);
			dia.addContent(p);
			dia.show();
			eraseErrorMessage(errLabel);
			if (dia.wasCancelled())
				return 0;
			if (dia.wasBackPressed())
				return -1;
			if (r == null) {
				r = new RealRectangle();
			}
			error = false;
			for (int i = 0; i < 4 && !error; i++) {
				String str = CManager.getTextFromField(tf[i]);
				String txt = null;
				switch (i) {
				//following text:"Starting X-coordinate"
				case 0:
					txt = res.getString("Starting_X_coordinate");
					break;
				//following text:"Starting Y-coordinate"
				case 1:
					txt = res.getString("Starting_Y_coordinate");
					break;
				//following text:"Ending X-coordinate"
				case 2:
					txt = res.getString("Ending_X_coordinate");
					break;
				//following text:"Ending Y-coordinate"
				case 3:
					txt = res.getString("Ending_Y_coordinate");
					break;
				}
				if (str == null) {
					//following text:" is not specified!
					showErrorMessage(txt + res.getString("is_not_specified_"), errLabel);
					error = true;
				} else {
					try {
						float val = Float.valueOf(str).floatValue();
						switch (i) {
						case 0:
							r.rx1 = val;
							break;
						case 1:
							r.ry1 = val;
							break;
						case 2:
							r.rx2 = val;
							break;
						case 3:
							r.ry2 = val;
							break;
						}
					} catch (NumberFormatException nfe) {
						//following text:": illegal number entered!"
						showErrorMessage(txt + res.getString("_illegal_number"), errLabel);
						error = true;
					}
				}
			}
			if (!error)
				if (r.rx1 >= r.rx2) {
					//following text:"Starting X must be less than ending X!"
					showErrorMessage(res.getString("Starting_X_must_be"), errLabel);
					error = true;
				} else if (r.ry1 >= r.ry2) {
					//following text:"Starting Y must be less than ending Y!"
					showErrorMessage(res.getString("Starting_Y_must_be"), errLabel);
					error = true;
				}
			if (error) {
				r = null;
			}
		} while (error);
		if (spec.bounds == null) {
			spec.bounds = new Vector(1, 1);
		} else {
			spec.bounds.removeAllElements();
		}
		spec.bounds.addElement(r);
		return 1;
	}

	/**
	* Gets information for loading data from a database. Returns
	*  1 if successfully got;
	*  0 if cancelled;
	* -1 if the button "Back" has been pressed.
	*/
	protected int getDBInfo(DataSourceSpec spec) {
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		Panel p = new Panel(gridbag);
		//following text:"Specify information for getting data from a database"
		Label l = new Label(res.getString("Specify_information3"), Label.CENTER);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		//following text:"Database URL:"
		l = new Label(res.getString("Database_URL_"));
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField urlTF = new TextField(30);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(urlTF, c);
		p.add(urlTF);
		//following text:"Driver:"
		l = new Label(res.getString("Driver_"));
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField driverTF = new TextField(30);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(driverTF, c);
		p.add(driverTF);
		//following text:"User name:"
		l = new Label(res.getString("User_name_"));
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField userTF = new TextField(15);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(userTF, c);
		p.add(userTF);
		//following text:"Password:"
		l = new Label(res.getString("Password_"));
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField pswTF = new TextField(15);
		pswTF.setEchoChar('*');
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(pswTF, c);
		p.add(pswTF);
		//following text:"Table:"
		l = new Label(res.getString("Table_"));
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField tableTF = new TextField(15);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(tableTF, c);
		p.add(tableTF);
		//following text:"Enter appropriate NAMES of table columns:"
		l = new Label(res.getString("Enter_appropriate"), Label.CENTER);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		//following text:"Identifiers are in column"
		l = new Label(res.getString("Identifiers_are_in"));
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField idTF = new TextField(15);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(idTF, c);
		p.add(idTF);
		//following text:"Object names are in column"
		l = new Label(res.getString("Object_names_are_in"));
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField nameTF = new TextField(15);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(nameTF, c);
		p.add(nameTF);
		TextField geoTF = null, xTF = null, yTF = null;
		if (spec.format.equalsIgnoreCase("oracle")) {
			//following text:"Object geometries are in column
			l = new Label(res.getString("Object_geometries_are"));
			c.gridwidth = 1;
			gridbag.setConstraints(l, c);
			p.add(l);
			geoTF = new TextField(15);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(geoTF, c);
			p.add(geoTF);
		} else {
			//following text:"X-coordinates are in column"
			l = new Label(res.getString("Xcoordinates_are_in"));
			c.gridwidth = 1;
			gridbag.setConstraints(l, c);
			p.add(l);
			xTF = new TextField(15);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(xTF, c);
			p.add(xTF);
			//following text:"Y-coordinates are in column"
			l = new Label(res.getString("Ycoordinates_are_in"));
			c.gridwidth = 1;
			gridbag.setConstraints(l, c);
			p.add(l);
			yTF = new TextField(15);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(yTF, c);
			p.add(yTF);
		}
		//following text:"Load following columns (enter single column name per row):"
		l = new Label(res.getString("Load_following"), Label.CENTER);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		Panel pp = new Panel(new BorderLayout());
		pp.add(p, "North");
		TextArea tar = new TextArea(5, 15);
		pp.add(tar, "Center");
		Label errLabel = new Label("");
		pp.add(errLabel, "South");
		if (spec.url != null) {
			urlTF.setText(spec.url);
		} else if (dbURL != null) {
			urlTF.setText(dbURL);
		} else if (spec.format.equalsIgnoreCase("oracle")) {
			urlTF.setText("jdbc:oracle:thin:@");
		} else {
			urlTF.setText("jdbc:odbc:@");
		}
		if (spec.driver != null) {
			driverTF.setText(spec.driver);
		} else if (spec.format.equalsIgnoreCase("oracle")) {
			driverTF.setText("oracle.jdbc.driver.OracleDriver");
		} else {
			driverTF.setText("sun.jdbc.odbc.JdbcOdbcDriver");
		}
		if (spec.user != null) {
			userTF.setText(spec.user);
		} else if (userName != null) {
			userTF.setText(userName);
		}
		if (spec.password != null) {
			pswTF.setText(spec.password);
		} else if (dbPassword != null) {
			pswTF.setText(dbPassword);
		}
		if (spec.source != null) {
			spec.source = CopyFile.getNameWithoutExt(spec.source);
			if (spec.source != null) {
				tableTF.setText(spec.source);
			}
		}
		if (spec.idFieldName != null) {
			idTF.setText(spec.idFieldName);
		}
		if (spec.nameFieldName != null) {
			nameTF.setText(spec.nameFieldName);
		}
		if (geoTF != null)
			if (spec.geoFieldName != null) {
				geoTF.setText(spec.geoFieldName);
			} else {
				geoTF.setText("GEOM");
			}
		if (xTF != null && spec.xCoordFieldName != null) {
			xTF.setText(spec.xCoordFieldName);
		}
		if (yTF != null && spec.yCoordFieldName != null) {
			yTF.setText(spec.yCoordFieldName);
		}
		if (spec.columns != null && spec.columns.size() > 0) {
			for (int i = 0; i < spec.columns.size(); i++) {
				tar.append((String) spec.columns.elementAt(i) + "\n");
			}
		}
		boolean error = false;
		do {
			//following text:"Specify database info"
			dia = new OKDialog(CManager.getAnyFrame(), res.getString("Specify_database_info"), true, true);
			dia.addContent(pp);
			dia.show();
			if (dia.wasCancelled())
				return 0;
			if (dia.wasBackPressed())
				return -1;
			eraseErrorMessage(errLabel);
			error = false;
			String str = CManager.getTextFromField(urlTF);
			if (str == null) {
				//following text:"The database URL is not specified!
				showErrorMessage(res.getString("The_database_URL_is"), errLabel);
				error = true;
			} else if (str.endsWith(":") || str.endsWith("@")) {
				//following text:"Illegal database URL!"
				showErrorMessage(res.getString("Illegal_database_URL_"), errLabel);
				error = true;
			}
			if (error) {
				continue;
			}
			spec.url = str;
			dbURL = str;
			str = CManager.getTextFromField(driverTF);
			if (str == null) {
				//following text:"The database driver is not specified!"
				showErrorMessage(res.getString("The_database_driver"), errLabel);
				error = true;
			}
			if (error) {
				continue;
			}
			spec.driver = str;
			str = CManager.getTextFromField(userTF);
			if (str == null && spec.format.equalsIgnoreCase(res.getString("oracle"))) {
				//following text:"The user name is not specified!
				showErrorMessage(res.getString("The_user_name_is_not"), errLabel);
				error = true;
			}
			if (error) {
				continue;
			}
			spec.user = str;
			userName = str;
			str = CManager.getTextFromField(pswTF);
			if (str == null && spec.format.equalsIgnoreCase("oracle")) {
				//following text:"The password is not specified!"
				showErrorMessage(res.getString("The_password_is_not"), errLabel);
				error = true;
			}
			if (error) {
				continue;
			}
			spec.password = str;
			dbPassword = str;
			str = CManager.getTextFromField(tableTF);
			if (str == null) {
				//following text:"The table name is not specified!"
				showErrorMessage(res.getString("The_table_name_is_not"), errLabel);
				error = true;
			}
			if (error) {
				continue;
			}
			spec.source = str;
			spec.idFieldName = CManager.getTextFromField(idTF);
			spec.nameFieldName = CManager.getTextFromField(nameTF);
			if (geoTF != null) {
				spec.geoFieldName = CManager.getTextFromField(geoTF);
			}
			if (xTF != null) {
				spec.xCoordFieldName = CManager.getTextFromField(xTF);
			}
			if (yTF != null) {
				spec.yCoordFieldName = CManager.getTextFromField(yTF);
			}
			String txt = tar.getText();
			if (txt != null) {
				txt = txt.trim();
			}
			if (txt.length() < 1) {
				txt = null;
			}
			if (txt != null) {
				if (spec.columns == null) {
					spec.columns = new Vector(20, 10);
				} else {
					spec.columns.removeAllElements();
				}
				StringTokenizer st = new StringTokenizer(txt, "\r\n");
				while (st.hasMoreTokens()) {
					str = st.nextToken();
					if (str == null) {
						break;
					}
					str = str.trim();
					if (str.length() > 0) {
						spec.columns.addElement(str);
					}
				}
			} else if (spec.columns != null) {
				spec.columns.removeAllElements();
			}
		} while (error);
		return 1;
	}

	protected void eraseErrorMessage(Label errLabel) {
		errLabel.setText("");
		Component parent = errLabel.getParent();
		if (parent != null) {
			errLabel.setForeground(parent.getForeground());
			errLabel.setBackground(parent.getBackground());
		}
	}

	protected void showErrorMessage(String message, Label errLabel) {
		errLabel.setBackground(Color.red);
		errLabel.setForeground(Color.white);
		errLabel.setText(message);
	}

	/**
	* Finishes the dialog when ENTER is pressed in a text field
	*/
	public void actionPerformed(ActionEvent e) {
		if ((e.getSource() instanceof TextField) && dia != null) {
			dia.dispose();
		}
	}
}
