package ru.ifmo.md.lesson1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Random;

/**
 * Created by thevery on 11/09/14.
 */
class WhirlView extends SurfaceView implements Runnable {
    Paint paint = null;
    int [] field = null;
    int [] field2 = null;
    int [] pixels = null;
    int width = 240;
    int height = 320;
    final int MAX_COLOR = 10;
    int[] palette = {0xFFFF0000, 0xFF800000, 0xFF808000, 0xFF008000, 0xFF00FF00, 0xFF008080, 0xFF0000FF, 0xFF000080, 0xFF800080, 0xFFFFFFFF};
    SurfaceHolder holder;
    Thread thread = null;
    volatile boolean running = false;
    Bitmap bmp, scaledBmp;

    public WhirlView(Context context) {
        super(context);
        holder = getHolder();
    }

    public void resume() {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void pause() {
        running = false;
        try {
            thread.join();
        } catch (InterruptedException ignore) {}
    }

    public void run() {
        while (running) {
            if (holder.getSurface().isValid()) {
                long startTime = System.nanoTime();
                Canvas canvas = holder.lockCanvas();
                updateField();
                draw(canvas);
                holder.unlockCanvasAndPost(canvas);
                long finishTime = System.nanoTime();
                Log.i("TIME", "Circle: " + (finishTime - startTime) / 1000000);
                Log.i("FPS", "" + 1000000000.f / (finishTime - startTime));
                try {
                    Thread.sleep(16);
                } catch (InterruptedException ignore) {}
            }
        }
    }

    @Override
    public void onSizeChanged(int w, int h, int oldW, int oldH) {
        initField();
    }

    void initField() {
        paint = new Paint();
        bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        field = new int[width * height];
        pixels = new int[width * height];

        Random rand = new Random();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                field[y * width + x] = rand.nextInt(MAX_COLOR);
            }
        }
    }

    int getNewColor(int x, int y) {
        int newColor = field[y * width + x] + 1;
        if (newColor == MAX_COLOR) {
            newColor = 0;
        }

        for (int dx = -1; dx <= 1; dx++) {
            int x2 = x + dx;
            if (x2 < 0) x2 += width;
            if (x2 >= width) x2 -= width;
            for (int dy = -1; dy <= 1; dy++) {
                int y2 = y + dy;
                if (y2 < 0) y2 += height;
                if (y2 >= height) y2 -= height;
                if (newColor == field[y2 * width + x2]) {
                    return newColor;
                }
            }
        }
        return field[y * width + x];
    }

    void updateField() {
        field2 = new int[width * height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int newColor = getNewColor(x, y);
                field2[y * width + x] = newColor;
                pixels[y * width + x] = palette[newColor];
            }
        }
        field = field2;
    }

    @Override
    public void draw(Canvas canvas) {
        bmp.setPixels(pixels, 0, width, 0, 0, width, height);
        scaledBmp = Bitmap.createScaledBitmap(bmp, canvas.getWidth(), canvas.getHeight(), false);
        canvas.drawBitmap(scaledBmp, 0, 0, paint);
    }
}