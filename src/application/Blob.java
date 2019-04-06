package application;

public class Blob {

	double minx;
	double miny;
	double maxx;
	double maxy;
	double distThreshold = 600;

	public Blob(double x, double y) {
		minx = x;
		miny = y;
		maxx = x;
		maxy = y;
	}

	public double getDistThreshold() {
		return distThreshold;
	}

	public void setDistThreshold(double distThreshold) {
		this.distThreshold = distThreshold;
	}

	public void add(double x, double y) {
		minx = Math.min(minx, x);
		miny = Math.min(miny, y);
		maxx = Math.max(maxx, x);
		maxy = Math.max(maxy, y);
	}

	public double size() {
		return (maxx - minx) * (maxy - miny);
	}

	public boolean isNear(double x, double y) {
		double cx = (minx + maxx) / 2;
		double cy = (miny + maxy) / 2;
		double d = distSq(cx, cy, x, y);
		if (d < distThreshold * distThreshold) {
			return true;
		} else {
			return false;
		}
	}

	public double distSq(double x1, double y1, double x2, double y2) {
		double d = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
		return d;
	}

}
