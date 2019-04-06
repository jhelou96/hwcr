package application;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Monika on 2016-06-13.
 */
public class ProjectionHistograms implements ExtractionAlgorithm {

    private boolean calculateHalf;
    private int[][] pixels;
    private int width, height;

    public ProjectionHistograms(boolean calculateHalf) {
        this.calculateHalf = calculateHalf;
    }

    public List<Integer> computeVertical() {
        List<Integer> countPixels = new ArrayList<>();
        int effectiveWidth = calculateHalf ? width / 2 : width;
        int count;
        for (int idx = 0; idx < effectiveWidth; idx++) {
            count = 0;
            for (int j = 0; j < height; j++) {
                if (pixels[idx][j] == 1) {
                    count++;
                }
            }
            countPixels.add(count);
        }
        return countPixels;
    }

    public List<Integer> computeHorizontal() {
        List<Integer> countPixels = new ArrayList<>();
        int effectiveHeight = calculateHalf ? height / 2 : height;
        int count;
        for (int idx = 0; idx < effectiveHeight; idx++) {
            count = 0;
            for (int j = 0; j < width; j++) {
                if (pixels[j][idx] == 1) {
                    count++;
                }
            }
            countPixels.add(count);

        }
        return countPixels;
    }

    private int[][] getPixels(BufferedImage bufferedImage) {
        int[][] pixelsArray = new int[width][height];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int tmpColor = bufferedImage.getRGB(j, i);
                if (tmpColor == -1) {
                    pixelsArray[j][i] = 0;
                } else {
                    pixelsArray[j][i] = 1;
                }
            }
        }
        return pixelsArray;
    }

    @Override
    public double[] extractFeatures(BufferedImage bufferedImage) {
        if (bufferedImage == null) {
            throw new IllegalArgumentException("Image cannot be null!");
        }
        width = bufferedImage.getWidth();
        height = bufferedImage.getHeight();
        pixels = getPixels(bufferedImage);
        List<Integer> featuresVector = new ArrayList<>();
        featuresVector.addAll(computeHorizontal());
        featuresVector.addAll(computeVertical());
        double[] vector = new double[featuresVector.size()];
        for (int i = 0; i < featuresVector.size(); i++) {
            vector[i] = featuresVector.get(i);
        }
        return vector;
    }

}
