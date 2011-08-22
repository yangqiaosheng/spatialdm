package spade.analysis.geocomp.voronoi;

import external.paul_chew.DelaunayTriangulation;
import external.paul_chew.Simplex;
import external.paul_chew.Pnt;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.Computing;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Feb 3, 2009
 * Time: 3:18:09 PM
 * Generates Voronoi (Thiessen) polygons from a set of points.
 * Uses the Delaunay triangulation code by L. Paul Chew
 */
public class Voronoi {
  protected Vector<RealPoint> points=null; //the original points
  protected Vector<Pnt> dtPoints=null; //the points as instances of Pnt used for the triangulation
  protected DelaunayTriangulation dt;     // The Delaunay triangulation
  protected Simplex initialTriangle;      // The large initial triangle

  protected double argminx = Double.NaN, argminy = Double.NaN;
  protected double argmaxx = Double.NaN, argmaxy = Double.NaN;
  protected double dx=0, dy=0;
  protected int dIdx=0;
  /**
   * The resulting Voronoi cells
   */
  protected RealPolyline resultingCells[]=null;
  /**
   * Whether to derive a neighbourhood matrix for the points
   */
  protected boolean buildNeighbourhoodMatrix = false;
  /**
   * The neighbourhood matrix for the points derived from the triangulation
   */
  protected HashMap<Integer, Integer> neighbourMap = null;

  public Voronoi (Vector<RealPoint> points) {
    if (points==null || points.size()<2) return;
    this.points=points;
    for (int i=0; i<points.size(); i++)
      if (points.elementAt(i)!=null) {
        RealPoint p=points.elementAt(i);
        if (Double.isNaN(argmaxx)) {
          argmaxx=argminx=p.x;  argmaxy=argminy=p.y;
        }
        else {
          if (argmaxx<p.x) argmaxx=p.x;
          if (argminx>p.x) argminx=p.x;
          if (argmaxy<p.y) argmaxy=p.y;
          if (argminy>p.y) argminy=p.y;
        }
      }
    this.dx=argmaxx-argminx;
    this.dy=argmaxy-argminy;
    //-- the initial simplex must contain all points
    //-- take the bounding box, move the diagonals (sidewards)
    //	 the meeting point will be the mirrored bbox-center on the top edge
    Pnt llp=new Pnt(argminx-(1.5*dx), argminy-dy); //lower left
    Pnt lrp=new Pnt(argmaxx+(1.5*dx), argminy-dy); //lower right
    //new Pnt(argminx+(dx/2.0), argmaxy+(dy/2.0))});	//center, top
    Pnt tcp=new Pnt(argminx+(dx/2.0), argmaxy+1.5*dy); //center, top
    //generate appropriate indexes (numeric identifiers) for these points
    dIdx=points.size();
    for (int num=100; dIdx%10>0; num*=10)
      if (dIdx<num) dIdx=num;
    llp.numId=dIdx+1; lrp.numId=dIdx+2; tcp.numId=dIdx+3;

    this.initialTriangle = new Simplex(new Pnt[] {llp,lrp,tcp});

    this.dt = new DelaunayTriangulation(initialTriangle);
    this.addPoints(points);
  }

  public void addPoints(Vector<RealPoint> points) {
    if (points==null || points.size()<2 || dt==null) return;
    dtPoints=new Vector<Pnt>(points.size());
    long t0=System.currentTimeMillis();
    for (int i=0; i<points.size(); i++)
      if (points.elementAt(i)!=null) {
        RealPoint p=points.elementAt(i);
        Pnt point = new Pnt(p.x, p.y);
        point.numId=i;
        dtPoints.addElement(point);
        dt.delaunayPlace(point);
      }
    long t1=System.currentTimeMillis();
    System.out.println("Delaunay triangulation took "+(t1-t0)+" msec for "+points.size()+" points.");
  }

  public boolean isValid() {
    return dt!=null;
  }

  public void setBuildNeighbourhoodMatrix(boolean mustBuild) {
    this.buildNeighbourhoodMatrix = mustBuild;
  }

  public ProtoPolygon[] getProtoPolygons () {
    if (dt==null) return null;
    long t0=System.currentTimeMillis();
    Pnt dummy[]=new Pnt[0]; //needed for the method toArray(...)
    int startIdx=2*dIdx;
    ProtoPolygon proto[]=new ProtoPolygon[points.size()+3];
    for (int i=0; i<proto.length; i++) proto[i]=null;
    if (buildNeighbourhoodMatrix) {
//      neiMatrix=new boolean[points.size()][points.size()];
//      for (int i=0; i<points.size(); i++)
//        for (int j=0; j<points.size(); j++)
//          neiMatrix[i][j]=i==j;
    }
    // Loop through all the edges of the DT (each is done twice)
    for (Iterator it = dt.iterator(); it.hasNext();) {
      Simplex triangle = (Simplex) it.next();
      Pnt vrt0[] =(Pnt[]) triangle.toArray(dummy);
      if (triangle.centre==null) {
        triangle.centre = Pnt.circumcenter(vrt0);
        triangle.centre.numId=startIdx++;
      }
      for (Iterator otherIt = dt.neighbors(triangle).iterator(); otherIt.hasNext();) {
        Simplex other = (Simplex) otherIt.next();
        Pnt vrt1[]=(Pnt[]) other.toArray(dummy);
        if (other.centre==null) {
          other.centre = Pnt.circumcenter(vrt1);
          other.centre.numId=startIdx++;
        }
        //find the common edge (two points) of the two triangles
        Pnt p1=null, p2=null;
        for (int i=0; i<vrt0.length && (p1==null || p2==null); i++)
          for (int j=0; j<vrt1.length && (p1==null || p2==null); j++)
            if (vrt0[i].same(vrt1[j]))
              if (p1==null) p1=vrt0[i];
              else
                if (!p1.same(vrt0[i])) p2=vrt0[i];
        if (p1==null || p2==null) //some error?
          continue;
        Edge edge=null;
        if (p1.numId>=0) {
          int idx=p1.numId;
          if (idx>dIdx) idx=points.size()+idx-dIdx-1;
          if (proto[idx]==null) {
            proto[idx]=new ProtoPolygon();
            proto[idx].centre=p1;
          }
          edge=proto[idx].addEdge(triangle.centre,other.centre);
        }
        if (p2.numId>=0) {
          int idx=p2.numId;
          if (idx>dIdx) idx=points.size()+idx-dIdx-1;
          if (proto[idx]==null) {
            proto[idx]=new ProtoPolygon();
            proto[idx].centre=p2;
          }
          if (edge!=null)
            proto[idx].addEdge(edge);
          else
            edge=proto[idx].addEdge(triangle.centre,other.centre);
        }

      }
    }
    int nPolygons=0;
    for (int i=0; i<points.size(); i++)
      if (proto[i]!=null)
        ++nPolygons;
    long t1=System.currentTimeMillis();
    System.out.println("Getting and grouping edges took "+(t1-t0)+" msec; "+
      nPolygons+" prototype polygons built.");
    if (nPolygons<1) return null;
    return proto;
  }

  /**
   * the size of the bounding box has been empirically defined to get "undistorted"
   * outer thiessen polygons
   */
  public RealPolyline[] getPolygons () {
    if (dt==null) return null;
    double minX=argminx-dx/10;
    double maxX=argmaxx+dx/10;
    double minY=argminy-dy/10;
    double maxY=argmaxy+dy/10;
    return getPolygons(minX,minY,maxX,maxY);
  }

  public RealPolyline[] getPolygons (double minX, double minY, double maxX, double maxY) {
    if (dt==null) return null;
    ProtoPolygon proto[]=getProtoPolygons();
    if (proto==null) return null;
    long t0=System.currentTimeMillis();
    resultingCells=new RealPolyline[points.size()];
    int nInter=0, nPolygons=0;
    for (int i=0; i<resultingCells.length; i++) {
      resultingCells[i]=null;
      ProtoPolygon pp=proto[i];
      if (pp==null) continue;
      Pnt polyPoints[]=pp.getPolygon();
      if (polyPoints ==null || polyPoints.length<2) continue;
      if (hasPointOutside(polyPoints,minX,minY,maxX,maxY)) {
        RealPolyline pline=cutPolygon(getRealPolyline(polyPoints),(float)minX,(float)minY,(float)maxX,(float)maxY);
        resultingCells[i]=pline;
        ++nInter;
      }
      else
        resultingCells[i]=getRealPolyline(polyPoints);
      if (resultingCells[i]!=null) {
        ++nPolygons;
        if (pp.centre!=null)
          resultingCells[i].setCentroid((float)pp.centre.coord(0),(float)pp.centre.coord(1));
        resultingCells[i].isClosed=true;
      }
    }
    long t1=System.currentTimeMillis();
    System.out.println("Building polygons (including "+nInter+
      " intersection operations) took "+(t1-t0)+" msec; "+
      nPolygons+" polygons built for "+points.size()+" points.");
    if (nPolygons<1) return null;
    return resultingCells;
  }
  /**
   * Returns the resulting Voronoi cells, if successfully computed.
   */
  public RealPolyline[] getResultingCells() {
    return resultingCells;
  }

  /**
   * Checks if any of the points in the given array are outside
   * of the given boundary rectangle
   */
  public boolean hasPointOutside(Pnt[] pts, double minX, double minY, double maxX, double maxY) {
    if (pts==null) return false;
    for (int i=0; i<pts.length; i++)
      if (pts[i].coord(0)<minX || pts[i].coord(0)>maxX ||
          pts[i].coord(1)<minY || pts[i].coord(1)>maxY)
        return true;
    return false;
  }

  public RealPolyline cutPolygon (RealPolyline poly, float minX, float minY, float maxX, float maxY) {
    if (poly==null || poly.p==null || poly.p.length<3) return poly;
    double pMinX=poly.p[0].x, pMinY=poly.p[0].y, pMaxX=pMinX, pMaxY=pMinY;
    for (int i=1; i<poly.p.length; i++) {
      if (poly.p[i].x<pMinX) pMinX=poly.p[i].x;
      else
      if (poly.p[i].x>pMaxX) pMaxX=poly.p[i].x;
      if (poly.p[i].y<pMinY) pMinY=poly.p[i].y;
      else
      if (poly.p[i].y>pMaxY) pMaxY=poly.p[i].y;
    }
    if (pMinX>=minX && pMaxX<=maxX && pMinY>=minY && pMaxY<=maxY)
      return poly;
    if (pMinX>=maxX || pMaxX<=minX || pMinY>=maxY || pMaxY<=minY)
      return null;
//    if ((pMinX<minX || pMaxX>maxX) && pMaxY>maxY)
//      System.out.println("Cut X and Y!");
    if (pMinX<minX) {
      //intersect with the left foundary
      RealPoint ll=new RealPoint(minX,(float)Math.min(minY,pMinY));
      RealPoint ul=new RealPoint(minX,(float)Math.max(maxY,pMaxY));
      RealPoint inter[]=Computing.findIntersections(ll,ul,poly);
      if (inter!=null && inter.length==2) {
        int iRight=-1, nRight=0;
        for (int i=0; i<poly.p.length; i++)
          if (poly.p[i].x>=minX) {
            ++nRight;
            if (iRight<0) iRight=i;
          }
        boolean fromEnd=false;
        if (iRight==0) {
          for (int i=poly.p.length-1; i>1 && poly.p[i].x>=minX; i--)
            iRight=i;
          fromEnd=iRight>0;
          if (fromEnd && poly.p[poly.p.length-1].equals(poly.p[0]))
            --nRight;
        }
        RealPoint p[]=new RealPoint[nRight+2+1];
        int k=0;
        for (int i=iRight; i<poly.p.length && poly.p[i].x>=minX; i++)
          p[k++]=poly.p[i];
        if (fromEnd) {
          int i1=0;
          if (poly.p[poly.p.length-1].equals(poly.p[0])) i1=1;
          for (int i=i1; i<iRight && poly.p[i].x>=minX; i++)
            p[k++]=poly.p[i];
        }
        float d1=inter[1].y-inter[0].y, d2=p[k-1].y-p[0].y;
        if ((d2>0 && d1<0) || (d2<0 && d1>0)) {
          p[k++]=inter[0]; p[k++]=inter[1];
        }
        else {
          p[k++]=inter[1]; p[k++]=inter[0];
        }
        if (k<p.length)
          p[k]=p[0];
        poly.p=p;
        for (int i=0; i<2; i++)
          if (inter[i].y<pMinY) pMinY=inter[i].y;
          else
          if (inter[i].y>pMaxY) pMaxY=inter[i].y;
      }
    }
    if (pMaxX>maxX) {
      //intersect with the right foundary
      RealPoint lr=new RealPoint(maxX,(float)Math.min(minY,pMinY));
      RealPoint ur=new RealPoint(maxX,(float)Math.max(maxY,pMaxY));
      RealPoint inter[]=Computing.findIntersections(lr,ur,poly);
      if (inter!=null && inter.length==2) {
        int iRight=-1, nRight=0;
        for (int i=0; i<poly.p.length; i++)
          if (poly.p[i].x<=maxX) {
            ++nRight;
            if (iRight<0) iRight=i;
          }
        boolean fromEnd=false;
        if (iRight==0) {
          for (int i=poly.p.length-1; i>1 && poly.p[i].x<=maxX; i--)
            iRight=i;
          fromEnd=iRight>0;
          if (fromEnd && poly.p[poly.p.length-1].equals(poly.p[0]))
            --nRight;
        }
        RealPoint p[]=new RealPoint[nRight+2+1];
        int k=0;
        for (int i=iRight; i<poly.p.length && poly.p[i].x<=maxX; i++)
          p[k++]=poly.p[i];
        if (fromEnd) {
          int i1=0;
          if (poly.p[poly.p.length-1].equals(poly.p[0])) i1=1;
          for (int i=i1; i<iRight && poly.p[i].x<=maxX; i++)
            p[k++]=poly.p[i];
        }
        float d1=inter[1].y-inter[0].y, d2=p[k-1].y-p[0].y;
        if ((d2>0 && d1<0) || (d2<0 && d1>0)) {
          p[k++]=inter[0]; p[k++]=inter[1];
        }
        else {
          p[k++]=inter[1]; p[k++]=inter[0];
        }
        if (k<p.length)
          p[k]=p[0];
        poly.p=p;
        for (int i=0; i<2; i++)
          if (inter[i].y<pMinY) pMinY=inter[i].y;
          else
          if (inter[i].y>pMaxY) pMaxY=inter[i].y;
      }
    }
    if (pMinY<minY) {
      //intersect with the bottom foundary
      RealPoint ll=new RealPoint((float)Math.min(minX,pMinX),minY);
      RealPoint lr=new RealPoint((float)Math.max(maxX,pMaxX),minY);
      RealPoint inter[]=Computing.findIntersections(ll,lr,poly);
      if (inter!=null && inter.length==2) {
        int iRight=-1, nRight=0;
        for (int i=0; i<poly.p.length; i++)
          if (poly.p[i].y>=minY) {
            ++nRight;
            if (iRight<0) iRight=i;
          }
        boolean fromEnd=false;
        if (iRight==0) {
          for (int i=poly.p.length-1; i>1 && poly.p[i].y>=minY; i--)
            iRight=i;
          fromEnd=iRight>0;
          if (fromEnd && poly.p[poly.p.length-1].equals(poly.p[0]))
            --nRight;
        }
        RealPoint p[]=new RealPoint[nRight+2+1];
        int k=0;
        for (int i=iRight; i<poly.p.length && poly.p[i].y>=minY; i++)
          p[k++]=poly.p[i];
        if (fromEnd) {
          int i1=0;
          if (poly.p[poly.p.length-1].equals(poly.p[0])) i1=1;
          for (int i=i1; i<iRight && poly.p[i].y>=minY; i++)
            p[k++]=poly.p[i];
        }
        float d1=inter[1].x-inter[0].x, d2=p[k-1].x-p[0].x;
        if ((d2>0 && d1<0) || (d2<0 && d1>0)) {
          p[k++]=inter[0]; p[k++]=inter[1];
        }
        else {
          p[k++]=inter[1]; p[k++]=inter[0];
        }
        if (k<p.length)
          p[k]=p[0];
        poly.p=p;
      }
    }
    if (pMaxY>maxY) {
      //intersect with the top foundary
      RealPoint ul=new RealPoint((float)Math.min(minX,pMinX),maxY);
      RealPoint ur=new RealPoint((float)Math.max(maxX,pMaxX),maxY);
      RealPoint inter[]=Computing.findIntersections(ul,ur,poly);
      if (inter!=null && inter.length==2) {
        int iRight=-1, nRight=0;
        for (int i=0; i<poly.p.length; i++)
          if (poly.p[i].y<=maxY) {
            ++nRight;
            if (iRight<0) iRight=i;
          }
        boolean fromEnd=false;
        if (iRight==0) {
          for (int i=poly.p.length-1; i>1 && poly.p[i].y<=maxY; i--)
            iRight=i;
          fromEnd=iRight>0;
          if (fromEnd && poly.p[poly.p.length-1].equals(poly.p[0]))
            --nRight;
        }
        RealPoint p[]=new RealPoint[nRight+2+1];
        int k=0;
        for (int i=iRight; i<poly.p.length && poly.p[i].y<=maxY; i++)
          p[k++]=poly.p[i];
        if (fromEnd) {
          int i1=0;
          if (poly.p[poly.p.length-1].equals(poly.p[0])) i1=1;
          for (int i=i1; i<iRight && poly.p[i].y<=maxY; i++)
            p[k++]=poly.p[i];
        }
        float d1=inter[1].x-inter[0].x, d2=p[k-1].x-p[0].x;
        if ((d2>0 && d1<0) || (d2<0 && d1>0)) {
          p[k++]=inter[0]; p[k++]=inter[1];
        }
        else {
          p[k++]=inter[1]; p[k++]=inter[0];
        }
        if (k<p.length)
          p[k]=p[0];
        poly.p=p;
      }
    }
    int len=poly.p.length;
    if (!poly.p[len-1].equals(poly.p[0])) {
      RealPoint p[]=new RealPoint[len+1];
      for (int i=0; i<len; i++)
        p[i]=poly.p[i];
      p[len]=p[0];
      poly.p=p;
    }
    poly.boundRect=null;
    return poly;
  }

  protected RealPolyline getRealPolyline (Pnt polyPoints[]) {
    if (polyPoints==null || polyPoints.length<3) return null;
    RealPolyline poly=new RealPolyline();
    poly.p=new RealPoint[polyPoints.length];
    for (int j=0; j<polyPoints.length; j++)
      poly.p[j]=new RealPoint((float)polyPoints[j].coord(0),(float)polyPoints[j].coord(1));
    return poly;
  }

	/**
	 * Returns the neighbourhood matrix for the points derived from the triangulation.
	 * To have the matrix built, it is necessary to set an appropriate
	 * flag before getting the polygons.
	 * Use the method setBuildNeighbourhoodMatrix(true).
	 */
	public Map<Integer, Integer> getNeighbourhoodMap() {
		return neighbourMap;
	}
}
