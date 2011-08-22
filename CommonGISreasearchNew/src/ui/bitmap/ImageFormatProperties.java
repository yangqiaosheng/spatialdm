package ui.bitmap;

import java.io.Serializable;

import com.sun.jimi.core.options.PNGOptions;

public class ImageFormatProperties implements Serializable {

	public String mimeType = "image/png";
	public int pngCompression = PNGOptions.COMPRESSION_DEFAULT;
	public int jpgQuality = 85;

	public static String fmt2MimeType(String fmtExt) {
		if (fmtExt == null || fmtExt.length() < 1)
			return null;
		if (fmtExt.equalsIgnoreCase("jpg") || fmtExt.equalsIgnoreCase("jpeg"))
			return "image/jpeg";
		else if (fmtExt.equalsIgnoreCase("tif") || fmtExt.equalsIgnoreCase("tiff"))
			return "image/tiff";
		return "image/" + fmtExt;
	}
}
