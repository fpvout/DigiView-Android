package com.fpvout.digiview.helpers;


import org.jcodec.codecs.h264.BufferH264ES;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.common.Codec;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.mp4parser.Container;
import org.mp4parser.muxer.FileDataSourceImpl;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.builder.DefaultMp4Builder;
import org.mp4parser.muxer.container.mp4.MovieCreator;
import org.mp4parser.muxer.tracks.AACTrackImpl;
import org.mp4parser.muxer.tracks.ClippedTrack;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.util.Log;

import com.fpvout.digiview.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import static com.fpvout.digiview.dvr.DVR.LATEST_THUMB_FILE;

public class Mp4Muxer extends Thread {

    private static final int TIMESCALE = 60;
    private static final long DURATION = 1;

    private final File h264Dump;
    private final File ambientAudioFile;
    private final File videoFile;
    private final File output;
    private final Context context;
    private final File dumpDir;


    SeekableByteChannel file;
    MP4Muxer muxer;
    BufferH264ES es;

    public Mp4Muxer(Context context, File dumpDir , File h264Dump, File ambientAudio, File output) {
        this.context = context;
        this.dumpDir = dumpDir;
        this.h264Dump = h264Dump;
        this.ambientAudioFile = ambientAudio;
        this.videoFile = new File(output.getAbsolutePath() + ".tmp");
        this.output = output;
    }

    private void init() throws IOException {
        file = NIOUtils.writableChannel(videoFile);
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
            //save first frame as img (thumb)

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

            Movie movie = MovieCreator.build(videoFile.getAbsolutePath());
            AACTrackImpl aacTrack = new AACTrackImpl(new FileDataSourceImpl(ambientAudioFile));
            ClippedTrack aacCroppedTrack = new ClippedTrack(aacTrack, 1, aacTrack.getSamples().size());
            movie.addTrack(aacCroppedTrack);

            Container mp4file = new DefaultMp4Builder().build(movie);

            FileOutputStream fileOutputStream = new FileOutputStream(output);
            FileChannel fc = fileOutputStream.getChannel();
            mp4file.writeContainer(fc);
            fileOutputStream.close();

            Bitmap thumb = ThumbnailUtils.createVideoThumbnail(output.getAbsolutePath() , MediaStore.Images.Thumbnails.MINI_KIND);
            FileOutputStream thumbOutputStream = new FileOutputStream(new File(dumpDir.getAbsolutePath() + "/" + LATEST_THUMB_FILE));
            thumb.compress(Bitmap.CompressFormat.JPEG, 100, thumbOutputStream);
            thumbOutputStream.flush();
            thumbOutputStream.close();

            // add mp4 to gallery
            MediaScannerConnection.scanFile(context,
                    new String[]{output.toString()},
                    null, null);

            // cleanup
            h264Dump.delete();
            videoFile.delete();
            ambientAudioFile.delete();
        } catch (IOException exception){
            Log.e("DIGIVIEW", "MUXER: " + exception.getMessage());
        }
    }
}
