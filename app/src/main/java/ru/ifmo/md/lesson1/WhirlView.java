package ru.ifmo.md.lesson1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Random;

/**
 * Created by thevery on 11/09/14.
 */
class WhirlView extends SurfaceView implements Runnable {
    public static final int WIDTH = 240;
    public static final int HEIGHT = 320;
    public static final int MAX_IN_CACHE = 50;

    Paint paint = null;
    int [] field = null;
    int [] field2 = null;
    int [] pixels = null;

    int [][] cache = null;
    int [] hash = null;

    int curHash = 0;
    int cachePtr = 0;
    int firstCache, lastCache;
    boolean cacheWasFound = false;
    boolean canUpdate = false;

    final int MAX_COLOR = 10;
    int[] palette = {0xFFFF0000, 0xFF800000, 0xFF808000, 0xFF008000, 0xFF00FF00, 0xFF008080, 0xFF0000FF, 0xFF000080, 0xFF800080, 0xFFFFFFFF};
    SurfaceHolder holder;
    volatile boolean running = false;
    Bitmap bmp, scaledBmp;

    class RunDraw implements  Runnable {
        public void run() {
            while (running) {
                canUpdate = true;
                if (holder.getSurface().isValid()) {
                    Canvas canvas = holder.lockCanvas();
                    long startTime = System.currentTimeMillis();
                    draw(canvas);
                    long finishTime = System.currentTimeMillis();
                    canvas.drawText("" + 1000.0f / (finishTime - startTime), 30, 100, paint);
                    holder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    Thread mainThread = null;
    Thread drawThread = null;

    public WhirlView(Context context) {
        super(context);
        holder = getHolder();
    }

    public void resume() {
        running = true;
        mainThread = new Thread(this);
        drawThread = new Thread(new RunDraw());
        //mainThread = new Thread(this);
        //mainThread.start();
        drawThread.start();
        mainThread.start();
    }

    public void pause() {
        running = false;
        try {
            drawThread.join();
            mainThread.join();
        } catch (InterruptedException ignore) {}
    }

    public void run() {
        while (running) {
            if (holder.getSurface().isValid()) {
                long startTime = System.nanoTime();
                Canvas canvas = holder.lockCanvas();
                //draw(canvas);
                updateField();
                holder.unlockCanvasAndPost(canvas);
                long finishTime = System.nanoTime();
                Log.i("TIME", "Circle: " + (finishTime - startTime) / 1000000);
                Log.i("FPS", "" + 1000000000.f / (finishTime - startTime));
            }
        }
    }

    @Override
    public void onSizeChanged(int w, int h, int oldW, int oldH) {
        initField();
    }

    void initField() {
        paint = new Paint();
        bmp = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.RGB_565);
        field = new int[WIDTH * HEIGHT];
        pixels = new int[WIDTH * HEIGHT];
        cache = new int[MAX_IN_CACHE][WIDTH * HEIGHT];
        hash = new int[MAX_IN_CACHE];

        paint.setTextSize(100);
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Random rand = new Random();
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                field[y * WIDTH + x] = rand.nextInt(MAX_COLOR);
            }
        }
    }

    int getNewColor(int x, int y) {
        int newColor = field[y * WIDTH + x] + 1;
        if (newColor == MAX_COLOR) {
            newColor = 0;
        }

        for (int dx = -1; dx <= 1; dx++) {
            int x2 = x + dx;
            if (x2 < 0) x2 += WIDTH;
            if (x2 >= WIDTH) x2 -= WIDTH;
            for (int dy = -1; dy <= 1; dy++) {
                int y2 = y + dy;
                if (y2 < 0) y2 += HEIGHT;
                if (y2 >= HEIGHT) y2 -= HEIGHT;
                if (newColor == field[y2 * WIDTH + x2]) {
                    return newColor;
                }
            }
        }
        return field[y * WIDTH + x];
    }

    void updateField() {
        if (!canUpdate) return;
        canUpdate = false;
        if (cacheWasFound) {
            cachePtr++;
            if (cachePtr == MAX_IN_CACHE) {
                cachePtr = 0;
            }
            if (cachePtr == lastCache) {
                cachePtr = firstCache;
            }
            pixels = cache[cachePtr];
            return;
        }

        field2 = new int[WIDTH * HEIGHT];
        curHash = 0;
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                int newColor = getNewColor(x, y);
                field2[y * WIDTH + x] = newColor;
                pixels[y * WIDTH + x] = palette[newColor];
                cache[cachePtr][y * WIDTH + x] = palette[newColor];
                curHash = curHash * 31 + palette[newColor] + 1;
            }
        }
        field = field2;
        hash[cachePtr] = curHash;

        for (int i = 0; i < MAX_IN_CACHE; i++) {
            if (i != cachePtr && hash[i] == curHash && Arrays.equals(cache[i], pixels)) {
                cacheWasFound = true;
                firstCache = i;
                lastCache = cachePtr;
                cachePtr = firstCache;
                return;
            }
        }

        cachePtr++;
        if (cachePtr == MAX_IN_CACHE) {
            cachePtr = 0;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        bmp.setPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT);
        scaledBmp = Bitmap.createScaledBitmap(bmp, canvas.getWidth(), canvas.getHeight(), false);
        canvas.drawBitmap(scaledBmp, 0, 0, paint);
    }
}