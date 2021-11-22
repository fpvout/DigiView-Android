package com.fpvout.digiview.streaming;

public class StreamAudioBitrate {
    public static final String DEFAULT = "128";

    public static int getBitrate(String value) {
        return Integer.parseInt(value) * 1024;
    }
}
