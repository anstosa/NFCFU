package com.nfcfu.desktop;

import org.java_websocket.WebSocketImpl;

import java.util.concurrent.LinkedBlockingQueue;

public class App {
    static LinkedBlockingQueue<String> ips = new LinkedBlockingQueue<String>();
    static volatile boolean nfcConnected = true;
    static Thread nfc, files;

    public static void main(String[] args) {
        WebSocketImpl.DEBUG = true;


        //NFCListener t1 = new NFCListener();
        DragAndDrop t2 = new DragAndDrop();
        //nfc = new Thread(t1);
        files = new Thread(t2);
        //nfc.start();
        files.start();
    }
}
