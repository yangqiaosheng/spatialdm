package ui.bitmap;

import java.awt.Image;
import java.io.FileOutputStream;
import java.io.IOException;

import ui.ImageSaver;
import ui.ImageSaverProperties;

import com.sun.jimi.core.Jimi;
import com.sun.jimi.core.JimiException;
import com.sun.jimi.core.JimiWriter;
import com.sun.jimi.core.options.OptionException;
import com.sun.jimi.core.options.PNGOptions;

public class SavePNGImage implements ImageSaver {

	protected Image img = null;
	protected JimiWriter imageWriter = null;
	protected FileOutputStream fosSaveTo = null;
	protected int pngCompression = PNGOptions.COMPRESSION_MAX;

	public SavePNGImage() {
		/*
		  String[] types=Jimi.getEncoderTypes();
		  int i=0;
		  boolean stop=false;
		  while(!stop) {
		    try {
		      System.out.println(types[i++]);
		    } catch (ArrayIndexOutOfBoundsException e) { stop=true; }
		  }
		 */
	}

	public SavePNGImage(FileOutputStream fos) {
		fosSaveTo = fos;
		try {
			imageWriter = Jimi.createJimiWriter("image/png", fosSaveTo);
		} catch (JimiException jimiEx) {
			System.out.println("ERROR: Cannot create Jimi PNG encoder!");
		}
	}

	public void setImage(Image img) {
		this.img = img;
	}

	public void setOutputFile(FileOutputStream fos) {
		fosSaveTo = fos;
		try {
			imageWriter = Jimi.createJimiWriter("image/png", fosSaveTo);
		} catch (JimiException jimiEx) {
			System.out.println("ERROR: Cannot create Jimi PNG encoder!");
		}
	}

	public void saveImageAsPNG(Image img) {
		if (imageWriter != null) {
			try {
				PNGOptions pngOptions = new PNGOptions();
				if (pngOptions != null) {
					pngOptions.setCompressionType(pngCompression);
					imageWriter.setOptions(pngOptions);
				}
			} catch (OptionException optEx) {
				System.out.println("ERROR: Illegal PNG encoder option!");
			}

			try {
				imageWriter.setSource(img);
				imageWriter.putImage(fosSaveTo);
				fosSaveTo.close();
			} catch (IOException ioe) {
				System.out.println("ERROR: Cannot close output file.");
			} catch (JimiException jimiEx) {
			}
		} else {
			System.out.println("ERROR: Cannot run PNG encoder!");
		}
	}

	public void saveImageAsPNG(FileOutputStream fos, Image img) {
		setOutputFile(fos);
		setImage(img);
		this.saveImageAsPNG(img);
	}

	// implementation of the interface ImageSaver
	@Override
	public void saveImage(FileOutputStream fos, Image img, int w, int h) {
		saveImageAsPNG(fos, img);
	}

	@Override
	public void setProperties(ImageSaverProperties props) {
		if (props.getSelectedFormat().equalsIgnoreCase("png")) {
			pngCompression = props.getPNGCompression();
		}
	}

	@Override
	public boolean isAvailable() {
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
		return null;
	}
}
