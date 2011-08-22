package spade.vis.mapvis;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import spade.lib.basicwin.MyCanvas;
import spade.lib.basicwin.Slider;
import spade.vis.geometry.DrawableSymbol;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 8, 2010
 * Time: 5:55:01 PM
 * Changes the size of a symbol
 */
public class SymbolSizeChanger extends Panel implements ActionListener, PropertyChangeListener {
	/**
	 * An instance of Sign to change the size
	 */
	protected DrawableSymbol sign = null;
	/**
	 * Changes the size through moving the slider
	 */
	protected Slider slider = null;
	/**
	 * Text field for entering the exact values
	 */
	protected TextField tf = null;
	/**
	 * The canvas in which the sign is drawn
	 */
	protected MyCanvas canvas = null;

	public SymbolSizeChanger(DrawableSymbol sign, int minSize, int maxSize, int currSize) {
		this.sign = sign;
		setLayout(new BorderLayout());
		canvas = new MyCanvas();
		Dimension sz = new Dimension(maxSize + 20, maxSize + 20);
		canvas.setPreferredSize(sz);
		canvas.setMinimumSize(sz);
		canvas.setPainter(this);
		add(canvas, BorderLayout.CENTER);
		tf = new TextField(String.valueOf(currSize), 3);
		slider = new Slider(this, minSize, maxSize, currSize);
		slider.setTextField(tf);
		slider.setNAD(true);
		slider.setValueIsInteger(true);
		Panel p = new Panel(new BorderLayout());
		p.add(slider, BorderLayout.CENTER);
		p.add(tf, BorderLayout.EAST);
		add(p, BorderLayout.SOUTH);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(slider)) {
			int val = (int) Math.round(slider.getValue());
			sign.setSize(val);
			Graphics g = canvas.getGraphics();
			if (g != null) {
				Dimension sz = canvas.getSize();
				g.setColor(canvas.getBackground());
				g.fillRect(0, 0, sz.width + 1, sz.height + 1);
				sign.draw(g, 0, 0, sz.width, sz.height);
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(canvas)) {
			Graphics g = canvas.getGraphics();
			if (g != null) {
				Dimension sz = canvas.getSize();
				sign.draw(g, 0, 0, sz.width, sz.height);
			}
		}
	}
}
