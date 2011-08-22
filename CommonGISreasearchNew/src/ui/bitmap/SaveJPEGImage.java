package ui.bitmap;

//import com.sun.image.codec.jpeg.*;

import java.awt.Image;
import java.io.FileOutputStream;
import java.io.IOException;

import ui.ImageSaver;
import ui.ImageSaverProperties;

import com.sun.jimi.core.Jimi;
import com.sun.jimi.core.JimiException;
import com.sun.jimi.core.JimiWriter;
import com.sun.jimi.core.options.JPGOptions;
import com.sun.jimi.core.options.OptionException;

class JPEGProperties implements ImageSaverProperties {

	boolean saveMap = true, saveLegend = true, saveMapAsIs = false, saveMapAndLegend = false;
	float jq = 1.00f;

	@Override
	public String getSelectedFormat() {
		return "jpg";
	}

	@Override
	public boolean isSaveMap() {
		return saveMap;
	}

	@Override
	public boolean isSaveLegend() {
		return saveLegend;
	}

	@Override
	public boolean isSaveMapAsIs() {
		return saveMapAsIs;
	}

	@Override
	public boolean isSaveMapAndLegend() {
		return saveMapAndLegend;
	}

	@Override
	public float getJPEGQuality() {
		return jq;
	}

	@Override
	public String fmt2MimeType(String fmt) {
		return "image/jpeg";
	}

	@Override
	public int getPNGCompression() {
		return -1;
	} // not applicable
}

public class SaveJPEGImage implements ImageSaver {

	//protected JPEGImageEncoder encoder=null;
	protected float jpegQuality = 1.00f;
	protected FileOutputStream fosSaveTo = null;
	//protected BufferedImage bimg=null;
	protected Image img = null;
	protected JimiWriter imageWriter = null;
	private JPEGProperties jprop = null;

	public SaveJPEGImage() {
	}

	public SaveJPEGImage(FileOutputStream fos, float jpegQuality) {
		fosSaveTo = fos;
		//encoder=JPEGCodec.createJPEGEncoder(fosSaveTo);
		try {
			imageWriter = Jimi.createJimiWriter("image/jpeg", fosSaveTo);
		} catch (JimiException jimiEx) {
			System.out.println("ERROR: Cannot create Jimi JPEG encoder!");
		}
		this.jpegQuality = jpegQuality;
	}

	public void setImage(Image img) {
		this.img = img;

		// image serialization testing here:
		/*
		ObjectOutputStream os=null;
		JimiImageSerializer im = new JimiImageSerializer(img);
		try {
		  os = new ObjectOutputStream(System.out);
		} catch (IOException ioEx) { System.out.println("ERROR: Cannot create object output stream!"); }
		try {
		  if (os!=null) os.writeObject(im);
		} catch (IOException ioEx) { System.out.println("Write error to object output stream!"); }
		try {
		  os.flush();
		  os.close();
		} catch (IOException ioEx) { System.out.println("Cannot close object output stream!"); }

		// end of testing
		*/

		/* if (bimg!=null) bimg=null;
		if (img!=null &&  img instanceof BufferedImage)
		  bimg=(BufferedImage)img;
		if (bimg==null) {
		  System.out.println("ERROR: wrong image source data");
		  return;
		}
		*/
	}

	public void setOutputFile(FileOutputStream fos) {
		fosSaveTo = fos;
		//encoder=JPEGCodec.createJPEGEncoder(fosSaveTo);
		try {
			imageWriter = Jimi.createJimiWriter("image/jpeg", fosSaveTo);
		} catch (JimiException jimiEx) {
			System.out.println("ERROR: Cannot create Jimi JPEG encoder!");
		}
	}

	public void setJPEGQuality(float jpegQuality) {
		this.jpegQuality = jpegQuality;

	}

	public void saveImageAsJPEG(Image img) {
		//if (encoder!=null) {
		if (imageWriter != null) {
			/*
			try {
			  JPEGEncodeParam jpegEP=encoder.getDefaultJPEGEncodeParam(bimg);
			  if (jpegEP!=null) {
			    jpegEP.setQuality(jpegQuality,false);
			    encoder.setJPEGEncodeParam(jpegEP);
			  }
			} catch (NullPointerException npe) {
			  System.out.println("WARNING: cannot access to JPEG parameters!");
			}
			*/
			try {
				JPGOptions jpegOptions = new JPGOptions();
				if (jpegOptions != null) {
					jpegOptions.setQuality(Math.round(100 * jpegQuality));
					imageWriter.setOptions(jpegOptions);
				}
			} catch (OptionException optEx) {
				System.out.println("ERROR: Illegal JPEG-encoder option!");
			}
			try {
				//encoder.encode(bimg);
				imageWriter.setSource(img);
				imageWriter.putImage(fosSaveTo);
				fosSaveTo.close();
			} catch (IOException ioe) {
				System.out.println("ERROR: Cannot close output file.");
			} catch (JimiException jimiEx) {
			}
		} else {
			System.out.println("ERROR: Cannot run JPEG encoder!");
		}
	}

	public void saveImageAsJPEG(FileOutputStream fos, Image img) {
		setOutputFile(fos);
		setImage(img);
		this.saveImageAsJPEG(img);
	}

	// implementation of the interface ImageSaver
	@Override
	public void saveImage(FileOutputStream fos, Image img, int w, int h) {
		saveImageAsJPEG(fos, img);
	}

	@Override
	public void setProperties(ImageSaverProperties props) {
		if (props.getSelectedFormat().equalsIgnoreCase("jpg")) {
			setJPEGQuality(props.getJPEGQuality());
			if (jprop == null) {
				jprop = new JPEGProperties();
			}
			jprop.jq = jpegQuality;
			jprop.saveMap = props.isSaveMap();
			jprop.saveLegend = props.isSaveLegend();
			jprop.saveMapAndLegend = props.isSaveMapAndLegend();
			jprop.saveMapAsIs = props.isSaveMapAsIs();
		}
	}

	@Override
	public boolean isAvailable() {
		/* Class cJPGE=null, cBImg=null;
		boolean avail=false;
		try {
		  cJPGE=Class.forName("com.sun.image.codec.jpeg.JPEGImageEncoder");
		  cBImg=Class.forName("java.awt.image.BufferedImage");
		} catch (Exception e) { }
		avail=(cJPGE!=null && cBImg!=null);
		return avail;
		*/
		Class cJimiWriter = null;
		boolean avail = false;
		try {
			cJimiWriter = Class.forName("com.sun.jimi.core.JimiWriter");
		} catch (Exception e) {
		}
		avail = cJimiWriter != null;
		return avail;
	}

	@Override
	public ImageSaverProperties getProperties() {
		return jprop;
	}
}
