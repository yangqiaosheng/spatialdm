package spade.time.ui;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.SelectDialog;
import spade.time.Date;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: May 8, 2009
 * Time: 3:31:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class TimeDialogs {

	/**
	 * Asks the user about the desired precision of the dates
	 */
	public static char askDesiredDatePrecision() {
		return askDesiredDatePrecision(null, null);
	}

	public static char askDesiredDatePrecision(String comments[]) {
		return askDesiredDatePrecision(comments, null);
	}

	public static char askDesiredDatePrecision(Date sampleDate) {
		return askDesiredDatePrecision(null, sampleDate);
	}

	public static char askDesiredDatePrecision(String comments[], Date sampleDate) {
		SelectDialog selDia = new SelectDialog(CManager.getAnyFrame(), "Desired precision of dates?", "What is the desired precision of the dates/times?");
		if (comments != null) {
			for (String comment : comments) {
				selDia.addLabel(comment);
			}
		}
		char prec = 's';
		if (sampleDate != null) {
			prec = sampleDate.getPrecision();
		}
		if (sampleDate == null || sampleDate.hasElement('s')) {
			selDia.addOption("second", "s", prec == 's');
		}
		if (sampleDate == null || sampleDate.hasElement('t')) {
			selDia.addOption("minute", "t", prec == 't');
		}
		if (sampleDate == null || sampleDate.hasElement('h')) {
			selDia.addOption("hour", "h", prec == 'h');
		}
		if (sampleDate == null || sampleDate.hasElement('d')) {
			selDia.addOption("day", "d", prec == 'd');
		}
		if (sampleDate == null || sampleDate.hasElement('m')) {
			selDia.addOption("month", "m", prec == 'm');
		}
		if (sampleDate == null || sampleDate.hasElement('y')) {
			selDia.addOption("year", "y", prec == 'y');
		}
		selDia.show();
		return selDia.getSelectedOptionId().charAt(0);
	}

	/**
	 * For the given desired precision of dates, creates a suitable scheme
	 */
	public static String getSuitableDateScheme(char precision) {
		if (precision == 's')
			return "dd/mm/yyyy hh:tt:ss";
		if (precision == 't')
			return "dd/mm/yyyy hh:tt";
		if (precision == 'h')
			return "dd/mm/yyyy hh";
		if (precision == 'd')
			return "dd/mm/yyyy";
		if (precision == 'm')
			return "mm/yyyy";
		if (precision == 'y')
			return "yyyy";
		return "dd/mm/yyyy hh:tt:ss";
	}

}
