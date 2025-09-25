package com.jobprep.resume_feedback.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.RescaleOp;

public class ImageProcessor {

    private static final float CONTRAST_SCALE_FACTOR = 1.8f;
    private static final float CONTRAST_OFFSET = 20f;
    private static final int BLUR_RADIUS = 2;

    public static BufferedImage preprocessForOcr(BufferedImage image) {
        BufferedImage grayImage = convertToGrayscale(image);
        BufferedImage contrastEnhanced = enhanceContrast(grayImage);
        return applyGaussianBlur(contrastEnhanced);
    }

    private static BufferedImage convertToGrayscale(BufferedImage image) {
        BufferedImage grayImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = grayImage.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return grayImage;
    }

    private static BufferedImage enhanceContrast(BufferedImage image) {
        RescaleOp rescaleOp = new RescaleOp(CONTRAST_SCALE_FACTOR, CONTRAST_OFFSET, null);
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        rescaleOp.filter(image, result);
        return result;
    }

    private static BufferedImage applyGaussianBlur(BufferedImage image) {
        int size = BLUR_RADIUS * 2 + 1;
        float[] matrix = createGaussianKernel(size, BLUR_RADIUS);

        Kernel kernel = new Kernel(size, size, matrix);
        ConvolveOp convolutionOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        return convolutionOp.filter(image, null);
    }

    private static float[] createGaussianKernel(int size, int radius) {
        float[] matrix = new float[size * size];
        float sigma = radius / 3.0f;
        float sum = 0;

        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                float value = (float) Math.exp(-(x * x + y * y) / (2 * sigma * sigma));
                matrix[(y + radius) * size + (x + radius)] = value;
                sum += value;
            }
        }

        for (int i = 0; i < matrix.length; i++) {
            matrix[i] /= sum;
        }

        return matrix;
    }
}
