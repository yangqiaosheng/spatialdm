package spade.vis.mapvis;

import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.DialogContent;
import spade.lib.basicwin.Slider;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 30-Jan-2007
 * Time: 11:34:30
 * Used for changing sizes, e.g. minimum and maximum line thickness
 */
public class SizeChanger extends Panel implements DialogContent, ActionListener {
	/**
	 * The object that checks if the sizes selected by the user are correct
	 */
	protected SizeChecker sizeChecker = null;
	/**
	 * Indicates that the sizes are expected to be integer
	 */
	protected boolean sizesAreInteger = true;
	/**
	 * Sliders used to regulate the sizes
	 */
	protected Slider sliders[] = null;
	/**
	 * Text fields for entering the exact values
	 */
	protected TextField tf[] = null;
	/**
	 * Error message, which may be received from the SizeChecker
	 */
	protected String err = null;

	/**
	 * Constructs the UI.
	 * @param sizes - the current values of the sizes
	 * @param names - the names of the sizes (i.e. texts to be shown to the user)
	 * @param minSize - the minimum possible value for a size
	 * @param maxSize - the maximum possible value for a size
	 * @param sizeChecker - the object that will check the sizes
	 */
	public SizeChanger(float sizes[], String names[], float minSize, float maxSize, SizeChecker sizeChecker) {
		this.sizeChecker = sizeChecker;
		sizesAreInteger = false;
		if (sizes == null || sizes.length < 1)
			return;
		constructUI(sizes, names, minSize, maxSize);
	}

	public SizeChanger(int sizes[], String names[], int minSize, int maxSize, SizeChecker sizeChecker) {
		this.sizeChecker = sizeChecker;
		sizesAreInteger = true;
		if (sizes == null || sizes.length < 1)
			return;
		float fSizes[] = new float[sizes.length];
		for (int i = 0; i < sizes.length; i++) {
			fSizes[i] = sizes[i];
		}
		constructUI(fSizes, names, minSize, maxSize);
	}

	protected void constructUI(float sizes[], String names[], float minSize, float maxSize) {
		sliders = new Slider[sizes.length];
		tf = new TextField[sizes.length];
		for (int i = 0; i < sizes.length; i++) {
			String strVal = (sizesAreInteger) ? String.valueOf(Math.round(sizes[i])) : String.valueOf(sizes[i]);
			tf[i] = new TextField(strVal, 5);
			sliders[i] = new Slider(this, minSize, maxSize, sizes[i]);
			sliders[i].setTextField(tf[i]);
			sliders[i].setNAD(true);
			sliders[i].setValueIsInteger(sizesAreInteger);
		}
		ColumnLayout cl = new ColumnLayout();
		cl.setAdjustWidthToViewport(true);
		setLayout(cl);
		for (int i = 0; i < sizes.length; i++) {
			Panel p = new Panel(new BorderLayout());
			p.add(new Label(names[i]), BorderLayout.WEST);
			p.add(tf[i], BorderLayout.EAST);
			add(p);
			add(sliders[i]);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	}

	public float[] getSizes() {
		if (sliders == null)
			return null;
		float sizes[] = new float[sliders.length];
		for (int i = 0; i < sliders.length; i++) {
			sizes[i] = (float) sliders[i].getValue();
		}
		return sizes;
	}

	public int[] getIntSizes() {
		if (sliders == null)
			return null;
		int sizes[] = new int[sliders.length];
		for (int i = 0; i < sliders.length; i++) {
			sizes[i] = sliders[i].getIntValue();
		}
		return sizes;
	}

	@Override
	public boolean canClose() {
		err = null;
		if (sizeChecker == null)
			return true;
		if (sliders == null)
			return true;
		err = sizeChecker.checkSizes(getSizes());
		return err == null;
	}

	@Override
	public String getErrorMessage() {
		return err;
	}
}
