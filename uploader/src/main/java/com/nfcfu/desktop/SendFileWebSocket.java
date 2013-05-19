package com.nfcfu.desktop;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
x`
public class SendFileWebSocket extends WebSocketClient {
    private File file;

    public SendFileWebSocket(File file, URI uri, Draft draft) {
        super(uri, draft);

        this.file = file;
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] content = new byte[(int) file.length()];
            fis.read(content);

            this.getConnection().send(content);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public SendFileWebSocket(File file, String ipAddress, Draft draft) throws URISyntaxException {
        this(file, new URI("ws://" + ipAddress + ":8081/"), draft);
    }

    /*@Override
    public void run() {
        HttpPost method = new HttpPost(destination);
        MultipartEntity entity = new MultipartEntity();
        entity.addPart("file", new FileBody(file));
        method.setEntity(entity);

        try {
            HttpResponse response = client.execute(method);
            DragAndDrop.statuses.offer("Success!");
            System.out.println(response.getStatusLine());
        } catch (HttpHostConnectException e) {
            DragAndDrop.statuses.offer("Cannot find phone...");
        } catch (IOException e) {
            DragAndDrop.statuses.offer("Failed!");
            e.printStackTrace();
        }
    }*/

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Open");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("Message");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Close");
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("Error");
    }
}
