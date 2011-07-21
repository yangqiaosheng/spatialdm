package jogl.youtube;
public class HelloJogl extends JoglApp {

	HelloJogl(String Name_value, int x, int y) {
		super(Name_value, x, y);
	}

	public static void main(String[] args) {
		new HelloJogl("Hallo Jogl", 800, 600);

	}

}
