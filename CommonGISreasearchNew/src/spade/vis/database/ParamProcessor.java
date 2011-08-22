package spade.vis.database;

import java.util.Vector;

import spade.lib.util.BubbleSort;
import spade.lib.util.Comparator;
import spade.lib.util.IdUtil;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.time.Date;
import spade.time.TimeMoment;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.ParamDescription;

/**
* Using information about parameters specified in the Data Source
* Specification (DataSourceSpec) of a DataTable, finds table columns containing
* parameters. Creates attributes depending on parameters
* and "super-attributes" having these attributes as their children.
*/
public class ParamProcessor implements TableProcessor, Comparator {
	/**
	 * The index of the column with the temporal parameter, if any
	 */
	protected int tParColIdx = -1;

	/**
	* Using information about parameters specified in the Data Source
	* Specification (DataSourceSpec) of a DataTable, finds table columns containing
	* parameters. Creates attributes depending on parameters
	* and "super-attributes" having these attributes as their children.
	*/
	@Override
	public void processTable(DataTable table) {
		if (table == null || !table.hasData() || table.getDataSource() == null || !(table.getDataSource() instanceof DataSourceSpec))
			return;
		Vector descriptors = ((DataSourceSpec) table.getDataSource()).descriptors;
		if (descriptors == null)
			return; //no information about parameters
		Vector pds = new Vector(descriptors.size(), 1);//all parameter descriptors found
		for (int i = 0; i < descriptors.size(); i++)
			if (descriptors.elementAt(i) != null && (descriptors.elementAt(i) instanceof ParamDescription)) {
				pds.addElement(descriptors.elementAt(i));
			}
		if (pds.size() < 1)
			return;
		//retrieve names of all columns containing parameters
		Vector parColumns = new Vector(pds.size(), 1);
		for (int i = 0; i < pds.size(); i++) {
			ParamDescription pd = (ParamDescription) pds.elementAt(i);
			if (pd.columnName == null && pd.columnIdx >= 0) {
				pd.columnName = table.getAttributeName(pd.columnIdx);
			}
			if (pd.columnName != null) {
				parColumns.addElement(pd.columnName);
			}
		}
		System.out.println("Found " + pds.size() + " parameters");
		for (int i = 0; i < pds.size(); i++) {
			processParamDescr((ParamDescription) pds.elementAt(i), table, parColumns);
		}
	}

	/**
	* Processes a particular parameter description. Uses information in which
	* table columns there are parameters provided by the argument parColumns
	* (contains the names of the columns with parameters).
	*/
	protected void processParamDescr(ParamDescription pd, DataTable table, Vector parColumns) {
		System.out.println("Processing parameter " + pd.columnName);
		if (pd.columnName != null) {
			pd.columnIdx = table.findAttrByName(pd.columnName);
		}
		if (pd.columnIdx < 0 || pd.columnIdx >= table.getAttrCount())
			return;
		if (pd.isTemporalParameter() && pd.protractKnownValues) {
			//sort the table according to the dates/times;
			//this is important for protracting values!
			System.out.println("Sorting the table according to the temporal references...");
			tParColIdx = pd.columnIdx;
			BubbleSort.sort(table.getData(), this);
			System.out.println("Finished sorting the table according to the temporal references!");
		}
		if (pd.dependentCols != null && pd.dependentCols.size() < 1) {
			pd.dependentCols = null;
		}
		Parameter par = new Parameter();
		par.setName(table.getAttributeName(pd.columnIdx));
		//boolean allStrings=true;
		for (int j = 0; j < table.getDataItemCount(); j++) {
			Object obj = table.getAttrValue(pd.columnIdx, j);
			if (obj != null) {
				/*
				//unclear part: removal of internal zeros from parameter values
				//apparently, was needed for a particular project, but now the purpose
				//cannot be recollected
				if (allStrings && (obj instanceof String)) {
				  String str=(String)obj;
				  int i1=str.length()-1;
				  while (i1>0 && str.charAt(i1)>='0' && str.charAt(i1)<='9') --i1;
				  if (i1<str.length()-2 && str.charAt(i1+1)=='0') {
				    int i2=-1;
				    for (int k=i1+2; k<str.length() && i2<0; k++)
				      if (str.charAt(k)!='0') i2=k;
				    if (i2>i1)
				      obj=str.substring(0,i1+1)+str.substring(i2);
				    else
				      obj=str.substring(0,i1+2);
				    table.getDataRecord(j).setAttrValue(obj,pd.columnIdx);
				  }
				}
				else allStrings=false;
				*/
				par.addValue(obj);
			}
		}
		int nparvalOrig = par.getValueCount();
		if (pd.isTemporalParameter()) {
			par.sortValues();
			//ensure that all parameter values from the minimum to the maximum are present
			TimeMoment t0 = ((TimeMoment) par.getFirstValue()).getCopy(), tLast = ((TimeMoment) par.getLastValue()).getCopy();
			int diff = (int) tLast.subtract(t0), nParVal = par.getValueCount();
			boolean precisionChanged = false;
			if (diff > nParVal * 10 && (t0 instanceof Date)) {
				//if too many parameter values are expected, the precision of the
				//dates/times should be reduced!
				Date startDate = (Date) t0, endDate = (Date) tLast;
				char prec = startDate.getPrecision(), prec1 = prec;
				int precIdx = -1;
				for (int i = Date.time_symbols.length - 1; i > 0 && precIdx < 0; i--)
					if (Date.time_symbols[i] == prec) {
						precIdx = i;
					}
				while (precIdx > 0 && diff > nParVal * 10) {
					--precIdx;
					prec1 = Date.time_symbols[precIdx];
					if (!startDate.hasElement(prec1)) {
						prec1 = prec;
						++precIdx;
						break;
					}
					startDate.setPrecision(prec1);
					endDate.setPrecision(prec1);
					diff = (int) endDate.subtract(startDate);
					/*
					if (diff<nParVal) {
					  ++precIdx;
					  prec1=Date.time_symbols[precIdx];
					  startDate.setPrecision(prec1);
					  endDate.setPrecision(prec1);
					  break;
					}
					*/
				}
				if (prec1 != prec) {
					precisionChanged = true;
					startDate.roundDown();
					endDate.roundUp();
					//change the precision also in the table records
					for (int i = 0; i < table.getDataItemCount(); i++) {
						Object val = table.getAttrValue(pd.columnIdx, i);
						if (val != null && (val instanceof Date)) {
							((Date) val).setPrecision(prec1);
						}
					}
				}
				diff = (int) endDate.subtract(startDate);
			}
			if (precisionChanged || nParVal < diff + 1) {
				Vector v = par.getValues();
				v.removeAllElements();
				//replace the old value list with the list of all consecutive values
				//from t0 up to tLast
				v.addElement(t0);
				TimeMoment t = t0;
				for (int i = 0; i < diff; i++) {
					t = t.getNext();
					v.addElement(t);
				}
			}
		} else if (pd.mustBeOrdered()) {
			Vector order = pd.getValueOrder();
			if (order == null || order.size() < 2) {
				par.sortValues();
			} else {
				par.setValueOrder(order);
			}
		}
		System.out.println("Parameter <" + par.getName() + ">: number of values = " + par.getValueCount());
		for (int j = 0; j < par.getValueCount(); j++) {
			System.out.println(par.getValue(j).toString());
		}
		//transform the table attributes dependent on parameters
		int nparval = par.getValueCount();
		int nattr = (pd.dependentCols != null) ? pd.dependentCols.size() : table.getAttrCount();
		int nResAttr = nattr * nparval + table.getAttrCount() - nattr;
		if (pd.colsToRemove != null) {
			nResAttr -= pd.colsToRemove.size();
		}
		Vector attrs = new Vector(nResAttr, 10);
		IntArray parValN = new IntArray(nResAttr, 10); //N of param. value each new attribute corresponds to
		IntArray sourceColN = new IntArray(nResAttr, 10); //N of the source table columns
		//First process columns with other parameters.
		IntArray parColNs = new IntArray(parColumns.size(), 1);
		for (int i = 0; i < parColumns.size(); i++)
			if (!pd.columnName.equalsIgnoreCase((String) parColumns.elementAt(i))) {
				int idx = table.findAttrByName((String) parColumns.elementAt(i));
				if (idx >= 0) {
					parColNs.addElement(idx);
					attrs.addElement(table.getAttribute(idx));
					parValN.addElement(-1);
					sourceColN.addElement(idx);
				}
			}
		//System.out.println(">>> 1 <<<");
		for (int i = 0; i < table.getAttrCount(); i++) {
			if (i == pd.columnIdx) {
				continue;
			}
			if (parColNs.indexOf(i) >= 0) {
				continue;
			}
			if (pd.colsToRemove != null && pd.colsToRemove.contains(table.getAttributeId(i))) {
				continue;
			}
			Attribute attr = table.getAttribute(i);
			String name = (attr.getParent() == null) ? attr.getName() : attr.getParent().getName();
			String idStr = IdUtil.getPureAttrId(attr.getIdentifier()); //String used for generation of identifiers
			idStr = idStr.replace(' ', '_');
			for (int j = 0; j < attr.getParameterCount(); j++) {
				idStr += "_" + attr.getParamValue(j);
			}
			if (pd.dependentCols == null || StringUtil.isStringInVectorIgnoreCase(name, pd.dependentCols)) {
				//This attribute depends on the parameter.
				//We shall produce from it as many attributes as there are parameter values.
				//We shall also create a super-attribute referring to these derived attributes
				Attribute superAttr = attr.getParent();
				if (superAttr == null) {
					superAttr = attr;
				} else {
					superAttr.removeChild(superAttr.getChildIndex(attr));
				}
				for (int j = 0; j < nparval; j++) {
					Attribute depAttr = new Attribute(idStr + "_" + par.getValue(j).toString(), attr.getType());
					superAttr.addChild(depAttr);
					if (attr.hasParameters()) {
						for (int k = 0; k < attr.getParameterCount(); k++) {
							depAttr.addParamValPair(attr.getParamValPair(k));
						}
					}
					depAttr.addParamValPair(par.getName(), par.getValue(j));
					attrs.addElement(depAttr);
					parValN.addElement(j);
					sourceColN.addElement(i);
					//System.out.println("Produced attribute "+depAttr.getIdentifier()+" "+depAttr.getName());
				}
			} else {//this attribute does not depend on the parameter
				attrs.addElement(attr);
				parValN.addElement(-1);
				sourceColN.addElement(i);
			}
		}
		//System.out.println(">>> 2 <<<");
		/*
		System.out.println("Produced attributes:");
		for (int i=0; i<attrs.size(); i++) {
		  Attribute attr=(Attribute)attrs.elementAt(i);
		  if (attr.getParent()!=null)
		    System.out.println(attr.getName()+" <-- "+attr.getParent().getName());
		  else
		    System.out.println(attr.getName());
		}
		/**/
		//Now start rewriting table data to the new structure.
		Vector data = new Vector(table.getDataItemCount() / nparvalOrig + 10, 10); //vector of DataRecords
		for (int i = 0; i < table.getDataItemCount(); i++) {
			DataRecord rec = table.getDataRecord(i), newRec = null;
			int parValIdx = -1;
			if (nparval < 2) {
				parValIdx = 0;
			} else {
				//get the value of the parameter from the record
				Object parVal = rec.getAttrValue(pd.columnIdx);
				if (parVal == null) {
					System.out.println("ERROR: NULL parameter value found in table record " + i + ", column " + pd.columnIdx + " (" + pd.columnName + ")");
					continue;
				}
				if (parVal instanceof String) {
					parValIdx = StringUtil.indexOfStringInVectorIgnoreCase((String) parVal, par.getValues());
				} else {
					parValIdx = par.getValues().indexOf(parVal);
				}
				if (parValIdx < 0) {
					System.out.println("ERROR: unknown parameter value [" + parVal + "] found in table record " + i + ", column " + pd.columnIdx + " (" + pd.columnName + ")");
					continue;
				}
/*
        if (pd.isTemporalParameter() && rec.getId().equals("60"))
          System.out.println("Parameter value: "+parVal+" idx = "+parValIdx);
*/
			}
			/*
			if (i%100==0)
			  System.out.println(i+") "+parVal);
			/**/
			if (nparval > 1) {
				//find the corresponding record among the new records
				for (int j = 0; j < data.size() && newRec == null; j++) {
					DataRecord r = (DataRecord) data.elementAt(j);
					if (!r.getId().equalsIgnoreCase(rec.getId())) {
						continue;
					}
					boolean ok = true;
					for (int k = 0; k < parColNs.size() && ok; k++) {
						Object value = rec.getAttrValue(parColNs.elementAt(k));
						if (value == null || value instanceof String) {
							ok = StringUtil.sameStringsIgnoreCase((String) value, r.getAttrValueAsString(k));
						} else {
							ok = value.equals(r.getAttrValue(k));
						}
					}
					if (ok) {
						newRec = r;
					}
				}
			}
			if (newRec == null) {
				newRec = new DataRecord(rec.getId(), rec.getName());
				newRec.setAttrList(attrs);
				for (int k = 0; k < parColNs.size(); k++) {
					newRec.addAttrValue(rec.getAttrValue(parColNs.elementAt(k)));
				}
				for (int j = parColNs.size(); j < attrs.size(); j++) {
					newRec.addAttrValue(null);
				}
				data.addElement(newRec);
			}
			for (int j = parColNs.size(); j < attrs.size(); j++)
				if (parValN.elementAt(j) < 0 || parValN.elementAt(j) == parValIdx) {
					Object value = rec.getAttrValue(sourceColN.elementAt(j));
					if (value != null) {
						newRec.setAttrValue(value, j);
						if (pd.isTemporalParameter() && pd.protractKnownValues) {
							//fill the same value for the following time moments
							for (int k = j + 1; k < attrs.size(); k++)
								if (sourceColN.elementAt(k) == sourceColN.elementAt(j) && parValN.elementAt(k) > parValIdx) {
									newRec.setAttrValue(value, k);
								}
						}
					}
				}
			/*
			if (pd.isTemporalParameter() && newRec.getId().equals("60") && newRec.getAttrCount()>=400) {
			  System.out.print(newRec.getId()+":");
			  for (int j=150; j<250; j++)
			    System.out.print(" "+newRec.getAttrValue(j));
			  System.out.println();
			}
			*/
		}
		//System.out.println(">>> 3 <<<");
		if (data.size() < 1) {
			System.out.println("ERROR: failed to transform the table according to" + " parameter " + pd.columnName);
		} else {
			table.removeAllData();
			table.setAttrList(attrs);
			for (int i = 0; i < data.size(); i++) {
				table.addDataRecord((DataRecord) data.elementAt(i));
			}
			table.addParameter(par);
			System.out.println("Resulting table contains " + data.size() + " records");
			/*
			System.out.println("Identifiers of resulting attributes:");
			for (int i=0; i<table.getAttrCount(); i++)
			  System.out.println(table.getAttributeId(i));
			*/
		}
	}

	@Override
	public int compare(Object item1, Object item2) {
		if (tParColIdx < 0)
			return 0;
		if (item1 == null)
			if (item2 == null)
				return 0;
			else
				return 1;
		if (item2 == null)
			return -1;
		if (!(item1 instanceof DataRecord) || !(item2 instanceof DataRecord))
			return 0;
		DataRecord rec1 = (DataRecord) item1, rec2 = (DataRecord) item2;
		Object val1 = rec1.getAttrValue(tParColIdx), val2 = rec2.getAttrValue(tParColIdx);
		if (val1 == null)
			if (val2 == null)
				return 0;
			else
				return 1;
		if (val2 == null)
			return -1;
		if (!(val1 instanceof TimeMoment) || !(val2 instanceof TimeMoment))
			return 0;
		return ((TimeMoment) val1).compareTo((TimeMoment) val2);
	}
}