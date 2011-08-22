package core;

import java.io.StringWriter;
import java.util.GregorianCalendar;
import java.util.Vector;

import spade.lib.util.Parameters;
import spade.lib.util.StringUtil;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.space.GeoLayer;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 10, 2009
 * Time: 11:19:09 AM
 * Describes an action done in the process of analysis.
 */
public class ActionDescr {
	/**
	 * The name of the action
	 */
	public String aName = null;
	/**
	 * The time moment (in milliseconds) when the action was started
	 */
	public long startTime = 0L;
	/**
	 * The time moment (in milliseconds) when the action was accomplished
	 */
	public long endTime = 0L;
	/**
	 * The parameters of the action: pairs (name,value)
	 */
	public Parameters params = null;
	/**
	 * The results of the action, e.g. map layers, tables, attributes in a table, ...
	 */
	public Vector<ResultDescr> results = null;
	/**
	 * User's comment about the action (if any)
	 */
	public String comment = null;
	/**
	 * This may be a URL of a document describing the action
	 */
	public String descrURL = null;

	public void addParamValue(String paramName, Object value) {
		if (paramName == null)
			return;
		if (params == null) {
			params = new Parameters();
		}
		params.setParameter(paramName, value);
	}

	public void addResultDescr(ResultDescr rd) {
		if (rd == null)
			return;
		if (results == null) {
			results = new Vector<ResultDescr>(10, 10);
		}
		results.addElement(rd);
	}

	public String getDescription() {
		StringWriter writer = new StringWriter();
		if (startTime > 0 || endTime > 0) {
			GregorianCalendar date = new GregorianCalendar();
			if (startTime > 0) {
				date.setTimeInMillis(startTime);
				writer.write(StringUtil.dateTimeToString(date));
				if (endTime > 0) {
					writer.write(" - ");
				}
			}
			if (endTime > 0) {
				date.setTimeInMillis(endTime);
				writer.write(StringUtil.dateTimeToString(date));
			}
			if (startTime > 0 && endTime > 0 && endTime > startTime) {
				double dur = endTime - startTime;
				writer.write(" (duration " + dur + "msec");
				dur /= 1000;
				if (dur >= 1) {
					writer.write(", " + dur + "sec");
					dur /= 60;
					if (dur >= 1) {
						writer.write(", " + dur + "min");
					}
				}
				writer.write(")");
			}
			writer.write("\r\n");
		}
		writer.write(aName + "\r\n");
		if (params != null && params.getParamCount() > 0) {
			for (int i = 0; i < params.getParamCount(); i++) {
				Object pair[] = params.getParamValuePair(i);
				if (pair != null) {
					writer.write(pair[0] + " = " + pair[1] + "\r\n");
				}
			}
		}
		if (comment != null) {
			writer.write(comment + "\r\n");
		}
		if (results != null && results.size() > 0) {
			writer.write(((results.size() < 2) ? "Result" : "Results") + ":\r\n");
			for (int i = 0; i < results.size(); i++) {
				ResultDescr rd = results.elementAt(i);
				if (results.size() > 1) {
					writer.write((i + 1) + ") ");
				}
				writer.write(describeObject(rd.product));
				if (rd.owner != null) {
					writer.write(" in " + describeObject(rd.owner));
				}
				if (rd.comment != null) {
					writer.write(" (" + rd.comment + ")");
				}
				writer.write("\r\n");
			}
		}
		return writer.toString();
	}

	protected String describeObject(Object obj) {
		if (obj == null)
			return null;
		if (obj instanceof GeoLayer)
			return "layer " + ((GeoLayer) obj).getContainerIdentifier() + " \"" + ((GeoLayer) obj).getName() + "\"";
		if (obj instanceof AttributeDataPortion)
			return "table " + ((AttributeDataPortion) obj).getContainerIdentifier() + " \"" + ((AttributeDataPortion) obj).getName();
		if (obj instanceof Attribute)
			return "attribute " + ((Attribute) obj).getName();
		return obj.toString();
	}
}
