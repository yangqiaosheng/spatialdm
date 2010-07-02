package de.fraunhofer.iais.spatial.scheduling;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;

import de.fraunhofer.iais.spatial.servlet.RequestKml;

/**
 * Timer Task to delete the old temporary kml files in the kml\ directory
 * @author zhi
 *
 */
public class CleanOldKmlsTimerTask extends TimerTask {
	
	// period of existing, format "yyMMddHHmmss"
	private long period = 000000000100;	// 1 minute
	
	@Override
	public void run() {
		System.out.println("delete kmls:");		
		File kmlPath = new File("../webapps/OracleSpatialWeb/" + RequestKml.kmlPath);
		File files[]=kmlPath.listFiles(); 
		System.out.println(kmlPath.getAbsolutePath());	
		long currentDate = Long.parseLong(new SimpleDateFormat("yyMMddHHmmss").format(new Date()));
		for(File f : files){
			String filename = f.getName();
			if(filename.length()==25 && filename.matches("\\d{12}-\\w{8}.kml")){
				long fileDate = Long.parseLong(filename.substring(0, 12));
				if(currentDate > fileDate + period){
					System.out.println("delete:"+filename);		
					f.delete();
				}
			}
		}
	}

}
