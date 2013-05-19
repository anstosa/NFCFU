package com.nfcfu.android;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.nfc.FormatException;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;

public class MainActivity extends Activity {
    NfcAdapter adapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];

    Tag tagToWrite;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v("NFCFU", "Creating activity");

        setContentView(R.layout.activity_main);
        Button btnWrite = (Button) findViewById(R.id.writeNfcButton);

        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (tagToWrite == null) {
                        Log.e("NFCFU", "Error Detected");
                    } else {
                        NfcHelper.writeIpAddress(tagToWrite, (WifiManager) getSystemService(WIFI_SERVICE));
                        Log.v("NFCFU", "NFC Tag Written");
                    }
                } catch (IllegalStateException e) {

                } catch (IOException e) {
                    Log.e("NFCFU", "Error Writing");
                    e.printStackTrace();
                } catch (FormatException e) {
                    Log.e("NFCFU", "Error Writing");
                    e.printStackTrace();
                }
            }
        });

        // Create adaptor to listen for NFC tag
        adapter = NfcAdapter.getDefaultAdapter(this);

        // Create the intent that we are listening for
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // Filter intents
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[]{tagDetected};
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            tagToWrite = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Log.v("NFCFU", "Detected new Intent");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        adapter.disableForegroundDispatch(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }
}