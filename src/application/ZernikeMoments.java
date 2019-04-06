package application;

import org.apache.commons.math3.complex.Complex;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ZernikeMoments implements ExtractionAlgorithm {

    private static final int ORDER = 12;  //for order<13 works well, but for larger odrder there are numerical errors
    private MyZernike zs1;
    private int imageSize;

    public double[] extractFeatures(BufferedImage bufferedImage) {
        int tempImageSize;
        if (bufferedImage == null) {
            throw new IllegalArgumentException("Image cannot be null!");
        }
        if (bufferedImage.getHeight() != bufferedImage.getWidth()) {
            System.out.println("The image is not square, it will be cropped!");
            if (bufferedImage.getHeight() < bufferedImage.getWidth()) {
                tempImageSize = bufferedImage.getHeight();
            } else {
                tempImageSize = bufferedImage.getWidth();
            }
        } else {
            tempImageSize = bufferedImage.getHeight();
        }
        int[][] pixels = new int[tempImageSize][tempImageSize]; //cutting and prepearing image
        for (int i = 0; i < tempImageSize; i++) {
            for (int j = 0; j < tempImageSize; j++) {
                int temp = bufferedImage.getRGB(i, j);
                if (temp == -1) {
                    pixels[j][i] = 1;
                } else {
                    pixels[j][i] = 0;
                }
            }
        }
        if (tempImageSize != imageSize) {  //if the image size is the same, we don't need to recompute base functions
            imageSize = tempImageSize;
            zs1 = zernikeBaseFunction(); //computing Zernike base functions
        }
        Complex[] v1 = zernikeMoments(pixels, zs1);   //computing Zernike moments
        double[] v1Abs = new double[v1.length]; //getting abs from complex moments
        for (int i = 0; i < v1.length; i++) {
            v1Abs[i] = v1[i].abs();
        }
        return v1Abs;
    }

    private MyZernike zernikeBaseFunction() {
        int[] factorial = factorial(ORDER);
        ArrayList<int[]> orderList = zernikeOrderList(ORDER);
        int length = orderList.size();
        double halfSize = imageSize / 2;
        int[][] pqind = new int[2 * ORDER + 1][2 * ORDER + 1];
        for (int[] row : pqind) {
            Arrays.fill(row, -1);
        }
        ArrayList<int[]> src = orderList.stream().map(int[]::clone).collect(Collectors.toCollection(ArrayList::new));
        for (int[] temp : src) {
            temp[0] += ORDER + 1;
            temp[1] += ORDER + 1;
        }
        for (int i = 0; i < length; i++) {
            pqind[src.get(i)[0] - 1][src.get(i)[1] - 1] = i;
        }
        int[][][] rmns = new int[1 + 2 * ORDER + 1][1 + 2 * ORDER + 1][1 + 2 * ORDER + 1];
        for (int[][] temp1 : rmns) {
            for (int[] temp2 : temp1) {
                Arrays.fill(temp2, 0);
            }
        }
        int min = length;
        for (int flat = 0; flat < min; flat++) {
            int m = orderList.get(flat)[0];
            int n = orderList.get(flat)[1];
            int mpnh = (int) Math.floor((m + Math.abs(n)) / 2);
            int mmnh = (int) Math.floor((m - Math.abs(n)) / 2);
            for (int s = 0; s <= mmnh; s++) {
                rmns[ORDER + m][ORDER + n][s] = (((int) Math.pow(-1, s)) * factorial[m - s]) / (factorial[s] * factorial[mpnh - s] * factorial[mmnh - s]);
            }
        }
        double rho, theta;
        Complex[][][] zernikeBaseFunction = new Complex[imageSize][imageSize][length];
        for (int i = 0; i < imageSize; i++) {
            for (int j = 0; j < imageSize; j++) {
                for (int k = 0; k < length; k++) {
                    zernikeBaseFunction[i][j][k] = new Complex(0, 0);
                }
            }
        }
        for (int y = 1; y <= imageSize; y++) {
            for (int x = 1; x <= imageSize; x++) {
                rho = Math.sqrt(Math.pow(halfSize - x, 2) + Math.pow(halfSize - y, 2));
                theta = Math.atan2(halfSize - y, halfSize - x);
                if (rho > halfSize) {
                    continue;
                }
                rho = rho / halfSize;
                if (theta < 0) theta = theta + 2 * Math.PI;
                for (int flat = 0; flat < length; flat++) {
                    int m = orderList.get(flat)[0];
                    int n = orderList.get(flat)[1];
                    double r = 0;
                    for (int s = 0; s <= ((m - Math.abs(n)) / 2); s++) {
                        r = r + rmns[ORDER + m][ORDER + n][s] * (Math.pow(rho, (m - 2 * s)));

                    }
                    zernikeBaseFunction[y - 1][x - 1][flat] = multiply(new Complex(r, 0), exp(new Complex(0, n * theta)));
                }
            }
        }
        return new MyZernike(ORDER, orderList, pqind, zernikeBaseFunction);
    }

    public Complex multiply(Complex a, Complex b) {
        return new Complex(a.getReal() * b.getReal() - a.getImaginary() * b.getImaginary(),
                a.getReal() * b.getImaginary() + a.getImaginary() * b.getReal());
    }

    public Complex exp(Complex c) {
        return new Complex(Math.exp(c.getReal()) * Math.cos(c.getImaginary()), Math.exp(c.getReal()) * Math.sin(c.getImaginary()));
    }

    private Complex[] zernikeMoments(int[][] pixels, MyZernike zernike) {
        Complex[][][] baseFunction = zernike.baseFunction;
        ArrayList<int[]> orderList = zernike.orders;
        int len = orderList.size();
        Complex[] zernikeMoments = new Complex[len];
        for (int i = 0; i < len; i++) {
            zernikeMoments[i] = new Complex(0, 0);
        }
        for (int flat = 0; flat < len; flat++) {
            int m = orderList.get(flat)[0];
            zernikeMoments[flat] = new Complex((m + 1) / Math.PI);
            Complex temp = new Complex(0, 0);
            for (int x = 0; x < pixels[0].length; x++) {
                for (int y = 0; y < pixels[0].length; y++) {
                    temp = temp.add(multiply(new Complex(pixels[x][y], 0), baseFunction[x][y][flat].conjugate()));
                }
            }
            zernikeMoments[flat] = multiply(zernikeMoments[flat], temp);
        }
        return zernikeMoments;
    }

    public class MyZernike {

        private int order;
        private ArrayList<int[]> orders;
        private int[][] index;
        private Complex[][][] baseFunction;

        public MyZernike(int order, ArrayList<int[]> orders, int[][] index, Complex[][][] baseFunction) {
            this.order = order;
            this.orders = orders;
            this.index = index;
            this.baseFunction = baseFunction;
        }

    }

    private int[] factorial(int n) {
        int[] output = new int[n + 1];
        output[0] = 1;
        for (int i = 1; i <= n; i++) {
            output[i] = output[i - 1] * i;
        }
        return output;
    }

    private ArrayList<int[]> zernikeOrderList(int order) {
        ArrayList<int[]> orderList = new ArrayList<>();
        for (int p = 0; p <= order; p++) {
            for (int q = 0; q <= p; q++) {
                if (Math.abs(p - q) % 2 == 0) {
                    orderList.add(new int[]{p, q});
                }
            }
        }
        return orderList;
    }

}