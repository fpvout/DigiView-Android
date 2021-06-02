package com.fpvout.digiview.streaming;

public class StreamResolution {
    public static final String DEFAULT = "720p";
    private final int width;
    private final int height;

    private StreamResolution(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public static StreamResolution getResolution(String value) {
        switch (value) {
            case "240p":
                return new StreamResolution(426, 240);
            case "360p":
                return new StreamResolution(640, 360);
            case "480p":
                return new StreamResolution(854, 480);
            case DEFAULT:
                return new StreamResolution(1280, 720);
            case "1080p":
                return new StreamResolution(1920, 1080);
        }

        return null;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
