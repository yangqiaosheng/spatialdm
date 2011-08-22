package spade.analysis.system;

import org.w3c.dom.Document;

import spade.vis.dmap.DGeoLayer;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 6, 2009
 * Time: 5:33:55 PM
 * This interface may be used for an analysis tool capable of using some
 * of earlier obtained analysis results for further analysis.
 * Processors are registered at the system's core and can be accessed from
 * there for use.
 */
public interface Processor {
	/**
	 * The possible types of objects that can be processed by a processor
	 */
	public final int TABLE_RECORD = 0, GEO_POINT = 1, GEO_AREA = 2, GEO_LINE = 3, GEO_TRAJECTORY = 4;
	public final int OBJECT_TYPES[] = { TABLE_RECORD, GEO_POINT, GEO_AREA, GEO_LINE, GEO_TRAJECTORY };

	/**
	 * Returns the name of this tool
	 */
	public String getName();

	/**
	 * Sets the name of this tool
	 */
	public void setName(String aName);

	/**
	 * Returns the class name of the result obtained after processing a single object
	 */
	public String getSingleResultClassName();

	/**
	 * Returns the class name of the final result obtained after processing all objects
	 */
	public String getFinalResultClassName();

	/**
	 * Replies whether this processor is applicable to the given type of object
	 * @param objType - the type of the object, which must be equal to one of the
	 *   above-defined constants
	 * @return true if applicable
	 */
	public boolean isApplicableTo(int objType);

	/**
	 * Initialises what is needed for the operation.
	 * The Processor may access any system components, data, maps, etc.
	 * through the provided system core.
	 */
	public void initialise(ESDACore core);

	/**
	 * Processes the given object
	 * @param obj - the object to process
	 * @return the result of object processing or null if the object cannot be
	 *         processed
	 */
	public Object processObject(Object obj);

	/**
	 * Returns the final result of the processing
	 */
	public Object getResult();

	/**
	 * Replies if this processor can operate in an automatic mode,
	 * i.e. without any user interface. This method returns true if the
	 * user interface is optional or not needed at all.
	 */
	public boolean canWorkAutomatically();

	/**
	 * If the Processor has some user interface (possibly, optional),
	 * it is created in this method.
	 */
	public void createUI();

	/**
	 * Closes the user interface, if it has been previously created.
	 */
	public void closeUI();

	/**
	 * Stores the information about the processor in XML format
	 */
	public String toXML();

	/**
	 * Checks if this type of processor can be restored from the given
	 * XML document
	 */
	public boolean canRestoreFromXML(Document doc);

	/**
	 * Restores itself (i.e. all necessary internal settings) from
	 * the given XML document. Returns true if successful.
	 */
	public boolean restoreFromXML(Document doc);

	/**
	 * Informs if two or more processors of this type can be joined
	 * in a single processor.
	 */
	public boolean canJoin();

	/**
	 * Joins this processor with another processor of the same type.
	 * Returns the resulting processor or null if not successful.
	 */
	public Processor join(Processor proc);

	/**
	 * Returns an error message, if some operation has failed.
	 */
	public String getErrorMessage();

	/**
	 * Informs if this processor can generate a map layer from its content.
	 */
	public boolean canMakeMapLayer();

	/**
	 * Produces a map layer from its content and, possibly, a table with
	 * thematic data attached to the layer. Returns the layer, if successful.
	 * The layer has a reference to the produced table, if any.
	 */
	public DGeoLayer makeMapLayer(String name);
}
