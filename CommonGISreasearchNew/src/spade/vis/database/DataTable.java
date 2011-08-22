package spade.vis.database;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import spade.lib.util.DoubleArray;
import spade.lib.util.Frequencies;
import spade.lib.util.IdMaker;
import spade.lib.util.IdUtil;
import spade.lib.util.IdentifierUseChecker;
import spade.lib.util.InfoSaver;
import spade.lib.util.IntArray;
import spade.lib.util.NumRange;
import spade.lib.util.NumStat;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.spec.DataSourceSpec;
import core.ActionDescr;

/**
* This is an extension of the class DataPortion for thematic data. DataItems
* included in a DataTable should be instances of DataRecord.
* A DataTable has a list of attribute descriptors and gives references to
* it to all the dataRecords.
* A DataTable may have listeners of changes of the thematic data.
* The listeners should implement the PropertyChangeListener interface.
* They are notified about changes of properties "ObjectSet" and "ObjectData"
* and about the table being destroyed (i.e. removed from the system).
* ==========================================================================
* last updates:
* ==========================================================================
* => hdz, 03.2004:
*   ---------------
* - new polymorfism fot method setCharAttributeValue (String v, int attrN, int recN)
*  extended for String Attributes
* - new polymorfism fot method setCharAttributeValues (String v[], int attrN)
* => hdz, 2004.04.28
* - new Method isValuesCountBelow(): Compares the Number of Values with a given Limit
* ==========================================================================
*/

public class DataTable extends GenericDataPortion implements AttributeDataPortion, ThematicDataSupplier, IdentifierUseChecker, ObjectContainer, java.io.Serializable {
	/**
	 * List of known classes implementing the interface TableProcessor.
	 * Table processors use metadata for completing table structure and
	 * transforming content, e.g. strings to time moments.
	 */
	public static final String tableProcessors[] = { "spade.vis.database.CaptionParamProcessor", "spade.vis.database.TimeRefProcessor", "spade.vis.database.ParamProcessor" };
	/**
	 * Possible values for the variable indicating the nature of the objects in the table
	 */
	public static final int NATURE_UNKNOWN = 0, NATURE_GEO = 1, NATURE_TIME = 2, NATURE_ABSTRACT = 3, NATURE_FIRST = 0, NATURE_LAST = 3;
	/**
	* The list of descriptors of attributes.
	*/
	protected Vector attrList = null;
	/**
	* A table may have attributes depending on parameters. If so, this vector
	* contains references to the parameter(s). Elements of the vector are
	* instances of the class spade.vis.database.Parameter
	*/
	protected Vector params = null;
	/**
	* While the data are not actually loaded, this variable indicates the presence
	* of information about temporal parameters in the data source specification
	* of this table.
	*/
	protected boolean hasTemporalParamInfo = false;
	/**
	 * Indicates whether the records in the table are time-referenced
	 */
	protected boolean timeReferenced = false;
	/**
	 * If the objects are time-referenced, these are the earliest and latest
	 * times among all time references
	 */
	protected TimeMoment firstTime = null, lastTime = null;
	/**
	* A SemanticsManager keeps knowledge about relationships between attributes
	* of the table
	*/
	protected SemanticsManager sm = null;
	/**
	* The TableContentSupplier is used for "delayed" loading of table data.
	* A table asks its supplier to provide the data when they are first needed.
	*/
	protected TableContentSupplier tsuppl = null;
	/**
	 * Indicates the nature of the items described by the table rows
	 */
	protected int natureOfItems = NATURE_UNKNOWN;
	/**
	 * This may be a matrix of computed distances (differences) between the
	 * records of the table, e.g. Euclidean distances in the multidimensional
	 * space of attributes
	 */
	protected float distMatrix[][];
	/**
	 * The "title" of the distance matrix, which can explain how it was produced
	 */
	protected String distMatrixTitle = null;
	/**
	 * If the table has been produced by means of some analysis operation,
	 * this is a description of the operation
	 */
	protected ActionDescr madeByAction = null;
	/**
	* The objects table data refer to may be options in a decision-making task.
	* There may be a component interested in results of decision support tools.
	* This component should be notified when ranking or ordered classification
	* is added to the table. Here is a reference to such component (may be
	* null). To notify the component, the method notifyDecisionColumnAdded
	* should be used.
	*/
	protected PropertyChangeListener decisionSupporter = null;
	/**
	* If the table has a column with URLs attached to data items, a URLOpener may
	* be created to support opening of the pages. The table keeps a reference
	* to this URLOpener in order to avoid its destroying by the garbage collector.
	* Although a URLOpener is an object implementing spade.analysis.system.URLOpener
	* interface, the table refers to it as simply to an Object instance (to avoid
	* unnecessary dependencies between classes and packages).
	*/
	protected Object urlOpener = null;
	/**
	* If the table has a column(s) with file names of different levels of geometry
	* an AppOpener may be created to support loading of the data. The table keeps
	* a reference to this AppOpener in order to avoid its destroying by
	* the garbage collector.
	*/
	protected Object appOpener = null;

	public DataTable() {
		sm = new SemanticsManager(this);
	}

	/**
	 * Returns the variable indicating the nature of the items described by the table rows.
	 * Possible values: NATURE_UNKNOWN=0, NATURE_GEO=1, NATURE_TIME=2, NATURE_ABSTRACT=3
	 */
	public int getNatureOfItems() {
		return natureOfItems;
	}

	/**
	 * Sets the variable indicating the nature of the items described by the table rows.
	 * Possible values: NATURE_UNKNOWN=0, NATURE_GEO=1, NATURE_TIME=2, NATURE_ABSTRACT=3
	 */
	public void setNatureOfItems(int nature) {
		if (nature >= NATURE_FIRST && nature <= NATURE_LAST) {
			this.natureOfItems = nature;
		}
	}

	/**
	* The TableContentSupplier is used for "delayed" loading of table data.
	* A table asks its supplier to provide the data when they are first needed.
	*/
	public void setTableContentSupplier(TableContentSupplier suppl) {
		tsuppl = suppl;
	}

	public SemanticsManager getSemanticsManager() {
		return sm;
	}

	/**
	* The objects table data refer to may be options in a decision-making task.
	* There may be a component interested in results of decision support tools.
	* This method sets a reference to such a component.
	*/
	public void setDecisionSupporter(PropertyChangeListener dsupp) {
		decisionSupporter = dsupp;
	}

	/**
	* The objects table data refer to may be options in a decision-making task.
	* There may be a component interested in results of decision support tools.
	* This method returns the reference to such a component.
	*/
	public PropertyChangeListener getDecisionSupporter() {
		return decisionSupporter;
	}

	/**
	* The objects table data refer to may be options in a decision-making task.
	* There may be a component interested in results of decision support tools.
	* This method replies whether the table has such a component.
	*/
	public boolean hasDecisionSupporter() {
		return decisionSupporter != null;
	}

	/**
	* This method replies whether the table has AppOpener component.
	*/
	public boolean hasAppOpener() {
		return appOpener != null;
	}

	/**
	* For uniqueness of attribute identifiers, attaches the container identifier
	* to the identifier of each attribute.
	*/
	public void makeUniqueAttrIdentifiers() {
		if (attrList == null || portionId == null || portionId.length() < 1)
			return;
		Vector parents = new Vector(20, 20);
		for (int i = 0; i < getAttrCount(); i++) {
			Attribute attr = getAttribute(i);
			String id = attr.getIdentifier();
			attr.setIdentifier(IdUtil.makeUniqueAttrId(id, portionId));
			if (attr.getName().equals(attr.getIdentifier())) {
				attr.setName(IdUtil.getPureAttrId(id));
			}
			Attribute parent = attr.getParent();
			if (parent != null && !parents.contains(parent)) {
				parents.addElement(parent);
			}
		}
		for (int i = 0; i < parents.size(); i++) {
			Attribute attr = (Attribute) parents.elementAt(i);
			String id = attr.getIdentifier();
			attr.setIdentifier(IdUtil.makeUniqueAttrId(id, portionId));
		}
	}

	/**
	 * Sets the unique identifier of this data portion. For uniqueness of attribute
	 * identifiers, attaches the container identifier to the identifier of each
	 * attribute.
	 */
	@Override
	public void setContainerIdentifier(String tblId) {
		portionId = tblId;
		makeUniqueAttrIdentifiers();
	}

	/**
	* Tries to get data from its TableContentSupplier if the data has not been
	* loaded yet.
	*/
	protected synchronized boolean getDataFromSupplier() {
		if (data == null && attrList == null)
			if (tsuppl != null) {
				//When the data supplier fills the table, it may call some of the
				//methods of this table (e.g. getAttributeCount) which, in their turn,
				//will call the method getDataFromSupplier. To avoid an endless
				//recursion, the internal reference to the supplier must be set to null
				//before the supplier is asked to fill the table.
				//This trick also allows to avoid repeatedly addressing the supplier
				//when data loading fails due to some errors.
				TableContentSupplier supplier = tsuppl;
				tsuppl = null;
				boolean result = supplier.fillTable();
				return result;
			} else
				return false;
		return true;
	}

	/**
	* If the data actually have not been loaded in the table yet, this method
	* loads them. Returns true if data has been successfully loaded.
	* In this implementation the method getDataFromSupplier is called.
	*/
	@Override
	public boolean loadData() {
		return getDataFromSupplier();
	}

	/**
	* This method allows the data suplier of the table to signalize that all the
	* data has been loaded. The table can now perform necessary preprocessing and
	* transfromations.
	*/
	public void finishedDataLoading() {
		System.out.println("Finished loading data to table " + getName());
		makeUniqueAttrIdentifiers();
		completeTableStructure();
	}

	/**
	* Replies whether the table already has any attributes.
	*/
	public boolean hasAttributes() {
		return attrList != null && attrList.size() > 0;
	}

	/**
	* Returns a list of descriptors of attributes.
	*/
	public Vector getAttrList() {
		if (attrList == null && !getDataFromSupplier())
			return null;
		return attrList;
	}

	/**
	* Sets a reference to the list of attribute descriptors
	*/
	public void setAttrList(Vector list) {
		attrList = list;
		makeUniqueAttrIdentifiers();
	}

	/**
	* Returns the number of attributes.
	*/
	@Override
	public int getAttrCount() {
		if (attrList == null && !getDataFromSupplier())
			return 0;
		if (attrList == null)
			return 0;
		return attrList.size();
	}

	/**
	* Adds the attribute descriptor to the list of attribute descriptors
	*/
	@Override
	public void addAttribute(Attribute attr) {
		if (attr == null)
			return;
		boolean needSetInRecords = false;
		if (attrList == null) {
			attrList = new Vector(200, 100);
			needSetInRecords = true;
		}
		if (portionId != null) {
			String id = attr.getIdentifier();
			attr.setIdentifier(IdUtil.makeUniqueAttrId(id, portionId));
			if (attr.getName().equals(attr.getIdentifier())) {
				attr.setName(IdUtil.getPureAttrId(id));
			}
		}
		attrList.addElement(attr);
		if (needSetInRecords) {
			for (int i = 0; i < getDataItemCount(); i++) {
				getDataRecord(i).setAttrList(attrList);
			}
		}
	}

	/**
	* Constructs the descriptor of the attribute with the given identifier and
	* type and adds it to the list of attribute descriptors.
	*/
	public void addAttribute(String identifier, char attrType) {
		if (identifier == null)
			return;
		Attribute attr = new Attribute(identifier, attrType);
		addAttribute(attr);
	}

	/**
	* Constructs the descriptor of the attribute with the given name,
	* identifier (possibly, null) and
	* type and adds it to the list of attribute descriptors.
	*/
	@Override
	public void addAttribute(String name, String identifier, char attrType) {
		if (name == null)
			return;
		if (identifier == null) {
			identifier = IdMaker.makeId(name, this);
		}
		Attribute attr = new Attribute(identifier, attrType);
		attr.setName(name);
		addAttribute(attr);
	}

	/**
	* Adds a derived attribute. The argument origin indicates the method of
	* derivation (@see AttributeTypes)
	* The DataTable itself produces a default identifier for the new attribute.
	* Returns the number (index) of the new attribute in the list of attributes.
	* A vector of source attributes the new attribute was derived from can be
	* attached
	*/
	public int addDerivedAttribute(String name, char type, int origin, Vector sourceAttr) {
		String id = IdMaker.makeId(name, this);
		if (name == null) {
			name = id;
		}
		//If there are other attributes with the same name or with names starting
		//with this name, modify the name by adding a number to it
		int nsame = 0;
		for (int i = 0; i < getAttrCount(); i++)
			if (getAttributeName(i).startsWith(name)) {
				++nsame;
			}
		if (nsame > 0) {
			name = name + "_" + (nsame + 1);
		}
		Attribute attr = new Attribute(id, type);
		attr.setName(name);
		attr.origin = origin;
		if (sourceAttr != null) {
			sourceAttr.trimToSize();
			attr.setSourceAttributes(sourceAttr);
		}
		addAttribute(attr);
		return attrList.size() - 1;
	}

	/**
	* Adds new columns to the table. Arguments:
	* objIds - identifiers of objects (array of strings);
	* attrNames - names of the attributes to add (array of strings);
	* attrTypes - types of the attributes (array of characters;
	*             see spade.vis.database.AttributeTypes);
	* values - values of the attributes (2D array; the first index corresponds
	*          to attributes and the second to the objects in the vector objIds).
	*          The values are specified as strings.
	* Returns true if the data have been successfully added, i.e. all objects
	* listed in the array objIds have been found in the table.
	*/
	@Override
	public boolean addColumns(String objIds[], String attrNames[], char attrTypes[], String values[][]) {
		if (objIds == null || attrNames == null || attrTypes == null || values == null)
			return false;
		int idx[] = new int[objIds.length];
		for (int i = 0; i < objIds.length; i++) {
			idx[i] = indexOf(objIds[i]);
			if (idx[i] < 0)
				return false; //the object is absent in the table
		}
		Vector attr = new Vector(attrNames.length, 1);
		int prevAttrN = getAttrCount();
		for (int i = 0; i < attrNames.length; i++) {
			addAttribute(attrNames[i], null, attrTypes[i]);
			attr.addElement(this.getAttributeId(getAttrCount() - 1));
		}
		for (int i = 0; i < idx.length; i++) {
			DataRecord rec = (DataRecord) getDataItem(idx[i]);
			for (int j = 0; j < attrNames.length; j++) {
				rec.setAttrValue(values[j][i], prevAttrN + j);
			}
		}
		notifyPropertyChange("new_attributes", null, attr);
		return true;
	}

	/**
	* Adds new columns with numeric (double) values to the table. Arguments:
	* objIds - identifiers of objects (array of strings);
	* attrNames - names of the attributes to add (array of strings);
	* values - values of the attributes (2D array; the first index corresponds
	*          to attributes and the second to the objects in the vector objIds).
	*          The values are specified as double numbers.
	* Returns true if the data have been successfully added, i.e. all objects
	* listed in the array objIds have been found in the table.
	*/
	@Override
	public boolean addNumericColumns(String objIds[], String attrNames[], double values[][]) {
		if (objIds == null || attrNames == null || values == null)
			return false;
		int idx[] = new int[objIds.length];
		for (int i = 0; i < objIds.length; i++) {
			idx[i] = indexOf(objIds[i]);
			if (idx[i] < 0)
				return false; //the object is absent in the table
		}
		Vector attr = new Vector(attrNames.length, 1);
		int prevAttrN = getAttrCount();
		for (String attrName : attrNames) {
			addAttribute(attrName, null, AttributeTypes.real);
			attr.addElement(this.getAttributeId(getAttrCount() - 1));
		}
		for (int i = 0; i < idx.length; i++) {
			DataRecord rec = (DataRecord) getDataItem(idx[i]);
			for (int j = 0; j < attrNames.length; j++) {
				rec.setNumericAttrValue(values[j][i], prevAttrN + j);
			}
		}
		notifyPropertyChange("new_attributes", null, attr);
		return true;
	}

	/**
	* The objects table data refer to may be options in a decision-making task.
	* There may be a component interested in results of decision support tools.
	* This method notifies this component about adding a column with ranking
	* or ordered classification.
	*/
	public void notifyDecisionColumnAdded(String columnId) {
		if (decisionSupporter != null) {
			decisionSupporter.propertyChange(new PropertyChangeEvent(this, "column_added", null, columnId));
		}
	}

	/**
	* Removes the attribute with the given index
	*/
	@Override
	public void removeAttribute(int n) {
		if (attrList == null || attrList.size() <= n)
			return;
		attrList.removeElementAt(n);
		for (int i = 0; i < getDataItemCount(); i++) {
			this.getDataRecord(i).removeAttribute(n);
		}
	}

	/**
	* Removes the attribute with the given identifier
	*/
	@Override
	public void removeAttribute(String attrId) {
		int n = getAttrIndex(attrId);
		if (n >= 0) {
			removeAttribute(n);
		}
	}

	/**
	* removes attributes within a given interval of numbers
	*/
	public void removeAttributes(int n1, int n2) {
		if (attrList == null || attrList.size() <= n1)
			return;
		if (n2 >= attrList.size()) {
			n2 = attrList.size() - 1;
		}
		for (int n = n2; n >= n1; n--) {
			attrList.removeElementAt(n);
		}
		for (int i = 0; i < getDataItemCount(); i++) {
			this.getDataRecord(i).removeAttributes(n1, n2);
		}
	}

	/**
	 * Removes attributes contained in the given vector.
	 * The elements of the vector must be instances of Attribute.
	 * These may also be top-level attributes.
	 */
	public void removeAttributes(Vector attributes) {
		if (attrList == null || attrList.size() < 1 || attributes == null || attributes.size() < 1)
			return;
		boolean toRemove[] = new boolean[attrList.size()];
		for (int i = 0; i < attrList.size(); i++) {
			Attribute a = (Attribute) attrList.elementAt(i);
			if (a.getParent() == null) {
				toRemove[i] = attributes.contains(a);
			} else {
				toRemove[i] = attributes.contains(a.getParent());
			}
		}
		for (int i = attrList.size() - 1; i >= 0; i--)
			if (toRemove[i]) {
				attrList.removeElementAt(i);
			}
		for (int j = 0; j < getDataItemCount(); j++) {
			for (int i = attrList.size() - 1; i >= 0; i--)
				if (toRemove[i]) {
					this.getDataRecord(j).removeAttribute(i);
				}
		}
	}

	/**
	* Returns its top-level attributes. This means that, if some attribute has a
	* parent attribute, the parent is included in the result rather than the
	* child attribute. The elements of the resulting vector are instances of
	* Attribute.
	*/
	@Override
	public Vector getTopLevelAttributes() {
		if (attrList == null || attrList.size() < 1)
			return null;
		Vector attrs = new Vector(attrList.size(), 1);
		for (int i = 0; i < attrList.size(); i++) {
			Attribute a = (Attribute) attrList.elementAt(i);
			if (a.getParent() == null) {
				attrs.addElement(a);
			} else if (!attrs.contains(a.getParent())) {
				attrs.addElement(a.getParent());
			}
		}
		attrs.trimToSize();
		return attrs;
	}

	/**
	* Returns the descriptor of the attribute with the given index in the
	* attribute list
	*/
	@Override
	public Attribute getAttribute(int n) {
		if (n < 0 || n >= getAttrCount())
			return null;
		return (Attribute) attrList.elementAt(n);
	}

	/**
	* Returns the descriptor of the attribute with the given identifier.
	* This may be, in particular, a super-attribute, i.e. a parameter-dependent
	* attribute.
	*/
	@Override
	public Attribute getAttribute(String attrId) {
		if (attrId == null || attrList == null || attrList.size() < 1)
			return null;
		String tblId = IdUtil.getTableId(attrId);
		if (tblId != null)
			if (portionId == null || !portionId.equals(tblId))
				return null; //another table
			else {
				;
			}
		else {
			attrId = IdUtil.makeUniqueAttrId(attrId, portionId);
		}

		Attribute parent = null;
		for (int i = 0; i < attrList.size(); i++) {
			Attribute attr = (Attribute) attrList.elementAt(i);
			if (attrId.equalsIgnoreCase(attr.getIdentifier()))
				return attr;
			Attribute par = ((Attribute) attrList.elementAt(i)).getParent();
			if (par != null && par != parent)
				if (par.getIdentifier().equalsIgnoreCase(attrId))
					return par;
				else {
					parent = par;
				}
		}
		return null;
	}

	/**
	* Returns the identifier of the attribute with the given index in the
	* attribute list. For uniqueness, the attribute identifiers are combined with
	* the table identifier.
	*/
	@Override
	public String getAttributeId(int n) {
		Attribute attr = getAttribute(n);
		if (attr == null)
			return null;
		return attr.getIdentifier();
	}

	/**
	* Returns the name of the attribute with the given index .
	*/
	@Override
	public String getAttributeName(int n) {
		Attribute attr = getAttribute(n);
		if (attr == null)
			return null;
		return attr.getName();
	}

	/**
	* Returns the name of the attribute with the given identifier.
	*/
	@Override
	public String getAttributeName(String attrId) {
		Attribute attr = getAttribute(attrId);
		if (attr == null)
			return null;
		return attr.getName();
	}

	/**
	* Returns the type of the attribute with the given index.
	*/
	@Override
	public char getAttributeType(int n) {
		Attribute attr = getAttribute(n);
		if (attr == null)
			return 0;
		return attr.getType();
	}

	/**
	* Returns the type of the attribute with the given identifier.
	*/
	@Override
	public char getAttributeType(String attrId) {
		Attribute attr = getAttribute(attrId);
		if (attr == null)
			return 0;
		return attr.getType();
	}

	/**
	* Returns true if the attribute with the given index is numeric.
	*/
	@Override
	public boolean isAttributeNumeric(int n) {
		Attribute attr = getAttribute(n);
		if (attr == null)
			return false;
		return attr.isNumeric();
	}

	/**
	* Returns true if the attribute with the given index is temporal.
	*/
	@Override
	public boolean isAttributeTemporal(int n) {
		Attribute attr = getAttribute(n);
		if (attr == null)
			return false;
		return attr.isTemporal();
	}

	/**
	* Returns the origin of the attribute, i.e. whether is was derived and,
	* if yes, how. The return value is one of the constants defined in
	* the class AttributeTypes (@see AttributeTypes)
	*/
	@Override
	public int getAttributeOrigin(int n) {
		Attribute attr = getAttribute(n);
		if (attr == null)
			return -1;
		return attr.origin;
	}

	/**
	* Returns the list of identifiers of attributes this attribute has been derived from
	*/
	public Vector getAttributeDependencyList(int n) {
		Attribute attr = getAttribute(n);
		if (attr == null)
			return null;
		return attr.getSourceAttributes();
	}

	/**
	* Returns the index of the attribute with the given identifier in the
	* attribute list
	*/
	@Override
	public int getAttrIndex(String attrId) {
		if (attrList == null && !getDataFromSupplier())
			return -1;
		if (attrId == null || attrList == null)
			return -1;
		String tblId = IdUtil.getTableId(attrId);
		if (tblId != null)
			if (portionId == null || !portionId.equals(tblId))
				return -1; //another table
			else {
				;
			}
		else {
			attrId = IdUtil.makeUniqueAttrId(attrId, portionId);
		}
		for (int i = 0; i < attrList.size(); i++) {
			Attribute attr = (Attribute) attrList.elementAt(i);
			if (attrId.equalsIgnoreCase(attr.getIdentifier()))
				return i;
		}
		return -1;
	}

	/**
	* Returns an array with the indices of the attributes with the given identifiers.
	*/
	@Override
	public int[] getAttrIndices(Vector attrIds) {
		if (attrList == null && !getDataFromSupplier())
			return null;
		if (attrIds == null || attrIds.size() < 1)
			return null;
		int fn[] = new int[attrIds.size()];
		for (int i = 0; i < fn.length; i++) {
			fn[i] = getAttrIndex((String) attrIds.elementAt(i));
		}
		return fn;
	}

	/**
	* Returns the index of the attribute with the given name.
	*/
	@Override
	public int findAttrByName(String attrName) {
		for (int i = 0; i < getAttrCount(); i++)
			if (StringUtil.sameStringsIgnoreCase(attrName, getAttributeName(i)))
				return i;
		return -1;
	}

	/**
	* Adds the data item to its vector of data items. Checks whether the data
	* item is an instance of DataRecord. Gives to the data record a reference
	* to its list of attribute descriptors.
	*/
	@Override
	public void addDataItem(DataItem item) {
		if (item == null)
			return;
		if (!(item instanceof DataRecord))
			return;
		addDataRecord((DataRecord) item);
	}

	/**
	* Adds the data record to its vector of data items. Gives to the data record
	* a reference to its list of attribute descriptors. Calls the function
	* addDataItem of its parent.
	* In fact, does the same as addDataItem, only does not check the type.
	*/
	public void addDataRecord(DataRecord drec) {
		if (drec == null)
			return;
		drec.setAttrList(attrList);
		super.addDataItem(drec);
		TimeReference tref = drec.getTimeReference();
		if (tref != null) {
			TimeMoment t1 = tref.getValidFrom(), t2 = tref.getValidUntil();
			if (t1 != null) {
				if (firstTime == null || firstTime.compareTo(t1) > 0) {
					firstTime = t1.getCopy();
				}
				if (lastTime == null || lastTime.compareTo(t1) < 0) {
					lastTime = t1.getCopy();
				}
			}
			if (t2 != null) {
				if (lastTime == null || lastTime.compareTo(t2) < 0) {
					lastTime = t2.getCopy();
				}
			}
		}
		if (data.size() == 1) {
			notifyPropertyChange("got_data", null, null);
		}
	}

	/**
	* Returns the data record with the given index. The result may be null.
	* Calls the getDataItem function of the parent and casts the result into
	* the dataRecord type.
	*/
	public DataRecord getDataRecord(int idx) {
		if (attrList == null && !getDataFromSupplier())
			return null;
		DataItem item = getDataItem(idx);
		if (item == null)
			return null;
		return (DataRecord) item;
	}

	/**
	 * If the data records describe some objects existing in the system
	 * (e.g. time moments, geo objects), returns the reference to the object
	 * from the data record with the given index
	 */
	public Object getDescribedObject(int idx) {
		DataRecord rec = getDataRecord(idx);
		if (rec == null)
			return null;
		return rec.getDescribedObject();
	}

	/**
	* Returns the data record with the given index.
	*/
	@Override
	public Object getObject(int idx) {
		return getDataRecord(idx);
	}

	/**
	* sets values of the attribute for all records
	*/
	public void setNumericAttributeValues(double v[], int attrN) {
		double vmin = Double.NaN, vmax = Double.NaN;
		for (double element : v) {
			if (Double.isNaN(vmin) || vmin > element) {
				vmin = element;
			}
			if (Double.isNaN(vmax) || vmax < element) {
				vmax = element;
			}
		}
		for (int i = 0; i < getDataItemCount(); i++)
			if (i < v.length) {
				setNumericAttributeValue(v[i], vmin, vmax, attrN, i);
			}
	}

	public void setNumericAttributeValues(float v[], int attrN) {
		float vmin = Float.NaN, vmax = Float.NaN;
		for (float element : v) {
			if (Float.isNaN(vmin) || vmin > element) {
				vmin = element;
			}
			if (Float.isNaN(vmax) || vmax < element) {
				vmax = element;
			}
		}
		for (int i = 0; i < getDataItemCount(); i++)
			if (i < v.length) {
				setNumericAttributeValue(v[i], vmin, vmax, attrN, i);
			}
	}

	public void setNumericAttributeValues(int v[], int attrN) {
		for (int i = 0; i < this.getDataItemCount(); i++)
			if (i < v.length) {
				setNumericAttributeValue(v[i], attrN, i);
			}
	}

	//hdz extended for String Attributes
	public void setCharAttributeValues(String v[], int attrN) {
		for (int i = 0; i < this.getDataItemCount(); i++)
			if (i < v.length) {
				setCharAttributeValue(v[i], attrN, i);
			}
	}

	/**
	* sets value of the numeric attribute for a given record
	*/
	public void setNumericAttributeValue(int v, int attrN, int recN) {
		getDataRecord(recN).setNumericAttrValue(v, String.valueOf(v), attrN);
	}

	public void setNumericAttributeValue(double v, int attrN, int recN) {
		getDataRecord(recN).setNumericAttrValue(v, String.valueOf(v), attrN);
	}

	public void setNumericAttributeValue(double v, double vmin, double vmax, int attrN, int recN) {
		getDataRecord(recN).setNumericAttrValue(v, (Double.isNaN(v)) ? "" : spade.lib.util.StringUtil.doubleToStr(v, vmin, vmax), attrN);
	}

	//hdz extended for String Attributes
	public void setCharAttributeValue(String v, int attrN, int recN) {
		getDataRecord(recN).setAttrValue(v, attrN);
	}

	/**
	* Returns the value (as an Object) of the attribute with the index attrN
	* from the data record with the number recN.
	*/
	@Override
	public Object getAttrValue(int attrN, int recN) {
		DataRecord rec = getDataRecord(recN);
		if (rec == null)
			return null;
		return rec.getAttrValue(attrN);
	}

	/**
	* Returns the value (in String format) of the attribute with the index attrN
	* from the data record with the number recN.
	*/
	@Override
	public String getAttrValueAsString(int attrN, int recN) {
		DataRecord rec = getDataRecord(recN);
		if (rec == null)
			return null;
		return rec.getAttrValueAsString(attrN);
	}

	/**
	* Returns the value (in double format) of the attribute with the index attrN
	* from the data record with the number recN. If the attribute is non-numeric,
	* returns Double.NaN.
	*/
	@Override
	public double getNumericAttrValue(int attrN, int recN) {
		DataRecord rec = getDataRecord(recN);
		if (rec == null)
			return Double.NaN;
		return rec.getNumericAttrValue(attrN);
	}

	/**
	 * Returns a sample attribute value (the first non-null value in the column)
	 */
	public Object getSampleAttrValue(int colN) {
		if (colN < 0 || attrList == null || colN >= attrList.size())
			return null;
		for (int i = 0; i < getDataItemCount(); i++) {
			DataRecord rec = getDataRecord(i);
			if (rec == null) {
				continue;
			}
			Object obj = rec.getAttrValue(colN);
			if (obj != null)
				return obj;
		}
		return null;
	}

	/**
	* Sometimes attribute types are not specified. By default, all attributes
	* are assumed to be of the character type. In this method the DataTable
	* looks through the available values of the attributes in order to
	* determine the actual type of each attribute. It can recornize numeric
	* attributes (all values are transformable into numbers) and logical
	* attributes (having values T and F). Missing values are ignored.
	*/
	public void determineAttributeTypes() {
		if (attrList == null && !getDataFromSupplier())
			return;
		if (getDataItemCount() < 1)
			return;
		int nattr = getAttrCount();
		if (nattr < 1)
			return;
		char types[] = new char[nattr];
		for (int i = 0; i < nattr; i++) {
			types[i] = 0; //type is unknown
		}
		int nCharAttr = 0;
		for (int i = 0; i < getDataItemCount() && nCharAttr < nattr; i++) {
			DataRecord rec = getDataRecord(i);
			if (rec == null) {
				continue;
			}
			for (int j = 0; j < nattr; j++)
				if (types[j] != AttributeTypes.character) {
					String str = rec.getAttrValueAsString(j);
					if (str == null) {
						continue;
					}
					if (types[j] == 0 || types[j] == AttributeTypes.logical) {
						boolean isLogical = str.equalsIgnoreCase("T") || str.equalsIgnoreCase("F");
						if (isLogical)
							if (types[j] == 0) {
								types[j] = AttributeTypes.logical; //the unknown type becomes logical
							} else {
								; //the type remains logical
							}
						else if (types[j] == AttributeTypes.logical) {
							types[j] = AttributeTypes.character;
						} else {
							; //the type is still unknown
						}
					}
					if (types[j] == 0 || AttributeTypes.isNumericType(types[j])) {
						boolean isNumeric = false;
						try { //transform the string into a number
							double v = Double.valueOf(str).doubleValue();
							isNumeric = !Double.isNaN(v);
						} catch (NumberFormatException nfe) {
						}
						if (!isNumeric) {
							types[j] = AttributeTypes.character;
						} else //determine whether the value is integer or real
						if (types[j] == 0 || types[j] == AttributeTypes.integer)
							//look whether there is a decimal separator
							if (str.indexOf('.') >= 0 || str.indexOf('E') >= 0 || str.indexOf('e') >= 0) {
								types[j] = AttributeTypes.real;
							} else {
								types[j] = AttributeTypes.integer;
							}
					}
					if (types[j] == AttributeTypes.character) {
						++nCharAttr;
					}
				}
		}
		for (int i = 0; i < nattr; i++) {
			if (types[i] == 0) {
				types[i] = AttributeTypes.character;
			}
			Attribute attr = getAttribute(i);
			attr.setType(types[i]);
		}
	}

	@Override
	public boolean isIdentifierUsed(String ident) {
		if (getAttrIndex(ident) >= 0)
			return true;
		//check among the super-attributes
		Vector topAttr = getTopLevelAttributes();
		if (topAttr == null)
			return false;
		for (int i = 0; i < topAttr.size(); i++) {
			Attribute attr = (Attribute) topAttr.elementAt(i);
			if (ident.equalsIgnoreCase(attr.getIdentifier()))
				return true;
		}
		return false;
	}

	/**
	 * Sets time references in the table records using the values from the
	 * specified columns. The values are expected to be instances of TimeMoment.
	 * The first column contains the starting date/time of the record validity,
	 * and the second column contains the ending date/time. The second column
	 * may be absent (the column index is <1); this means that the records
	 * describe instant events, i.e. the starting moments equal the ending moments.
	 */
	public void setTimeReferences(int startColIdx, int endColIdx) {
		if (startColIdx < 0)
			return;
		if (attrList == null || attrList.size() <= startColIdx)
			return;
		if (getDataItemCount() < 1)
			return;
		Attribute at1 = getAttribute(startColIdx), at2 = null;
		if (!at1.isTemporal())
			return;
		if (endColIdx >= attrList.size()) {
			endColIdx = -1;
		}
		if (endColIdx >= 0) {
			at2 = getAttribute(endColIdx);
			if (!at2.isTemporal()) {
				endColIdx = -1;
				at2 = null;
			}
		}
		if (at2 != null) {
			at1.timeRefMeaning = Attribute.VALID_FROM;
			at2.timeRefMeaning = Attribute.VALID_UNTIL;
		} else {
			at1.timeRefMeaning = Attribute.OCCURRED_AT;
		}
		int nTimeRefs = 0;
		for (int i = 0; i < getDataItemCount(); i++) {
			DataRecord rec = getDataRecord(i);
			if (rec == null) {
				continue;
			}
			Object val = rec.getAttrValue(startColIdx);
			if (val == null || !(val instanceof TimeMoment)) {
				continue;
			}
			TimeMoment t1 = (TimeMoment) val, t2 = null;
			if (endColIdx >= 0) {
				val = rec.getAttrValue(endColIdx);
				if (val != null && (val instanceof TimeMoment)) {
					t2 = (TimeMoment) val;
				}
			}
			TimeReference tref = new TimeReference();
			tref.setValidFrom(t1);
			if (endColIdx >= 0) {
				tref.setValidUntil(t2);
			} else {
				tref.setValidUntil(t1);
			}
			rec.setTimeReference(tref);
			++nTimeRefs;
		}
		timeReferenced = nTimeRefs > 0;
		firstTime = lastTime = null;
	}

	/**
	 * Sets time references in the table records using the values of temporal
	 * attributes, if any.
	 */
	public void setTimeReferences() {
		int occurTimeFN = -1, validFromFN = -1, validUntilFN = -1;
		for (int i = 0; i < getAttrCount(); i++)
			if (getAttribute(i).isTemporal()) {
				int timeRefMeaning = getAttribute(i).timeRefMeaning;
				if (timeRefMeaning == Attribute.OCCURRED_AT)
					if (occurTimeFN < 0) {
						occurTimeFN = i;
					} else {
						;
					}
				else if (timeRefMeaning == Attribute.VALID_FROM)
					if (validFromFN < 0) {
						validFromFN = i;
					} else {
						;
					}
				else if (timeRefMeaning == Attribute.VALID_UNTIL)
					if (validUntilFN < 0) {
						validUntilFN = i;
					} else {
						;
					}
			}
		if (validFromFN >= 0 && validUntilFN >= 0) {
			setTimeReferences(validFromFN, validUntilFN);
		} else if (occurTimeFN >= 0) {
			setTimeReferences(occurTimeFN, -1);
		}
	}

	/**
	 * Returns true if at least one record in the table is time-referenced
	 */
	public boolean isTimeReferenced() {
		return timeReferenced;
	}

	/**
	 * Reports whether the objects in this container represent entities
	 * changing over time, e.g. moving, growing, shrinking, etc.
	 * The ObjectContainer returns true only if it contains data about
	 * these changes. A DataTable returns false.
	 */
	@Override
	public boolean containsChangingObjects() {
		return false;
	}

	//----------------- ObjectDataSupplier interface -------------------
	//At the moment we assume that there is 1 data item per each object
	/**
	* Returns the number of currently available objects.
	*/
	@Override
	public int getObjectCount() {
		return this.getDataItemCount();
	}

	/**
	* Returns the thematic data item (data record) contained at the given index.
	*/
	@Override
	public DataItem getObjectData(int idx) {
		return getThematicData(idx);
	}

	/**
	* Returns the thematic data item (data record) contained at the given index.
	*/
	@Override
	public ThematicDataItem getThematicData(int idx) {
		return getDataRecord(idx);
	}

	/**
	* Finds the object with the given identifier and returns its index.
	*/
	@Override
	public int getObjectIndex(String id) {
		return indexOf(id);
	}

	/**
	* Returns the ID of the object with the given index. The result may be null.
	*/
	@Override
	public String getObjectId(int idx) {
		return getDataItemId(idx);
	}

	/**
	* Finds object with the given identifier and returns the data
	* associated with the object.
	*/
	@Override
	public ThematicDataItem getThematicData(String objectId) {
		int idx = getObjectIndex(objectId);
		if (idx < 0)
			return null;
		return getThematicData(idx);
	}

	// Saving the table in CSV format
	public void saveTableAsCsv(DataOutputStream dos) {
		try {
			String str = "id,Name";
			for (int j = 0; j < getAttrCount(); j++) {
				str += "," + getAttributeName(j);
			}
			dos.writeBytes(str + "\n");
			for (int i = 0; i < getDataItemCount(); i++) {
				str = getDataItemId(i) + "," + getDataItemName(i);
				for (int j = 0; j < getAttrCount(); j++) {
					String val = getAttrValueAsString(j, i);
					if (val == null) {
						val = "";
					}
					str += "," + val;
				}
				dos.writeBytes(str + "\n");
			}
			dos.flush();
		} catch (IOException ie) {
			System.out.println(ie);
		}
	}

	public void storeData(Vector attrs, boolean storeNames, InfoSaver saver) {
		if (attrs == null || attrs.size() < 1 || getDataItemCount() < 1 || saver == null)
			return;
		int attrN[] = new int[attrs.size()];
		int nattr = 0;
		for (int i = 0; i < attrs.size(); i++)
			if (attrs.elementAt(i) != null) {
				int n = getAttrIndex((String) attrs.elementAt(i));
				if (n >= 0) {
					attrN[nattr++] = n;
				}
			}
		if (nattr < 1)
			return;
		String str = "id";
		if (storeNames) {
			str += ",Name";
		}
		for (int i = 0; i < nattr; i++) {
			str += "," + getAttributeName(attrN[i]);
		}
		saver.saveString(str);
		for (int i = 0; i < getDataItemCount(); i++) {
			str = getDataItemId(i);
			if (storeNames) {
				str += "," + getDataItemName(i);
			}
			for (int j = 0; j < nattr; j++) {
				String val = getAttrValueAsString(attrN[j], i);
				if (val == null) {
					val = "";
				}
				str += "," + val;
			}
			saver.saveString(str);
		}
		saver.finish();
	}

	/**
	* Sets a reference to a URLOpener.
	* If the table has a column with URLs attached to data items, a URLOpener may
	* be created to support opening of the pages. The table may keep a reference
	* to this URLOpener in order to avoid its destroying by the garbage collector.
	*/
	public void setURLOpener(Object urlOpener) {
		this.urlOpener = urlOpener;
	}

	public Object getURLOpener() {
		return urlOpener;
	}

	/**
	* Sets a reference to a AppOpener.
	* If the table has a column(s) with different levels of geometry, a AppOpener may
	* be created to support opening of these data. The table may keep a reference
	* to this AppOpener in order to avoid its destroying by the garbage collector.
	*/
	public void setAppOpener(Object appOpener) {
		this.appOpener = appOpener;
	}

	public Object getAppOpener() {
		return appOpener;
	}

	/**
	* Updates the data within the table using the data from another table sent as
	* an argument. If the second argument is true, the old data are completely
	* removed and replaced with the new data. If it is false, the data for new
	* objects are added at the end of the table while the data for existing
	* objects are updated.
	*/
	public void update(DataTable newData, boolean replace) {
		if (newData == null || !newData.hasData())
			return;
		boolean attrListIsNew = false, sameStructure = true;
		if (newData.hasAttributes())
			if (attrList == null || attrList.size() < 1) {
				attrList = newData.getAttrList();
				attrListIsNew = true;
				for (int i = 0; i < newData.getParamCount(); i++) {
					addParameter(newData.getParameter(i));
				}
				if (newData.getSemanticsManager().hasAnySemantics()) {
					sm = newData.getSemanticsManager();
					sm.setTable(this);
				}
			} else {
				for (int i = 0; i < newData.getAttrCount(); i++) {
					int k = getAttrIndex(IdUtil.getPureAttrId(newData.getAttributeId(i)));
					if (k < 0) {
						addAttribute(newData.getAttribute(i));
					} else {
						sameStructure = sameStructure && (k == i);
					}
				}
			}
		if ((replace && sameStructure) || data == null || data.size() < 1) {
			data = newData.getData();
			if (!attrListIsNew) {
				for (int i = 0; i < data.size(); i++) {
					getDataRecord(i).setAttrList(attrList);
				}
			}
		} else {
			if (replace && data != null) {
				removeAllData();
			}
			if (data == null) {
				data = new Vector(newData.getDataItemCount(), 50);
			}
			if (data.size() < 1) {
				replace = true; //to avoid unnecessary checks
			}
			for (int i = 0; i < newData.getDataItemCount(); i++) {
				DataRecord rec = null, rec0 = newData.getDataRecord(i);
				String id = rec0.getId();
				if (!replace) {
					int idx = indexOf(id);
					if (idx >= 0) {
						rec = getDataRecord(idx);
						data.removeElementAt(idx);
						data.insertElementAt(rec0, idx);
					}
				}
				if (rec == null) {
					addDataItem(rec0);
				}
				if (sameStructure) {
					rec0.setAttrList(attrList);
					//probably, the table contains additional attributes
					//in comparison to the update (e.g. derived attributes)
					if (rec != null) {
						for (int j = newData.getAttrCount(); j < getAttrCount(); j++) {
							rec0.setAttrValue(rec.getAttrValue(j), j);
						}
					}
				} else {
					DataRecord rec1 = (DataRecord) rec0.clone();
					if (rec != null) {
						rec.copyTo(rec0);
					} else {
						rec0.setAttrList(attrList);
						Vector values = new Vector(attrList.size(), 10);
						for (int j = 0; j < attrList.size(); j++) {
							values.addElement(null);
						}
						rec0.setAttrValues(values);
					}
					for (int j = 0; j < newData.getAttrCount(); j++) {
						String attrId = IdUtil.getPureAttrId(newData.getAttributeId(j));
						int aidx = getAttrIndex(attrId);
						if (aidx >= 0) {
							rec0.setAttrValue(rec1.getAttrValue(j), aidx);
						}
					}
				}
			}
		}
		for (int i = 0; i < getDataItemCount(); i++) {
			getDataRecord(i).setIndexInContainer(i);
		}
		notifyPropertyChangeByThread("data_updated", null, null);
	}

	/**
	 * Removes table records whose identifiers do not occur in the given vector.
	 * Returns the number of records removed.
	 */
	public int removeExtraRecords(Vector<String> idsToKeep) {
		if (data == null || data.size() < 1 || idsToKeep == null || idsToKeep.size() < 1)
			return 0;
		//check if at least one table record occurs in idsToKeep
		boolean keep[] = new boolean[data.size()];
		int nRemove = 0;
		for (int i = 0; i < data.size(); i++) {
			keep[i] = idsToKeep.contains(((DataItem) data.elementAt(i)).getId());
			if (!keep[i]) {
				++nRemove;
			}
		}
		if (nRemove < 1 || nRemove >= data.size())
			return 0;
		objIndex = new Hashtable(Math.max(1000, data.size() * 2));
		Vector data1 = new Vector(data.size() * 2, 100);
		for (int i = 0; i < keep.length; i++)
			if (keep[i]) {
				DataItem dit = (DataItem) data.elementAt(i);
				data1.addElement(dit);
				int idx = data1.size() - 1;
				dit.setIndexInContainer(idx);
				objIndex.put(dit.getId(), new Integer(idx));
			} else {
/*
        DataItem dit=(DataItem)data.elementAt(i);
        System.out.println("Remove "+dit.getId()+" "+dit.getName());
*/
			}
		data = data1;
		//notifyPropertyChangeByThread("data_updated",null,null);
		return nRemove;
	}

	/**
	* When necessary, transforms the table structure according to specifications
	* contained in its data source descriptor (if they exist).
	* For example, may construct temporal attributes from time references
	* contained in the data.
	*/
	public void completeTableStructure() {
		if (getDataSource() == null || !(getDataSource() instanceof DataSourceSpec))
			return;
		DataSourceSpec dss = (DataSourceSpec) getDataSource();
		if (dss.descriptors != null) {
			for (String tableProcessor : tableProcessors) {
				try {
					TableProcessor tpr = (TableProcessor) Class.forName(tableProcessor).newInstance();
					tpr.processTable(this);
				} catch (Exception e) {
				}
			}
			if (attrList == null || portionId == null || portionId.length() < 1)
				return;
			Vector parents = new Vector(20, 20);
			for (int i = 0; i < getAttrCount(); i++) {
				Attribute attr = getAttribute(i).getParent();
				if (attr != null && !parents.contains(attr)) {
					parents.addElement(attr);
				}
			}
			for (int i = 0; i < parents.size(); i++) {
				Attribute attr = (Attribute) parents.elementAt(i);
				String id = attr.getIdentifier();
				attr.setIdentifier(IdUtil.makeUniqueAttrId(id, portionId));
			}
		}
		setTimeReferences();
		notifyPropertyChange("structure_complete", null, null);
	}

	/**
	 * Returns the indexes of its temporal attributes, i.e. columns with temporal
	 * references.
	 */
	@Override
	public IntArray getTimeRefColumnNs() {
		IntArray timeAttrNs = null;
		for (int i = 0; i < getAttrCount(); i++)
			if (getAttribute(i).isTemporal()) {
				if (timeAttrNs == null) {
					timeAttrNs = new IntArray(5, 5);
				}
				timeAttrNs.addElement(i);
			}
		return timeAttrNs;
	}

	/**
	* Returns the number of its parameters.
	*/
	@Override
	public int getParamCount() {
		if (params == null)
			return 0;
		return params.size();
	}

	/**
	* Returns the parameter with the given index.
	*/
	@Override
	public Parameter getParameter(int idx) {
		if (idx < 0 || idx >= getParamCount())
			return null;
		return (Parameter) params.elementAt(idx);
	}

	/**
	* Returns the parameter with the given name.
	*/
	@Override
	public Parameter getParameter(String name) {
		if (name == null || getParamCount() < 1)
			return null;
		for (int i = 0; i < getParamCount(); i++)
			if (name.equalsIgnoreCase(((Parameter) params.elementAt(i)).getName()))
				return (Parameter) params.elementAt(i);
		return null;
	}

	/**
	* Returns all parameters.
	*/
	@Override
	public Vector getParameters() {
		if (params == null)
			return null;
		return (Vector) params.clone();
	}

	/**
	* Adds a new parameter to the list of parameters.
	*/
	public void addParameter(Parameter par) {
		if (par == null)
			return;
		if (params == null) {
			params = new Vector(5, 5);
		}
		params.addElement(par);
	}

	/**
	* Informs whether the table has at least one temporal parameter
	*/
	@Override
	public boolean hasTemporalParameter() {
		if (!hasData())
			return hasTemporalParamInfo;
		if (params == null || params.size() < 1)
			return false;
		for (int i = 0; i < params.size(); i++)
			if (((Parameter) params.elementAt(i)).isTemporal())
				return true;
		return false;
	}

	/**
	* Returns the temporal parameter, if exists.
	*/
	@Override
	public Parameter getTemporalParameter() {
		if (params == null || params.size() < 1)
			return null;
		for (int i = 0; i < params.size(); i++)
			if (((Parameter) params.elementAt(i)).isTemporal())
				return (Parameter) params.elementAt(i);
		return null;
	}

	/**
	* While the data are not actually loaded, informs the table about the presence
	* or absence of information about temporal parameters in the data source
	* specification of this table.
	*/
	public void setHasTemporalParamInfo(boolean value) {
		hasTemporalParamInfo = value;
	}

	/**
	* Returns an ObjectFilter associated with this table. If there is no filter
	* yet, a TableFilter is constructed.
	*/
	@Override
	public ObjectFilter getObjectFilter() {
		if (filter != null)
			return filter;
		TableFilter tf = new TableFilter();
		tf.setDataTable(this);
		filter = tf;
		notifyPropertyChange("filter", null, filter);
		return filter;
	}

	/**
	* Combines oFilter with the previously existing filter(s). If oFilter is a
	* TableFilter, and the table had no filter yet, simply takes the oFilter
	* as its filter. If oFilter is not a TableFilter, and the table had no
	* TableFilter yet, creates a TableFilter and combines its with oFilter.
	*/
	@Override
	public void setObjectFilter(ObjectFilter oFilter) {
		if (oFilter == null || oFilter.equals(filter) || !oFilter.isRelevantTo(setId))
			return;
		if (filter == null && (oFilter instanceof TableFilter)) {
			filter = oFilter;
		} else {
			if (filter == null) {
				getObjectFilter();
			}
			if (filter instanceof CombinedFilter) {
				CombinedFilter cFilter = (CombinedFilter) filter;
				if (!cFilter.hasFilter(oFilter)) {
					cFilter.addFilter(oFilter);
				}
			} else {
				CombinedFilter cFilter = new CombinedFilter();
				cFilter.setEntitySetIdentifier(setId);
				cFilter.setObjectContainer(this);
				cFilter.addFilter(filter);
				cFilter.addFilter(oFilter);
				filter = cFilter;
			}
		}
		notifyPropertyChange("filter", null, filter);
	}

	/**
	* Typically, must be re-implemented in descendants.
	*/
	@Override
	public void removeObjectFilter(ObjectFilter oFilter) {
		if (oFilter != null && oFilter.equals(filter)) {
			filter = null;
		} else if (filter instanceof CombinedFilter) {
			CombinedFilter cFilter = (CombinedFilter) filter;
			cFilter.removeFilter(oFilter);
			if (cFilter.getFilterCount() < 1) {
				filter = null;
			}
		}
		notifyPropertyChange("filter", null, oFilter);
	}

	/**
	* Returns true if there is at least one temporal attribute.
	*/
	@Override
	public boolean hasTimeReferences() {
		if (timeReferenced)
			return true;
		for (int i = 0; i < getAttrCount(); i++)
			if (getAttribute(i).isTemporal())
				return true;
		return false;
	}

	/**
	 * If the objects in this container are time-referenced, returns the earliest
	 * and the latest times among the time references; otherwise returns null.
	 */
	@Override
	public TimeReference getTimeSpan() {
		if (firstTime == null || lastTime == null) {
			firstTime = lastTime = null;
			int nRec = getDataItemCount();
			if (nRec < 1)
				return null;
			for (int i = 0; i < nRec; i++) {
				DataRecord rec = (DataRecord) data.elementAt(i);
				if (rec == null) {
					continue;
				}
				TimeReference tref = rec.getTimeReference();
				if (tref == null) {
					continue;
				}
				TimeMoment t1 = tref.getValidFrom(), t2 = tref.getValidUntil();
				if (t1 != null) {
					if (firstTime == null || firstTime.compareTo(t1) > 0) {
						firstTime = t1;
					}
					if (lastTime == null || lastTime.compareTo(t1) < 0) {
						lastTime = t1;
					}
				}
				if (t2 != null) {
					if (lastTime == null || lastTime.compareTo(t2) < 0) {
						lastTime = t2;
					}
				}
			}
			if (firstTime != null) {
				firstTime = firstTime.getCopy();
			}
			if (lastTime != null) {
				lastTime = lastTime.getCopy();
			}
		}
		if (firstTime == null || lastTime == null)
			return null;
		TimeReference tref = new TimeReference();
		tref.setValidFrom(firstTime);
		tref.setValidUntil(lastTime);
		return tref;
	}

	/**
	 * If the objects in this container are time-referenced, returns the earliest
	 * and the latest times among the original time references irrespective of the
	 * current transformation of the times; otherwise returns null.
	 */
	@Override
	public TimeReference getOriginalTimeSpan() {
		int nRec = getDataItemCount();
		if (nRec < 1)
			return null;
		TimeMoment tFirst = null, tLast = null;
		for (int i = 0; i < nRec; i++) {
			DataRecord rec = (DataRecord) data.elementAt(i);
			if (rec == null) {
				continue;
			}
			TimeReference tref = rec.getTimeReference();
			if (tref == null) {
				continue;
			}
			TimeMoment t1 = tref.getOrigFrom(), t2 = tref.getOrigUntil();
			if (t1 != null) {
				if (tFirst == null || tFirst.compareTo(t1) > 0) {
					tFirst = t1;
				}
				if (tLast == null || tLast.compareTo(t1) < 0) {
					tLast = t1;
				}
			}
			if (t2 != null) {
				if (tLast == null || tLast.compareTo(t2) < 0) {
					tLast = t2;
				}
			}
		}
		if (tFirst == null || tLast == null)
			return null;
		tFirst = tFirst.getCopy();
		tLast = tLast.getCopy();
		TimeReference tref = new TimeReference();
		tref.setValidFrom(tFirst);
		tref.setValidUntil(tLast);
		return tref;
	}

	/**
	* For the given list of attribute identifiers, returns an array of numbers of
	* the relevant columns. If the attribute list contains super-attributes, all
	* its children are included.
	*/
	@Override
	public IntArray getRelevantColumnNumbers(Vector attrIds) {
		if (data == null || data.size() < 1 || attrList == null || attrList.size() < 1 || attrIds == null || attrIds.size() < 1)
			return null;
		IntArray colNs = new IntArray(attrList.size(), 1);
		for (int i = 0; i < attrIds.size(); i++) {
			Attribute a = getAttribute((String) attrIds.elementAt(i));
			if (a == null) {
				continue;
			}
			if (!a.hasChildren()) {
				colNs.addElement(getAttrIndex(a.getIdentifier()));
			} else {
				for (int j = 0; j < a.getChildrenCount(); j++) {
					int idx = getAttrIndex(a.getChild(j).getIdentifier());
					if (idx >= 0) {
						colNs.addElement(idx);
					}
				}
			}
		}
		if (colNs.size() < 1)
			return null;
		return colNs;
	}

	/**
	* Determines the value range of the numeric attribute with the given identifier.
	* If this is a super-attribute, processes all its children.
	*/
	@Override
	public NumRange getAttrValueRange(String attrId) {
		return getAttrValueRange(attrId, false);
	}

	/**
	* Determines the value range of the numeric attribute with the given identifier.
	* If this is a super-attribute, processes all its children.
	*/
	public NumRange getAttrValueRange(String attrId, boolean accountForFilter) {
		if (attrId == null)
			return null;
		Vector v = new Vector(1, 1);
		v.addElement(attrId);
		return getAttrValueRange(v, accountForFilter);
	}

	/**
	* Determines the common value range of the numeric attributes with the
	* specified identifiers.
	*/
	@Override
	public NumRange getAttrValueRange(Vector attrIds) {
		return getValueRangeInColumns(getRelevantColumnNumbers(attrIds), false);
	}

	/**
	* Determines the common value range of the numeric attributes with the
	* specified identifiers.
	*/
	public NumRange getAttrValueRange(Vector attrIds, boolean accountForFilter) {
		return getValueRangeInColumns(getRelevantColumnNumbers(attrIds), accountForFilter);
	}

	/**
	* Determines the common value range in the columns with the specified numbers.
	*/
	@Override
	public NumRange getValueRangeInColumns(IntArray colNs) {
		return getValueRangeInColumns(colNs, false);
	}

	/**
	* Determines the common value range in the columns with the specified numbers.
	*/
	public NumRange getValueRangeInColumns(IntArray colNs, boolean accountForFilter) {
		if (data == null || data.size() < 1 || attrList == null || attrList.size() < 1 || colNs == null || colNs.size() < 1)
			return null;
		NumRange r = new NumRange();
		for (int i = 0; i < data.size(); i++) {
			boolean ok = !accountForFilter || filter == null || filter.isActive(i);
			if (!ok) {
				continue;
			}
			ThematicDataItem dit = (ThematicDataItem) data.elementAt(i);
			for (int j = 0; j < colNs.size(); j++)
				if (colNs.elementAt(j) >= 0) {
					double val = dit.getNumericAttrValue(colNs.elementAt(j));
					if (!Double.isNaN(val)) {
						if (Double.isNaN(r.minValue) || r.minValue > val) {
							r.minValue = val;
						}
						if (Double.isNaN(r.maxValue) || r.maxValue < val) {
							r.maxValue = val;
						}
					}
				}
		}
		if (Double.isNaN(r.maxValue))
			return null;
		return r;
	}

	/**
	 * For each of the given numeric columns, finds the appropriate precision
	 * for the string representation of the numbers, transforms the numbers into
	 * strings with the appropriate precision, and puts the strings in the table
	 * instead of the original ones.
	 * @param colNs - the numbers of the columns in which to do the transformation
	 * @param samePrecision - whether the same precision is required for all these columns
	 */
	public void setNiceStringsForNumbers(int colNs[], boolean samePrecision) {
		if (data == null || data.size() < 1 || attrList == null || attrList.size() < 1 || colNs == null || colNs.length < 1)
			return;
		NumRange r[] = new NumRange[colNs.length];
		for (int i = 0; i < r.length; i++) {
			r[i] = new NumRange();
		}
		for (int i = 0; i < data.size(); i++) {
			DataRecord rec = getDataRecord(i);
			for (int j = 0; j < colNs.length; j++)
				if (colNs[j] >= 0) {
					double val = rec.getNumericAttrValue(colNs[j]);
					if (!Double.isNaN(val)) {
						if (Double.isNaN(r[j].minValue) || r[j].minValue > val) {
							r[j].minValue = val;
						}
						if (Double.isNaN(r[j].maxValue) || r[j].maxValue < val) {
							r[j].maxValue = val;
						}
					}
				}
		}
		double totalMin = r[0].minValue, totalMax = r[0].maxValue;
		for (int j = 1; j < r.length; j++)
			if (!Double.isNaN(r[j].minValue)) {
				if (Double.isNaN(totalMin) || totalMin > r[j].minValue) {
					totalMin = r[j].minValue;
				}
				if (Double.isNaN(totalMax) || totalMax < r[j].maxValue) {
					totalMax = r[j].maxValue;
				}
			}
		if (Double.isNaN(totalMin) || Double.isNaN(totalMax))
			return; //no data in the columns!
		int prec[] = new int[r.length];
		if (samePrecision || r.length == 1) {
			prec[0] = StringUtil.getPreferredPrecision((totalMin + totalMax) / 2, totalMin, totalMax);
			for (int j = 1; j < r.length; j++) {
				prec[j] = prec[0];
			}
		} else {
			for (int j = 0; j < r.length; j++)
				if (Double.isNaN(r[j].minValue)) {
					prec[j] = 0;
				} else {
					prec[j] = StringUtil.getPreferredPrecision((r[j].minValue + r[j].maxValue) / 2, r[j].minValue, r[j].maxValue);
				}
		}
		for (int i = 0; i < data.size(); i++) {
			DataRecord rec = getDataRecord(i);
			for (int j = 0; j < colNs.length; j++)
				if (colNs[j] >= 0) {
					double val = rec.getNumericAttrValue(colNs[j]);
					if (!Double.isNaN(val)) {
						String str = StringUtil.doubleToStr(val, prec[j]);
						rec.setNumericAttrValue(val, str, colNs[j]);
					}
				}
		}
	}

	/**
	 * For each of the given numeric columns, transforms the numbers into
	 * strings with the chosen precision, and puts the strings in the table
	 * instead of the original ones.
	 * @param colNs - the numbers of the columns in which to do the transformation
	 * @param precision - the desired precision
	 */
	public void setNiceStringsForNumbers(int colNs[], int precision) {
		if (data == null || data.size() < 1 || attrList == null || attrList.size() < 1 || colNs == null || colNs.length < 1)
			return;
		for (int i = 0; i < data.size(); i++) {
			DataRecord rec = getDataRecord(i);
			for (int colN : colNs)
				if (colN >= 0) {
					double val = rec.getNumericAttrValue(colN);
					if (!Double.isNaN(val)) {
						String str = StringUtil.doubleToStr(val, precision);
						rec.setNumericAttrValue(val, str, colN);
					}
				}
		}
	}

	/**
	* Returns statistics for the numeric attribute specified through its column
	* index
	*/
	@Override
	public NumStat getNumAttrStatistics(int attrIdx) {
		if (data == null || data.size() < 1 || attrList == null || attrIdx < 0 || attrIdx >= attrList.size())
			return null;
		if (!isAttributeNumeric(attrIdx))
			return null;
		DoubleArray values = new DoubleArray(data.size(), 1);
		NumStat ns = new NumStat();
		ns.sum = 0.0f;
		for (int i = 0; i < data.size(); i++) {
			double val = ((DataRecord) data.elementAt(i)).getNumericAttrValue(attrIdx);
			if (Double.isNaN(val)) {
				continue;
			}
			++ns.nValues;
			ns.sum += val;
			if (Double.isNaN(ns.minValue) || ns.minValue > val) {
				ns.minValue = val;
			}
			if (Double.isNaN(ns.maxValue) || ns.maxValue < val) {
				ns.maxValue = val;
			}
			boolean inserted = false;
			for (int j = 0; j < values.size() && !inserted; j++)
				if (val < values.elementAt(j)) {
					values.insertElementAt(val, j);
					inserted = true;
				}
			if (!inserted) {
				values.addElement(val);
			}
		}
		if (ns.nValues < 1)
			return null;
		ns.mean = ns.sum / ns.nValues;
		if (ns.nValues < 3) {
			ns.median = (ns.minValue + ns.maxValue) / 2;
		} else {
			int k = ns.nValues / 2;
			if (ns.nValues % 2 == 0) {
				//System.out.println("N="+ns.nValues+", median between "+(k-1)+" and "+k);
				ns.median = (values.elementAt(k - 1) + values.elementAt(k)) / 2;
				--k;
			} else {
				//System.out.println("N="+ns.nValues+", median at "+k);
				ns.median = values.elementAt(k);
			}
			if (ns.nValues > 4) {
				int k1 = k / 2, k2 = ns.nValues - k1 - 1;
				if (k % 2 == 0) {
					ns.lowerQuart = values.elementAt(k1);
					ns.upperQuart = values.elementAt(k2);
				} else {
					ns.lowerQuart = (values.elementAt(k1) + values.elementAt(k1 + 1)) / 2;
					ns.upperQuart = (values.elementAt(k2 - 1) + values.elementAt(k2)) / 2;
				}
			}
		}
		return ns;
	}

	/**
	 * hdz, 2004.04.28
	 * Compares the Number of Values with a given Limit
	 * @param attributes: Identifiers of attributes
	 * @param maxCount: Limit to Compare with
	 * @return (boolean) true if valuesCount <= maxCount, else false
	 */
	@Override
	public boolean isValuesCountBelow(Vector attributes, int maxCount) {
		if (attributes == null)
			return false;
		boolean isTrue = true;
		IntArray colNs = getRelevantColumnNumbers(attributes);
		if (data == null || data.size() < 1 || attrList == null || attrList.size() < 1 || colNs == null || colNs.size() < 1)
			return !isTrue;
		int i = 0, j = 0;
		Vector v = new Vector(maxCount, maxCount);
		while (isTrue && i < data.size()) {
			ThematicDataItem dit = (ThematicDataItem) data.elementAt(i);
			j = 0;
			while (isTrue && j < colNs.size()) {
				if (colNs.elementAt(j) >= 0) {
					Object value = dit.getAttrValue(colNs.elementAt(j));
					if (value != null)
						if (value instanceof String)
							if (!StringUtil.isStringInVectorIgnoreCase((String) value, v)) {
								v.addElement(value);
							} else {
								;
							}
						else if (!v.contains(value)) {
							v.addElement(value);
						}
				}
				isTrue = v.size() < maxCount;
				++j;
			}
			++i;
		}
		return isTrue;
	}

	/**
	* Retrieves all (different and not null) values of the attribute with the
	* given identifier. If this is a super-attribute, processes all its children.
	*/
	@Override
	public Vector getAllAttrValues(String attrId) {
		if (attrId == null)
			return null;
		Vector v = new Vector(1, 1);
		v.addElement(attrId);
		return getAllAttrValues(v);
	}

	/**
	* Retrieves all (different and not null) values of the attributes with the
	* specified identifiers.
	*/
	@Override
	public Vector getAllAttrValues(Vector attrIds) {
		return getAllValuesInColumns(getRelevantColumnNumbers(attrIds));
	}

	/**
	* Retrieves all (different and not null) values in the columns with the
	* specified numbers.
	*/
	@Override
	public Vector getAllValuesInColumns(IntArray colNs) {
		if (data == null || data.size() < 1 || attrList == null || attrList.size() < 1 || colNs == null || colNs.size() < 1)
			return null;
		Vector v = new Vector(Math.min(500, data.size()), 100);
		for (int i = 0; i < data.size(); i++) {
			ThematicDataItem dit = (ThematicDataItem) data.elementAt(i);
			for (int j = 0; j < colNs.size(); j++)
				if (colNs.elementAt(j) >= 0) {
					Object value = dit.getAttrValue(colNs.elementAt(j));
					if (value != null)
						if (value instanceof String)
							if (!StringUtil.isStringInVectorIgnoreCase((String) value, v)) {
								v.addElement(value);
							} else {
								;
							}
						else if (!v.contains(value)) {
							v.addElement(value);
						}
				}
		}
		if (v.size() < 1)
			return null;
		v.trimToSize();
		return v;
	}

	/**
	* Retrieves K different and not null values of the attribute with the
	* given identifier. If this is a super-attribute, processes all its children.
	*/
	@Override
	public Vector getKAttrValues(String attrId, int K) {
		if (attrId == null)
			return null;
		Vector v = new Vector(1, 1);
		v.addElement(attrId);
		return getKAttrValues(v, K);
	}

	/**
	* Retrieves K different and not null values of the attributes with the
	* specified identifiers.
	*/
	@Override
	public Vector getKAttrValues(Vector attrIds, int K) {
		return getKValuesFromColumns(getRelevantColumnNumbers(attrIds), K);
	}

	/**
	* Retrieves K different and not null values from the columns with the
	* specified numbers.
	*/
	@Override
	public Vector getKValuesFromColumns(IntArray colNs, int K) {
		if (data == null || data.size() < 1 || attrList == null || attrList.size() < 1 || colNs == null || colNs.size() < 1 || K < 1)
			return null;
		Vector v = new Vector(K, 50);
		for (int i = 0; i < data.size() && v.size() < K; i++) {
			ThematicDataItem dit = (ThematicDataItem) data.elementAt(i);
			for (int j = 0; j < colNs.size(); j++)
				if (colNs.elementAt(j) >= 0) {
					Object value = dit.getAttrValue(colNs.elementAt(j));
					if (value != null)
						if (value instanceof String)
							if (!StringUtil.isStringInVectorIgnoreCase((String) value, v)) {
								v.addElement(value);
							} else {
								;
							}
						else if (!v.contains(value)) {
							v.addElement(value);
						}
				}
		}
		if (v.size() < 1)
			return null;
		v.trimToSize();
		return v;
	}

	/**
	* Retrieves all (different and not null) values of the attribute with the
	* given identifier. If this is a super-attribute, processes all its children.
	* If the values are not strings, transforms them into strings.
	*/
	@Override
	public Vector getAllAttrValuesAsStrings(String attrId) {
		if (attrId == null)
			return null;
		Vector v = new Vector(1, 1);
		v.addElement(attrId);
		return getAllAttrValuesAsStrings(v);
	}

	/**
	* Retrieves all (different and not null) values of the attributes with the
	* specified identifiers. If the values are not strings, transforms them into
	* strings.
	*/
	@Override
	public Vector getAllAttrValuesAsStrings(Vector attrIds) {
		return getAllValuesInColumnsAsStrings(getRelevantColumnNumbers(attrIds));
	}

	/**
	* Retrieves all (different and not null) values in the columns with the
	* specified numbers. If the values are not strings, transforms them into
	* strings.
	*/
	@Override
	public Vector getAllValuesInColumnsAsStrings(IntArray colNs) {
		if (data == null || data.size() < 1 || attrList == null || attrList.size() < 1 || colNs == null || colNs.size() < 1)
			return null;
		Vector v = new Vector(Math.min(500, data.size()), 100);
		for (int i = 0; i < data.size(); i++) {
			ThematicDataItem dit = (ThematicDataItem) data.elementAt(i);
			for (int j = 0; j < colNs.size(); j++)
				if (colNs.elementAt(j) >= 0) {
					Object value = dit.getAttrValue(colNs.elementAt(j));
					if (value != null) {
						String str = value.toString();
						if (!StringUtil.isStringInVectorIgnoreCase(str, v)) {
							v.addElement(str);
						}
					}
				}
		}
		if (v.size() < 1)
			return null;
		v.trimToSize();
		return v;
	}

	/**
	* Retrieves K different and not null values of the attribute with the
	* given identifier. If this is a super-attribute, processes all its children.
	* If the values are not strings, transforms them into strings.
	*/
	@Override
	public Vector getKAttrValuesAsStrings(String attrId, int K) {
		if (attrId == null)
			return null;
		Vector v = new Vector(1, 1);
		v.addElement(attrId);
		return getKAttrValuesAsStrings(v, K);
	}

	/**
	* Retrieves K different and not null values of the attributes with the
	* specified identifiers. If the values are not strings, transforms them into
	* strings.
	*/
	@Override
	public Vector getKAttrValuesAsStrings(Vector attrIds, int K) {
		return getKValuesFromColumnsAsStrings(getRelevantColumnNumbers(attrIds), K);
	}

	/**
	* Retrieves K different and not null values from the columns with the
	* specified numbers. If the values are not strings, transforms them into
	* strings.
	*/
	@Override
	public Vector getKValuesFromColumnsAsStrings(IntArray colNs, int K) {
		if (data == null || data.size() < 1 || attrList == null || attrList.size() < 1 || colNs == null || colNs.size() < 1 || K < 1)
			return null;
		Vector v = new Vector(K, 50);
		for (int i = 0; i < data.size() && v.size() < K; i++) {
			ThematicDataItem dit = (ThematicDataItem) data.elementAt(i);
			for (int j = 0; j < colNs.size(); j++)
				if (colNs.elementAt(j) >= 0) {
					Object value = dit.getAttrValue(colNs.elementAt(j));
					if (value != null) {
						String str = value.toString();
						if (!StringUtil.isStringInVectorIgnoreCase(str, v)) {
							v.addElement(str);
						}
					}
				}
		}
		if (v.size() < 1)
			return null;
		v.trimToSize();
		return v;
	}

	/**
	 * Returns the frequencies of values in the column specified by its index.
	 * @param colN - column index
	 * @param accountForFilter - whether to take into account the current filter
	 * @param treatValuesAsStrings - whether values should be transformed into
	 *                               strings if they are not strings
	 * @return frequencies of values found
	 */
	public Frequencies getValueFrequencies(int colN, boolean accountForFilter, boolean treatValuesAsStrings) {
		if (data == null || data.size() < 1 || attrList == null || attrList.size() < 1 || colN < 0 || colN > getAttrCount())
			return null;
		Frequencies freq = new Frequencies();
		freq.init(100, 100);
		freq.itemsAreStrings = treatValuesAsStrings;
		for (int i = 0; i < data.size(); i++) {
			boolean ok = !accountForFilter || filter == null || filter.isActive(i);
			if (ok) {
				freq.incrementFrequency(getAttrValue(colN, i));
			}
		}
		if (freq.getItemCount() < 1)
			return null;
		freq.trimToSize();
		return freq;
	}

	/**
	 * Returns the frequencies of values in the column specified by its index
	 * occurring in the specified rows.
	 * @param colN - column index
	 * @param rowNs - indexes of the rows in which to count the frequencies
	 * @param accountForFilter - whether to take into account the current filter
	 * @param treatValuesAsStrings - whether values should be transformed into
	 *                               strings if they are not strings
	 * @return frequencies of values found
	 */
	public Frequencies getValueFrequencies(int colN, IntArray rowNs, boolean accountForFilter, boolean treatValuesAsStrings) {
		if (rowNs == null || rowNs.size() < 1)
			return null;
		if (data == null || data.size() < 1 || attrList == null || attrList.size() < 1 || colN < 0 || colN > getAttrCount())
			return null;
		Frequencies freq = new Frequencies();
		freq.init(100, 100);
		freq.itemsAreStrings = treatValuesAsStrings;
		for (int i = 0; i < rowNs.size(); i++) {
			int idx = rowNs.elementAt(i);
			boolean ok = !accountForFilter || filter == null || filter.isActive(idx);
			if (ok) {
				freq.incrementFrequency(getAttrValue(colN, idx));
			}
		}
		if (freq.getItemCount() < 1)
			return null;
		freq.trimToSize();
		return freq;
	}

	/**
	* For the given list of column identifiers (not superattribute identifiers!)
	* checks whether the columns are children of the same parent. If not, returns
	* null. If yes, returns a vector of vectors: for each parameter by which
	* these columns differentiate the corresponding vector contains the parameter
	* name as its first element and the relevant parameter values following the
	* name.
	*/
	@Override
	public Vector getDistinguishingParameters(Vector columnIds) {
		if (columnIds == null || columnIds.size() < 2)
			return null;
		Attribute attr = getAttribute((String) columnIds.elementAt(0));
		if (attr == null || attr.getParameterCount() < 1)
			return null;
		Attribute parent = attr.getParent();
		if (parent == null)
			return null;
		int nPar = getParamCount();
		Vector result = new Vector(nPar, 1);
		for (int i = 0; i < nPar; i++) {
			result.addElement(null);
		}
		for (int i = 0; i < columnIds.size(); i++) {
			attr = getAttribute((String) columnIds.elementAt(i));
			if (attr == null || !parent.equals(attr.getParent()) || attr.getParameterCount() < 1)
				return null;
			for (int j = 0; j < nPar; j++) {
				Parameter par = getParameter(j);
				Object val = attr.getParamValue(par.getName());
				if (val == null) {
					continue;
				}
				Vector v = (Vector) result.elementAt(j);
				if (v == null) {
					v = new Vector(par.getValueCount(), 1);
					v.addElement(val);
					result.setElementAt(v, j);
				} else if (!v.contains(val)) {
					v.addElement(val);
				}
			}
		}
		for (int i = nPar - 1; i >= 0; i--) {
			Vector v = (Vector) result.elementAt(i);
			if (v == null || v.size() < 2) {
				result.removeElementAt(i);
			} else {
				Parameter par = getParameter(i);
				Vector v1 = new Vector(v.size() + 1, 1);
				v1.addElement(par.getName());
				for (int j = 0; j < par.getValueCount(); j++) {
					Object value = par.getValue(j);
					if (v.contains(value)) {
						v1.addElement(value);
					}
				}
				result.setElementAt(v1, i);
			}
		}
		if (result.size() < 1)
			return null;
		result.trimToSize();
		return result;
	}

	/**
	 * This method is called after a transformation of the time references
	 * of the objects, e.g. from absolute to relative times. The ObjectContainer
	 * may need to change some of its internal settings.
	 */
	@Override
	public void timesHaveBeenTransformed() {
		firstTime = lastTime = null;
		notifyPropertyChange("time_references", null, null);
	}

	/**
	 * If the table has been produced by means of some analysis operation,
	 * returns a description of the operation
	 */
	public ActionDescr getMadeByAction() {
		return madeByAction;
	}

	/**
	 * If the table has been produced by means of some analysis operation,
	 * sets a reference to a description of the operation
	 */
	public void setMadeByAction(ActionDescr madeByAction) {
		this.madeByAction = madeByAction;
	}

	/**
	 * Attaches to the table a matrix of computed distances (differences) between the
	 * records of the table, e.g. Euclidean distances in the multidimensional
	 * space of attributes
	 */
	public void setDistanceMatrix(float distances[][]) {
		distMatrix = distances;
	}

	/**
	 * Returns a previously attached matrix of computed distances (differences) between the
	 * records of the table, e.g. Euclidean distances in the multidimensional
	 * space of attributes. The matrix does not necessarily exist.
	 */
	public float[][] getDistanceMatrix() {
		return distMatrix;
	}

	/**
	 * Returns the "title" of the distance matrix, which can explain how it was produced
	 */
	public String getDistMatrixTitle() {
		return distMatrixTitle;
	}

	/**
	 * Sets the "title" of the distance matrix, which can explain how it was produced
	 */
	public void setDistMatrixTitle(String distMatrixTitle) {
		this.distMatrixTitle = distMatrixTitle;
	}
}
