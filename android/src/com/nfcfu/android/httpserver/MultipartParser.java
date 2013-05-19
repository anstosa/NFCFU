package com.nfcfu.android.httpserver;

import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * @author Tony Grosinger
 */
public class MultipartParser {
    private final Hashtable<String, String> parameters;
    private byte[] fileBytes;

    public MultipartParser(byte[] bytes) throws UnsupportedEncodingException {
        parameters = new Hashtable<String, String>();

        parseMessage(bytes);
    }

    public String getFileName() {
        return parameters.get("fileName");
    }

    public String getContentType() {
        return parameters.get("contentType");
    }

    public Hashtable<String, String> getParameters() {
        return parameters;
    }

    public byte[] getFileBytes() {
        return fileBytes;
    }

    private Hashtable<String, String> parseMessage(byte[] bytes) throws UnsupportedEncodingException {
        Vector<Byte> vFileBytes = new Vector<Byte>();
        int paramCount = 0;

        String data = new String(bytes, "ISO-8859-1");
        String boundary = data.substring(0, data.indexOf('\n'));
        String[] elements = data.split(boundary);

        for (String element : elements) {
            if (element.length() <= 0) {
                continue;
            }

            String[] descval = element.split("\n");

            if (descval.length > 4) {
                // If it has more than 4 lines it's a file

                // take the first line of this element and split it by ";"
                String[] contentDisposition = descval[1].split(";");

                // the long file name is the second part of the first line.. take only what is between the quotes
                String longFileName = contentDisposition[2].substring(contentDisposition[2].indexOf('"') + 1, contentDisposition[2].length() - 2).trim();
                parameters.put("longFileName", longFileName);

                // the fileName is the longFileName without the directories
                String fileName = longFileName.substring(longFileName.lastIndexOf("\\") + 1, longFileName.length());
                parameters.put("fileName", fileName);

                // gab the content type from the second line in this element
                String contentType = descval[2].substring(descval[2].indexOf(' ') + 1, descval[2].length() - 1);
                parameters.put("contentType", contentType);

                int pos = 0;
                int lineCount = 0;

                // count the lines and the bytes up to this point
                while (lineCount < 4) {
                    if ((char) bytes[pos] == '\n') {
                        lineCount++;
                    }
                    pos++;
                }

                // Grab all the bytes from the current position all the way to right before the last boundary
                for (int k = pos; k < (bytes.length - boundary.length() - 4); k++) {
                    vFileBytes.add(bytes[k]);
                }

                // Convert the Vector to a byte array
                fileBytes = new byte[vFileBytes.size()];
                for (int j = 0; j < fileBytes.length; j++) {
                    fileBytes[j] = vFileBytes.get(j);
                }

                parameters.put("contentLength", "" + fileBytes.length);
            } else if (descval.length == 4) {
                // if it's got 4 lines, it's just a regular parameter

                String key = descval[1].substring(descval[1].indexOf('"') + 1, descval[1].length() - 2).trim();
                String value = descval[3].trim();

                paramCount++;
                parameters.put(key, value);
            }
        }
        return parameters;
    }
}
