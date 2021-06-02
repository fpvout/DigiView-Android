package com.fpvout.digiview.streaming;

import android.media.MediaRecorder;

public class StreamAudioSource {
    public static final String DEFAULT = "default";
    public static final String INTERNAL = "internal";
    public static final String PERFORMANCE = "performance";

    public static int getAudioSource(String value) {
        switch (value) {
            case DEFAULT:
                return MediaRecorder.AudioSource.DEFAULT;
            case "mic":
                return MediaRecorder.AudioSource.MIC;
            case "cam":
                return MediaRecorder.AudioSource.CAMCORDER;
            case "communication":
                return MediaRecorder.AudioSource.VOICE_COMMUNICATION;
            case PERFORMANCE:
                return MediaRecorder.AudioSource.VOICE_PERFORMANCE;
        }

        return -1;
    }
}
