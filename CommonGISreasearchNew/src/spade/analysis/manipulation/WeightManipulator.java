package spade.analysis.manipulation;

import java.awt.event.ActionListener;
import java.util.Vector;

import spade.analysis.calc.WAPanel;
import spade.lib.basicwin.Slider;
import spade.lib.util.FloatArray;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;

/**
* A WeightManipulator allows the user to set weights of attributes to reflect
* their relative importance for user's decision problem. A WeightManipulator
* is used to manipulate the "utility signs" map visualisation.
*/
public class WeightManipulator extends WAPanel {
	protected Vector weightUsers = null;

	public WeightManipulator(AttributeDataPortion dTable, Vector attr) {
		super(null, dTable, attr);
	}

	public WeightManipulator(ActionListener al, AttributeDataPortion dTable, Vector attr) {
		super(al, dTable, attr);
	}

	public void addWeightUser(AttrWeightUser user) {
		if (user == null)
			return;
		user.setWeights(getWeightsList());
		if (weightUsers == null) {
			weightUsers = new Vector(2, 2);
		}
		if (weightUsers.indexOf(user) < 0) {
			weightUsers.addElement(user);
		}
	}

	public void removeWeightUser(AttrWeightUser user) {
		if (user == null || weightUsers == null)
			return;
		weightUsers.removeElement(user);
	}

	protected FloatArray getWeightsList() {
		float w[] = getWeights();
		if (w == null)
			return null;
		boolean b[] = getIsMax();
		FloatArray weights = new FloatArray(w.length, 5);
		for (int i = 0; i < w.length; i++) {
			weights.addElement((b[i]) ? w[i] : -w[i]);
		}
		return weights;
	}

	@Override
	protected void removeAttribute(int n) {
		if (n < 0 || n >= attr.size())
			return;
		super.removeAttribute(n);
		for (int i = 0; i < weightUsers.size(); i++) {
			((AttrWeightUser) weightUsers.elementAt(i)).removeAttribute(n);
		}
		passWeightsToUsers();
	}

	@Override
	protected boolean addAttributes(Vector attributes) {
		if (!super.addAttributes(attributes))
			return false;
		for (int i = 0; i < weightUsers.size(); i++) {
			for (int j = 0; j < attributes.size(); j++) {
				Attribute a = (Attribute) attributes.elementAt(j);
				((AttrWeightUser) weightUsers.elementAt(i)).addAttribute(a.getIdentifier(), a.getName());
			}
		}
		passWeightsToUsers();
		return true;
	}

	protected void passWeightsToUsers() {
		if (weightUsers == null)
			return;
		if (attr == null) {
			for (int i = 0; i < weightUsers.size(); i++) {
				((AttrWeightUser) weightUsers.elementAt(i)).setWeights(null);
			}
			return;
		}
		FloatArray weights = getWeightsList();
		for (int i = 0; i < weightUsers.size(); i++) {
			((AttrWeightUser) weightUsers.elementAt(i)).setWeights(weights);
		}
	}

	@Override
	protected void weightsChanged() {
		passWeightsToUsers();
		super.weightsChanged();
	}

//ID
	public void setWeightsList(FloatArray weights) {
		if (weights == null)
			return;
		float sum = 0;
		for (int i = 0; i < weights.size(); i++) {
			sum += weights.elementAt(i);
		}
		for (int i = 0; i < weights.size(); i++) {
			((Slider) sliders.elementAt(i)).setValue(weights.elementAt(i) / sum);
		}
		adjustWeights(0, weights.elementAt(0));
	}
//~ID
}
