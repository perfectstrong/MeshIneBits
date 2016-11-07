package bitSlicer.util;

import java.util.Vector;

/**
 * Segment2D represents a line in 2D space.
 */
public class Segment2D extends AABBrect
{
	public final static int TYPE_MODEL_SLICE = 0;
	public final static int TYPE_PERIMETER = 1;
	public final static int TYPE_MOVE = 2;
	public final static int TYPE_FILL = 3;
	public final static int TYPE_ERROR = 0xFFFF;
	
	public Vector2 start;
	public Vector2 end;
	private Vector2 normal;
	private Segment2D next, prev;
	
	public double lineWidth;
	public double feedRate;
	private int type;
	
	public Segment2D(int type, Vector2 start, Vector2 end)
	{
		// Make the AABB 1mm larger then the actual segment, to account for inaccuracies and moving
		// around the segment ends a bit.
		super(start, end, 1.0);
		
		this.type = type;
		this.lineWidth = -1;
		update(start, end);
	}
	
	public Segment2D(int type, Segment2D prev, Segment2D next)
	{
		super(prev.end, next.start, 1.0);
		this.type = type;
		this.start = prev.end;
		this.end = next.start;
		
		if (prev.next != null)
			prev.next.prev = null;
		prev.next = this;
		if (next.prev != null)
			next.prev.next = null;
		next.prev = this;
		
		this.prev = prev;
		this.next = next;
		
		update(this.start, this.end);
	}
	
	/**
	 * For large updates we need to fix the normal, and the AABB. Only call this when the segment is
	 * not in a Tree2D
	 */
	public void update(Vector2 start, Vector2 end)
	{
		this.start = start;
		this.end = end;
		this.normal = end.sub(start).crossZ().normal();
		updateAABB(start, end, 1.0);
	}
	
	public String toString()
	{
		return "Segment:" + start + " " + end;
	}
	
	public Vector2 getIntersectionPoint(Segment2D other)
	{
		double x12 = start.x - end.x;
		double x34 = other.start.x - other.end.x;
		double y12 = start.y - end.y;
		double y34 = other.start.y - other.end.y;
		
		// Calculate the intersection of the 2 segments.
		double c = x12 * y34 - y12 * x34;
		if (Math.abs(c) < 0.0001)
		{
			return null;
		} else
		{
			double a = start.x * end.y - start.y * end.x;
			double b = other.start.x * other.end.y - other.start.y * other.end.x;
			
			return new Vector2((a * x34 - b * x12) / c, (a * y34 - b * y12) / c);
		}
	}
	
	
	/**
	 * Collision detection taken from http://stackoverflow.com/a/1968345
	 * @param other
	 */
	public Vector2 getCollisionPoint(Segment2D other){
		double p0_x = this.start.x;
		double p0_y = this.start.y;
		double p1_x = this.end.x;
		double p1_y = this.end.y;
		double p2_x = other.start.x;
		double p2_y = other.start.y;
		double p3_x = other.end.x;
		double p3_y = other.end.y;
		
		double s1_x, s1_y, s2_x, s2_y;
	    s1_x = p1_x - p0_x;
	    s1_y = p1_y - p0_y;
	    s2_x = p3_x - p2_x;
	    s2_y = p3_y - p2_y;

	    double s, t;
	    s = (-s1_y * (p0_x - p2_x) + s1_x * (p0_y - p2_y)) / (-s2_x * s1_y + s1_x * s2_y);
	    t = ( s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) / (-s2_x * s1_y + s1_x * s2_y);

	    if (s >= 0 && s <= 1 && t >= 0 && t <= 1)
	    {
	        // Collision detected
	        return new Vector2(p0_x + (t * s1_x), p0_y + (t * s1_y));
	    }

	    return null; // No collision
	}
	
	/**
	 * Check if this segment contain a point. Taken from http://stackoverflow.com/a/11908158
	 * @param point
	 */
	public boolean contains(Vector2 point)
	{
		double dxc = point.x - start.x;
		double dyc = point.y - start.y;

		double dxl = end.x - start.x;
		double dyl = end.y - start.y;

		double cross = dxc * dyl - dyc * dxl;
		if (cross != 0)
			  return false;
		
		if (Math.abs(dxl) >= Math.abs(dyl))
			return (dxl > 0) ? start.x <= point.x && point.x <= end.x :	end.x <= point.x && point.x <= start.x;
		else
			return (dyl > 0) ? start.y <= point.y && point.y <= end.y :	end.y <= point.y && point.y <= start.y;
	}
	
	public Vector2 getNormal()
	{
		return normal;
	}
	
	public int getType()
	{
		return type;
	}
	
	public void setType(int type)
	{
		this.type = type;
	}
	
	public void setNext(Segment2D newNext)
	{
		if (newNext == null)
		{
			if (next == null)
				throw new UnsupportedOperationException();
			next.prev = null;
			next = null;
		} else
		{
			if (next != null)
				throw new UnsupportedOperationException();
			if (newNext.prev != null)
				throw new UnsupportedOperationException();
			next = newNext;
			next.prev = this;
		}
	}
	
	public Segment2D getNext()
	{
		return next;
	}
	
	public Segment2D getPrev()
	{
		return prev;
	}
		
	/**
	 * Return two segments.
	 * @param point
	 * @return
	 */
	public Vector<Segment2D> split(Vector2 point) {
		Segment2D s1 = new Segment2D(this.type, this.start, point);
		Segment2D s2 = new Segment2D(this.type, point, this.end);
			
		Vector<Segment2D> segments = new Vector<Segment2D>();
		segments.add(s1);
		segments.add(s2);
		
		return segments;
	}
	
	public Vector<Segment2D> split(Vector<Vector2> points) {
		points.add(this.start);
		points.add(this.end);
		
		// Convert Vector<Vector2> to Vector2[] since we can't swipe item in Vector<Vector2>
		Vector2[] pointsArray = new Vector2[points.size()];
		for (int i=0; i < points.size(); i++)
			pointsArray[i] = points.get(i);
		
		// Sort the points by x and y
		for (int i = 0; i < pointsArray.length-1; i++) {
			int min = i;
			for (int j = i+1; j < pointsArray.length; j++) {
				if((pointsArray[j].x < pointsArray[min].x))
					min = j;
				else if ((pointsArray[j].x == pointsArray[min].x)) {
					if((pointsArray[j].y < pointsArray[min].y))
						min = j;
				}
			}
			if (min != i) {
				Vector2 temp = pointsArray[min];
				pointsArray[min] = pointsArray[i];
				pointsArray[i] = temp;
			}
		}
		
		Vector<Vector2> sortedPoints = new Vector<Vector2>();
		for (int i = 0; i < pointsArray.length; i++) {
			sortedPoints.add(pointsArray[i]);
		}
		
		Vector<Segment2D> segments = new Vector<Segment2D>();
		
		// Create segments from ordered points
		for (int i = 0; i < sortedPoints.size()-1; i += 1) {
			segments.add(new Segment2D(this.type, sortedPoints.get(i), sortedPoints.get(i+1)));
		}
		
		return segments;
	}
	
	public Vector2 getMidPoint() {
		return new Vector2((this.start.x + this.end.x)/2.0, (this.start.y + this.end.y)/2.0);
	}
	
	public void resetLinks() {
		this.next = null;
		this.prev = null;
	}
}
