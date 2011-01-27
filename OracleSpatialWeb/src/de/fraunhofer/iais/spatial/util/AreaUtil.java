package de.fraunhofer.iais.spatial.util;

import de.fraunhofer.iais.spatial.entity.FlickrPolygon.Radius;

public class AreaUtil {

	public static int getZoom(Radius radius) {
		int zoom = 0;
		switch (radius) {
		case R5000:
			zoom = 11;
			break;
		case R10000:
			zoom = 10;
			break;
		case R20000:
			zoom = 9;
			break;
		case R40000:
			zoom = 8;
			break;
		case R80000:
			zoom = 7;
			break;
		case R160000:
			zoom = 6;
			break;
		case R320000:
			zoom = 5;
			break;
		}
		return zoom;
	}


	public static Radius getRadius(int zoom) {
		Radius radius = null;
		if (zoom <= 5) {
			radius = Radius.R320000;
		} else if (zoom <= 6) {
			radius = Radius.R160000;
		} else if (zoom <= 7) {
			radius = Radius.R80000;
		} else if (zoom <= 8) {
			radius = Radius.R40000;
		} else if (zoom <= 9) {
			radius = Radius.R20000;
		} else if (zoom <= 10) {
			radius = Radius.R10000;
		} else if (zoom >= 11) {
			radius = Radius.R5000;
		}
		return radius;
	}
}
