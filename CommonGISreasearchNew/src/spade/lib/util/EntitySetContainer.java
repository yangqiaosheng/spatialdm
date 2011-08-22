package spade.lib.util;

/**
* The interface EntitySetContainer may be implemented by classes that refer to
* some collections of objects. A collection must have a unique identifier.
* The container itself has its identifier that is not the same as the identifier
* of the entity set. In particular, several containers may refer to the same
* set of entities.
*/
public interface EntitySetContainer {
	/**
	* Returns the identifier of the entity set kept or referred to by this
	* container
	*/
	public String getEntitySetIdentifier();

	/**
	* Sets the identifier of the entity set kept or referred to by this
	* container
	*/
	public void setEntitySetIdentifier(String setId);

	/**
	* Returns the identifier of the container (that is not the same as the
	* identifier of the entity set!)
	*/
	public String getContainerIdentifier();

	/**
	 * Sets a generic name of the entities in the container
	 */
	public void setGenericNameOfEntity(String name);

	/**
	 * Returns the generic name of the entities in the container.
	 * May return null, if the name was not previously set.
	 */
	public String getGenericNameOfEntity();
}