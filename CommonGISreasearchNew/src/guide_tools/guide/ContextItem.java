package guide_tools.guide;

import java.util.Vector;

import spade.kbase.scenarios.ContextElement;

/**
* A ContextItem can specify such elements of task context as map, layers,
* attributes etc. used in currently followed scenarion or task.
* A ContextItem is formed on the basis of a ContextElement specified in
* the knowledge base.
* This is the base class for classes  
*/

public class ContextItem {
	/**
	* The ContextElement from which this ContextItem was produced (by means of
	* specification)
	*/
	protected ContextElement template = null;
	/**
	* The content of this context element. The nature of this object is determined
	* by the type of this context element. In particular, this may be a layer
	* identifier or a vector of layer identifiers
	*/
	protected Object content = null;
	/**
	* Each context element can refer to exactly one map. Maps available during
	* a session of the system are distinguished by their numbers (numeric
	* identifiers). This data element keeps the number of the map this context
	* item refers to. By default, the system has just one map, and all context
	* items refer to it.
	*/
	protected int mapN = 0;
	/**
	* Indicates whether this context item is currently active, i.e. refers to
	* a task or scenario being currently performed. Only active context items
	* should be shown in the context view.
	*/
	protected boolean active = true;

	/**
	* The argument cel is the ContextElement from which this ContextItem is
	* produced. The argument content defines the content of this context element.
	* The nature of this object is determined by the type of this context element.
	* In particular, this may be a layer identifier or a vector of layer
	* identifiers, or a vector of attribute identifiers.
	* mapN is the number of the map this context item refers to.
	*/
	public ContextItem(ContextElement cel, int mapN, Object content) {
		template = cel;
		this.mapN = mapN;
		this.content = content;
	}

	/**
	* Returns the ContextElement from which this ContextItem was produced
	*/
	public ContextElement getTemplate() {
		return template;
	}

	/**
	* Returns the type of this ContextItem ("layer", "attributes", "attr_filter"
	* etc., see ContextElement.contextTypes) The type is taken from the
	* template.
	*/
	public String getType() {
		if (template == null)
			return null;
		return template.type;
	}

	/**
	* Returns the identifier of this ContextItem (takes it from the template)
	*/
	public String getId() {
		if (template == null)
			return null;
		return template.localId;
	}

	/**
	* Returns the identifier of the task this ContextItem was defined for
	*/
	public String getTaskId() {
		if (template == null)
			return null;
		return template.getTaskId();
	}

	/**
	* A ContextItem can refer to some other ContextItem, for example,
	* a table to a geographical layer. This method returns the identifier
	* of the item this ContextItem refers to (takes it from the template).
	*/
	public String getRefersTo() {
		if (template == null)
			return null;
		return template.refersTo;
	}

	/**
	* Returns the content of this context element.
	*/
	public Object getContent() {
		return content;
	}

	/**
	* Checks if the content of this context item is the same as the given content
	*/
	public boolean sameContent(Object cc) {
		if (cc == null)
			return content == null;
		if (content == null)
			return false;
		if (content instanceof Vector) {
			if (!(cc instanceof Vector))
				return false;
			Vector cv1 = (Vector) content, cv2 = (Vector) cc;
			if (cv1.size() != cv2.size())
				return false;
			for (int i = 0; i < cv1.size(); i++)
				if (!cv1.elementAt(i).equals(cv2.elementAt(i)))
					return false;
			return true;
		}
		return content.equals(cc);
	}

	/**
	* Returns the number (numeric identifier) of the map this context item refers to.
	*/
	public int getMapNumber() {
		return mapN;
	}

	/**
	* Replies whether this context item is currently active, i.e. refers to
	* a task or scenario being currently performed. Only active context items
	* should be shown in the context view.
	*/
	public boolean isActive() {
		return active;
	}

	public void setIsActive(boolean value) {
		active = value;
	}
}
