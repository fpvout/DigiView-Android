package com.fpvout.digiview;

import android.media.MediaScannerConnection;
import android.util.Log;

import org.jcodec.codecs.h264.BufferH264ES;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.common.Codec;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

import java.io.File;
import java.io.IOException;

public class Mp4Muxer extends Thread {

    private static final int TIMESCALE = 60;
    private static final long DURATION = 1;

    private final File h264Dump;
    private final File output;

    SeekableByteChannel file;
    MP4Muxer muxer;
    BufferH264ES es;

    public Mp4Muxer(File h264Dump, File output) {
        this.h264Dump = h264Dump;
        this.output = output;
    }

    private void init() throws IOException {
        file = NIOUtils.writableChannel(output);
        muxer = MP4Muxer.createMP4MuxerToChannel(file);

        es = new BufferH264ES(NIOUtils.mapFile(h264Dump));
    }


    private MuxerTrack initVideoTrack(Packet frame){
        VideoCodecMeta md = new H264Decoder().getCodecMeta(frame.getData());
        return muxer.addVideoTrack(Codec.H264, md);
    }

    private Packet skipToFirstValidFrame(){
        return nextValidFrame(null, null);
    }

    /**
     * Seek next valid frame.
     * For every invalid frame, insert placeholder frame into track
     */
    private Packet nextValidFrame(Packet placeholder, MuxerTrack track){
        Packet frame = null;
        // drop invalid frames
        while (frame == null) {
            try{
                frame = es.nextFrame();
                if(frame == null){
                    return null; // end of input
                }
            }catch (Exception ignore){
                try {
                    if(track != null){
                        track.addFrame(placeholder);
                    }
                } catch (IOException ignored) { }
                // invalid frames can cause a variety of exceptions on read
                // continue
            }
        }
        return frame;
    }

    @Override
    public void run() {

        try{

            init();

            Packet frame = skipToFirstValidFrame();

            MuxerTrack track = null;
            while (frame  != null) {
                if (track == null) {
                    track = initVideoTrack(frame);
                }

                frame.setTimescale(TIMESCALE);
                frame.setDuration(DURATION);
                track.addFrame(frame);

                frame = nextValidFrame(frame, track);
            }

            muxer.finish();

            file.close();

            // cleanup
            h264Dump.delete();

            // add mp4 to gallery
            MediaScannerConnection.scanFile(MainActivity.getContext(),
                    new String[]{output.toString()},
                    null, null);

        } catch (IOException exception){
            Log.e("DIGIVIEW", "MUXER: " + exception.getMessage());
        }
    }
}
