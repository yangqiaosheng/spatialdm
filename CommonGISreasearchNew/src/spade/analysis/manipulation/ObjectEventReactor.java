package spade.analysis.manipulation;

/**
* This interface is to be implemented by display manipulators that can react
* to object events. For example, selection of an object may mean setting of
* a reference value/object in visual comparison.
*/
public interface ObjectEventReactor {
	/**
	* An ObjectEventReactor may process object events either from all displays
	* or from the component (e.g. map) it is attached to. This component is a
	* primary event source for the ObjectEventReactor. A reference to the
	* primary event source is set using this method.
	*/
	public void setPrimaryEventSource(Object evtSource);
}
