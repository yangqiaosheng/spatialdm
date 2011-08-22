package ui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.lang.Language;
import spade.vis.map.Zoomable;

public class Zoomer extends Panel implements ActionListener, PropertyChangeListener {
	static ResourceBundle res = Language.getTextResource("ui.Res");
	protected Zoomable map = null;
	protected TextField factorTF = null;
	protected Button buttons[] = new Button[9];

	public Zoomer() {
		super();
		setLayout(new ColumnLayout());
		Panel p = new Panel(new BorderLayout());
		buttons[0] = new Button("+");
		buttons[0].setActionCommand("increase");
		buttons[0].addActionListener(this);
		p.add(buttons[0], "West");
		buttons[1] = new Button("-");
		buttons[1].setActionCommand("decrease");
		buttons[1].addActionListener(this);
		p.add(buttons[1], "East");
		add(p);
		p = new Panel(new FlowLayout());
		// following string: "Fit to the window
		buttons[2] = new Button(res.getString("Fit_to_the_window"));
		buttons[2].setActionCommand("fit");
		buttons[2].addActionListener(this);
		p.add(buttons[2]);
		add(p);
		p = new Panel(new BorderLayout());
		// following string: "Multiply by"
		buttons[3] = new Button(res.getString("Multiply_by"));
		buttons[3].setActionCommand("multiply");
		buttons[3].addActionListener(this);
		p.add(buttons[3], "West");
		factorTF = new TextField(3);
		factorTF.addActionListener(this);
		p.add(factorTF, "East");
		Panel pp = new Panel(new FlowLayout(FlowLayout.CENTER));
		pp.add(p);
		add(pp);
		p = new Panel(new GridLayout(3, 3));
		p.add(new Label(""));
		// following string: "North"
		buttons[4] = new Button(res.getString("North"));
		buttons[4].setActionCommand("North");
		buttons[4].addActionListener(this);
		p.add(buttons[4]);
		p.add(new Label(""));
		// following string: "West"
		buttons[5] = new Button(res.getString("West"));
		buttons[5].setActionCommand("West");
		buttons[5].addActionListener(this);
		p.add(buttons[5]);
		p.add(new Label(""));
		// following string: ("East"
		buttons[6] = new Button(res.getString("East"));
		buttons[6].setActionCommand("East");
		buttons[6].addActionListener(this);
		p.add(buttons[6]);
		p.add(new Label(""));
		// following string: "South"
		buttons[7] = new Button(res.getString("South"));
		buttons[7].setActionCommand("South");
		buttons[7].addActionListener(this);
		p.add(buttons[7]);
		add(p);
		add(new Line(false));
		p = new Panel(new BorderLayout());
		// following string: "Undo"
		buttons[8] = new Button(res.getString("Undo"));
		buttons[8].setActionCommand("undo");
		buttons[8].addActionListener(this);
		p.add(buttons[8], "East");
		add(p);
	}

	public void setObjectToZoom(Zoomable zoomable) {
		map = zoomable;
		map.addPropertyChangeListener(this);
		setButtonStatus();
	}

	protected void setButtonStatus() {
		if (map == null) {
			for (Button button : buttons) {
				button.setEnabled(false);
			}
		} else {
			for (Button button : buttons) {
				String cmd = button.getActionCommand();
				if (cmd.equals("increase")) {
					button.setEnabled(map.canZoomIn());
				} else if (cmd.equals("decrease")) {
					button.setEnabled(map.canZoomOut());
				} else if (cmd.equals("fit")) {
					button.setEnabled(map.canPan());
				} else if (cmd.equals("North") || cmd.equals("South") || cmd.equals("East") || cmd.equals("West")) {
					button.setEnabled(map.canMove(cmd));
				} else if (cmd.equals("undo")) {
					button.setEnabled(map.canUndo());
				}
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (map == null)
			return;
		if (e.getSource() == factorTF) {
			zoomByFactor();
			return;
		}
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("increase")) {
			map.zoomIn();
		} else if (cmd.equals("decrease")) {
			map.zoomOut();
		} else if (cmd.equals("fit")) {
			map.pan();
		} else if (cmd.equals("multiply")) {
			zoomByFactor();
		} else if (cmd.equals("North") || cmd.equals("South") || cmd.equals("East") || cmd.equals("West")) {
			map.move(cmd);
		} else if (cmd.equals("undo")) {
			map.undo();
		}
	}

	protected void zoomByFactor() {
		String str = factorTF.getText();
		if (str == null)
			return;
		str = str.trim();
		if (str.length() < 1)
			return;
		float factor = 1.0f;
		try {
			factor = Float.valueOf(str).floatValue();
		} catch (NumberFormatException nfe) {
			factorTF.setText("");
			return;
		}
		if (factor <= 0) {
			factorTF.setText("");
			return;
		}
		map.zoomByFactor(factor);
	}

	/**
	* When map scale changes, state of the buttons may also change
	*/
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getSource() == map && evt.getPropertyName().equals("MapScale")) {
			setButtonStatus();
		}
	}
}
