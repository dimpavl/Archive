package tintor.geometry.extended;

import tintor.geometry.Line3;
import tintor.geometry.Ray3;
import tintor.geometry.Vector3;

/** @see vcg.isti.cnr.it/~ponchio/computergraphics/exercises/plucker.pdf */
public class Plucker {
	public final Vector3 u, v;

	public Plucker(final Vector3 u, final Vector3 v) {
		this.u = u;
		this.v = v;
	}

	public static Plucker line(final Vector3 a, final Vector3 b) {
		return new Plucker(b.sub(a), b.cross(a));
	}

	public static Plucker ray(final Vector3 origin, final Vector3 dir) {
		return new Plucker(dir, dir.cross(origin));
	}

	public Plucker(final Line3 p) {
		u = p.b.sub(p.a);
		v = p.b.cross(p.a);
	}

	public Plucker(final Ray3 p) {
		u = p.dir;
		v = p.dir.cross(p.origin);
	}

	/** <0 Clockwise (if you look in direction of one line, other will go CW around it) 
	 *  =0 Intersect or Parallel
	 *  >0 Counterclockwise*/
	public float side(final Plucker p) {
		return u.dot(p.v) + v.dot(p.u);
	}

	public float side(final Vector3 a, final Vector3 b) {
		return u.mixed(a, b) + v.dot(a, b);
	}

	public float side(final Line3 p) {
		return u.mixed(p.a, p.b) + v.dot(p.a, p.b);
	}

	public float side(final Ray3 p) {
		return u.mixed(p.dir, p.origin) + v.dot(p.dir);
	}

	//	public boolean inside(final Vector3... convex) {
	//		boolean p = false, n = false;
	//		for (int j = convex.length - 1, i = 0; i < convex.length; j = i++)
	//			switch (Side.classify(side(convex[j], convex[i]))) {
	//			case Positive:
	//				if (n) return false;
	//				p = true;
	//				break;
	//			case Negative:
	//				if (p) return false;
	//				n = true;
	//				break;
	//			case Zero:
	//				return false;
	//			}
	//		return true;
	//	}
}