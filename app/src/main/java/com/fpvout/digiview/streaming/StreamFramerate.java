package com.fpvout.digiview.streaming;

public class StreamFramerate {
    public static final String DEFAULT = "60";

    public static int getFramerate(String value) {
        return Integer.parseInt(value);
    }
}
