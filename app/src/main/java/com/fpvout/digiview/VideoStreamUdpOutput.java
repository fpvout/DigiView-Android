package com.fpvout.digiview;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class VideoStreamUdpOutput implements VideoStreamServiceListener {
    public int udpPort = 8543;
    public InetAddress broadcastAddress = InetAddress.getByName("192.168.1.255");
    public DatagramSocket socket;
    public VideoStreamService videoStreamService;

    public VideoStreamUdpOutput(VideoStreamService v) throws UnknownHostException {
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        videoStreamService.addVideoStreamListener(this::onVideoStreamData);
    }

    public void onVideoStreamData(byte[] buffer, int receivedBytes) {
        DatagramPacket sendPacket = new DatagramPacket(buffer, receivedBytes, broadcastAddress, udpPort);
        try {
            socket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
