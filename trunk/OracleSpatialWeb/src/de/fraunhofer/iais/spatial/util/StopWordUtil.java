package de.fraunhofer.iais.spatial.util;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import au.com.bytecode.opencsv.CSVReader;

public class StopWordUtil {

	public static Set<String> stopwordsFlickr = new HashSet<String>();
	public static Set<String> stopwordsCamera = new HashSet<String>();
	public static Set<String> stopwordsDe = new HashSet<String>();
	public static Set<String> stopwordsEn = new HashSet<String>();
	public static Set<String> stopwordsIt = new HashSet<String>();
	public static Set<String> stopwords = new HashSet<String>();

	static {
		String filenameFlickr = "data/stopwords/stopwords_flickr.txt";
		String filenameCamera = "data/stopwords/stopwords_camera.txt";
		String filenameDe = "data/stopwords/stopwords_de.txt";
		String filenameEn = "data/stopwords/stopwords_en.txt";
		String filenameIt = "data/stopwords/stopwords_it.txt";
		initStopwords(stopwordsCamera, filenameCamera);
		initStopwords(stopwordsFlickr, filenameFlickr);
		initStopwords(stopwordsDe, filenameDe);
		initStopwords(stopwordsEn, filenameEn);
		initStopwords(stopwordsIt, filenameIt);
		stopwords.addAll(stopwordsFlickr);
		stopwords.addAll(stopwordsCamera);
		stopwords.addAll(stopwordsDe);
		stopwords.addAll(stopwordsEn);
		stopwords.addAll(stopwordsIt);
	}

	private static void initStopwords(Set<String> stopwords, String filename){
		CSVReader readerDe = null;
		try {
			readerDe = new CSVReader(new FileReader(filename));
			String[] nextLine;

			for (int i = 0; (nextLine = readerDe.readNext()) != null; i++) {
				if (i > 0) {
					stopwords.add(StringUtils.trimToEmpty(nextLine[0]));
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try {
				readerDe.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
