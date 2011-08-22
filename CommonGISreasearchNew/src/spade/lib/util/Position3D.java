package spade.lib.util;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 22, 2009
 * Time: 11:03:01 AM
 * Represents a position in (x, y, z) coordinate space.
 * The coordinates are integers. Initially all 3 coordinates are 0.
 */
public class Position3D {
	public int x = 0, y = 0, z = 0;

	public Position3D() {
	}

	public Position3D(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public Position3D(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ", " + z + ")";
	}
}
