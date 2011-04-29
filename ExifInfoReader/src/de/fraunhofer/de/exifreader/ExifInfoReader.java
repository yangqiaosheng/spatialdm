package de.fraunhofer.de.exifreader;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
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

	final static File INPUT_FOLDER = new File("images");
	final static File OUTPUT_FILE = new File(new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".csv");

	public static void main(String[] args) throws ImageReadException, IOException, ParseException {
		List<InfoItem> exitInfoItems = new ArrayList<InfoItem>();

		for (File file : new ImageLoader().load(INPUT_FOLDER)) {
			exitInfoItems.add(readMetadata(file));
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
		IImageMetadata metadata = Sanselan.getMetadata(file);

		JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;

		TiffImageMetadata.GPSInfo gpsInfo = jpegMetadata.getExif().getGPS();

		String gpsDescription = gpsInfo.toString();
		double longitude = gpsInfo.getLongitudeAsDegreesEast();
		double latitude = gpsInfo.getLatitudeAsDegreesNorth();

		TiffField createDateField = jpegMetadata.findEXIFValueWithExactMatch(TiffConstants.EXIF_TAG_CREATE_DATE);
		String createDateStr = createDateField.getValueDescription();
		createDateStr = StringUtils.removeStart(createDateStr, "'");
		createDateStr = StringUtils.removeEnd(createDateStr, "'");
		SimpleDateFormat createDateInputFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
		Date createDate = createDateInputFormat.parse(createDateStr);

		InfoItem infoItem = new InfoItem(createDate, latitude, longitude, file);

		// print out various interesting EXIF tags.
		System.out.println("file: " + file.getPath());
		System.out.println("    " + "Create Date: " + createDateField.getValueDescription());
		System.out.println("    " + "GPS Description: " + gpsDescription);
		System.out.println("    " + "GPS Longitude (Degrees East): " + longitude);
		System.out.println("    " + "GPS Latitude (Degrees North): " + latitude);
		System.out.println();

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
			String line = createDateOutputFormat.format(infoItem.getCreateDate()) + ", "
						+ infoItem.getLatitude() + ", "
						+ infoItem.getLongitude() + ", "
						+ infoItem.getFile().getName();
			lines.add(line);
		}

		FileUtils.writeLines(OUTPUT_FILE, lines);
	}
}
