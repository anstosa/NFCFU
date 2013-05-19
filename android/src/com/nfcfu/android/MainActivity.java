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
import android.widget.TextView;
import org.apache.http.examples.HttpServer;

import java.io.IOException;

public class MainActivity extends Activity {
    NfcAdapter adapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];

    Tag tagToWrite;

    HttpServer webServer;

    TextView step;
    TextView stepDesc;
    TextView error;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Find the required UI components
        Button btnWrite = (Button) findViewById(R.id.writeNfcButton);
        step = (TextView) findViewById(R.id.step_textView);
        stepDesc = (TextView) findViewById(R.id.stepDesc_textView);
        error = (TextView) findViewById(R.id.error_textView);

        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear the error before attempting anything
                error.setText("");

                try {
                    if (tagToWrite == null) {
                        error.setText(R.string.tagNotFound);
                    } else {
                        NfcHelper.writeIpAddress(tagToWrite, (WifiManager) getSystemService(WIFI_SERVICE));
                        step.setText(R.string.stepTwo);
                        stepDesc.setText(R.string.stepTwoDesc);
                        v.setVisibility(View.GONE);

                        if(webServer == null) {
                            webServer = new HttpServer(".");
                        }
                    }
                } catch (IllegalStateException e) {
                    error.setText(R.string.notOnWifi);
                } catch (IOException e) {
                    error.setText(R.string.writeFailure);
                } catch (FormatException e) {
                    error.setText(R.string.writeFailure);
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

    @Override
    public void onStop() {
        super.onStop();
        webServer.stop();
    }
}