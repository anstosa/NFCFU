package com.nfcfu.desktop;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class SendFileWebSocket implements Runnable {
    private File file;
    private String ipAddress;

    private class WebSocketConnection extends WebSocketClient {
        public WebSocketConnection( URI serverUri , Draft draft ) {
            super( serverUri, draft );
        }

        public WebSocketConnection( URI serverURI ) {
            super( serverURI );
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
        }

        @Override
        public void onMessage(String message) {
            if(message.equals("File Received")) {
                DragAndDrop.statuses.offer("Success!");
                this.close();
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
        }

        @Override
        public void onError(Exception ex) {
            DragAndDrop.statuses.offer("Failed!");
        }
    }

    public SendFileWebSocket(File file, String ipAddress) throws URISyntaxException {
        this.ipAddress = ipAddress;
        this.file = file;
    }

    @Override
    public void run() {
        byte[] content = null;
        URI uri = null;
        try {
            uri = new URI("ws://" + ipAddress + ":8081/");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        WebSocketConnection connection = new WebSocketConnection( uri, new Draft_17() );
        connection.connect();

        try {
            FileInputStream fis = new FileInputStream(file);
            content = new byte[(int) file.length()];
            fis.read(content);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        connection.send(file.getName());
        connection.send(content);
    }
}
