package de.fraunhofer.iais.spatial.util;

public class StringTF {

	/**
	 * change the format of the month
	 * eg. "January" --> "01"
	 * @param s - String
	 * @return String
	 */
	public static String FullMonth2Num(String s){
		s = s.replaceAll("January", "01")
		 	 .replaceAll("February", "02")
		 	 .replaceAll("March", "03")
		 	 .replaceAll("April", "04")
		 	 .replaceAll("May", "05")
		 	 .replaceAll("June", "06")
		 	 .replaceAll("Juli", "07")
		 	 .replaceAll("August", "08")
		 	 .replaceAll("September", "09")
		 	 .replaceAll("October", "10")
		 	 .replaceAll("November", "11")
		 	 .replaceAll("December", "12");
	return s;
	}
	
	/**
	 * change the format of the month
	 * eg. "Jan." --> "01"
	 * @param s - String
	 * @return String
	 */	 
	public static String ShortMonth2Num(String s){
		s = s.replaceAll("Jan.", "01")
			 .replaceAll("Feb.", "02")
			 .replaceAll("Mar.", "03")
			 .replaceAll("Apr.", "04")
			 .replaceAll("May.", "05")
			 .replaceAll("Jun.", "06")
			 .replaceAll("Jul.", "07")
			 .replaceAll("Aug.", "08")
			 .replaceAll("Sep.", "09")
			 .replaceAll("Oct.", "10")
			 .replaceAll("Nov.", "11")
			 .replaceAll("Dec.", "12");
		return s;
	}
	
	/**
	 * change the format of the day for the xml request
	 * eg. "1" --> "01"
	 * @param s - String
	 * @return String
	 */
	public static String ShortNum2Long(String s){
		s = s.replaceAll("<day>1</day>", "<day>01</day>")
			 .replaceAll("<day>2</day>", "<day>02</day>")
			 .replaceAll("<day>3</day>", "<day>03</day>")
			 .replaceAll("<day>4</day>", "<day>04</day>")
			 .replaceAll("<day>5</day>", "<day>05</day>")
			 .replaceAll("<day>6</day>", "<day>06</day>")
			 .replaceAll("<day>7</day>", "<day>07</day>")
			 .replaceAll("<day>8</day>", "<day>08</day>")
			 .replaceAll("<day>9</day>", "<day>09</day>");
		return s;
	}
}
