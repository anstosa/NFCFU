package com.nfcfu.android.websocketServer;

import android.util.Log;
import com.nfcfu.android.FileAccessor;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by tony on 5/19/13.
 */
public class AndroidWebsocketServer extends WebSocketServer {
    private String fileName = "defaultName.unknown";

    public AndroidWebsocketServer(int port) throws UnknownHostException {
        super(new InetSocketAddress(port));

        Log.v(this.getClass().getSimpleName(), "Created Socket");
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        Log.v(this.getClass().getSimpleName(), "Connection made");
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        Log.v(this.getClass().getSimpleName(), "Connection closed");
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        Log.v(this.getClass().getSimpleName(), "Filename Recieved");
        fileName = s;
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        Log.v(this.getClass().getSimpleName(), "Byte Message Recieved");

        try {
            File writeTo = new File(FileAccessor.getRootFile(), fileName);
            FileOutputStream output = new FileOutputStream(writeTo);
            FileChannel fileChannel = output.getChannel();

            fileChannel.write(message);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        conn.send("File Stored");
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        Log.v(this.getClass().getSimpleName(), "An Error?");
    }
}
