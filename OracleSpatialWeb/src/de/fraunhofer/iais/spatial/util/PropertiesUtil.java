package de.fraunhofer.iais.spatial.util;

import java.io.File;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.http.HttpServlet;

public class PropertiesUtil {

	public void showServletProperties(HttpServlet servlet) {
		System.out.println("ServletContextPath:" + servlet.getServletContext().getContextPath());
		System.out.println("ServletRealPath:" + servlet.getServletContext().getRealPath(""));
	}

	public void showSystemProperties() {
		Properties p = System.getProperties();
		for (Enumeration e = p.propertyNames(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
			System.out.println(key + "===>" + p.getProperty(key));
		}
	}

	public void showPath() {
		System.out.println("classpath:" + this.getClass().getResource("/../../kml/").getPath());
		System.out.println("current class dir:" + this.getClass().getResource(""));
		System.out.println("current path:" + new File("").getAbsolutePath());
		System.out.println("user.dir:" + System.getProperty("user.dir"));
		System.out.println("catalina.home:" + System.getProperty("catalina.home"));
	}
}
