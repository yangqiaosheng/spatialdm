package spade.analysis.space_time_cube;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Node;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Switch;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import spade.analysis.system.ESDACore;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.time.TimeReference;
import spade.time.manage.TemporalDataManager;
import spade.time.ui.TimeUI;
import spade.vis.dmap.DAggregateLayer;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.MapCanvas;
import spade.vis.dmap.MapMetrics;
import spade.vis.map.Mappable;
import spade.vis.space.LayerManager;

import com.sun.j3d.utils.universe.SimpleUniverse;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 25, 2008
 * Time: 2:16:48 PM
 */
public class SpaceTimeCubeView extends Panel implements Destroyable, PropertyChangeListener, ItemListener {
	protected ESDACore core = null;

	protected Canvas3D canvas = null;
	protected BranchGroup scene = null;
	/**
	 * Includes a plane with a flat map and a 3d representation of
	 * spatio-temporal objects
	 */
	protected TransformGroup stCube = null;
	/**
	 * Includes only the 3D representzations of the spatio-temporal objects but not the flat map
	 */
	protected TransformGroup objects3D = null;
	/**
	 * Used for scaling in the z-dimension, which represents time
	 */
	protected TimeFocuser timeFocuser = null;
	/**
	 * Moves a semi-transparent map in z-dimension to show the
	 * position of a selected time moment within the view
	 */
	protected MapPlaneMover mpMover = null;
	/**
	 * Marks a selected spatial position in the cube by a vertical line.
	 * Moves the line when the position changes.
	 */
	protected PositionMarker posMarker = null;
	/**
	 * Translates mouse clicks in the cube into object selections
	 */
	protected ObjectSelector objSelector = null;
	/**
	 * All objects (instances of SpaceTimeObject) among which the selection is done
	 */
	protected Vector<SpaceTimeObject> stObjects = null;
	/**
	 * SimpleUniverse is a Convenience Utility class
	 */
	protected SimpleUniverse simpleU = null;
	/**
	 * All objects that must be destroyed when the view is closed
	 */
	protected Vector destroyables = new Vector(10, 10);
	/**
	 * To switch on and off the visibility of the upper plane
	 */
	protected Checkbox upPlaneCB = null;
	/**
	 * The switcher of drawing of the upper plane
	 */
	protected Switch upPlaneSwitch = null;

	public SpaceTimeCubeView(ESDACore core) {
		this.core = core;
	}

	public void construct() {
		setLayout(new BorderLayout());

		GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
		canvas = new Canvas3D(config);
		add(canvas, BorderLayout.CENTER);
		createSTCube();
		TransformGroup view = new TransformGroup();
		view.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		view.addChild(stCube);
		ViewController vcont = new ViewController(view);
		vcont.setSchedulingBounds(new BoundingSphere());
		scene = createSceneGraph(view);
		scene.addChild(vcont);
		if (stObjects != null) {
			objSelector = new ObjectSelector(canvas, scene, core.getSupervisor());
			objSelector.setSchedulingBounds(new BoundingSphere());
			objSelector.setObjectsToSelect(stObjects);
			scene.addChild(objSelector);
		}
		// Let Java 3D perform optimizations on this scene graph.
		scene.compile();
		simpleU = new SimpleUniverse(canvas);
		// This will move the ViewPlatform back a bit so the
		// objects in the scene can be viewed.
		simpleU.getViewingPlatform().setNominalViewingTransform();
		simpleU.addBranchGraph(scene);

		Panel bottomP = new Panel(new ColumnLayout());
		if (mpMover != null) {
			Component mpControls = mpMover.getInterface();
			if (mpControls != null) {
				bottomP.add(mpControls);
			}
		}
		upPlaneCB = new Checkbox("Show the map on top of the cube", true);
		upPlaneCB.addItemListener(this);
		bottomP.add(upPlaneCB);
		add(bottomP, BorderLayout.SOUTH);
	}

	public BranchGroup createSceneGraph(TransformGroup world) {
		// Create the root of the branch graph
		BranchGroup objRoot = new BranchGroup();
		Background backg = new Background(0.8f, 0.8f, 0.8f);
		backg.setApplicationBounds(new BoundingSphere());
		objRoot.addChild(backg);
		objRoot.addChild(world);
		return objRoot;
	}

	protected void createSTCube() {
		Transform3D rotate = new Transform3D();
		//rotate around the x-axis
		rotate.rotX(-Math.PI / 2.0);
		Transform3D translate = new Transform3D();
		Vector3f vector = new Vector3f(0.0f, 0.0f, -0.5f);
		translate.setTranslation(vector);
		rotate.mul(translate);
		stCube = new TransformGroup(rotate);
		MapCanvas map = null;
		LayerManager lman = null;
		CubeMetrics cubeMetr = null;
		FlatMap flatMap = null;
		if (core != null && core.getUI() != null && core.getUI().getCurrentMapViewer() != null) {
			map = (MapCanvas) core.getUI().getCurrentMapViewer().getMapDrawer();
			lman = core.getUI().getCurrentMapViewer().getLayerManager();
		}
		if (map != null && lman != null) {
			MapMetrics mmetr = (MapMetrics) map.getMapContext();
			cubeMetr = new CubeMetrics();
			mmetr.copyTo(cubeMetr);
			cubeMetr.setup();
			flatMap = new FlatMap((Mappable) lman, cubeMetr, map.getBackground());
		} else {
			flatMap = new FlatMap(null, null, null);
		}
		stCube.addChild(flatMap);
		if (map == null || flatMap.getImageSize() == null)
			return;
		Shape3D mapCopy = flatMap.getMapPlaneCopy(0);
		TransformGroup upMap = new TransformGroup();
		upMap.addChild(mapCopy);
		translate = new Transform3D();
		translate.setTranslation(new Vector3d(0, 0, 1));
		upMap.setTransform(translate);
		upPlaneSwitch = new Switch(Switch.CHILD_ALL);
		upPlaneSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		upPlaneSwitch.addChild(upMap);
		stCube.addChild(upPlaneSwitch);
		/**/
		TemporalDataManager timeMan = core.getDataKeeper().getTimeManager();
		if (timeMan == null || timeMan.getContainerCount() < 1)
			return;
		TimeReference minMaxTimes = timeMan.getMinMaxTimes();
		if (minMaxTimes == null)
			return;
		cubeMetr.setMinTime(minMaxTimes.getValidFrom());
		cubeMetr.setMaxTime(minMaxTimes.getValidUntil());
		cubeMetr.setup();

		posMarker = new PositionMarker();
		posMarker.initialize(cubeMetr, stCube);
		core.getSupervisor().addPositionSelectListener(posMarker);

		Shape3D mapPlane = flatMap.getMapPlaneCopy(0.0f);
		TransformGroup mapPlaneTG = new TransformGroup();
		mapPlaneTG.addChild(mapPlane);
		mpMover = new MapPlaneMover(mapPlaneTG);
		mpMover.setTemporalDataManager(timeMan);
		mpMover.setTimeUI((TimeUI) core.getUI().getTimeUI());
		stCube.addChild(mapPlaneTG);
		objects3D = new TransformGroup();
		objects3D.setCapability(Node.ENABLE_PICK_REPORTING);
		stCube.addChild(objects3D);
		for (int i = 0; i < timeMan.getContainerCount(); i++)
			if (timeMan.getContainer(i) instanceof DGeoLayer) {
				DGeoLayer layer = (DGeoLayer) timeMan.getContainer(i);
				if (layer.getObjectCount() < 1) {
					continue;
				}
				Vector<SpaceTimeObject> obj3D = null;
				if (layer instanceof DAggregateLayer) {
					obj3D = Interactions3DRepresenter.get3DInteractions((DAggregateLayer) layer, cubeMetr);
				} else if (layer.getObjectAt(0) instanceof DMovingObject) {
					obj3D = Tracks3DRepresenter.get3DTracks(layer, cubeMetr);
				} else {
					obj3D = GeoObj3DRepresenter.get3DObjects(layer, cubeMetr);
				}
				if (obj3D != null) {
					addObjectsToSelect(obj3D);
					Switch drawSwitch = new Switch(Switch.CHILD_ALL);
					drawSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
					FilterApplicator filt = new FilterApplicator(drawSwitch);
					filt.setObjectContainer(layer);
					filt.setDrawnObjects(obj3D);
					destroyables.addElement(filt);
					filt.doFiltering();
					for (int j = 0; j < obj3D.size(); j++) {
						drawSwitch.addChild(obj3D.elementAt(j).getObj3d());
					}
					objects3D.addChild(drawSwitch);
					SelectionApplicator selApp = new SelectionApplicator();
					selApp.setObjectContainer(layer);
					selApp.setDrawnObjects(obj3D);
					selApp.setSupervisor(core.getSupervisor());
					selApp.applySelection();
					destroyables.addElement(selApp);
					AppearanceController apc = new AppearanceController();
					apc.setLayer(layer);
					apc.setDrawnObjects(obj3D);
					apc.setSupervisor(core.getSupervisor());
					destroyables.addElement(apc);
					layer.addPropertyChangeListener(this);
				}
				timeFocuser = new TimeFocuser(objects3D);
				timeFocuser.setTimeUI((TimeUI) core.getUI().getTimeUI());
				stCube.addChild(timeFocuser.getTransformer());
				destroyables.addElement(timeFocuser);
			}
	}

	/**
	 * Adds the given group of objects to the list of objects among which the selection is done
	 */
	public void addObjectsToSelect(Vector<SpaceTimeObject> objGroup) {
		if (objGroup == null || objGroup.size() < 1)
			return;
		if (stObjects == null) {
			stObjects = new Vector<SpaceTimeObject>(objGroup.size(), 100);
		} else {
			stObjects.ensureCapacity(stObjects.size() + objGroup.size());
		}
		stObjects.addAll(objGroup);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(800, 850);
	}

	protected boolean destroyed = false;

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public void destroy() {
		if (destroyed)
			return;
		System.out.println("SpaceTimeCubeView is destroyed");
		if (destroyables != null) {
			for (int i = 0; i < destroyables.size(); i++) {
				((Destroyable) destroyables.elementAt(i)).destroy();
			}
			destroyables.removeAllElements();
		}
		if (posMarker != null) {
			core.getSupervisor().removePositionSelectListener(posMarker);
		}
		destroyed = true;
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getPropertyName().equals("time_references")) {
			destroy();
		}
	}

	/**
	 * Switches on and off the visibility of the upper map plane
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(upPlaneCB)) {
			if (upPlaneCB.getState()) {
				upPlaneSwitch.setWhichChild(Switch.CHILD_ALL);
			} else {
				upPlaneSwitch.setWhichChild(Switch.CHILD_NONE);
			}
		}
	}
}
