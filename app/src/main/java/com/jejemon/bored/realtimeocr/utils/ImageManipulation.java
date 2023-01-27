package com.jejemon.bored.realtimeocr.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicConvolve3x3;


public class ImageManipulation {
    public static Bitmap doBlackAndWhite(Bitmap src, int threshold) {
        int width = src.getWidth();
        int height = src.getHeight();
        Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());

        int A, R, G, B;
        int pixelColor;
        int heightWidth = height * width;

        int[] pixels = new int[heightWidth];
        src.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixelColor = pixels[y * width + x];
                A = Color.alpha(pixelColor);
                R = Color.red(pixelColor);
                G = Color.green(pixelColor);
                B = Color.blue(pixelColor);
                int gray = (int)(0.2126 * R + 0.7152 * G + 0.0722 * B);
                if (gray > threshold) {
                    bmOut.setPixel(x, y, Color.argb(A, 255, 255, 255));
                }else{
                    bmOut.setPixel(x, y, Color.argb(A, 0, 0, 0));
                }
            }
        }
        return bmOut;
    }


    public static Bitmap doRemoveNoise(Bitmap bmap) {
        for (int x = 0; x < bmap.getWidth(); x++) {
            for (int y = 0; y < bmap.getHeight(); y++) {
                int pixel = bmap.getPixel(x, y);
                int R = Color.red(pixel);
                int G = Color.green(pixel);
                int B = Color.blue(pixel);
                if (R < 150 && G < 150 && B < 150)
                    bmap.setPixel(x, y, Color.BLACK);
            }
        }
        for (int  x = 0; x < bmap.getWidth(); x++) {
            for (int y = 0; y < bmap.getHeight(); y++) {
                int pixel = bmap.getPixel(x, y);
                int R = Color.red(pixel);
                int G = Color.green(pixel);
                int B = Color.blue(pixel);
                if (R > 150 && G > 150 && B > 150)
                    bmap.setPixel(x, y, Color.WHITE);
            }
        }
        return bmap;
    }
    public static Bitmap doSharpen(Context context, Bitmap original, float[] radius) {
        Bitmap bitmap = Bitmap.createBitmap(
                original.getWidth(), original.getHeight(),
                Bitmap.Config.ARGB_8888);

        RenderScript rs = RenderScript.create(context);

        Allocation allocIn = Allocation.createFromBitmap(rs, original);
        Allocation allocOut = Allocation.createFromBitmap(rs, bitmap);

        ScriptIntrinsicConvolve3x3 convolution
                = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs));
        convolution.setInput(allocIn);
        convolution.setCoefficients(radius);
        convolution.forEach(allocOut);

        allocOut.copyTo(bitmap);
        rs.destroy();

        return bitmap;

    }
    public static Bitmap invertImage(Bitmap src) {
        int width = src.getWidth();
        int height = src.getHeight();
        Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());

        int A, R, G, B;
        int pixelColor;
        int heightWidth = height * width;

        int[] pixels = new int[heightWidth];
        src.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixelColor = pixels[y * width + x];
                A = Color.alpha(pixelColor);
                R = 255 - Color.red(pixelColor);
                G = 255 - Color.green(pixelColor);
                B = 255 - Color.blue(pixelColor);
                bmOut.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }
        return bmOut;
    }

}
