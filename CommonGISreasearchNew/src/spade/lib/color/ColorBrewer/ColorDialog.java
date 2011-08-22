package spade.lib.color.ColorBrewer;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Oct 29, 2009
 * Time: 11:47:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class ColorDialog extends Panel implements ActionListener {

	protected int classes = 0;
	protected Button less;
	protected Button more;
	protected Button edit;
	protected Button add;
	protected TextField current;
	protected Vector gradients;
	protected Panel p, n;
	public static TextArea descr = new TextArea("Click to color gradient to see more information", 100, 60);
	public static int[][] clicked;

	//Construct the frame
	public ColorDialog() {
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try {
			less = new Button("<<");
			more = new Button(">>");
			less.addActionListener(this);
			more.addActionListener(this);
			edit = new Button("Edit current scheme");
			add = new Button("New scheme");
			edit.addActionListener(this);
			add.addActionListener(this);
			current = new TextField("0"/*, Label.CENTER*/);
			current.addActionListener(this);

			jbInit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//Component initialization
	private void jbInit() throws Exception {
		//setIconImage(Toolkit.getDefaultToolkit().createImage(ColorDialog.class.getResource("[Your Icon]")));

		p = new Panel(new GridLayout(Schemes.register.size(), 1));
		n = new Panel(new GridLayout(Schemes.register.size(), 1));
		Panel h = new Panel(new GridLayout(1, 3));
		Panel r = new Panel(new BorderLayout());
		Panel rb = new Panel(new GridLayout(1, 2));
		descr.setEditable(false);
		ScrollPane sp = new ScrollPane();
		Panel sp1 = new Panel(new BorderLayout());
		sp1.setLayout(new BorderLayout());
		setLayout(new BorderLayout());
		sp1.add(p, "Center");
		sp.add(sp1);
		add(sp, "Center");
		add(h, "North");
		add(r, "East");
		sp1.add(n, "West");

		r.add(descr, "Center");
		r.add(rb, "South");

		rb.add(edit);
		rb.add(add);

		h.add(less);
		h.add(current);
		h.add(more);

		gradients = new Vector(Schemes.register.size());

		for (int i = 0; i < Schemes.register.size(); i++) {
			gradients.addElement(new Gradient((int[][]) Schemes.register.elementAt(i), classes));
		}
		for (int i = 0; i < Schemes.register.size(); i++) {
			p.add((Component) gradients.elementAt(i));
		}
		for (int i = 0; i < Schemes.register.size(); i++) {
			n.add(new Label((String) Schemes.names.elementAt(i)));
		}

	}

	public static void schemeChanged(int[][] scheme, String text) {
		descr.setText(text);
		clicked = scheme;
	}

	public void reinitialize() {
		removeAll();
		try {
			jbInit();
		} catch (Exception e) {
			e.printStackTrace();
		}
		validate();
	}

	@Override
	public void actionPerformed(ActionEvent ev) {
		if (ev.getSource().equals(less)) {
			classes -= 1;
		}
		if (ev.getSource().equals(more)) {
			classes += 1;
		}
		if (classes < 0) {
			classes = 0;
		}
		if (ev.getSource().equals(current)) {
			try {
				classes = Integer.parseInt(current.getText());
			} catch (Exception ex) {
			}
		}
		current.setText(Integer.toString(classes));
		for (int i = 0; i < Schemes.register.size(); i++) {
			((Gradient) gradients.elementAt(i)).setClasses(classes);
			/*
			if(ev.getSource().equals(add)) {
			  Schemes.names.addElement("new");
			  Schemes.register.addElement(new int[][] {
			    {0, 0, 0},
			    {255, 255, 255}
			  });
			  reinitialize();
			}
			if(ev.getSource().equals(edit)) {
			  if(clicked!=null) {
			    Frame ed = new EditDialog((Panel)this, clicked);
			    ed.setVisible(true);
			  }
			}
			*/
//    for (int i=0; i<Schemes.register.length; i++) ((Component)gradients[i]).repaint();
		}
	}

}
