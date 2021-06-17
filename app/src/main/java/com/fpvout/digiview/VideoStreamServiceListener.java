package com.fpvout.digiview;

public interface VideoStreamServiceListener {
    void onVideoStreamData(byte[] buffer, int receivedBytes);
}
