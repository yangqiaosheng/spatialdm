package spade.analysis.tools.db_tools;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.time.TimeCount;
import spade.time.TimeMoment;
import db_work.data_descr.ColumnDescriptor;
import db_work.data_descr.ColumnDescriptorDate;
import db_work.data_descr.ColumnDescriptorNum;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 27-Nov-2007
 * Time: 15:46:58
 * To change this template use File | Settings | File Templates.
 */
public class STAggregationUI extends Panel implements ActionListener, ItemListener {

	protected Frame parent = null;
	protected DBTableDescriptor currTblD = null;
	protected ColumnDescriptor cdd = null;

	// spatial aggregation
	protected Checkbox cbSpatialGrid = null, cbDirectionAndSpeed = null;
	protected float fMinSpeed = 5f; // Float.NaN;
	protected int iNDirections = 4; // -1;
	protected boolean bSDmeasuredValsAvailable = false, bSDcomputedValsAvailable = false;
	protected String sSpeedColumnName = null, sDirectionColumnName = null;

	public boolean getDoSpatialGrid() {
		return cbSpatialGrid != null && cbSpatialGrid.getState();
	}

	public boolean getDoDirectionAndSpeed() {
		return cbDirectionAndSpeed != null && cbDirectionAndSpeed.getState();
	}

	public float getDandSMinSpeed() {
		return fMinSpeed;
	}

	public int getDandSNDirections() {
		return iNDirections;
	}

	public String getDandSSpeedColumnName() {
		return (getDoDirectionAndSpeed()) ? sSpeedColumnName : null;
	}

	public String getDandSDirectionColumnName() {
		return (getDoDirectionAndSpeed()) ? sDirectionColumnName : null;
	}

	// temporal aggregation
	protected List listTime = null;
	protected Button bAddTime = null, bRemoveTime = null;
	public Vector vDivisionSpecTime = null;

	// attribute aggregation
	// ... to be done later

	public STAggregationUI(Frame parent, DBTableDescriptor currTblD, ColumnDescriptor cdd) {
		super();
		this.parent = parent;
		this.currTblD = currTblD;
		this.cdd = cdd;
		setLayout(new ColumnLayout());
		add(new Label("Space:"));
		add(cbSpatialGrid = new Checkbox("Spatial aggregation on regular grid", currTblD.xColIdx >= 0));
		cbSpatialGrid.setEnabled(false);
		if (currTblD.dataMeaning == DataSemantics.MOVEMENTS) {
			// check if speed/direction columns are present in the table
			bSDmeasuredValsAvailable = currTblD.relevantColumns[7] != null && currTblD.relevantColumns[8] != null;
			bSDcomputedValsAvailable = currTblD.relevantColumns[12] != null && currTblD.relevantColumns[14] != null;
			if (bSDmeasuredValsAvailable || bSDcomputedValsAvailable) {
				add(cbDirectionAndSpeed = new Checkbox("Aggregation by directions and speed", false));
				cbDirectionAndSpeed.addItemListener(this);
			}
		}
		// add listener for setting parameters
		if (cdd != null) {
			add(new Line(false));
			add(new Label("Time:"));
			add(listTime = new List(5));
			Panel p = new Panel(new BorderLayout());
			p.add(bAddTime = new Button("Add"), BorderLayout.WEST);
			bAddTime.addActionListener(this);
			p.add(bRemoveTime = new Button("Remove"), BorderLayout.EAST);
			add(p);
			bRemoveTime.addActionListener(this);
		}
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource().equals(cbDirectionAndSpeed) && cbDirectionAndSpeed.getState()) {
			Panel p = new Panel(new ColumnLayout()), pf = null;
			Checkbox cbMC[] = null, cb48[] = new Checkbox[2];
			if (bSDmeasuredValsAvailable && bSDcomputedValsAvailable) {
				p.add(new Label("Speed and direction values:"));
				pf = new Panel(new FlowLayout(FlowLayout.LEFT));
				CheckboxGroup cbg = new CheckboxGroup();
				cbMC = new Checkbox[2];
				pf.add(cbMC[0] = new Checkbox("measured", cbg, true));
				pf.add(cbMC[1] = new Checkbox("computed", cbg, true));
				p.add(pf);
				p.add(new Line(false));
			} else if (bSDmeasuredValsAvailable) {
				p.add(new Label("Speed and direction values: measured"));
			} else {
				p.add(new Label("Speed and direction values: computed"));
			}
			pf = new Panel(new FlowLayout(FlowLayout.LEFT));
			pf.add(new Label("Min speed for movement (km/h:"));
			TextField tf = new TextField("" + fMinSpeed, 5);
			pf.add(tf);
			p.add(pf);
			pf = new Panel(new FlowLayout(FlowLayout.LEFT));
			pf.add(new Label("N Directions?"));
			CheckboxGroup cbg48 = new CheckboxGroup();
			pf.add(cb48[0] = new Checkbox("4", cbg48, true));
			pf.add(cb48[1] = new Checkbox("8", cbg48, false));
			p.add(pf);
			p.add(new Line(false));
			OKDialog okd = new OKDialog(parent, "Aggregation by Speed and Direction", true);
			okd.addContent(p);
			okd.show();
			boolean ok = true;
			if (okd.wasCancelled()) {
				ok = false;
			} else {
				float f = -1f;
				try {
					f = Float.valueOf(tf.getText()).floatValue();
				} catch (NumberFormatException nfe) {
					ok = false;
				}
				if (f >= 0) {
					fMinSpeed = f;
					iNDirections = (cb48[0].getState()) ? 4 : 8;
					boolean bMeasured = true;
					if (cbMC == null) {
						bMeasured = bSDmeasuredValsAvailable;
					} else {
						bMeasured = cbMC[0].getState();
					}
					if (bMeasured) {
						sSpeedColumnName = currTblD.relevantColumns[7];
						sDirectionColumnName = currTblD.relevantColumns[8];
					} else {
						sSpeedColumnName = currTblD.relevantColumns[12];
						sDirectionColumnName = currTblD.relevantColumns[14];
					}
				} else {
					ok = false;
				}
			}
			if (!ok) {
				cbDirectionAndSpeed.setState(false);
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource().equals(bAddTime)) {
			DivisionSpec div = specifyTemporalAggregation();
			if (div == null)
				return;
			if (vDivisionSpecTime == null) {
				vDivisionSpecTime = new Vector(5, 5);
			}
			vDivisionSpecTime.addElement(div);
			String strListEntry = "Time division: " + div.getPartitionCount() + " partitions";
			if (div instanceof TimeCycleDivisionSpec) {
				TimeCycleDivisionSpec tcdiv = (TimeCycleDivisionSpec) div;
				strListEntry += " by " + tcdiv.cycleCode;
			}
			listTime.add(strListEntry);
		}
		if (ae.getSource().equals(bRemoveTime)) {
			int n = listTime.getSelectedIndex();
			if (n < 0)
				return;
			listTime.remove(n);
			vDivisionSpecTime.removeElementAt(n);
		}
	}

	/**
	 * Runs a dialog for the user to specify parameters for temporal aggregation
	 */
	protected DivisionSpec specifyTemporalAggregation() {
		if (cdd == null)
			return null;
		TimeDivisionUI tdUI = null;
		if (cdd instanceof ColumnDescriptorDate) {
			ColumnDescriptorDate cddd = (ColumnDescriptorDate) cdd;
			tdUI = new TimeDivisionUI(cddd.min, cddd.max);
			OKDialog ok = new OKDialog(parent, "Temporal aggregation", true);
			ok.addContent(tdUI);
			ok.show();
			if (!ok.wasCancelled() && tdUI.getTimeBreaks() != null) {
				if (currTblD.dbProc == null) {
					currTblD.dbProc = new DBProcedureSpec();
				}
				if (tdUI.useCycle()) {
					TimeCycleDivisionSpec div = new TimeCycleDivisionSpec();
					div.columnIdx = currTblD.timeColIdx;
					div.columnName = currTblD.timeColName;
					char cycleUnit = tdUI.getCycleUnit();
					switch (cycleUnit) {
					case 's':
						div.cycleCode = "SS";
						break;
					case 't': //minute
						div.cycleCode = "MI";
						break;
					case 'h':
						div.cycleCode = "HH24";
						break;
					case 'd': //day of a week
						div.cycleCode = "D";
						break;
					case 'm': //month of a year
						div.cycleCode = "MM";
						break;
					}
					Vector br = tdUI.getTimeBreaks();
					div.partitions = new Vector(br.size(), 10);
					for (int i = 0; i < br.size(); i++) {
						TimeMoment t = (TimeMoment) br.elementAt(i);
						int k1 = (int) t.toNumber();
						if (i < br.size() - 1) {
							t = (TimeMoment) br.elementAt(i + 1);
						} else {
							t = (TimeMoment) br.elementAt(0);
						}
						int k2 = (int) t.toNumber() - 1;
						if (k1 <= k2) {
							int vals[] = new int[k2 - k1 + 1];
							for (int j = 0; j < vals.length; j++) {
								vals[j] = k1 + j;
							}
							div.partitions.addElement(vals);
						} else {
							int cycleIdx = tdUI.getCycleIndex();
							int cycleLen = TimeDivisionUI.nCycleElements[cycleIdx];
							int max = TimeDivisionUI.minCycleElement[cycleIdx] + cycleLen - 1;
							int vals[] = new int[cycleLen - k1 + k2 + 1];
							vals[0] = k1;
							for (int j = 1; j < vals.length; j++) {
								vals[j] = vals[j - 1] + 1;
								if (vals[j] > max) {
									vals[j] -= cycleLen;
								}
							}
							div.partitions.addElement(vals);
						}
					}
					return div;
					//currTblD.dbProc.addDivisionSpec(div);
				} else {
					TimeLineDivisionSpec div = new TimeLineDivisionSpec();
					div.columnIdx = currTblD.timeColIdx;
					div.columnName = currTblD.timeColName;
					div.breaks = tdUI.getTimeBreaks();
					TimeMoment t = tdUI.getStart();
					if (t != null) {
						div.breaks.insertElementAt(t, 0);
					}
					t = tdUI.getEnd();
					if (t != null) {
						div.breaks.addElement(t);
					}
					return div;
					//currTblD.dbProc.addDivisionSpec(div);
				}
			} else
				return null;
		} else {
			ColumnDescriptorNum cddn = (ColumnDescriptorNum) cdd;
			tdUI = new TimeDivisionUI("" + (int) cddn.min, "" + (int) cddn.max);
			OKDialog ok = new OKDialog(parent, "Temporal aggregation", true);
			ok.addContent(tdUI);
			ok.show();
			if (!ok.wasCancelled() && tdUI.getTimeBreaks() != null) {
				if (currTblD.dbProc == null) {
					currTblD.dbProc = new DBProcedureSpec();
				}
				TimeLineDivisionSpec div = new TimeLineDivisionSpec();
				div.columnIdx = currTblD.timeColIdx;
				div.columnName = currTblD.timeColName;
				div.breaks = tdUI.getTimeBreaks();
				div.breaks.insertElementAt(new TimeCount((int) cddn.min), 0);
				div.breaks.addElement(new TimeCount((int) cddn.max));
				return div;
			} else
				return null;
		}
		/*
		if (currTblD.dbProc!=null && currTblD.dbProc.getDivisionSpecCount()>0) {
		  System.out.println(">>> Specified "+currTblD.dbProc.getDivisionSpecCount()+" divisions!");
		  for (int i=0; i<currTblD.dbProc.getDivisionSpecCount(); i++) {
		    DivisionSpec div=currTblD.dbProc.getDivisionSpec(i);
		    System.out.println("Division "+(i+1)+": "+div.getClass().getName());
		    System.out.println("Column N "+div.columnIdx+" \""+div.columnName+"\"");
		    System.out.println("N partitions: "+div.getPartitionCount());
		    if (div instanceof NumAttrDivisionSpec) {
		      NumAttrDivisionSpec ndiv=(NumAttrDivisionSpec)div;
		      for (int j=0; j<ndiv.getPartitionCount(); j++) {
		        float[] pair=ndiv.getInterval(j);
		        System.out.print("["+pair[0]+";"+pair[1]+"] ");
		      }
		      System.out.println();
		    }
		    else
		    if (div instanceof TimeLineDivisionSpec) {
		      TimeLineDivisionSpec tldiv=(TimeLineDivisionSpec)div;
		      for (int j=0; j<tldiv.getPartitionCount(); j++) {
		        TimeMoment[] pair=tldiv.getInterval(j);
		        System.out.print("["+pair[0].toString()+";"+pair[1].toString()+"] ");
		      }
		      System.out.println();
		    }
		    else
		    if (div instanceof TimeCycleDivisionSpec) {
		      TimeCycleDivisionSpec tcdiv=(TimeCycleDivisionSpec)div;
		      System.out.println("Time cycle code: "+tcdiv.cycleCode);
		      for (int j=0; j<tcdiv.getPartitionCount(); j++) {
		        int vals[]=tcdiv.getPartition(j);
		        System.out.print("[");
		        for (int k=0; k<vals.length; k++) {
		          System.out.print(vals[k]);
		          if (k<vals.length-1) System.out.print(",");
		        }
		        System.out.print("] ");
		      }
		      System.out.println();
		    }
		  }
		}
		*/
	}

}
