package com.fpvout.digiview.streaming;

public class StreamBitrate {
    public static final String DEFAULT = "2500";

    public static int getBitrate(String value) {
        return Integer.parseInt(value) * 1024;
    }
}
