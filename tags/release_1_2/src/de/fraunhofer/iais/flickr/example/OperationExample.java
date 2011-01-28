package de.fraunhofer.iais.flickr.example;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.RequestContext;
import com.aetrion.flickr.activity.ActivityInterface;
import com.aetrion.flickr.activity.Event;
import com.aetrion.flickr.activity.Item;
import com.aetrion.flickr.activity.ItemList;
import com.aetrion.flickr.auth.Auth;
import com.aetrion.flickr.auth.Permission;
import com.aetrion.flickr.people.PeopleInterface;
import com.aetrion.flickr.photos.Extras;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotoList;
import com.aetrion.flickr.photos.PhotosInterface;
import com.aetrion.flickr.places.Location;
import com.aetrion.flickr.places.Place;
import com.aetrion.flickr.places.PlacesInterface;

/**
 * Demonstration of howto use the ActivityInterface.
 *
 * @author mago
 * @version $Id: ActivityExample.java,v 1.3 2008/07/05 22:19:48 x-mago Exp $
 */
public class OperationExample {
	static String apiKey;
	static String sharedSecret;
	Flickr f;
	RequestContext requestContext;
	Properties properties = null;

	public OperationExample() throws ParserConfigurationException, IOException {
		InputStream in = null;
		in = getClass().getResourceAsStream("/flickr.properties");
		properties = new Properties();
		properties.load(in);
		f = new Flickr(properties.getProperty("apiKey_0"), properties.getProperty("secret_0"), new REST());
		requestContext = RequestContext.getRequestContext();
		Auth auth = new Auth();
		auth.setPermission(Permission.READ);
		auth.setToken(properties.getProperty("token_0"));
		requestContext.setAuth(auth);
		Flickr.debugRequest = true;
		Flickr.debugStream = true;
	}

	public void showPeoplesPhotos() throws IOException, SAXException, FlickrException {
		PeopleInterface people = f.getPeopleInterface();
		Set<String> extras = new HashSet<String>();
		extras.add(Extras.DATE_TAKEN);
		extras.add(Extras.DATE_UPLOAD);
		extras.add(Extras.GEO);
		extras.add(Extras.VIEWS);
		extras.add(Extras.LICENSE);

//		String userId = "49596882@N02";
		String userId = "27076251@N05";
//		PhotoList pl = people.getPublicPhotos(userId, Extras.ALL_EXTRAS, 20, 2);
		Calendar maxTakenDate = Calendar.getInstance();
		maxTakenDate.set(2010, 10, 1);

		Calendar minTakenDate = Calendar.getInstance();
		minTakenDate.set(2010, 9, 1);

		PhotoList pl = people.getPhotos(userId, null, null, minTakenDate.getTime(), maxTakenDate.getTime(), extras, 20, 1);
		System.out.println("total:" + pl.getTotal());
		for (int i = 0; i < pl.size(); i++) {
			Photo p = (Photo) pl.get(i);
			System.out.println(p.getId() + ":" + p.getDateTaken().toLocaleString() + " | " + p.getDatePosted().toLocaleString() + ":" + p.getGeoData() + ":" + p.getDescription() + ":" + p.getPlaceId() + ":" + p.getWoeId());
			PlacesInterface placeI = f.getPlacesInterface();
			if (p.getPlaceId() != null && !"".equals(p.getPlaceId())) {
//				System.out.println("place_id:" + p.getPlaceId());
//				Location location = placeI.getInfo(p.getPlaceId(), p.getWoeId());
//				System.out.println(location.getPlaceUrl());
			}
		}
	}

	public void showRecentPhotos() throws IOException, SAXException, FlickrException {
		PhotosInterface photo = f.getPhotosInterface();
		Calendar date = Calendar.getInstance();
		date.roll(Calendar.YEAR, false);
		//		PhotoList pl = photo.recentlyUpdated(date.getTime(), null, 0, 0);
		PhotoList pl = photo.getRecent(Extras.ALL_EXTRAS, 50, 670);
		System.out.println("pages:" + pl.getPages());
		for (int i = 1; i < pl.size(); i++) {
			Photo p = (Photo) pl.get(i);

			System.out.println(p.getId() + "|" + p.getDateTaken() + "|" + p.getDatePosted());
		}
	}

	public void showSearchPhotos() throws IOException, SAXException, FlickrException {
		PhotosInterface photo = f.getPhotosInterface();
		Calendar date = Calendar.getInstance();
		date.roll(Calendar.YEAR, false);
		//		PhotoList pl = photo.recentlyUpdated(date.getTime(), null, 0, 0);
		PhotoList pl = photo.getRecent(Extras.ALL_EXTRAS, 50, 670);
		System.out.println("pages:" + pl.getPages());
		for (int i = 1; i < pl.size(); i++) {
			Photo p = (Photo) pl.get(i);

			System.out.println(p.getId() + "|" + p.getDateTaken() + "|" + p.getDatePosted());
		}
	}

	public void showActivity() throws FlickrException, IOException, SAXException {
		ActivityInterface iface = f.getActivityInterface();
		ItemList list = iface.userComments(10, 1);
		System.out.println(list.size());
		for (int j = 0; j < list.size(); j++) {
			Item item = (Item) list.get(j);
			System.out.println("Item " + (j + 1) + "/" + list.size() + " type: " + item.getType());
			System.out.println("Item-id:       " + item.getId() + "\n");
			ArrayList events = (ArrayList) item.getEvents();
			for (int i = 0; i < events.size(); i++) {
				System.out.println("Event " + (i + 1) + "/" + events.size() + " of Item " + (j + 1));
				System.out.println("Event-type: " + ((Event) events.get(i)).getType());
				System.out.println("User:       " + ((Event) events.get(i)).getUser());
				System.out.println("Username:   " + ((Event) events.get(i)).getUsername());
				System.out.println("Value:      " + ((Event) events.get(i)).getValue() + "\n");
			}
		}
		ActivityInterface iface2 = f.getActivityInterface();
		list = iface2.userPhotos(50, 0, "300d");
		for (int j = 0; j < list.size(); j++) {
			Item item = (Item) list.get(j);
			System.out.println("Item " + (j + 1) + "/" + list.size() + " type: " + item.getType());
			System.out.println("Item-id:       " + item.getId() + "\n");
			ArrayList events = (ArrayList) item.getEvents();
			for (int i = 0; i < events.size(); i++) {
				System.out.println("Event " + (i + 1) + "/" + events.size() + " of Item " + (j + 1));
				System.out.println("Event-type: " + ((Event) events.get(i)).getType());
				if (((Event) events.get(i)).getType().equals("note")) {
					System.out.println("Note-id:    " + ((Event) events.get(i)).getId());
				} else if (((Event) events.get(i)).getType().equals("comment")) {
					System.out.println("Comment-id: " + ((Event) events.get(i)).getId());
				}
				System.out.println("User:       " + ((Event) events.get(i)).getUser());
				System.out.println("Username:   " + ((Event) events.get(i)).getUsername());
				System.out.println("Value:      " + ((Event) events.get(i)).getValue());
				System.out.println("Dateadded:  " + ((Event) events.get(i)).getDateadded() + "\n");
			}
		}
	}

	public static void main(String[] args) throws ParserConfigurationException, IOException, FlickrException, SAXException {
		OperationExample t = new OperationExample();
		//		t.showActivity();
		t.showPeoplesPhotos();
//		t.showRecentPhotos();
	}

}
