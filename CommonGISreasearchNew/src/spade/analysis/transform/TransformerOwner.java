package spade.analysis.transform;

public interface TransformerOwner {
	/**
	* Returns its transformer of attribute values (if exists)
	*/
	public AttributeTransformer getAttributeTransformer();
}