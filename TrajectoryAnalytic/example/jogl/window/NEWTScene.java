package jogl.window;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;

public class NEWTScene {

	public static void main(String[] args) {
		GLProfile glp = GLProfile.getDefault();
		GLCapabilities caps = new GLCapabilities(glp);

		GLWindow window = GLWindow.create(caps);
		window.setSize(300, 300);
		window.setVisible(true);
		window.setTitle("NEWT Window Test");

		window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowDestroyNotify(WindowEvent arg0) {
				System.exit(0);
			};
		});

	}
}
