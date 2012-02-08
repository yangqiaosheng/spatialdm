package de.fraunhofer.iais.spatial.util;

import de.fraunhofer.iais.spatial.entity.FlickrArea.Radius;

public class FlickrAreaUtil {

	public static int judgeZoom(Radius radius) {
		int zoom = 0;
		switch (radius) {

		case R625:
			zoom = 14;
			break;
		case R1250:
			zoom = 13;
			break;
		case R2500:
			zoom = 12;
			break;
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
		case R640000:
			zoom = 4;
			break;
		case R1280000:
			zoom = 3;
			break;
		case R2560000:
			zoom = 2;
			break;
		}
		return zoom;
	}

	public static Radius judgeRadius(int zoom) {
		Radius radius = null;
		if (zoom <= 2) {
			radius = Radius.R2560000;
		} else if (zoom <= 3) {
			radius = Radius.R1280000;
		} else if (zoom <= 4) {
			radius = Radius.R640000;
		} else if (zoom <= 5) {
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
		} else if (zoom <= 11) {
			radius = Radius.R5000;
		} else if (zoom <= 12) {
			radius = Radius.R2500;
		} else if (zoom <= 13) {
			radius = Radius.R1250;
		} else if (zoom >= 14) {
			radius = Radius.R625;
		}

		return radius;
	}
}
