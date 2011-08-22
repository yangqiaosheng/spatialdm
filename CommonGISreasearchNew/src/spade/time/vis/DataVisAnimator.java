package spade.time.vis;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.Vector;

import spade.analysis.classification.TableClassifier;
import spade.lib.basicwin.Destroyable;
import spade.time.FocusInterval;
import spade.time.TimeMoment;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.Parameter;
import spade.vis.mapvis.DataPresenter;
import spade.vis.mapvis.Visualizer;
import spade.vis.spec.AnimatedVisSpec;
import spade.vis.spec.SaveableTool;
import spade.vis.spec.ToolSpec;

/**
* Animates usual cartographic presentation methods.
*/
public class DataVisAnimator implements PropertyChangeListener, Destroyable, SaveableTool {
	/**
	* The table with time-dependent data
	*/
	protected AttributeDataPortion table = null;
	/**
	* The descriptors of the attribute(s) to be represented. Each element of the
	* vector is an instance of VisAttrDescriptor.
	*/
	protected Vector aDescr = null;
	/**
	* The temporal parameter
	*/
	protected Parameter par = null;
	/**
	* The visualizer to be animated
	*/
	protected DataPresenter vis = null;
	/**
	* Alternatively to a DataPresenter, the visualization method to manipulate
	* may be a TableClassifier. This is a reference to such a TableClassifier.
	* Normally, one of the variables vis or tcl is null.
	*/
	protected TableClassifier tcl = null;
	/**
	* The visualizer that "wraps" the classifier. If there is no classifier, this
	* is the same as @see vis.
	*/
	protected Visualizer wrapVis = null;
	/**
	* The focus time interval specifying the moment or interval to show at each
	* moment
	*/
	protected FocusInterval focusInt = null;
	/**
	 * The index of the parameter value corresponding to the currently selected interval
	 */
	protected int parValIdx = 0;

	protected boolean destroyed = false;
	/**
	* As a SaveableTool, an animator may be registered somewhere and, hence, must
	* notify the component where it is registered about its destroying. This vector
	* contains the listeners to be notified about destroying of the animator.
	*/
	protected Vector destroyListeners = null;

	/**
	* Performs all necessary preparations for animation. In particular, informs the
	* visualizer about the sub-attributes of the time-dependent attributes.
	* The argument visualizer may be either a DataPresenter or a TableClassifier.
	* Returns true if successfully prepared.
	*/
	public boolean setup(AttributeDataPortion table, Vector aDescr, Parameter par, Object visualizer) {
		if (table == null || aDescr == null || aDescr.size() < 1 || par == null || par.getValueCount() < 2 || visualizer == null)
			return false;
		if (visualizer instanceof DataPresenter) {
			vis = (DataPresenter) visualizer;
		} else if (visualizer instanceof TableClassifier) {
			tcl = (TableClassifier) visualizer;
		}
		if (vis == null && tcl == null)
			return false;
		Vector attrs = (vis != null) ? vis.getAttributes() : tcl.getAttributes();
		if (attrs == null || attrs.size() < 1)
			return false;
		this.table = table;
		this.aDescr = aDescr;
		this.par = par;
		boolean hasTemporal = false;
		for (int i = 0; i < aDescr.size(); i++) {
			VisAttrDescriptor d = (VisAttrDescriptor) aDescr.elementAt(i);
			if (!d.isTimeDependent || d.parent == null) {
				continue;
			}
			int idx = i;
			if (attrs.elementAt(0) == null) {
				++idx;
			}
			Vector subAttr = new Vector(par.getValueCount(), 1);
			int nsub = 0;
			for (int k = 0; k < par.getValueCount(); k++) {
				subAttr.addElement(null);
				for (int n = 0; n < d.parent.getChildrenCount(); n++) {
					Attribute child = d.parent.getChild(n);
					if (child.hasParamValue(par.getName(), par.getValue(k))) {
						boolean ok = true;
						if (d.fixedParams != null) {
							for (int j = 0; j < d.fixedParams.size() && ok; j++) {
								ok = child.hasParamValue((String) d.fixedParams.elementAt(j), d.fixedParamVals.elementAt(j));
							}
						}
						if (ok) {
							subAttr.setElementAt(child.getIdentifier(), k);
							++nsub;
							break;
						}
					}
				}
			}
			if (nsub < 1) {
				subAttr = null;
			} else if (nsub < par.getValueCount()) { //not for all parameter values there are sub-attributes
				int nn = -1; //index of the first not-null element in subAttr
				for (int j = 0; j < subAttr.size() && nn < 0; j++)
					if (subAttr.elementAt(j) != null) {
						nn = j;
					}
				if (nsub == 1) {
					attrs.setElementAt(subAttr.elementAt(nn), idx);
					subAttr = null;
				} else {
					if (nn > 0) {
						for (int j = 0; j < nn - 1; j++) {
							subAttr.setElementAt(subAttr.elementAt(nn), j);
						}
					}
					for (int j = nn + 1; j < subAttr.size(); j++)
						if (subAttr.elementAt(j) == null) {
							subAttr.setElementAt(subAttr.elementAt(j - 1), j);
						}
				}
			}
			if (subAttr != null) {
				if (d.offset != 0) {
					//shift the vector of the subattributes
					if (d.offset > 0 && d.offset < subAttr.size()) {
						for (int j = 0; j < subAttr.size() - d.offset; j++) {
							subAttr.setElementAt(subAttr.elementAt(j + d.offset), j);
						}
						for (int j = subAttr.size() - d.offset; j < subAttr.size(); j++) {
							subAttr.setElementAt(null, j);
						}
					} else if (d.offset < 0 && -d.offset < subAttr.size()) {
						for (int j = subAttr.size() - 1; j >= -d.offset; j--) {
							subAttr.setElementAt(subAttr.elementAt(j + d.offset), j);
						}
						for (int j = 0; j < -d.offset; j++) {
							subAttr.setElementAt(null, j);
						}
					}
				}
				attrs.setElementAt(d.parent.getIdentifier(), idx);
				if (vis != null) {
					vis.setSubAttributes(subAttr, idx);
				} else {
					tcl.setSubAttributes(subAttr, idx);
				}
				hasTemporal = true;
				String invariant = null;
				if ((d.fixedParams != null && d.fixedParams.size() > 0) || d.offset != 0) {
					if (d.offset != 0)
						if (d.offset > 0) {
							invariant = " (t+" + d.offset;
						} else {
							invariant = " (t-" + d.offset;
						}
					else {
						invariant = " (";
					}
					if (d.fixedParams != null) {
						for (int j = 0; j < d.fixedParams.size(); j++) {
							if (!invariant.endsWith("(")) {
								invariant += "; ";
							}
							invariant += (String) d.fixedParams.elementAt(j) + "=" + d.fixedParamVals.elementAt(j).toString();
						}
					}
					invariant += ")";
				}
				if (vis != null)
					if (invariant != null) {
						vis.setAttrName(d.parent.getName() + invariant, idx);
						vis.setInvariant(invariant, idx);
					} else {
						vis.setAttrName(d.parent.getName(), idx);
					}
				else if (invariant != null) {
					tcl.setInvariant(invariant, idx);
				}
			}
		}
		if (vis != null) {
			vis.setup();
			vis.addDestroyingListener(this);
		} else {
			tcl.setup();
			tcl.addPropertyChangeListener(this);
		}
		return hasTemporal;
	}

	/**
	* Sets a reference to the visualizer that "wraps" the classifier in a case
	* when the animator operates a classifier.
	*/
	public void setWrapVisualizer(Visualizer wrapVis) {
		this.wrapVis = wrapVis;
	}

	/**
	* Links itself to the given FocusInterval; starts listening events of the
	* current time change.
	*/
	public void setFocusInterval(FocusInterval fint) {
		if (fint == focusInt)
			return;
		if (focusInt != null) {
			focusInt.removePropertyChangeListener(this);
		}
		focusInt = fint;
		if (focusInt == null || par == null || (vis == null && tcl == null))
			return;
		focusInt.addPropertyChangeListener(this);
		TimeMoment tm = focusInt.getCurrIntervalEnd();
		if (tm == null)
			return;
		int idx = getMomentIndex(tm);
		if (idx < 0)
			return;
		if (vis != null) {
			vis.setCurrentSubAttrIndex(idx);
			vis.notifyVisChange();
		} else if (tcl != null) {
			tcl.setCurrentSubAttrIndex(idx);
			tcl.notifyClassesChange();
		}
	}

	public FocusInterval getFocusInterval() {
		return focusInt;
	}

	public Parameter getParameter() {
		return par;
	}

	public TimeMoment getCurrParamValue() {
		if (parValIdx < 0)
			return null;
		return (TimeMoment) par.getValue(parValIdx);
	}

	/**
	* Finds the index of the givent TimeMoment (or the latest moment before the
	* given TimeMoment) in the list of values of the temporal parameter par.
	*/
	protected int getMomentIndex(TimeMoment tm) {
		if (tm == null)
			return -1;
		for (int i = 1; i < par.getValueCount(); i++) {
			TimeMoment mom = (TimeMoment) par.getValue(i);
			if (tm.compareTo(mom) < 0)
				return i - 1;
		}
		return par.getValueCount() - 1;
	}

	/**
	* Reacts to a change of the current time moment
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getPropertyName().equals("current_interval")) {
			TimeMoment tm = (TimeMoment) e.getNewValue();
			parValIdx = getMomentIndex(tm);
			if (parValIdx < 0)
				return;
			if (vis != null && parValIdx != vis.getCurrentSubAttrIndex()) {
				vis.setCurrentSubAttrIndex(parValIdx);
				vis.notifyVisChange();
			} else if (tcl != null && parValIdx != tcl.getCurrentSubAttrIndex()) {
				tcl.setCurrentSubAttrIndex(parValIdx);
				tcl.notifyClassesChange();
			}
		} else if (e.getPropertyName().equals("destroyed") && (e.getSource().equals(vis) || e.getSource().equals(tcl))) {
			destroy();
		}
	}

	public void setParamValIndex(int pvIdx) {
		if (par == null)
			return;
		if (pvIdx < 0) {
			pvIdx = 0;
		}
		if (pvIdx > par.getValueCount() - 1) {
			pvIdx = par.getValueCount() - 1;
		}
		parValIdx = pvIdx;
		if (vis != null && parValIdx != vis.getCurrentSubAttrIndex()) {
			vis.setCurrentSubAttrIndex(parValIdx);
			vis.notifyVisChange();
		} else if (tcl != null && parValIdx != tcl.getCurrentSubAttrIndex()) {
			tcl.setCurrentSubAttrIndex(parValIdx);
			tcl.notifyClassesChange();
		}
/*
    if (vis!=null) {
      String subAttrId=vis.getCurrentSubAttrId(0);
      if (subAttrId==null)
        System.out.println(parValIdx+" ("+par.getValue(parValIdx)+"): subAttrId = null!");
      else  {
        Attribute at=table.getAttribute(subAttrId);
        System.out.println(parValIdx+" ("+par.getValue(parValIdx)+"): attribute id = "+at.getIdentifier()+
         "; parameter value = "+at.getParamValue(par.getName()));
      }
    }
*/
	}

	/**
	* Returns the specification of this animated representation to be used
	* for saving the system's state.
	*/
	@Override
	public Object getSpecification() {
		AnimatedVisSpec spec = new AnimatedVisSpec();
		if (table != null) {
			spec.table = table.getContainerIdentifier();
		}
		if (aDescr != null && aDescr.size() > 0) {
			spec.attrSpecs = new Vector(aDescr.size(), 1);
			for (int i = 0; i < aDescr.size(); i++) {
				VisAttrDescriptor vd = (VisAttrDescriptor) aDescr.elementAt(i);
				spec.attrSpecs.addElement(vd.getSpecification());
			}
		}
		if (vis != null) {
			spec.visSpec = (ToolSpec) vis.getSpecification();
		} else if (wrapVis != null) {
			spec.visSpec = (ToolSpec) wrapVis.getSpecification();
		} else if (tcl != null) {
			spec.visSpec = tcl.getSpecification();
		}
		return spec;
	}

	/**
	* Returns the keyword used in the opening tag of a stored state description
	* of this tool. A tool state description (specification) is stored as a
	* sequence of lines starting with <tagName> and ending with </tagName>, where
	* tagName is a unique keyword for a particular class of tools.
	*/
	@Override
	public String getTagName() {
		return "animated_tool";
	}

	/**
	* After the tool is constructed, it may be requested to setup its individual
	* properties according to the given list of stored properties. Not relevant
	* for DataVisAnimator; it does not do anything in this method.
	*/
	@Override
	public void setProperties(Hashtable properties) {
	}

	/**
	* Adds a listener to be notified about destroying the tool.
	* As a SaveableTool, an animator may be registered somewhere and, hence, must
	* notify the component where it is registered about its destroying.
	*/
	@Override
	public void addDestroyingListener(PropertyChangeListener lst) {
		if (lst == null)
			return;
		if (destroyListeners == null) {
			destroyListeners = new Vector(5, 5);
		}
		if (!destroyListeners.contains(lst)) {
			destroyListeners.addElement(lst);
		}
	}

	/**
	* Sends a PropertyChangeEvent with the name "destroyed" to its
	* destroying listener(s), @see addDestroyingListener.
	*/
	@Override
	public void destroy() {
		if (isDestroyed())
			return;
		if (destroyListeners != null) {
			PropertyChangeEvent pce = new PropertyChangeEvent(this, "destroyed", null, null);
			for (int i = 0; i < destroyListeners.size(); i++) {
				((PropertyChangeListener) destroyListeners.elementAt(i)).propertyChange(pce);
			}
		}
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}
}