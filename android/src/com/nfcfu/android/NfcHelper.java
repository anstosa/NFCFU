package com.nfcfu.android;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * @author Tony Grosinger
 *         <p/>
 *         Helper functions for writing data to an NFC tag from an Android app
 */
public class NfcHelper {

    private NfcHelper() {
    }

    /**
     * Write the current IP address of the Android device to an NFC Tag. Must be connected to wifi network.
     *
     * @param tag  NFC Tag to write message to
     * @param wifi WifiManager which can be used to retrieve IP address
     * @throws IOException
     * @throws FormatException
     * @throws IllegalStateException when Android device is not connected to Wifi network
     */
    public static void writeIpAddress(Tag tag, WifiManager wifi) throws IOException, FormatException, IllegalStateException {
        String ipAddress = getIpAddr(wifi);
        if (ipAddress.equals("0.0.0.0")) {
            throw new IllegalStateException("Not connected to Wifi");
        }

        NdefRecord[] records = {createRecord(ipAddress)};
        NdefMessage message = new NdefMessage(records);
        Ndef ndef = Ndef.get(tag);
        ndef.connect();
        ndef.writeNdefMessage(message);
        ndef.close();
    }

    private static NdefRecord createRecord(String message) throws UnsupportedEncodingException {
        //create the message in according with the standard
        String lang = "en";
        byte[] textBytes = message.getBytes();
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

    private static String getIpAddr(WifiManager wifiManager) {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();

        String ipString = String.format("%d.%d.%d.%d",
                (ip & 0xff),
                (ip >> 8 & 0xff),
                (ip >> 16 & 0xff),
                (ip >> 24 & 0xff));

        return ipString;
    }
}
