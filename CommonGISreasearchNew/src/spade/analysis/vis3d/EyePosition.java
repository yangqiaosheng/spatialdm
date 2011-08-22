package spade.analysis.vis3d;

/**
* This class is needed to store (x,y,z) coordinates of viewpoint
* (viewer's eye) in real world scale
*/
public class EyePosition {

	private float X = Float.NaN;
	private float Y = Float.NaN;
	private float Z = Float.NaN;

	private int scrX = -1;
	private int scrY = -1;
	private int scrZ = -1;

	public EyePosition() {
		this(0.0f, 0.0f, 0.0f);
	}

	public EyePosition(float x, float y, float z) {
		X = x;
		Y = y;
		Z = z;
	}

	public float getX() {
		return X;
	}

	public float getY() {
		return Y;
	}

	public float getZ() {
		return Z;
	}

	public int getScreenX() {
		return scrX;
	}

	public int getScreenY() {
		return scrY;
	}

	public int getScreenZ() {
		return scrZ;
	}

	public EyePosition getPosition() {
		return new EyePosition(X, Y, Z);
	}

	public void setX(float px) {
		X = px;
	}

	public void setY(float py) {
		Y = py;
	}

	public void setZ(float pz) {
		Z = pz;
	}

	public void setScreenX(int sx) {
		scrX = sx;
	}

	public void setScreenY(int sy) {
		scrY = sy;
	}

	public void setScreenZ(int sz) {
		scrZ = sz;
	}

	public void setPosition(float px, float py, float pz) {
		X = px;
		Y = py;
		Z = pz;
	}
}
