package ui;

import java.awt.Image;
import java.net.URL;

public interface ImgSerializer {

	public URL serializeImage(Image img, ImageSaverProperties isp, String what);

	public void setServletURL(String sURL);

}
