package com.fpvout.digiview;

import androidx.annotation.NonNull;

public class PerformancePreset {
    int h264ReaderMaxSyncFrameSize;
    int h264ReaderSampleTime;
    int exoPlayerMinBufferMs;
    int exoPlayerMaxBufferMs;
    int exoPlayerBufferForPlaybackMs;
    int exoPlayerBufferForPlaybackAfterRebufferMs;
    DataSourceType dataSourceType;

    private PerformancePreset(int mH264ReaderMaxSyncFrameSize, int mH264ReaderSampleTime, int mExoPlayerMinBufferMs, int mExoPlayerMaxBufferMs, int mExoPlayerBufferForPlaybackMs, int mExoPlayerBufferForPlaybackAfterRebufferMs, DataSourceType mDataSourceType) {
        h264ReaderMaxSyncFrameSize = mH264ReaderMaxSyncFrameSize;
        h264ReaderSampleTime = mH264ReaderSampleTime;
        exoPlayerMinBufferMs = mExoPlayerMinBufferMs;
        exoPlayerMaxBufferMs = mExoPlayerMaxBufferMs;
        exoPlayerBufferForPlaybackMs = mExoPlayerBufferForPlaybackMs;
        exoPlayerBufferForPlaybackAfterRebufferMs = mExoPlayerBufferForPlaybackAfterRebufferMs;
        dataSourceType = mDataSourceType;
    }

    public enum PresetType {
        DEFAULT,
        CONSERVATIVE,
        AGGRESSIVE,
        LEGACY,
        LEGACY_BUFFERED
    }

    public enum DataSourceType {
        INPUT_STREAM,
        BUFFERED_INPUT_STREAM
    }

    static PerformancePreset getPreset(PresetType p) {
        switch (p) {
            case CONSERVATIVE:
                return new PerformancePreset(131072, 14000, 500, 2000, 34, 34, DataSourceType.INPUT_STREAM);
            case AGGRESSIVE:
                return new PerformancePreset(131072, 7000, 50, 2000, 17, 17, DataSourceType.INPUT_STREAM);
            case LEGACY:
                return new PerformancePreset(30720, 200, 32768, 65536, 0, 0, DataSourceType.BUFFERED_INPUT_STREAM);
            case LEGACY_BUFFERED:
                return new PerformancePreset(30720, 300, 32768, 65536, 34, 34, DataSourceType.BUFFERED_INPUT_STREAM);
            case DEFAULT:
            default:
                return new PerformancePreset(131072, 10000, 500, 2000, 17, 17, DataSourceType.INPUT_STREAM);
        }
    }

    static PerformancePreset getPreset(String p) {
        switch (p) {
            case "conservative":
                return getPreset(PresetType.CONSERVATIVE);
            case "aggressive":
                return getPreset(PresetType.AGGRESSIVE);
            case "legacy":
                return getPreset(PresetType.LEGACY);
            case "new_legacy":
                return getPreset(PresetType.LEGACY_BUFFERED);
            case "default":
            default:
                return getPreset(PresetType.DEFAULT);
        }
    }


    @Override
    @NonNull
    public String toString() {
        return "PerformancePreset{" +
                "h264ReaderMaxSyncFrameSize=" + h264ReaderMaxSyncFrameSize +
                ", h264ReaderSampleTime=" + h264ReaderSampleTime +
                ", exoPlayerMinBufferMs=" + exoPlayerMinBufferMs +
                ", exoPlayerMaxBufferMs=" + exoPlayerMaxBufferMs +
                ", exoPlayerBufferForPlaybackMs=" + exoPlayerBufferForPlaybackMs +
                ", exoPlayerBufferForPlaybackAfterRebufferMs=" + exoPlayerBufferForPlaybackAfterRebufferMs +
                ", dataSourceType=" + dataSourceType +
                '}';
    }
}
