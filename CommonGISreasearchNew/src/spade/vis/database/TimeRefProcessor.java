package spade.vis.database;

import java.util.Vector;

import spade.lib.util.IdMaker;
import spade.time.Date;
import spade.time.TimeCount;
import spade.time.TimeMoment;
import spade.time.TimeRefDescription;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.ParamDescription;

/**
* Using information about time references specified in the Data Source
* Specification (DataSourceSpec) of a DataTable, finds table columns containing
* time references and transforms them into temporal attributes.
*/
public class TimeRefProcessor implements TableProcessor {
	/**
	* Using information about time references specified in the Data Source
	* Specification (DataSourceSpec) of a DataTable, finds table columns containing
	* time references and transforms them into temporal attributes.
	*/
	@Override
	public void processTable(DataTable table) {
		if (table == null || !table.hasData() || table.getDataSource() == null || !(table.getDataSource() instanceof DataSourceSpec))
			return;
		Vector descriptors = ((DataSourceSpec) table.getDataSource()).descriptors;
		if (descriptors == null)
			return; //no information about time references
		for (int i = 0; i < descriptors.size(); i++)
			if (descriptors.elementAt(i) != null && (descriptors.elementAt(i) instanceof TimeRefDescription)) {
				TimeRefDescription td = (TimeRefDescription) descriptors.elementAt(i);
				if (td.sourceColumns == null) {
					continue;
				}
				//check if all the columns are present in the table
				int cn[] = new int[td.sourceColumns.length];
				boolean error = false;
				for (int j = 0; j < cn.length && !error; j++) {
					cn[j] = table.findAttrByName(td.sourceColumns[j]);
					error = cn[j] < 0;
					if (error) {
						System.out.println("ERROR processing time references: column <" + td.sourceColumns[j] + "> not found in the table " + table.getName());
					}
				}
				if (error) {
					continue;
				}
				//check each scheme whether it is simple (i.e. contains a single count)
				//or compound
				boolean isSimple[] = new boolean[cn.length];
				for (int k = 0; k < cn.length; k++) {
					int nsymb = 0;
					for (int j = 0; j < TimeRefDescription.TIME_SYMBOLS.length && nsymb < 2; j++)
						if (td.schemes[k].indexOf(TimeRefDescription.TIME_SYMBOLS[j]) >= 0) {
							++nsymb;
						}
					isSimple[k] = nsymb < 2;
					//check if the scheme contains any symbols other than the legal date/time
					//symbols
					for (int j = 0; j < td.schemes[k].length() && isSimple[k]; j++) {
						isSimple[k] = TimeRefDescription.isTimeSymbol(td.schemes[k].charAt(j));
					}
				}
				boolean simpleDate = cn.length == 1 && isSimple[0];
				//these Date structures will be used for transforming strings to dates
				Date utilDates[] = null;
				if (!simpleDate) {
					utilDates = new Date[cn.length];
					for (int j = 0; j < cn.length; j++)
						if (!isSimple[j]) {
							utilDates[j] = new Date();
							utilDates[j].setDateScheme(td.schemes[j]);
						}
				}
				//find or construct the corresponding temporal attribute in the table
				boolean replace = td.isParameter || !td.keepOrigColumns;
				if (td.attrName.equals("#replace#")) {
					replace = true;
					td.attrName = td.sourceColumns[0];
				}
				if (td.attrName == null)
					if (replace) {
						td.attrName = td.sourceColumns[0];
					} else {
						td.attrName = "_time_";
					}
				int aidx = table.findAttrByName((replace) ? td.sourceColumns[0] : td.attrName);
				if (aidx >= 0 && !replace && (!td.attrBuilt || !table.isAttributeTemporal(aidx))) {
					td.attrName = "_time_";
					for (int j = 0; j < table.getAttrCount() && table.findAttrByName(td.attrName) >= 0; j++) {
						td.attrName = "_time_" + (j + 1);
					}
					aidx = -1;
				}
				if (aidx < 0) {
					Attribute attr = new Attribute(IdMaker.makeId(td.attrName, table), AttributeTypes.time);
					attr.setName(td.attrName);
					attr.timeRefMeaning = td.meaning;
					table.addAttribute(attr);
					aidx = table.getAttrCount() - 1;
					td.attrBuilt = true;
				} else if (replace) {
					Attribute attr = table.getAttribute(aidx);
					if (!td.attrName.equals(attr.getName())) {
						attr.setName(td.attrName);
					}
					attr.setType(AttributeTypes.time);
					attr.timeRefMeaning = td.meaning;
				}
				//create time moments from strings
				String dateScheme = td.attrScheme; //the scheme of the dates to be formed
				for (int j = 0; j < table.getDataItemCount(); j++) {
					TimeMoment tm = null;
					boolean missing = false;
					for (int k = 0; k < cn.length && !missing; k++) {
						String val = table.getAttrValueAsString(cn[k], j);
						missing = (val == null || val.length() < 1);
						if (missing) {
							break;
						}
						if (simpleDate) {
							tm = new TimeCount();
							missing = !tm.setMoment(val);
						} else {
							if (utilDates[k] != null) {
								missing = !utilDates[k].setMoment(val);
								if (!missing) {
									Date d = null;
									if (cn.length == 1) {
										d = (Date) utilDates[k].getCopy();
									} else {
										if (tm != null) {
											d = (Date) tm;
										} else {
											d = new Date();
										}
										for (char elem : TimeRefDescription.TIME_SYMBOLS) {
											if (utilDates[k].hasElement(elem)) {
												d.setElementValue(elem, utilDates[k].getElementValue(elem));
											}
										}
									}
									if (tm == null) {
										tm = d;
									}
								}
							} else {
								//transform the string into a number and set the corresponding date element
								try {
									int num = Integer.valueOf(val).intValue();
									Date d = (tm != null) ? (Date) tm : new Date();
									for (char elem : TimeRefDescription.TIME_SYMBOLS) {
										if (td.schemes[k].indexOf(elem) >= 0) {
											d.setElementValue(elem, num);
											break;
										}
									}
									if (tm == null) {
										tm = d;
									}
								} catch (NumberFormatException nfe) {
									missing = true;
								}
							}
						}
					}
					if (missing) {
						continue;
					}
					if (tm != null) {
						table.getDataRecord(j).setAttrValue(tm, aidx);
						if (tm instanceof Date) {
							Date d = (Date) tm;
							if (dateScheme != null) {
								d.setDateScheme(dateScheme);
							} else if (d.scheme == null) {
								d.setDefaultScheme();
								dateScheme = d.scheme;
								System.out.println("Date scheme = " + dateScheme);
							}
						}
						//System.out.println(tm.toString());
					}
				}
				if (td.isParameter) {
					ParamDescription pd = new ParamDescription();
					pd.columnName = table.getAttributeName(aidx);
					pd.dependentCols = td.dependentCols;
					pd.internal = true;
					pd.isTemporal = true;
					pd.setMustBeOrdered(true);
					pd.protractKnownValues = td.protractKnownValues;
					descriptors.addElement(pd);
				}
				if (replace && cn.length > 1) {
					Vector colsToRemove = new Vector(cn.length, 1);
					for (int element : cn)
						if (element != aidx) {
							colsToRemove.addElement(table.getAttributeId(element));
						}
					for (int j = 0; j < colsToRemove.size(); j++) {
						table.removeAttribute((String) colsToRemove.elementAt(j));
					}
				}
			}
	}
}