package application;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;

/**
 *
 * @author Ardiansyah <ard333.ardiansyah@gmail.com>
 */
public class ZoneBasedFeature {
	
	private BufferedImage image;
	private Integer[][] matrixImage;
	private ArrayList<Double> feature;
	
	public ZoneBasedFeature(File imageFile) throws IOException{
		this.image = ImageIO.read(imageFile);
		this.matrixImage = new Integer[this.image.getWidth()][this.image.getHeight()];
	}
	
	public void extract() {
		
		this.threshold();
		this.thin();
		
		ArrayList<Integer[][]> zonedMatrix = new ArrayList<>();
		feature = new ArrayList<>();
		Coordinate cICZ = findCentroid(matrixImage);
		
		int div = 3;
		int r = matrixImage.length/div;
		int c = matrixImage[0].length/div;
		
		//9 zone
		for (int rowM = 0; rowM < div; rowM++) {
			for (int colM=0; colM < div; colM++) {
				zonedMatrix.add(makeZone(rowM*r, colM*c, r, c));
			}
		}
		
		for (Integer[][] matrix : zonedMatrix) {
			Coordinate cZCZ = findCentroid(matrix);
			
			if (cICZ.getX() == -1 || cZCZ.getY() == -1) {
				feature.add(0.0);
				feature.add(0.0);
			} else {
				Double tempICZ = 0.0;
				Double tempZCZ = 0.0;
				for (int x = 0; x < matrix.length; x++) {
					for (int y = 0; y < matrix[0].length; y++) {
						if (matrix[x][y] == 1) {
							tempICZ += Math.sqrt((Math.pow((x - cICZ.getX()),2) + Math.pow((y - cICZ.getY()),2)));
							tempZCZ += Math.sqrt((Math.pow((x - cZCZ.getX()),2) + Math.pow((y - cZCZ.getY()),2)));
						}
					}
				}
				feature.add(tempICZ/9);
				feature.add(tempZCZ/9);
			}
		}
	}
	
	public Integer[][] makeZone(int rStart, int cStart, int r, int c){
		Integer[][] zoned = new Integer[r][c];
		for(int i = 0; i < r; i++){
			for(int j = 0; j < c; j++){
				zoned[i][j] = matrixImage[rStart+i][cStart+j];
			}
		}
		return zoned;
	}
	
	private Coordinate findCentroid(Integer[][] input) {
		int total = 0;
		int sigX = 0;
		int sigY = 0;
		for (int x = 0; x < input.length; x++) {
			for (int y = 0; y < input[0].length; y++) {
				total += input[x][y];
				sigX += x*input[x][y];
				sigY += y*input[x][y];
			}
		}
		if (total == 0) {
			return new Coordinate(-1, -1);
		} else {
			return new Coordinate(Math.round(sigX/total), Math.round(sigY/total));
		}
	}
	
	/**
	 * By Nayef Reza
	 */
	public void threshold() {
		int thresholdvalues[] = new int[256];
		int w = image.getWidth();
		int h = image.getHeight();
		
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int color = image.getRGB(x, y);
				int cred = (color & 0x00ff0000) >> 16;
				int cgreen = (color & 0x0000ff00) >> 8;
				int cblue = (color & 0x000000ff);
				int cc = ((int) (cred + cgreen + cblue) / 3);
				thresholdvalues[cc]++;
			}
		}
		int laststate = 0;
		int currentstate = 0;
		int max1 = thresholdvalues[0];
		int nmax1 = 0;
		int max2 = 0;
		int nmax2 = 0;
		for (int i = 1; i < 256; i++) {
			
			if (thresholdvalues[i] > thresholdvalues[i - 1]) {
				currentstate = 1;
			} else if (thresholdvalues[i] == thresholdvalues[i - 1]) {
				currentstate = 0;
			} else {
				currentstate = -1;
			}
			
			if (currentstate != laststate || i == 255) {
				if (thresholdvalues[i] > max1) {
					max2 = max1;
					nmax2 = nmax1;
					max1 = thresholdvalues[i];
					nmax1 = i;
				} else if (thresholdvalues[i] > max2) {
					max2 = thresholdvalues[i];
					nmax2 = i;
				}
			}
			laststate = currentstate;
		}

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int color = image.getRGB(x, y);
				int cred = (color & 0x00ff0000) >> 16;
				int cgreen = (color & 0x0000ff00) >> 8;
				int cblue = (color & 0x000000ff);
				int cc = ((int) (cred + cgreen + cblue) / 3);
				if (cc == nmax2) {
					image.setRGB(x, y, new Color(0, 0, 0).getRGB());
				} else if (cc == nmax1) {
					image.setRGB(x, y, new Color(255, 255, 255).getRGB());
				} else {
					image.setRGB(x, y, new Color(255, 255, 255).getRGB());
				}
			}
		}
	}
	
	/**
	 * By Nayef Reza
	 */
	public void thin() {
		int w = matrixImage.length;
		int h = matrixImage[0].length;
		
		int mark[][] = new int[w][h];
		boolean hasdelete = true;
		
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				if (image.getRGB(x, y) == new Color(0,0,0).getRGB()) {
					matrixImage[x][y] = 1;
				} else {
					matrixImage[x][y] = 0;
				}
			}
		}
		
		while (hasdelete) {
			hasdelete = false;
			for (int y = 0; y < matrixImage[0].length; y++) {
				for (int x = 0; x < matrixImage.length; x++) {
					if (matrixImage[x][y] == 1) {
						int nb[] = this.getNeighbors(x, y, w, h);
						int a = 0;
						for (int i = 2; i < 9; i++) {
							if ((nb[i] == 0) && (nb[i + 1] == 1)) {
								a++;
							}
						}
						if ((nb[9] == 0) && (nb[2] == 1)) {
							a++;
						}
						int b = nb[2] + nb[3] + nb[4] + nb[5] + nb[6] + nb[7] + nb[8] + nb[9];
						int p1 = nb[2] * nb[4] * nb[6];
						int p2 = nb[4] * nb[6] * nb[8];
						if ((a == 1) && ((b >= 2) && (b <= 6)) && (p1 == 0) && (p2 == 0)) {
							mark[x][y] = 0;
							hasdelete = true;
						} else {
							mark[x][y] = 1;
						}
					} else {
						mark[x][y] = 0;
					}
				}
			}
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					matrixImage[x][y] = mark[x][y];
				}
			}
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					if (matrixImage[x][y] == 1) {
						int nb[] = getNeighbors(x, y, w, h);
						int a = 0;
						for (int i = 2; i < 9; i++) {
							if ((nb[i] == 0) && (nb[i + 1] == 1)) {
								a++;
							}
						}
						if ((nb[9] == 0) && (nb[2] == 1)) {
							a++;
						}
						int b = nb[2] + nb[3] + nb[4] + nb[5] + nb[6] + nb[7] + nb[8] + nb[9];
						int p1 = nb[2] * nb[4] * nb[8];
						int p2 = nb[2] * nb[6] * nb[8];
						if ((a == 1) && ((b >= 2) && (b <= 6)) && (p1 == 0) && (p2 == 0)) {
							mark[x][y] = 0;
							hasdelete = true;
						} else {
							mark[x][y] = 1;
						}
					} else {
						mark[x][y] = 0;
					}
				}
			}
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					matrixImage[x][y] = mark[x][y];
				}
			}
		}
		
	}

	/**
	 * By Nayef Reza
	 * 
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @return 
	 */
	private int[] getNeighbors(int x, int y, int w, int h) {
		int a[] = new int[10];
		for (int n = 1; n < 10; n++) {
			a[n] = 0;
		}
		if (y - 1 >= 0) {
			a[2] = matrixImage[x][y - 1];
			if (x + 1 < w) {
				a[3] = matrixImage[x + 1][y - 1];
			}
			if (x - 1 >= 0) {
				a[9] = matrixImage[x - 1][y - 1];
			}
		}
		if (y + 1 < h) {
			a[6] = matrixImage[x][y + 1];
			if (x + 1 < w) {
				a[5] = matrixImage[x + 1][y + 1];
			}
			if (x - 1 >= 0) {
				a[7] = matrixImage[x - 1][y + 1];
			}
		}
		if (x + 1 < w) {
			a[4] = matrixImage[x + 1][y];
		}
		if (x - 1 >= 0) {
			a[8] = matrixImage[x - 1][y];
		}
		return a;
	}
	
	public ArrayList<Double> getFeature() {
		return feature;
	}
	
	private class Coordinate {
		private int x;
		private int y;
		public Coordinate(int x, int y) {
			this.x = x;
			this.y = y;
		}
		public int getX() {
			return x;
		}
		public int getY() {
			return y;
		}
	}
}
