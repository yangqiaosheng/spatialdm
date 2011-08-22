package spade.vis.dataview;

/**
* Used for data record viewing.
*/
public interface DataViewInformer {
	public DataViewRegulator getDataViewRegulator();

	public TransformedDataPresenter getTransformedDataPresenter();
}