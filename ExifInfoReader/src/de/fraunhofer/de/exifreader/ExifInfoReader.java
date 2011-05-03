package de.fraunhofer.de.exifreader;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;

/**
 *
 * @author Haolin Zhi - haolin.zhi@gmail.com
 * @version $Id: ExitReader.java 2011-04-29 16:13:01Z $
 */
public class ExifInfoReader {

	final static File OUTPUT_FILE = new File(new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".csv");

	public static void main(String[] args) throws IOException {
		List<InfoItem> exitInfoItems = new ArrayList<InfoItem>();

		Set<File> inputFolders = new HashSet<File>();

		if (args.length > 0) {
			for (String inputPath : args) {
				File inputFolder = new File(inputPath);
				if (inputFolder.isDirectory()) {
					inputFolders.add(inputFolder);
					System.out.println("INPUT PATH: " + inputFolder.getAbsolutePath());
				} else {
					System.out.println("WARNNING: " + inputFolder.getAbsolutePath() + " is not a valid Directory Path");
				}
			}
		} else {
			File inputFolder = new File("images");
			if (inputFolder.isDirectory()) {
				inputFolders.add(inputFolder);
				System.out.println("INPUT PATH: " + inputFolder.getAbsolutePath() + "(DEFAULT)");
			}
		}
		System.out.println();

		for (File inputFolder : inputFolders) {
			for (File file : new ImageLoader().load(inputFolder)) {
				InfoItem infoItem = null;
				try {
					infoItem = readMetadata(file);
				} catch (ImageReadException e) {
					System.out.println("    " + e.getMessage() + "\n");
				} catch (IOException e) {
					System.out.println("    " + e.getMessage() + "\n");
				} catch (ParseException e) {
					System.out.println("    " + e.getMessage() + "\n");
				}
				if (infoItem != null) {
					exitInfoItems.add(infoItem);
				}
			}
		}

		exportCSV(exitInfoItems);
	}

	/**
	 * read createDate, latitude, longitude, filename of EXIF metadata from a jpeg file
	 * @param file
	 * @return InfoItem infoItem
	 * @throws ImageReadException
	 * @throws IOException
	 * @throws ParseException
	 */
	public static InfoItem readMetadata(File file) throws ImageReadException, IOException, ParseException {
		// 	get all metadata stored in EXIF format (ie. from JPEG or TIFF).
		//  org.w3c.dom.Node node = Sanselan.getMetadataObsolete(imageBytes);
		System.out.println("file: " + file.getPath());
		IImageMetadata metadata = Sanselan.getMetadata(file);

		JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;

		TiffImageMetadata.GPSInfo gpsInfo = jpegMetadata.getExif().getGPS();
		InfoItem infoItem = null;

		if (gpsInfo != null) {
			String gpsDescription = gpsInfo.toString();
			double longitude = gpsInfo.getLongitudeAsDegreesEast();
			double latitude = gpsInfo.getLatitudeAsDegreesNorth();

			TiffField createDateField = jpegMetadata.findEXIFValueWithExactMatch(TiffConstants.EXIF_TAG_CREATE_DATE);
			String createDateStr = createDateField.getValueDescription();
			createDateStr = StringUtils.removeStart(createDateStr, "'");
			createDateStr = StringUtils.removeEnd(createDateStr, "'");
			SimpleDateFormat createDateInputFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
			Date createDate = createDateInputFormat.parse(createDateStr);

			infoItem = new InfoItem(createDate, latitude, longitude, file);

			// print out various interesting EXIF tags.
			System.out.println("    " + "Create Date: " + createDateField.getValueDescription());
			System.out.println("    " + "GPS Description: " + gpsDescription);
			System.out.println("    " + "GPS Longitude (Degrees East): " + longitude);
			System.out.println("    " + "GPS Latitude (Degrees North): " + latitude);
			System.out.println();
		} else {
			System.out.println("    " + "No GPS Data");
			System.out.println();
		}
		return infoItem;
	}

	/**
	 * output the result to a csv file in the output folder
	 * @param exitInfoItems
	 * @throws IOException
	 */
	public static void exportCSV(List<InfoItem> exitInfoItems) throws IOException {
		List<String> lines = new ArrayList<String>();

		String title = "dt, lat, lon, filename";
		lines.add(title);

		for (InfoItem infoItem : exitInfoItems) {
			SimpleDateFormat createDateOutputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			String line = createDateOutputFormat.format(infoItem.getCreateDate()) + ", " + infoItem.getLatitude() + ", " + infoItem.getLongitude() + ", " + infoItem.getFile().getCanonicalPath();
			lines.add(line);
		}

		FileUtils.writeLines(OUTPUT_FILE, lines);
	}
}
