package com.aetrion.flickr.photos;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;

import com.aetrion.flickr.util.StringUtilities;

/**
 * A geographic position.
 *
 * @author mago
 * @version $Id: GeoData.java,v 1.4 2009/07/23 20:41:03 x-mago Exp $
 */
public class GeoData {
	private static final long serialVersionUID = 12L;
	private float longitude;
	private float latitude;
	private int accuracy;

	public GeoData() {
		super();
	}

	public GeoData(String longitudeStr, String latitudeStr, String accuracyStr) {
		longitude = Float.parseFloat(longitudeStr);
		latitude = Float.parseFloat(latitudeStr);
		accuracy = Integer.parseInt(accuracyStr);
	}

	/**
	 *
	 * @return Recorded accuracy level of the location information. World level is 1, Country is ~3, Region ~6, City ~11, Street ~16. Current range is 1-16. Defaults to 16 if not specified.
	 */
	public int getAccuracy() {
		return accuracy;
	}

	/**
	 * Set the accuracy level.<p>
	 *
	 * World level is 1, Country is ~3, Region ~6, City ~11, Street ~16.
	 *
	 * @param accuracy - level of the location information. World level is 1, Country is ~3, Region ~6, City ~11, Street ~16. Current range is 1-16. Defaults to 16 if not specified.

	 * @see com.aetrion.flickr.Flickr#ACCURACY_WORLD
	 * @see com.aetrion.flickr.Flickr#ACCURACY_COUNTRY
	 * @see com.aetrion.flickr.Flickr#ACCURACY_REGION
	 * @see com.aetrion.flickr.Flickr#ACCURACY_CITY
	 * @see com.aetrion.flickr.Flickr#ACCURACY_STREET
	 */
	public void setAccuracy(int accuracy) {
		this.accuracy = accuracy;
	}

	/**
	 *
	 * @return The latitude whose valid range is -90 to 90. Anything more than 6 decimal places will be truncated.
	 */
	public float getLatitude() {
		return latitude;
	}

	/**
	 *
	 * @param latitude - The latitude whose valid range is -90 to 90. Anything more than 6 decimal places will be truncated.
	 */
	public void setLatitude(float latitude) {
		this.latitude = latitude;
	}

	/**
	 *
	 * @return The longitude whose valid range is -180 to 180. Anything more than 6 decimal places will be truncated.
	 */
	public float getLongitude() {
		return longitude;
	}

	/**
	 *
	 * @param longitude - The longitude whose valid range is -180 to 180. Anything more than 6 decimal places will be truncated.
	 */
	public void setLongitude(float longitude) {
		this.longitude = longitude;
	}

	@Override
	public String toString() {
		return "GeoData[longitude=" + longitude + " latitude=" + latitude + " accuracy=" + accuracy + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj == null) || (obj.getClass() != this.getClass()))
			return false;
		// object must be GeoData at this point
		GeoData test = (GeoData) obj;
		Class cl = this.getClass();
		Method[] method = cl.getMethods();
		for (int i = 0; i < method.length; i++) {
			Matcher m = StringUtilities.getterPattern.matcher(method[i].getName());
			if (m.find() && !method[i].getName().equals("getClass")) {
				try {
					Object res = method[i].invoke(this, null);
					Object resTest = method[i].invoke(test, null);
					String retType = method[i].getReturnType().toString();
					if (retType.indexOf("class") == 0) {
						if (res != null && resTest != null) {
							if (!res.equals(resTest))
								return false;
						} else {
							//return false;
						}
					} else if (retType.equals("int")) {
						if (!((Integer) res).equals((resTest)))
							return false;
					} else if (retType.equals("float")) {
						if (!((Float) res).equals((resTest)))
							return false;
					} else {
						System.out.println(method[i].getName() + "|" + method[i].getReturnType().toString());
					}
				} catch (IllegalAccessException ex) {
					System.out.println("GeoData equals " + method[i].getName() + " " + ex);
				} catch (InvocationTargetException ex) {
					//System.out.println("equals " + method[i].getName() + " " + ex);
				} catch (Exception ex) {
					System.out.println("GeoData equals " + method[i].getName() + " " + ex);
				}
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 1;
		hash += new Float(longitude).hashCode();
		hash += new Float(latitude).hashCode();
		hash += new Integer(accuracy).hashCode();
		return hash;
	}
}
