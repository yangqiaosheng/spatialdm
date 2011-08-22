package ui.bitmap;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import ui.ImageSaverProperties;
import ui.ImgSerializer;

import com.sun.jimi.core.options.BasicFormatOptionSet;
import com.sun.jimi.core.options.JPGOptions;
import com.sun.jimi.core.options.OptionException;
import com.sun.jimi.core.options.PNGOptions;
import com.sun.jimi.core.util.JimiImageSerializer;

public class ImageSerializer implements ImgSerializer {

	protected ObjectOutputStream oos = null;
	protected FileOutputStream fos = null;
	protected Image img = null;
	protected Properties imgProps = null;
//  protected JimiWriter imageWriter=null;
	protected BasicFormatOptionSet fmtOptions = null;
	protected String imageServletURL = null;

	public ImageSerializer() {
	}

	public ImageSerializer(String imageServletURL) {
		this.imageServletURL = imageServletURL;
	}

	@Override
	public void setServletURL(String imageServletURL) {
		this.imageServletURL = imageServletURL;
	}

	@Override
	public URL serializeImage(Image img, ImageSaverProperties props, String what) {
		this.img = img;
		if (props != null) {
/* It's possible to get rid of jimi options and use more simple structures */
			try {
				if (props.getSelectedFormat().equalsIgnoreCase("JPG")) {
					JPGOptions jpgOpt = new JPGOptions();
					jpgOpt.setQuality(Math.round(props.getJPEGQuality() * 100));
					fmtOptions = jpgOpt;
				} else if (props.getSelectedFormat().equalsIgnoreCase("PNG")) {
					PNGOptions pngOpt = new PNGOptions();
					pngOpt.setCompressionType(props.getPNGCompression());
					fmtOptions = pngOpt;
				}
				if (props.getSelectedFormat().equalsIgnoreCase("BMP")) {
					fmtOptions = null;
				}
			} catch (OptionException oe) {
				System.out.println("Invalid option's parameter!");
				return null;
			}
/* end of redundant block */
		}

		// image serialization here:
		JimiImageSerializer im = new JimiImageSerializer(img);

		URL storeURL = null;
		try {
			if (imageServletURL != null && imageServletURL.length() > 0) {
				storeURL = new URL(imageServletURL);
			}
		} catch (MalformedURLException mfe) {
			System.out.println(mfe);
			return null;
		}
		OutputStream out = null;
		URLConnection con = null;
		if (storeURL == null) {
			System.out.println("Servlet URL not found!");
			return null;
		}
		try {
			con = storeURL.openConnection();
			con.setDoOutput(true);
			//con.setDoInput(true);
			//con.setDefaultUseCaches(false);
			//con.setUseCaches(false);
			//con.setRequestProperty("Content-type","java-internal/"+im.getClass().getName());
			out = con.getOutputStream();
			ObjectOutputStream objOut = new ObjectOutputStream(out);
			if (what != null) {
				objOut.writeObject(what);
			}
			System.out.println("Sending ImageSerializer");
			objOut.writeObject(im);
			System.out.println("ImageSerializer sent");
			System.out.println("Sending image properties");
			//ID
			imgProps = new Properties();
/* It's possible to get rid of jimi options and use more simple structures */
			if (fmtOptions == null) {
				imgProps.put("mime-type", "image/bmp");
			} else if (fmtOptions instanceof JPGOptions) {
				imgProps.put("mime-type", "image/jpeg");
				imgProps.put("JPEG quality", Integer.toString(((JPGOptions) fmtOptions).getQuality()));
			} else if (fmtOptions instanceof PNGOptions) {
				imgProps.put("mime-type", "image/png");
				imgProps.put("PNG compression", Integer.toString(((PNGOptions) fmtOptions).getCompressionType()));
			}
/* end of redundant block */

// for test purposes - saves to JPEG with 85% quality
//        imgProps.put("mime-type", "image/jpeg");
//        imgProps.put("JPEG quality", Integer.toString(85));

			objOut.writeObject(imgProps);
			//~ID
			System.out.println("Image properties sent");
			objOut.flush();
		} catch (Exception e) {
			System.out.println(e);
		}
		if (out != null) {
			try {
				out.close();
			} catch (IOException ioe) {
			}
		}
		InputStream in = null;
		String imgFilename = "";
		if (con != null) {
			try {
				in = con.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				while (true) {
					String str = reader.readLine();
					if (str == null) {
						break;
					}
					str = str.trim();
					if (str.substring(0, 2).equalsIgnoreCase("OK")) {
						imgFilename = str.substring(3);
					}
					System.out.println("Servlet's output: [" + str + "]");
					/**
					if (str.length()>6 && str.substring(0,6).equalsIgnoreCase("ERROR:"))
					  errorMsg=str.substring(6).trim();
					else
					if (str.length()>=2 && str.substring(0,2).equalsIgnoreCase("OK")) ok=true;
					*/
				}
			} catch (Exception e) {
				System.out.println(e);
			}
		}
		if (in != null) {
			try {
				in.close();
			} catch (IOException ioe) {
			}
		}
//ID
		URL u = null;
		try {
			String imageFile = imageServletURL.substring(0, imageServletURL.lastIndexOf("/")) + "/report/" + imgFilename;
			u = new URL(imageFile);
		} catch (Exception e) {
			System.err.println("Cannot show document: " + e);
		}
		return u;
//~ID
	}
/*
  public void setOutputFile(FileOutputStream fos) {
    this.fos=fos;
    try {
      imageWriter=Jimi.createJimiWriter("image/jpeg",fos);
    } catch (JimiException jimiEx) { System.out.println("ERROR: Cannot create Jimi JPEG encoder!"); }
  }

  public void saveImageAsJPEG(Image img) {
    if (imageWriter!=null) {
      try {
        JPGOptions jpegOptions = new JPGOptions();
        if (jpegOptions!=null) {
          jpegOptions.setQuality(85);
          imageWriter.setOptions(jpegOptions);
        }
      } catch (OptionException optEx) { System.out.println("ERROR: Illegal JPEG-encoder option!"); }
      try {
        imageWriter.setSource(img);
        imageWriter.putImage(fos);
        fos.close();
      } catch (IOException ioe) {
        System.out.println("ERROR: Cannot close output file.");
      } catch (JimiException jimiEx) { }
    } else System.out.println("ERROR: Cannot run JPEG encoder!");
  }

  public void saveImageAsJPEG(FileOutputStream fos, Image img) {
    setOutputFile(fos);
    this.saveImageAsJPEG(img);
  }
  // implementation of the interface ImageSaver
  public void saveImage(FileOutputStream fos, Image img, int w, int h) {
    saveImageAsJPEG(fos,img);
  }
*/
}
