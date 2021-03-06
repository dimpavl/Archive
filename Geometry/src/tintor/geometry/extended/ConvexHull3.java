/*
Copyright (C) 2007 Marko Tintor <tintor@gmail.com>

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
*/
package tintor.geometry.extended;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tintor.geometry.doubleN.Plane3d;
import tintor.geometry.doubleN.Ray3d;
import tintor.geometry.doubleN.Vector3d;
import tintor.geometry.extended.Polyhedrons.CenterOfMass;

public final class ConvexHull3 {
	// Constants
	private static final double ComplanarTolerance = 1e-8;

	// Fields (not private because of synthetic accessor warning)
	Vertex ivertices;
	Face ifaces;

	// Classes
	static class Edge {
		final Vertex vertex;
		Face face;
		Edge next; // next edge on this face

		Edge(final Vertex v, final Face f, final Edge n) {
			vertex = v;
			face = f;
			next = n;
		}
	}

	private static class Vertex {
		private final Vector3d point;
		Edge edges;
		Vertex prev, next; // linked list of all vertices

		Vertex(final ConvexHull3 hull, final Vector3d point) {
			this.point = point;
			// add to linked list
			next = hull.ivertices;
			if (next != null) next.prev = this;
			hull.ivertices = this;
		}

		Face getFace(final Vertex vertex) {
			for (Edge e = edges; e != null; e = e.next)
				if (e.vertex == vertex) return e.face;
			return null;
		}

		Vertex getVertex(final Face face) {
			for (Edge e = edges; e != null; e = e.next)
				if (e.face == face) return e.vertex;
			return null;
		}

		Edge get(final Face face) {
			for (Edge e = edges; e != null; e = e.next)
				if (e.face == face) return e;
			return null;
		}

		// removes half edge
		void remove(final ConvexHull3 hull, final Vertex v) {
			if (edges.vertex == v) {
				edges = edges.next;
				if (edges == null) {
					// remove from linked list
					if (next != null) next.prev = prev;
					if (prev != null) prev.next = next;
					if (hull.ivertices == this) hull.ivertices = next;
				}
				return;
			}
			for (Edge e = edges; e.next != null; e = e.next)
				if (e.next.vertex == v) {
					e.next = e.next.next;
					break;
				}
		}
	}

	private static class Face {
		Vertex first;
		Face next, prev; // linked list of all faces

		Plane3d plane;
		Vector3d color = new Vector3d(0, 0, 1);

		Face(final ConvexHull3 hull, final Vertex... list) {
			plane = new Plane3d(list[0].point, list[1].point, list[2].point);
			// attach faces to edges
			q: for (int j = list.length - 1, i = 0; i < list.length; j = i++) {
				final Vertex a = list[j], b = list[i];
				for (Edge e = a.edges; e != null; e = e.next)
					if (e.vertex == b) {
						e.face = this;
						continue q;
					}
				a.edges = new Edge(b, this, a.edges);
			}
			first = list[0];
			// add to linked list
			next = hull.ifaces;
			prev = null;
			if (next != null) next.prev = this;
			hull.ifaces = this;

			// merge complanar faces
			for (int j = list.length - 1, i = 0; i < list.length; j = i++) {
				final Vertex a = list[j], b = list[i];
				final Face f = b.getFace(a);
				if (f == null || f.plane.normal.dot(plane.normal) < 1 - ComplanarTolerance) continue;

				Vertex z = b;
				while (true) {
					final Edge e = z.get(f);
					e.face = this;
					z = e.vertex;
					if (z == b) break;
				}

				a.remove(hull, b);
				b.remove(hull, a);
				// remove face from linked list
				if (f.next != null) f.next.prev = f.prev;
				if (f.prev != null) f.prev.next = f.next;
				if (hull.ifaces == f) hull.ifaces = f.next;
			}
		}

		// ! also removes free edges and free vertices left after face removal
		void remove(final ConvexHull3 hull) {
			Vertex a = first;
			do {
				Vertex b = null;
				assert a.edges != null;
				for (Edge e = a.edges; e != null; e = e.next)
					if (e.face == this) {
						e.face = null;
						b = e.vertex;
						break;
					}

				assert b != null;
				if (b.getFace(a) == null) {
					a.remove(hull, b);
					b.remove(hull, a);
				}
				a = b;
			} while (a != first);
			first = null;
			// remove from linked list
			if (next != null) next.prev = prev;
			if (prev != null) prev.next = next;
			if (hull.ifaces == this) hull.ifaces = next;
		}
	}

	public ConvexHull3(final Vector3d... w) {
		this(Arrays.asList(w));
	}

	public ConvexHull3(final List<Vector3d> w) {
		if (w.size() < 4) throw new RuntimeException("Too little vertices!");
		try {
			initialTetrahedron(w);
			for (int i = 1; i < w.size(); i++)
				addVertex(w.get(i));
		} catch (final NullPointerException e) {
			for (final Vector3d v : w)
				System.out.println(v);
			throw e;
		}
	}

	private void initialTetrahedron(final List<Vector3d> w) {
		final Vertex a = new Vertex(this, w.get(0));
		final Vertex b = new Vertex(this, maxPointDistance(w));
		final Vertex c = new Vertex(this, maxRayDistance(b.point, w));

		// maxPlaneDistance from plane(a, b, c)
		final Plane3d plane = new Plane3d(w.get(0), b.point, c.point);
		int max = 1;
		double maxDist = plane.distance(w.get(1));
		for (int i = 2; i < w.size(); i++) {
			final double dist = plane.distance(w.get(i));
			if (Math.abs(dist) > Math.abs(maxDist)) {
				maxDist = dist;
				max = i;
			}
		}
		final Vertex d = new Vertex(this, w.get(max));

		if (maxDist < 0) {
			new Face(this, a, b, c);
			new Face(this, b, a, d);
			new Face(this, c, b, d);
			new Face(this, a, c, d);
		} else {
			new Face(this, c, b, a);
			new Face(this, a, b, d);
			new Face(this, b, c, d);
			new Face(this, c, a, d);
		}
	}

	public Polygon3[] faces() {
		int faceCount = 0;
		for (Face face = ifaces; face != null; face = face.next)
			faceCount++;

		final Polygon3[] faces = new Polygon3[faceCount];
		int f = 0;
		for (Face face = ifaces; face != null; face = face.next) {
			final List<Vector3d> list = new ArrayList<Vector3d>();
			Vertex vertex = face.first;
			do {
				list.add(vertex.point);
				vertex = vertex.getVertex(face);
			} while (vertex != face.first);

			faces[f++] = new Polygon3(face.plane, null, list.toArray(new Vector3d[list.size()]));
		}
		assert f == faceCount;

		return faces;
	}

	// TODO faster!
	public Vector3d centerOfMass() {
		final CenterOfMass c = new CenterOfMass();
		final List<Vector3d> list = new ArrayList<Vector3d>();
		for (Face face = ifaces; face != null; face = face.next) {
			list.clear();
			Vertex vertex = face.first;
			do {
				list.add(vertex.point);
				vertex = vertex.getVertex(face);
			} while (vertex != face.first);
			c.add(list.toArray(new Vector3d[list.size()]));
		}
		return c.get();
	}

	public Vector3d[] vertices() {
		final List<Vector3d> list = new ArrayList<Vector3d>();
		for (Vertex v = ivertices; v != null; v = v.next)
			list.add(v.point);
		return list.toArray(new Vector3d[list.size()]);
	}

	public void addVertex(final Vector3d w) {
		if (inside(w)) return;
		final Vertex c = new Vertex(this, w);
		for (Face face = ifaces; face != null; face = face.next)
			if (face.plane.distance(w) > -ComplanarTolerance) face.remove(this);
		for (Vertex a = ivertices; a != null; a = a.next)
			if (a != c) for (Edge e = a.edges; e != null; e = e.next)
				if (e.vertex != c && e.face == null) new Face(this, a, e.vertex, c);
	}

	private boolean inside(final Vector3d w) {
		for (Face face = ifaces; face != null; face = face.next)
			if (face.plane.distance(w) > ComplanarTolerance) return false;
		return true;
	}

	// from w[0]
	private static Vector3d maxPointDistance(final List<Vector3d> w) {
		int max = 1;
		double maxDist = w.get(1).distanceSquared(w.get(0));
		for (int i = 2; i < w.size(); i++) {
			final double dist = w.get(i).distanceSquared(w.get(0));
			if (dist > maxDist) {
				maxDist = dist;
				max = i;
			}
		}
		return w.get(max);
	}

	// from ray(w[0], b-w[0])
	private static Vector3d maxRayDistance(final Vector3d b, final List<Vector3d> w) {
		final Ray3d ray = new Ray3d(w.get(0), b.sub(w.get(0)));
		int max = 1;
		final Double x;
		double maxDist = ray.distanceSquared(w.get(1));
		for (int i = 2; i < w.size(); i++) {
			final double dist = ray.distanceSquared(w.get(i));
			if (dist > maxDist) {
				maxDist = dist;
				max = i;
			}
		}
		return w.get(max);
	}
}