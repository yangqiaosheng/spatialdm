package spade.analysis.system;

/**
* Implemented by components that can re-create various analysis tools the
* states of which were previously stored externally. For tool re-creation,
* the components use tool specifications retrieved from the external storage.
*/
public interface ToolReCreator {
	/**
	* Replies whether this component can use the given specification for
	* re-constructing the corresponding tool. The argument spec may be an instance
	* of some specification class, for example, spade.vis.spec.ToolSpec or
	* spade.vis.spec.AnimatedVisSpec. The method must check whether the class
	* of the specification is appropriate for this tool re-creator.
	*/
	public boolean canFulfillSpecification(Object spec);

	/**
	* On the basis of the given specification, re-constructs the corresponding
	* tool. The argument @arg spec may be an instance of some specification class,
	* for example, spade.vis.spec.ToolSpec or spade.vis.spec.AnimatedVisSpec.
	* The argument @arg visManager is a component used for creating visual data
	* displays and cartographic visualizers. This may be either a DisplayProducer
	* or a SimpleDataMapper, depending on the configuration.
	*/
	public void fulfillSpecification(Object spec, DataKeeper dKeeper, Supervisor supervisor, Object visManager, boolean makeMapManipulator);
}