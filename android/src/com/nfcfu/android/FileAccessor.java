package com.nfcfu.android;

import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * @author Tony Grosinger
 */
public class FileAccessor {
    private static File root;

    private FileAccessor() {
    }

    public static File getRootFile() {
        if (root == null) {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            } else {
                Log.e("NFCFU", "Could not get write access to file");
                throw new IllegalAccessError("Could not get read & write permissions to storage");
            }
        }

        return root;
    }
}
