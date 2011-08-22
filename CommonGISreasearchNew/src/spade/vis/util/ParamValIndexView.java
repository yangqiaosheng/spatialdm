package spade.vis.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import spade.vis.database.Parameter;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Nov 9, 2009
 * Time: 11:58:38 AM
 * Shows which of the parameter values are selected.
 * For this purpose, displays a matrix where each cell represents
 * one parameter value. The cells corresponding to the selected values
 * are marked e.g. by black filling. The user may vary the number of
 * columns in the matrix.
 */
public class ParamValIndexView extends Panel implements ActionListener {
	/**
	 * The default number of columns
	 */
	public static int nColumnsDefault = 12;
	/**
	 * The desired number of columns
	 */
	public int nColumns = nColumnsDefault;
	/**
	 * The parameter whose values may be selected
	 */
	public Parameter param = null;
	/**
	 * The canvas in which the selection status is shown
	 */
	protected MatrixCanvas matr = null;
	/**
	 * The text field for the user to enter the desired number of columns
	 * in the matrix
	 */
	protected TextField tfNColumns = null;

	public ParamValIndexView(Parameter param) {
		this(param, nColumnsDefault);
	}

	public ParamValIndexView(Parameter param, int nColumns) {
		this.param = param;
		if (nColumns > 0) {
			nColumnsDefault = nColumns;
		}
		setLayout(new BorderLayout());
		matr = new MatrixCanvas();
		matr.setNItems(param.getValueCount());
		matr.setNColumns(nColumnsDefault);
		add(matr, BorderLayout.CENTER);

		Panel p = new Panel(new BorderLayout());
		tfNColumns = new TextField(String.valueOf(nColumnsDefault), 2);
		p.add(tfNColumns, BorderLayout.WEST);
		tfNColumns.addActionListener(this);
		p.add(new Label("columns", Label.LEFT), BorderLayout.CENTER);
		add(p, BorderLayout.NORTH);
	}

	public void addSelectedValueIndex(int idx) {
		if (idx < 0 || idx >= param.getValueCount())
			return;
		matr.setItemSelectionStatus(idx, true);
	}

	public void addSelectedParamValue(Object value) {
		if (value == null)
			return;
		int idx = -1;
		for (int i = 0; i < param.getValueCount() && idx < 0; i++)
			if (param.getValue(i).equals(value)) {
				idx = i;
			}
		if (idx >= 0) {
			matr.setItemSelectionStatus(idx, true);
		}
	}

	public void deselectAllValues() {
		for (int i = 0; i < param.getValueCount(); i++) {
			matr.setItemSelectionStatus(i, false);
		}
	}

	public int getNColumns() {
		return nColumns;
	}

	public void setNColumns(int nColumns) {
		this.nColumns = nColumnsDefault = nColumns;
		if (matr != null) {
			matr.setNColumns(nColumns);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(tfNColumns)) {
			String str = tfNColumns.getText();
			if (str != null) {
				int nc = 0;
				try {
					nc = Integer.parseInt(str.trim());
				} catch (Exception ex) {
				}
				if (nc > 0 && nc != matr.getNColumns()) {
					matr.setNColumns(nc);
					matr.repaint();
					nColumns = nColumnsDefault = nc;
				}
			}
		}
	}

	public BufferedImage getImage() {
		int oldPixelSize = matr.pixelSize;
		matr.setPixelSize(4);
		Dimension size = matr.getPreferredSize();
		BufferedImage im = new BufferedImage(size.width + 1, size.height + 1, BufferedImage.TYPE_INT_RGB);
		Graphics g = im.getGraphics();
		g.setColor(Color.white);
		g.fillRect(0, 0, size.width + 2, size.height + 2);
		matr.draw(g, size);
		matr.setPixelSize(oldPixelSize);
		return im;
	}
}
