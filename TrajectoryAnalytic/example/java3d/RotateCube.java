package java3d;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.GraphicsConfiguration;

import javax.media.j3d.Alpha;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.RotationInterpolator;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3d;

import com.sun.j3d.utils.applet.MainFrame;
import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.universe.SimpleUniverse;

public class RotateCube extends Applet {

	private static final long serialVersionUID = 41293118815177506L;
	private SimpleUniverse u = null;

	public BranchGroup createSceneGraph() {
		BranchGroup objRoot = new BranchGroup();

		TransformGroup objTrans = new TransformGroup();
		objTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		objRoot.addChild(objTrans);

		// 创建一个3D对象，正方体
		objTrans.addChild(new ColorCube(0.4));

		Transform3D yAxis = new Transform3D();
		Alpha rotationAlpha = new Alpha(-1, 6000);

		RotationInterpolator rotator = new RotationInterpolator(rotationAlpha, objTrans, yAxis, 0.0f, (float) Math.PI * 2.0f);
		BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);
		rotator.setSchedulingBounds(bounds);
		objRoot.addChild(rotator);

		objRoot.compile();

		return objRoot;
	}

	public RotateCube() {
	}

	public void init() {
		setLayout(new BorderLayout());
		GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();

		Canvas3D c = new Canvas3D(config);
		add("Center", c);

		BranchGroup scene = createSceneGraph();
		u = new SimpleUniverse(c);

		u.getViewingPlatform().setNominalViewingTransform();

		u.addBranchGraph(scene);
	}

	public void destroy() {
		u.cleanup();
	}

	public static void main(String[] args) {
		new MainFrame(new RotateCube(), 512, 512);

	}
}