package com.fpvout.digiview;

public class PerformancePreset {
    int h264ReaderMaxSyncFrameSize = 131072;
    int h264ReaderSampleTime = 10000;
    int exoPlayerMinBufferMs = 500;
    int exoPlayerMaxBufferMs = 2000;
    int exoPlayerBufferForPlaybackMs = 17;
    int exoPlayerBufferForPlaybackAfterRebufferMs = 17;

    private PerformancePreset(){

    }

    private PerformancePreset(int mH264ReaderMaxSyncFrameSize, int mH264ReaderSampleTime, int mExoPlayerMinBufferMs, int mExoPlayerMaxBufferMs, int mExoPlayerBufferForPlaybackMs, int mExoPlayerBufferForPlaybackAfterRebufferMs){
        h264ReaderMaxSyncFrameSize = mH264ReaderMaxSyncFrameSize;
        h264ReaderSampleTime = mH264ReaderSampleTime;
        exoPlayerMinBufferMs = mExoPlayerMinBufferMs;
        exoPlayerMaxBufferMs = mExoPlayerMaxBufferMs;
        exoPlayerBufferForPlaybackMs = mExoPlayerBufferForPlaybackMs;
        exoPlayerBufferForPlaybackAfterRebufferMs = mExoPlayerBufferForPlaybackAfterRebufferMs;
    }

    public enum presetType {
        DEFAULT,
        CONSERVATIVE,
        AGGRESSIVE,
        LEGACY
    }

    static PerformancePreset getPreset(presetType p){
        switch(p){
            case CONSERVATIVE:
                return new PerformancePreset(131072,14000,500,2000,34,34);
            case AGGRESSIVE:
                return new PerformancePreset(131072, 9000, 500, 2000, 10, 10);
            case LEGACY:
                return new PerformancePreset(30720, 200, 32768, 65536, 0, 0 );
            case DEFAULT:
            default:
                return new PerformancePreset();
        }
    }
}
