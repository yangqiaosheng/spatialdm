package jogl.camera;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;

import com.jogamp.opengl.util.gl2.GLUT;



public class Graphics implements GLEventListener, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener
{
    private GLU glu;
    private GLUT glut;
    private int width, height;
    private ArrayList<Character> characters;

    // Keyboard input
    private boolean[] keys;

    // Camera variables
    private Camera camera;
    private float zoom = 10.0f;
    private float cameraXPosition = 0.0f;
    private float cameraYPosition = 0.0f;
    private float cameraZPosition = 10.0f;

    private float cameraLXPosition = cameraXPosition;
    private float cameraLYPosition = cameraYPosition;
    private float cameraLZPosition = cameraZPosition - zoom;

    private Pacman pacman;
    private Pacman missPacman;

    public Graphics()
    {
    	// boolean array for keyboard input
        keys = new boolean[256];

    	// Initialize the user camera
    	camera = new Camera();
        camera.yawLeft(2.5);
        camera.pitchDown(0.3);
        camera.moveForward(-10);
        camera.look(10);

        characters = new ArrayList<Character>();

        pacman = new Pacman(0.0,0.0,0.0);
        missPacman = new Pacman(6.0,3.0,8.0);
        characters.add(pacman);
        characters.add(missPacman);
    }

    public void init(GLAutoDrawable drawable)
    {
    	width = drawable.getWidth();
        height = drawable.getHeight();

        GL2 gl = drawable.getGL().getGL2();
        glu = new GLU();
        glut = new GLUT();

        gl.setSwapInterval(0); 												// Refreshes screen at 60fps

        float light_ambient[] = { 0.2f, 0.2f, 0.2f, 1.0f };
        float light_diffuse[] = { 0.8f, 0.8f, 0.8f, 1.0f };
        float light_specular[] = { 0.5f, 0.5f, 0.5f, 1.0f };

        /* light_position is NOT default value */
        float light_position[] = { 1.0f, 1.0f, 1.0f, 0.0f };


        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, light_ambient, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, light_diffuse, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, light_specular, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, light_position, 0);


        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);

        gl.glShadeModel(GL2.GL_SMOOTH);
        gl.glClearColor(0.0f,0.0f,0.0f,0.0f);
        gl.glClearDepth(1.0f);												// Depth Buffer Setup
    	gl.glEnable(GL2.GL_DEPTH_TEST);										// Enables Depth Testing
    	gl.glDepthFunc(GL2.GL_LEQUAL);										// The Type Of Depth Test To Do
    	gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);			// Really Nice Perspective Calculations

    	glu.gluPerspective(70.0, (float)width/(float)height, 1, 50);
        glu.gluLookAt(camera.getXPos(), camera.getYPos() , camera.getZPos(), camera.getXLPos(), camera.getYLPos(), camera.getZLPos(), 0.0, 1.0, 0.0);
    }

    public void display(GLAutoDrawable drawable)
    {
    	GL2 gl = drawable.getGL().getGL2();

        width = drawable.getWidth();
        height = drawable.getHeight();

        keyboardChecks();													// Responds to keyboard input

        gl.glClear(GL2.GL_COLOR_BUFFER_BIT|GL2.GL_DEPTH_BUFFER_BIT);			// Clear the colour and depth buffer

        gl.glViewport(0, 0, width, height);									// Reset The Current Viewport

        gl.glMatrixMode(GL2.GL_PROJECTION);									// Select The Projection Matrix
        gl.glLoadIdentity();												// Reset The Projection Matrix

        glu.gluPerspective(70.0, (float)width/(float)height, 1, 50);
        glu.gluLookAt(camera.getXPos(), camera.getYPos() , camera.getZPos(),
        		camera.getXLPos(), camera.getYLPos(), camera.getZLPos(), 0.0, 1.0, 0.0);

        gl.glMatrixMode(GL2.GL_MODELVIEW);									// Select The Modelview Matrix
        gl.glLoadIdentity();												// Reset The Modelview Matrix

        drawScene(drawable);												// Draw the scene
    }

	public void reshape(GLAutoDrawable drawable, int x, int y, int w2, int h2)
    {
		GL2 gl = drawable.getGL().getGL2();

        w2 = drawable.getWidth();
        h2 = drawable.getHeight();

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        // perspective view
        gl.glViewport(10, 10, width-20, height-20);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        //glu.gluPerspective(45.0f,(float)width/(float)height,0.1f,100.0f);
        glu.gluPerspective(70.0, (float)width/(float)height, 1, 50);
        glu.gluLookAt(camera.getXPos(), camera.getYPos() , camera.getZPos(), camera.getXLPos(), camera.getYLPos(), camera.getZLPos(), 0.0, 1.0, 0.0);
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged)
    {
    }

    public void drawScene(GLAutoDrawable drawable)
    {
    	GL2 gl = drawable.getGL().getGL2();

		for(Character c : characters)
		{
			c.draw(drawable, glut);
		}
    }

	@Override
	public void keyPressed(KeyEvent key)
    {
    	try
    	{
        char i = key.getKeyChar();
        keys[(int)i] = true;
    	}
    	catch(Exception e){};


    }

	@Override
    public void keyReleased(KeyEvent key)
    {
    	try
    	{
        char i = key.getKeyChar();
        keys[(int)i] = false;
    	}
    	catch(Exception e){};
    }

	@Override
	public void keyTyped(KeyEvent arg0)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseClicked(MouseEvent arg0)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent arg0)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent arg0)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent arg0)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent arg0)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseDragged(MouseEvent arg0)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseMoved(MouseEvent arg0)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent arg0)
	{
		// TODO Auto-generated method stub

	}

	public void keyboardChecks()
    {
        if(keys['w'])
        {
            cameraZPosition -= 0.1;
            cameraLZPosition -= 0.1;

            camera.moveForward(0.1);
            pacman.moveForward(0.1);
            camera.look(10);

        }

        if(keys['s'])
        {
            cameraZPosition += 0.1;
            cameraLZPosition += 0.1;

            camera.moveForward(-0.1);
            pacman.moveForward(-0.1);
            camera.look(10);
        }

        if(keys['j'])
        {
            camera.pitchUp(0.05);
            camera.look(10);
        }

        if(keys['k'])
        {
            camera.pitchDown(0.05);
            camera.look(10);
        }

        if(keys['q'])
        {
            camera.yawLeft(0.01);
            camera.look(10);
        }

        if(keys['e'])
        {
            camera.yawRight(0.01);
            camera.look(10);
        }

        if(keys['a'])
        {
            camera.strafeLeft(0.1);
            camera.look(10);
        }

        if(keys['d'])
        {
            camera.strafeRight(0.1);
            camera.look(10);
        }
    }

	@Override
	public void dispose(GLAutoDrawable drawable) {
		// TODO Auto-generated method stub

	}
}


