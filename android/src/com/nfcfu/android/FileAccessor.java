package com.nfcfu.android;

import android.app.Activity;
import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * @author Tony Grosinger
 */
public class FileAccessor {
    private static FileAccessor instance;
    private static File root;

    private FileAccessor() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            root =  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        } else {
            Log.e("NFCFU", "Could not get write access to file");
            throw new IllegalAccessError("Could not get read & write permissions to storage");
        }
    }

    public static FileAccessor getInstance() {
        if (instance == null) {
            instance = new FileAccessor();
        }
        return instance;
    }

    public static File getRootFile() {
        return root;
    }
}
