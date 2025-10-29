package io.github.jmecn.tiled.math2d;

/**
 * 
 * @author yanmaoyuan
 *
 */
public class Point {

    private int x;
    private int y;
    
    public Point() {
        x = y = 0;
    }

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public Point(float x, float y) {
        this.x = (int) Math.floor(x);
        this.y = (int) Math.floor(y);
    }

    public void set(float x, float y) {
        this.x = (int) Math.floor(x);
        this.y = (int) Math.floor(y);
    }

    public void set(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point sub(Point p) {
        return new Point(x - p.x, y - p.y);
    }
    
    public Point add(Point p) {
        return new Point(x + p.x, y + p.y);
    }

    public void addLocal(Point p) {
        x += p.x;
        y += p.y;
    }

    public void subLocal(Point p) {
        x -= p.x;
        y -= p.y;
    }

    public void mulLocal(int x, int y) {
        this.x *= x;
        this.y *= y;
    }

    public void mulLocal(Point p) {
        x *= p.x;
        y *= p.y;
    }


    public int lengthSquared() {
        return x * x + y * y;
    }
    
    public int distanceSquared(Point p) {
        int dx = this.x - p.x;
        int dy = this.y - p.y;
        return dx * dx + dy * dy;
    }
    
    public int distanceSquared(int x, int y) {
        int dx = this.x - x;
        int dy = this.y - y;
        return dx * dx + dy * dy;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Point) {
            Point v2d = (Point)obj;
            return (v2d.x == x && v2d.y == y);
        }
        
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return "Point [x=" + x + ", y=" + y + "]";
    }
}
