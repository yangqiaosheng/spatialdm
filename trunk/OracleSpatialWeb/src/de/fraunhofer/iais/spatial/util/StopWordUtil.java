package de.fraunhofer.iais.spatial.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import de.fraunhofer.iais.spatial.web.servlet.RequestKmlServlet;

import au.com.bytecode.opencsv.CSVReader;

public class StopWordUtil {

	private static Set<String> stopwordsFlickr = new HashSet<String>();
	private static Set<String> stopwordsCamera = new HashSet<String>();
	private static Set<String> stopwordsDe = new HashSet<String>();
	private static Set<String> stopwordsEn = new HashSet<String>();
	private static Set<String> stopwordsIt = new HashSet<String>();

	public static Set<String> stopwordsTemp = new HashSet<String>();
	public static Set<String> stopwordsGloble = new HashSet<String>();

	static {
		String basePath = "WebRoot/";
		if(!new File(basePath + "data/stopwords").isDirectory()){
			basePath = StopWordUtil.class.getResource("/../../").getPath();
		}
		String filenameFlickr = basePath + "data/stopwords/stopwords_flickr.txt";
		String filenameCamera =  basePath + "data/stopwords/stopwords_camera.txt";
		String filenameDe =  basePath + "data/stopwords/stopwords_de.txt";
		String filenameEn =  basePath + "data/stopwords/stopwords_en.txt";
		String filenameIt =  basePath + "data/stopwords/stopwords_it.txt";
		String filenameTemp =  basePath + "data/stopwords/stopwords_temp.txt";

		initStopwords(stopwordsCamera, filenameCamera);
		initStopwords(stopwordsFlickr, filenameFlickr);
		initStopwords(stopwordsDe, filenameDe);
		initStopwords(stopwordsEn, filenameEn);
		initStopwords(stopwordsIt, filenameIt);
		initStopwords(stopwordsTemp, filenameTemp);

		stopwordsGloble.addAll(stopwordsFlickr);
		stopwordsGloble.addAll(stopwordsCamera);
		stopwordsGloble.addAll(stopwordsDe);
		stopwordsGloble.addAll(stopwordsEn);
		stopwordsGloble.addAll(stopwordsIt);
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
