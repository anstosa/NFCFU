package com.nfcfu.android;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.*;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class MainActivity extends Activity {
    NfcAdapter adapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];
    boolean writeMode;
    Tag mytag;
    Context ctx;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v("NFCFU", "Creating activity");

        setContentView(R.layout.activity_main);
        ctx=this;
        Button btnWrite = (Button) findViewById(R.id.writeNfcButton);

        btnWrite.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                try {
                    if(mytag==null){
                        Log.e("NFCFU", "Error Detected");
                    }else{
                        write("This is the message 2", mytag);
                        Log.v("NFCFU", "NFC Tag Written");
                    }
                } catch (IOException e) {
                    Log.e("NFCFU", "Error Writing");
                    e.printStackTrace();
                } catch (FormatException e) {
                    Log.e("NFCFU", "Error Writing");
                    e.printStackTrace();
                }
            }
        });

        adapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[] { tagDetected };
        Log.v("NFCFU", "Finished Setup");
    }

    @Override
    protected void onNewIntent(Intent intent){
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
            mytag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Log.v("NFCFU", "Detected new Intent");
        }
    }

    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {

        //create the message in according with the standard
        String lang = "en";
        byte[] textBytes = text.getBytes();
        byte[] langBytes = lang.getBytes("US-ASCII");
        int langLength = langBytes.length;
        int textLength = textBytes.length;

        byte[] payload = new byte[1 + langLength + textLength];
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
        return recordNFC;
    }

    private void write(String text, Tag tag) throws IOException, FormatException {

        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);
        Ndef ndef = Ndef.get(tag);
        ndef.connect();
        ndef.writeNdefMessage(message);
        ndef.close();
    }

    @Override
    public void onPause(){
        super.onPause();
        WriteModeOff();
    }

    @Override
    public void onResume(){
        super.onResume();
        WriteModeOn();
    }

    private void WriteModeOn(){
        writeMode = true;
        adapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }

    private void WriteModeOff(){
        writeMode = false;
        adapter.disableForegroundDispatch(this);
    }

/*
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.v("NFCFU", "Creating Activity");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void write(Tag tag) throws IOException, FormatException {
        NdefRecord[] records = { createRecord() };
        NdefMessage message = new NdefMessage(records);

        // Get an instance of Ndef for the tag.
        Ndef ndef = Ndef.get(tag);

        // Enable I/O
        ndef.connect();

        // Write the message
        ndef.writeNdefMessage(message);

        // Close the connection
        ndef.close();
    }


    private NdefRecord createRecord() throws UnsupportedEncodingException {
        String text       = "Hello, World!";
        String lang       = "en";
        byte[] textBytes  = text.getBytes();
        byte[] langBytes  = lang.getBytes("US-ASCII");
        int    langLength = langBytes.length;
        int    textLength = textBytes.length;
        byte[] payload    = new byte[1 + langLength + textLength];

        // set status byte (see NDEF spec for actual bits)
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1,              langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_TEXT,
                new byte[0],
                payload);

        return record;
    }
    */
}