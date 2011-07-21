package jogl.shape;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;



public class Shapes implements GLEventListener
{
    private GLU glu;
    private int w, h;

    public Shapes()
    {
    }

    @Override
    public void init(GLAutoDrawable drawable)
    {
    	w = drawable.getWidth();
        h = drawable.getHeight();

        GL2 gl = drawable.getGL().getGL2();
        glu = new GLU();

        gl.glShadeModel(GL2.GL_SMOOTH);
        gl.glClearColor(0.0f,0.0f,0.0f,0.0f);
        gl.glClearDepth(1.0f);
        gl.glClearDepth(1.0f);												// Depth Buffer Setup
    	gl.glEnable(GL2.GL_DEPTH_TEST);										// Enables Depth Testing
    	gl.glDepthFunc(GL2.GL_LEQUAL);										// The Type Of Depth Test To Do
    	gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);			// Really Nice Perspective Calculations
    }

    @Override
    public void display(GLAutoDrawable drawable)
    {
    	GL2 gl = drawable.getGL().getGL2();

        w = drawable.getWidth();
        h = drawable.getHeight();

        gl.glClear(GL2.GL_COLOR_BUFFER_BIT|GL2.GL_DEPTH_BUFFER_BIT);			// Clear the colour and depth buffer

        gl.glViewport(0, 0, w, h);											// Reset The Current Viewport

        gl.glMatrixMode(GL2.GL_PROJECTION);									// Select The Projection Matrix
        gl.glLoadIdentity();												// Reset The Projection Matrix

        glu.gluPerspective(45.0f,(float)w/(float)h,0.1f,100.0f);			// Calculate The Aspect Ratio Of The Window

        gl.glMatrixMode(GL2.GL_MODELVIEW);									// Select The Modelview Matrix
        gl.glLoadIdentity();												// Reset The Modelview Matrix

        drawScene(drawable);												// Draw the scene
    }

    @Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int w2, int h2)
    {
		GL2 gl = drawable.getGL().getGL2();

        w2 = drawable.getWidth();
        h2 = drawable.getHeight();

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        // perspective view
        gl.glViewport(10, 10, w-20, h-20);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45.0f,(float)w/(float)h,0.1f,100.0f);
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged)
    {
    }

    public void drawScene(GLAutoDrawable drawable)
    {
    	GL2 gl = drawable.getGL().getGL2();

    	 gl.glClear(GL2.GL_COLOR_BUFFER_BIT|GL2.GL_DEPTH_BUFFER_BIT);   		// Clear the colour and depth buffer

    	gl.glPushMatrix();
    		gl.glTranslatef(-1.5f,1.5f,-8.0f);								// Move left 1.5 units, up 1.5 units, and back 8 units.

    		gl.glBegin(GL2.GL_TRIANGLES);									// Begin drawing triangles
    		gl.glVertex3f( 0.0f, 1.0f, 0.0f);								// Top vertex
    		gl.glVertex3f(-1.0f,-1.0f, 0.0f);								// Bottom left vertex
    		gl.glVertex3f( 1.0f,-1.0f, 0.0f);								// Bottom right vertex
    		gl.glEnd();														// Finish drawing triangles
    	gl.glPopMatrix();

    	gl.glPushMatrix();
    		gl.glTranslatef(1.5f,1.5f,-8.0f);								// Move left 1.5 units, up 1.5 units, and back 8 units.

    		gl.glBegin(GL2.GL_QUADS);										// Begin drawing quads
    		gl.glColor3f(1.0f,0.0f,0.0f);									// Set The Color To Blue
    		gl.glVertex3f(-1.0f, 1.0f, 0.0f);								// Top left vertex
    		gl.glVertex3f( 1.0f, 1.0f, 0.0f);								// Top right vertex
    		gl.glColor3f(0.0f,0.0f,1.0f);									// Set The Color To Red
    		gl.glVertex3f( 1.0f,-1.0f, 0.0f);								// Bottom right vertex
    		gl.glVertex3f(-1.0f,-1.0f, 0.0f);								// Bottom left vertex
    		gl.glEnd();														// Finish drawing quads
    	gl.glPopMatrix();

    	gl.glPushMatrix();
			gl.glTranslatef(-2.0f,-1.5f,-8.0f);								// Move left 2.0 units, up 1.5 units, and back 8 units.

			gl.glBegin(GL2.GL_TRIANGLE_STRIP);								// Begin drawing triangle strip
			gl.glColor3f(0.0f,0.0f,1.0f);									// Set The Color To Blue
			gl.glVertex3f(-1.0f,-0.5f, 0.0f);								// V1
			gl.glVertex3f( 1.0f,-0.5f, 0.0f);								// V2

			gl.glVertex3f( 0.0f, 0.5f, 0.0f);								// V3
			gl.glVertex3f( 1.5f, 0.0f, 0.0f);								// V4
			gl.glVertex3f( 2.0f, -1.5f, 0.0f);								// V5
			gl.glEnd();														// Finish drawing triangle strip
		gl.glPopMatrix();




		gl.glPushMatrix();
			gl.glTranslatef(2.0f,-1.5f,-8.0f);								// Move right 2.0 units, up 1.5 units, and back 8 units.
			gl.glBegin(GL2.GL_TRIANGLE_FAN);
			gl.glColor3f(1.0f,0.0f,0.0f);									// Set The Color To Red
			gl.glVertex3f( 0.0f, 0.0f, -4.0f);    							// V1
			gl.glColor3f(0.0f,1.0f,0.0f);									// Set The Color To Green
			gl.glVertex3f( 0.0f, 1.5f, -4.0f);    							// V2
			gl.glColor3f(0.0f,0.0f,1.0f);									// Set The Color To Blue
			gl.glVertex3f( 1.5f,  0.0f, -4.0f);    							// V3

			gl.glVertex3f( 1.5f, -1.5f, -4.0f);    							// V6
			gl.glColor3f(1.0f,0.0f,0.0f);									// Set The Color To Red
			gl.glVertex3f( 0.0f,  -1.5f, -4.0f);    						// V4
			gl.glColor3f(0.0f,1.0f,0.0f);									// Set The Color To Green
			gl.glVertex3f(-1.5f, 0f, -4.0f);    							// V5
			gl.glEnd();
		gl.glPopMatrix();

    }

	@Override
	public void dispose(GLAutoDrawable drawable) {
		// TODO Auto-generated method stub

	}
}


