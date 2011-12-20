package de.fraunhofer.iais.spatial.scheduling;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iais.spatial.web.servlet.RequestKmlServlet;

/**
 * Timer Task to delete the old temporary kml files in the kml\ directory
 * @author haolin_zhi
 *
 */
public class CleanOldKmlsTimerTask extends TimerTask {

	/**
	 * Logger for this class
	 */
	private static final Logger logger = LoggerFactory.getLogger(CleanOldKmlsTimerTask.class);

	// delay deleting period to avoid threads competition,
	// format: "yyMMddHHmmss"
	private long period = 000000000100; // 1 minute

	@Override
	public void run() {

		try {
			//		File kmlPath = new File("../webapps/OracleSpatialWeb/" + RequestKml.kmlPath);
			File kmlPath = new File(System.getProperty("ess.root") + RequestKmlServlet.kmlPath);
//			File kmlPath = new File(this.getClass().getResource("/../../" + RequestKmlServlet.kmlPath).getPath());

			File files[] = kmlPath.listFiles();
			logger.debug("run() - kmlPath:" + kmlPath.getAbsolutePath()); //$NON-NLS-1$
			String now = new SimpleDateFormat("yyMMddHHmmss").format(new Date());
			long currentDate = Long.parseLong(now);
			for (File f : files) {
				String filename = f.getName();
				if (filename.length() == 25 && filename.matches("\\d{12}-\\w{8}.km\\w{1}")) {
					long fileDate = Long.parseLong(filename.substring(0, 12));
					if (currentDate > fileDate + period) {
						logger.debug("run() - delete:" + filename); //$NON-NLS-1$
						f.delete();
					}
				}
			}
		} catch (Exception e) {
			logger.error("", e); //$NON-NLS-1$
		}

	}

}
