package io.github.jmecn.tiled.math2d;

public class Bound2D {

	private double x;
	private double y;
	private double width;
	private double height;

	public Bound2D() {
		this(0, 0, 0, 0);
	}

	public Bound2D(Bound2D r) {
		setBounds(r);
	}

	public Bound2D(double width, double height) {
		this(0, 0, width, height);
	}

	public Bound2D(double x, double y, double width, double height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public void setBounds(Bound2D r) {
		x = r.x;
		y = r.y;
		width = r.width;
		height = r.height;
	}

	public boolean contains(double x, double y) {
		double x0 = this.x;
		double y0 = this.y;
		double x1 = x0 + width;
		double y1 = y0 + height;
		return (x >= x0 && y >= y0 && x < x1 && y < y1);
	}

	public void translate(int dx, int dy) {
		x += dx;
		y += dy;
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public double getWidth() {
		return width;
	}

	public void setWidth(double width) {
		this.width = width;
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}
}