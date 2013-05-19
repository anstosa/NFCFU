package com.nfcfu.desktop;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.IOException;

public class SendFile implements Runnable {
    private File file;
    private DefaultHttpClient client;
    private String destination;

    public SendFile(File file, String ipAddress) {
        this.file = file;
        this.client = new DefaultHttpClient();;
        this.destination = "http://" + ipAddress + ":8080/";
    }

    @Override
    public void run() {
        HttpPost method = new HttpPost(destination);
        MultipartEntity entity = new MultipartEntity();
        entity.addPart("file", new FileBody(file));
        method.setEntity(entity);

        try {
            HttpResponse response = client.execute(method);
            DragAndDrop.statuses.offer(file.getName() + " uploaded successfully!");
            System.out.println(response.getStatusLine());
        } catch (HttpHostConnectException e) {
            DragAndDrop.statuses.offer("Cannot find phone...");
        } catch (IOException e) {
            DragAndDrop.statuses.offer("Failed!");
            e.printStackTrace();
        }
    }
}
