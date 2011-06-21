/*
 * Copyright (c) 2005 Aetrion LLC.
 */
package com.aetrion.flickr.people;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.Parameter;
import com.aetrion.flickr.Response;
import com.aetrion.flickr.Transport;
import com.aetrion.flickr.auth.AuthUtilities;
import com.aetrion.flickr.groups.Group;
import com.aetrion.flickr.photos.Extras;
import com.aetrion.flickr.photos.PhotoList;
import com.aetrion.flickr.photos.PhotoUtils;
import com.aetrion.flickr.util.StringUtilities;
import com.aetrion.flickr.util.XMLUtilities;

/**
 * Interface for finding Flickr users.
 *
 * @author Anthony Eden
 * @version $Id: PeopleInterface.java,v 1.28 2010/09/12 20:13:57 x-mago Exp $
 */
public class PeopleInterface {

	/**
	 * Bounding Box or the area that will be search
	 * @author haolin
	 *
	 */
	public class Bbox{
		double minLongitude;
		double minLatitude;
		double maxLongitude;
		double maxLatitude;
		public Bbox(double minLongitude, double minLatitude, double maxLongitude, double maxLatitude) {
			this.minLongitude = minLongitude;
			this.minLatitude = minLatitude;
			this.maxLongitude = maxLongitude;
			this.maxLatitude = maxLatitude;
		}

	}

	public static final String METHOD_FIND_BY_EMAIL = "flickr.people.findByEmail";
	public static final String METHOD_FIND_BY_USERNAME = "flickr.people.findByUsername";
	public static final String METHOD_GET_INFO = "flickr.people.getInfo";
	public static final String METHOD_GET_PHOTOS = "flickr.people.getPhotos";
	public static final String METHOD_SEARCH = "flickr.photos.search";
	public static final String METHOD_GET_PUBLIC_GROUPS = "flickr.people.getPublicGroups";
	public static final String METHOD_GET_PUBLIC_PHOTOS = "flickr.people.getPublicPhotos";
	public static final String METHOD_GET_UPLOAD_STATUS = "flickr.people.getUploadStatus";

	private String apiKey;
	private String sharedSecret;
	private Transport transport;

	public PeopleInterface(String apiKey, String sharedSecret, Transport transportAPI) {
		this.apiKey = apiKey;
		this.sharedSecret = sharedSecret;
		this.transport = transportAPI;
	}

	/**
	 * Find the user by their email address.
	 *
	 * This method does not require authentication.
	 *
	 * @param email The email address
	 * @return The User
	 * @throws IOException
	 * @throws SAXException
	 * @throws FlickrException
	 */
	public User findByEmail(String email) throws IOException, SAXException, FlickrException {
		List<Parameter> parameters = new ArrayList<Parameter>();
		parameters.add(new Parameter("method", METHOD_FIND_BY_EMAIL));
		parameters.add(new Parameter("api_key", apiKey));

		parameters.add(new Parameter("find_email", email));

		Response response = transport.get(transport.getPath(), parameters);
		if (response.isError())
			throw new FlickrException(response.getErrorCode(), response.getErrorMessage());
		Element userElement = response.getPayload();
		User user = new User();
		user.setId(userElement.getAttribute("nsid"));
		user.setUsername(XMLUtilities.getChildValue(userElement, "username"));
		return user;
	}

	/**
	 * Find a User by the username.
	 *
	 * This method does not require authentication.
	 *
	 * @param username The username
	 * @return The User object
	 * @throws IOException
	 * @throws SAXException
	 * @throws FlickrException
	 */
	public User findByUsername(String username) throws IOException, SAXException, FlickrException {
		List<Parameter> parameters = new ArrayList<Parameter>();
		parameters.add(new Parameter("method", METHOD_FIND_BY_USERNAME));
		parameters.add(new Parameter("api_key", apiKey));

		parameters.add(new Parameter("username", username));

		Response response = transport.get(transport.getPath(), parameters);
		if (response.isError())
			throw new FlickrException(response.getErrorCode(), response.getErrorMessage());
		Element userElement = response.getPayload();
		User user = new User();
		user.setId(userElement.getAttribute("nsid"));
		user.setUsername(XMLUtilities.getChildValue(userElement, "username"));
		return user;
	}

	/**
	 * Get info about the specified user.
	 *
	 * This method does not require authentication.
	 *
	 * @param userId The user ID
	 * @return The User object
	 * @throws IOException
	 * @throws SAXException
	 * @throws FlickrException
	 */
	public User getInfo(String userId) throws IOException, SAXException, FlickrException {
		List<Parameter> parameters = new ArrayList<Parameter>();
		parameters.add(new Parameter("method", METHOD_GET_INFO));
		parameters.add(new Parameter("api_key", apiKey));

		parameters.add(new Parameter("user_id", userId));
		parameters.add(new Parameter("api_sig", AuthUtilities.getSignature(sharedSecret, parameters)));

		Response response = transport.get(transport.getPath(), parameters);
		if (response.isError())
			throw new FlickrException(response.getErrorCode(), response.getErrorMessage());
		Element userElement = response.getPayload();
		User user = new User();
		user.setId(userElement.getAttribute("nsid"));
		user.setAdmin("1".equals(userElement.getAttribute("isadmin")));
		user.setPro("1".equals(userElement.getAttribute("ispro")));
		user.setIconFarm(userElement.getAttribute("iconfarm"));
		user.setIconServer(userElement.getAttribute("iconserver"));
		user.setRevContact("1".equals(userElement.getAttribute("revcontact")));
		user.setRevFriend("1".equals(userElement.getAttribute("revfriend")));
		user.setRevFamily("1".equals(userElement.getAttribute("revfamily")));
		user.setUsername(XMLUtilities.getChildValue(userElement, "username"));
		user.setRealName(XMLUtilities.getChildValue(userElement, "realname"));
		user.setLocation(XMLUtilities.getChildValue(userElement, "location"));
		user.setMbox_sha1sum(XMLUtilities.getChildValue(userElement, "mbox_sha1sum"));
		user.setPhotosurl(XMLUtilities.getChildValue(userElement, "photosurl"));
		user.setProfileurl(XMLUtilities.getChildValue(userElement, "profileurl"));
		user.setMobileurl(XMLUtilities.getChildValue(userElement, "mobileurl"));

		Element photosElement = XMLUtilities.getChild(userElement, "photos");
		user.setPhotosFirstDate(XMLUtilities.getChildValue(photosElement, "firstdate"));
		user.setPhotosFirstDateTaken(XMLUtilities.getChildValue(photosElement, "firstdatetaken"));
		user.setPhotosCount(XMLUtilities.getChildValue(photosElement, "count"));

		return user;
	}

	/**
	 * Get a collection of public groups for the user.
	 *
	 * The groups will contain only the members nsid, name, admin and eighteenplus.
	 * If you want the whole group-information, you have to call
	 * {@link com.aetrion.flickr.groups.GroupsInterface#getInfo(String)}.
	 *
	 * This method does not require authentication.
	 *
	 * @param userId The user ID
	 * @return The public groups
	 * @throws IOException
	 * @throws SAXException
	 * @throws FlickrException
	 */
	public Collection getPublicGroups(String userId) throws IOException, SAXException, FlickrException {
		List<Group> groups = new ArrayList<Group>();

		List<Parameter> parameters = new ArrayList<Parameter>();
		parameters.add(new Parameter("method", METHOD_GET_PUBLIC_GROUPS));
		parameters.add(new Parameter("api_key", apiKey));

		parameters.add(new Parameter("user_id", userId));

		Response response = transport.get(transport.getPath(), parameters);
		if (response.isError())
			throw new FlickrException(response.getErrorCode(), response.getErrorMessage());
		Element groupsElement = response.getPayload();
		NodeList groupNodes = groupsElement.getElementsByTagName("group");
		for (int i = 0; i < groupNodes.getLength(); i++) {
			Element groupElement = (Element) groupNodes.item(i);
			Group group = new Group();
			group.setId(groupElement.getAttribute("nsid"));
			group.setName(groupElement.getAttribute("name"));
			group.setAdmin("1".equals(groupElement.getAttribute("admin")));
			group.setEighteenPlus(groupElement.getAttribute("eighteenplus").equals("0") ? false : true);
			groups.add(group);
		}
		return groups;
	}

	/**
	 * Get a collection of public photos for the specified user ID.
	 *
	 * This method does not require authentication.
	 *
	 * @see com.aetrion.flickr.photos.Extras
	 * @param userId The User ID
	 * @param extras Set of extra-attributes to include (may be null)
	 * @param perPage Number of photos to return per page. If this argument is omitted, it defaults to 100. The maximum allowed value is 500.
	 * @param page The page of results to return. If this argument is omitted, it defaults to 1.
	 * @return The PhotoList collection
	 * @throws IOException
	 * @throws SAXException
	 * @throws FlickrException
	 */
	public PhotoList getPublicPhotos(String userId, Set<String> extras, int perPage, int page) throws IOException, SAXException, FlickrException {

		List<Parameter> parameters = new ArrayList<Parameter>();
		parameters.add(new Parameter("method", METHOD_GET_PUBLIC_PHOTOS));
		parameters.add(new Parameter("api_key", apiKey));

		parameters.add(new Parameter("user_id", userId));

		if (perPage > 0) {
			parameters.add(new Parameter("per_page", "" + perPage));
		}
		if (page > 0) {
			parameters.add(new Parameter("page", "" + page));
		}

		if (extras != null) {
			parameters.add(new Parameter(Extras.KEY_EXTRAS, StringUtilities.join(extras, ",")));
		}

		Response response = transport.get(transport.getPath(), parameters);
		if (response.isError())
			throw new FlickrException(response.getErrorCode(), response.getErrorMessage());
		Element photosElement = response.getPayload();
		PhotoList photos = PhotoUtils.createPhotoList(photosElement);
		return photos;
	}


	/**
	 * Return photos from the given user's photostream. Only photos visible to the calling user will be returned.
	 * This method must be authenticated;
	 * To return public photos for a user, use {@link PeopleInterface#getPublicPhotos(String, Set, int, int)}.
	 *
	 * @see com.aetrion.flickr.photos.Extras
	 * @param userId The User ID
	 * @param minUploadDate Minimum upload date. Photos with an upload date greater than or equal to this value will be returned. The date should be in the form of a unix timestamp.
	 * @param maxUploadDate Maximum upload date. Photos with an upload date less than or equal to this value will be returned. The date should be in the form of a unix timestamp.
	 * @param minTakenDate Minimum taken date. Photos with an taken date greater than or equal to this value will be returned. The date should be in the form of a mysql datetime.
	 * @param maxTakenDate Maximum taken date. Photos with an taken date less than or equal to this value will be returned. The date should be in the form of a mysql datetime.
	 * @param extras Set of extra-attributes to include (may be null)
	 * @param perPage Number of photos to return per page. If this argument is omitted, it defaults to 100. The maximum allowed value is 500.
	 * @param page The page of results to return. If this argument is omitted, it defaults to 1.
	 * @return The PhotoList collection
	 * @throws IOException
	 * @throws SAXException
	 * @throws FlickrException
	 */
	public PhotoList getPhotos(String userId, Date minUploadDate, Date maxUploadDate, Date minTakenDate, Date maxTakenDate, Set<String> extras, int perPage, int page) throws IOException, SAXException, FlickrException {

		List<Parameter> parameters = new ArrayList<Parameter>();
		parameters.add(new Parameter("method", METHOD_GET_PHOTOS));
		parameters.add(new Parameter("api_key", apiKey));

		parameters.add(new Parameter("user_id", userId));

		if (perPage > 0) {
			parameters.add(new Parameter("per_page", "" + perPage));
		}
		if (page > 0) {
			parameters.add(new Parameter("page", "" + page));
		}
		if (minUploadDate != null){
			parameters.add(new Parameter("min_upload_date", "" + (long)(minUploadDate.getTime()/1000)));
		}
		if (maxUploadDate != null){
			parameters.add(new Parameter("max_upload_date", "" + (long)(maxUploadDate.getTime()/1000)));
		}
		if (minTakenDate != null){
			parameters.add(new Parameter("min_taken_date", "" + (long)(minTakenDate.getTime()/1000)));
		}
		if (maxTakenDate != null){
			parameters.add(new Parameter("max_taken_date", "" + (long)(maxTakenDate.getTime()/1000)));
		}
		if (extras != null) {
			parameters.add(new Parameter(Extras.KEY_EXTRAS, StringUtilities.join(extras, ",")));
		}

		parameters.add(new Parameter("api_sig", AuthUtilities.getSignature(sharedSecret, parameters)));

		Response response = transport.get(transport.getPath(), parameters);
		if (response.isError())
			throw new FlickrException(response.getErrorCode(), response.getErrorMessage());
		Element photosElement = response.getPayload();
		PhotoList photos = PhotoUtils.createPhotoList(photosElement);
		return photos;
	}


	/**
	 * Return a list of geo-tagged photos matching some criteria. Only photos visible to the calling user will be returned.
	 * To return private or semi-private photos, the caller must be authenticated with 'read' permissions, and have permission to view the photos.
	 * Unauthenticated calls will only return public photos.
	 *
	 * This method does not require authentication
	 *
	 * A tag, for instance, is considered a limiting agent as are user defined min_date_taken and min_date_upload parameters — If no limiting factor is passed we return only photos added in the last 12 hours (though we may extend the limit in the future).
	 *
	 * Unlike standard photo queries, geo (or bounding box) queries will only return 250 results per page.
	 *
	 * @see com.aetrion.flickr.photos.Extras
	 * @param userId The User ID
	 * @param minUploadDate Minimum upload date. Photos with an upload date greater than or equal to this value will be returned. The date should be in the form of a unix timestamp.
	 * @param maxUploadDate Maximum upload date. Photos with an upload date less than or equal to this value will be returned. The date should be in the form of a unix timestamp.
	 * @param minTakenDate Minimum taken date. Photos with an taken date greater than or equal to this value will be returned. The date should be in the form of a mysql datetime.
	 * @param maxTakenDate Maximum taken date. Photos with an taken date less than or equal to this value will be returned. The date should be in the form of a mysql datetime.
	 * @param minLongitude
	 * @param minLatitude
	 * @param maxLongitude
	 * @param maxLatitude
	 * @param extras Set of extra-attributes to include (may be null)
	 * @param perPage Number of photos to return per page. If this argument is omitted, it defaults to 100. The maximum allowed value is 500.
	 * @param page The page of results to return. If this argument is omitted, it defaults to 1.
	 * @return The PhotoList collection
	 * @throws IOException
	 * @throws SAXException
	 * @throws FlickrException
	 */
	public PhotoList searchWithGeoPhotos(String userId, Date minUploadDate, Date maxUploadDate, Date minTakenDate, Date maxTakenDate, Bbox bbox, Set<String> extras, int perPage, int page) throws IOException, SAXException, FlickrException {

		List<Parameter> parameters = new ArrayList<Parameter>();
		parameters.add(new Parameter("method", METHOD_SEARCH));
		parameters.add(new Parameter("api_key", apiKey));

		parameters.add(new Parameter("user_id", userId));

		if (perPage > 0) {
			parameters.add(new Parameter("per_page", "" + perPage));
		}
		if (page > 0) {
			parameters.add(new Parameter("page", "" + page));
		}
		if (minUploadDate != null){
			parameters.add(new Parameter("min_upload_date", "" + (long)(minUploadDate.getTime()/1000)));
		}
		if (maxUploadDate != null){
			parameters.add(new Parameter("max_upload_date", "" + (long)(maxUploadDate.getTime()/1000)));
		}
		if (minTakenDate != null){
			parameters.add(new Parameter("min_taken_date", "" + (long)(minTakenDate.getTime()/1000)));
		}
		if (maxTakenDate != null){
			parameters.add(new Parameter("max_taken_date", "" + (long)(maxTakenDate.getTime()/1000)));
		}
		if (extras != null) {
			parameters.add(new Parameter(Extras.KEY_EXTRAS, StringUtilities.join(extras, ",")));
		}
		parameters.add(new Parameter("has_geo", 1));
		parameters.add(new Parameter("accuracy", 15));
		if(bbox != null){
			parameters.add(new Parameter("bbox", "" + bbox.minLongitude + "," + bbox.minLatitude + "," + bbox.maxLongitude + "," + bbox.maxLatitude + ","));
		}

		parameters.add(new Parameter("api_sig", AuthUtilities.getSignature(sharedSecret, parameters)));

		Response response = transport.get(transport.getPath(), parameters);
		if (response.isError())
			throw new FlickrException(response.getErrorCode(), response.getErrorMessage());
		Element photosElement = response.getPayload();
		PhotoList photos = PhotoUtils.createPhotoList(photosElement);
		return photos;
	}

	/**
	 * Return a list of geo-tagged photos matching some criteria. Only photos visible to the calling user will be returned.
	 * To return private or semi-private photos, the caller must be authenticated with 'read' permissions, and have permission to view the photos.
	 * Unauthenticated calls will only return public photos.
	 *
	 * This method does not require authentication
	 *
	 * A tag, for instance, is considered a limiting agent as are user defined min_date_taken and min_date_upload parameters — If no limiting factor is passed we return only photos added in the last 12 hours (though we may extend the limit in the future).
	 *
	 * Unlike standard photo queries, geo (or bounding box) queries will only return 250 results per page.
	 *
	 * @see com.aetrion.flickr.photos.Extras
	 * @param lat [-90, 90] A valid latitude, in decimal format, for doing radial geo queries.
	 * @param lon [-180, 180] A valid longitude, in decimal format, for doing radial geo queries.
	 * @param radius [0, 32] A valid radius used for geo queries, greater than zero and less than 20 miles (or 32 kilometers), for use with point-based geo queries. The default value is 5 (km).
	 * @param extras Set of extra-attributes to include (may be null)
	 * @param perPage Number of photos to return per page. If this argument is omitted, it defaults to 100. The maximum allowed value is 500.
	 * @param page The page of results to return. If this argument is omitted, it defaults to 1.
	 * @return The PhotoList collection
	 * @throws IOException
	 * @throws SAXException
	 * @throws FlickrException
	 */
	public PhotoList searchRandomLocationPhotos(double lat, double lon, float radius, Set<String> extras, int perPage, int page) throws IOException, SAXException, FlickrException {

		List<Parameter> parameters = new ArrayList<Parameter>();
		parameters.add(new Parameter("method", METHOD_SEARCH));
		parameters.add(new Parameter("api_key", apiKey));


		if (perPage > 0) {
			parameters.add(new Parameter("per_page", "" + perPage));
		}
		if (page > 0) {
			parameters.add(new Parameter("page", "" + page));
		}

		if (lat >= -90 & lat <= 90) {
			parameters.add(new Parameter("lat", "" + lat));
		} else {
			throw new IllegalArgumentException("wrong parameter lat:" + lat);
		}

		if (lat >= -180 & lat <= 180) {
			parameters.add(new Parameter("lon", "" + lon));
		} else{
			throw new IllegalArgumentException("wrong parameter lon:" + lon);
		}

		if (radius >= 0 & radius <= 32) {
			parameters.add(new Parameter("radius", "" + radius));
		} else {
			throw new IllegalArgumentException("wrong parameter radius:" + radius);
		}

		if (extras != null) {
			parameters.add(new Parameter(Extras.KEY_EXTRAS, StringUtilities.join(extras, ",")));
		}

		parameters.add(new Parameter("api_sig", AuthUtilities.getSignature(sharedSecret, parameters)));

		Response response = transport.get(transport.getPath(), parameters);
		if (response.isError())
			throw new FlickrException(response.getErrorCode(), response.getErrorMessage());
		Element photosElement = response.getPayload();
		PhotoList photos = PhotoUtils.createPhotoList(photosElement);
		return photos;
	}

	/**
	 * Get upload status for the currently authenticated user.
	 *
	 * Requires authentication with 'read' permission using the new authentication API.
	 *
	 * @return A User object with upload status data fields filled
	 * @throws IOException
	 * @throws SAXException
	 * @throws FlickrException
	 */
	public User getUploadStatus() throws IOException, SAXException, FlickrException {
		List<Parameter> parameters = new ArrayList<Parameter>();
		parameters.add(new Parameter("method", METHOD_GET_UPLOAD_STATUS));
		parameters.add(new Parameter("api_key", apiKey));
		parameters.add(new Parameter("api_sig", AuthUtilities.getSignature(sharedSecret, parameters)));

		Response response = transport.get(transport.getPath(), parameters);
		if (response.isError())
			throw new FlickrException(response.getErrorCode(), response.getErrorMessage());
		Element userElement = response.getPayload();
		User user = new User();
		user.setId(userElement.getAttribute("id"));
		user.setPro("1".equals(userElement.getAttribute("ispro")));
		user.setUsername(XMLUtilities.getChildValue(userElement, "username"));

		Element bandwidthElement = XMLUtilities.getChild(userElement, "bandwidth");
		user.setBandwidthMax(bandwidthElement.getAttribute("max"));
		user.setBandwidthUsed(bandwidthElement.getAttribute("used"));

		Element filesizeElement = XMLUtilities.getChild(userElement, "filesize");
		user.setFilesizeMax(filesizeElement.getAttribute("max"));

		return user;
	}
}
