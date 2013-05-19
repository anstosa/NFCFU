NFC: File Uploader
==================

File Uploader is a combination Android app, Arduino script, and Java Swing Applet. These three components work together, allowing you to seamlessly send files from your (currently only) Linux computer to your Android NFC-enabled device.

We have developed this application using the `PN532 NFC Shield by Adafruit`_.

Features
--------

- No cables necessary for file transfers
- Zero manual configuration of IP addresses or accounts
- Fast (when compared to manually writing bytes) file transfers


Installation
------------

**Android App**

This application is in very very early phases of development and a all code must be compiled and packaged including the Android app. If are familiar with creating APKs, the android folder should be simple enough to package. If not, there are countless `tutorials`_ available online.

**Java Swing Application**

Coming Soon


Usage
-----

Currently there are limitations between the NFC shield that we have been using for NFC and Android's implementation of NFC. To get around this we are currently using a NFC token which the phone will write to and can then be scanned by the NFC reader. Once scanned by the reader, files can be dragged into or selected from the Swing application and will instantly start uploading.

1. Launch Android app and place NFC token against NFC reader in device
2. Press "Program Tag"
3. Launch Java application from computer after plugging in Arduino with PN532 NFC Shield
4. Scan NFC token
5. Upload files by dragging into Java application. Multiple files may be uploaded simultaneously
6. Press "Finished Uploading" when done transfering files or kill the Android app to shutdown WebSockets server 

.. _`tutorials`: http://stackoverflow.com/questions/4600891/how-to-build-apk-file
.. _`PN532 NFC Shield by Adafruit`: www.adafruit.com/products/789
