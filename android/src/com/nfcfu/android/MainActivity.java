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
import com.nfcfu.android.httpserver.HttpServer;
import com.nfcfu.android.websocketServer.AndroidWebsocketServer;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    private NfcAdapter adapter;
    private PendingIntent pendingIntent;
    private IntentFilter writeTagFilters[];

    private Tag tagToWrite;
    private AtomicBoolean handlingIntent;

    private AndroidWebsocketServer server;
    private boolean serverRunning;

    private TextView step;
    private TextView stepDesc;
    private TextView error;
    private TextView nextSteps;
    private Button btnStop;
    private Button btnWrite;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find the required UI components
        btnWrite = (Button) findViewById(R.id.writeNfcButton);
        btnStop = (Button) findViewById(R.id.stopServerButton);
        step = (TextView) findViewById(R.id.step_textView);
        stepDesc = (TextView) findViewById(R.id.stepDesc_textView);
        error = (TextView) findViewById(R.id.error_textView);
        nextSteps = (TextView) findViewById(R.id.nextSteps_textView);

        btnWrite.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean result = handlingIntent.compareAndSet(false, true);
                if(!result) {
                    // Someone else is already handling an intent, skip our turn
                    return;
                }

                Log.v(this.getClass().getSimpleName(), "btnWrite clicked");

                // Clear the error before attempting anything
                error.setText("");

                try {
                    if (tagToWrite == null) {
                        error.setText(R.string.tagNotFound);
                    } else {
                        NfcHelper.writeIpAddress(tagToWrite, (WifiManager) getSystemService(WIFI_SERVICE));
                        step.setText(R.string.stepTwo);
                        stepDesc.setText(R.string.stepTwoDesc);
                        nextSteps.setVisibility(View.VISIBLE);

                        btnWrite.setVisibility(View.INVISIBLE);
                        btnStop.setVisibility(View.VISIBLE);

                        if (server == null) {
                            server = new AndroidWebsocketServer(8081);
                            server.start();
                            serverRunning = true;
                        }
                    }
                } catch (IllegalStateException e) {
                    error.setText(R.string.notOnWifi);
                } catch (IOException e) {
                    error.setText(R.string.writeFailure);
                } catch (FormatException e) {
                    error.setText(R.string.writeFailure);
                } finally {
                    handlingIntent.set(false);
                }
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.v(this.getClass().getSimpleName(), "btnStop clicked");

                if (serverRunning) {
                    // Stop the web server
                    try {
                        server.stop();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    serverRunning = false;
                    btnStop.setText(R.string.restartUploadingButton);
                } else {
                    // Create a new webserver
                    server.start();
                    serverRunning = true;
                    btnStop.setText(R.string.finishedUploadingButton);
                }
            }
        });

        handlingIntent = new AtomicBoolean(false);

        // Create adaptor to listen for NFC tag
        adapter = NfcAdapter.getDefaultAdapter(this);

        // Create the intent that we are listening for
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // Filter intents
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[]{
                tagDetected
        };
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
    public void onDestroy() {
        super.onDestroy();
        try {
            server.stop();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}