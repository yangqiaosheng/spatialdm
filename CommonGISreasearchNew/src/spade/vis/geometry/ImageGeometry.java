package spade.vis.geometry;

import java.awt.Image;

public class ImageGeometry extends RealRectangle {
	public Image img = null;

	@Override
	public char getType() {
		return image;
	}

	@Override
	public Object clone() {
		ImageGeometry ig = new ImageGeometry();
		ig.rx1 = rx1;
		ig.ry1 = ry1;
		ig.rx2 = rx2;
		ig.ry2 = ry2;
		ig.img = img;
		return ig;
	}
}
