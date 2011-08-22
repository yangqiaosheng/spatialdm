package ui;

import java.awt.Image;
import java.io.FileOutputStream;

public interface ImageSaver {

	public void saveImage(FileOutputStream fos, Image img, int width, int height);

	public void setProperties(ImageSaverProperties props);

	public ImageSaverProperties getProperties();

	public boolean isAvailable();
}
