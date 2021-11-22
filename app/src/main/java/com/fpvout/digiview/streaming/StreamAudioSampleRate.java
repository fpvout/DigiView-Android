package com.fpvout.digiview.streaming;

public class StreamAudioSampleRate {
    public static final String DEFAULT = "44100hz";

    public static int getSampleRate(String value) {
        switch (value) {
            case "8khz":
                return 8000;
            case "11025hz":
                return 11025;
            case "16khz":
                return 16000;
            case "22050hz":
                return 22050;
            case "32000hz":
                return 32000;
            case DEFAULT:
                return 44100;
            case "48khz":
                return 48000;
            case "96khz":
                return 96000;
        }

        return -1;
    }
}
