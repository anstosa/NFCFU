#include <Wire.h>
#include <Adafruit_NFCShield_I2C.h>
#include <NDEFlib.h>

#define IRQ (2)
#define RESET (3)    // Not connected by default on the NFC Shield

Adafruit_NFCShield_I2C nfc(IRQ, RESET);
NDEF ndef;

void setup(void) {
    // has to be fast to dump the entire memory contents!
    Serial.begin(115200);
    Serial.println("Looking for NFC Shield...");

    nfc.begin();

    uint32_t versiondata = nfc.getFirmwareVersion();
    if (! versiondata) {
        Serial.print("Didn't find PN53x board");
        while (1); // halt
    }
    // Got ok data, print it out!
    Serial.print("Found chip PN5"); Serial.println((versiondata>>24) & 0xFF, HEX);
    Serial.print("Firmware ver. "); Serial.print((versiondata>>16) & 0xFF, DEC);
    Serial.print('.'); Serial.println((versiondata>>8) & 0xFF, DEC);

    // configure board to read RFID tags
    nfc.SAMConfig();

    Serial.println("Waiting for an ISO14443A Card...");
}


void loop(void) {
    uint8_t success;                            // Flag to check if there was an error with the PN532
    uint8_t uid[] = { 0, 0, 0, 0, 0, 0, 0 };    // Buffer to store the returned UID
    uint8_t uidLength;                          // Length of the UID (4 or 7 bytes depending on ISO14443A card type)
    bool authenticated = false;
    uint8_t currentblock;                       // Counter to keep track of which block we're on
    uint8_t currentbyte;
    uint8_t block[16];
    uint8_t sector[64];

    // Keyb on NDEF and Mifare Classic should be the same
    uint8_t keyuniversal[6] = { 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF };

    // Wait for an ISO14443A type cards (Mifare, etc.).    When one is found
    // 'uid' will be populated with the UID, and uidLength will indicate
    // if the uid is 4 bytes (Mifare Classic) or 7 bytes (Mifare Ultralight)
    success = nfc.readPassiveTargetID(PN532_MIFARE_ISO14443A, uid, &uidLength);

    if (success)
    {
        // Display some basic information about the card
        Serial.flush();
        Serial.println("Found an ISO14443A card");
        Serial.print("    UID Length: ");Serial.print(uidLength, DEC);Serial.println(" bytes");
        Serial.print("    UID Value: ");
        nfc.PrintHex(uid, uidLength);
        Serial.println("");

        if (uidLength == 4) 
        {
            // We probably have a Mifare Classic card ...
            Serial.println("Seems to be a Mifare Classic card (4 byte UID)");

            // Now we try to go through all 16 sectors (each having 4 blocks)
            // authenticating each sector, and then dumping the blocks
            for (currentblock = 4; currentblock < 8; currentblock++)
            {
                // Check if this is a new block so that we can reauthenticate
                if (nfc.mifareclassic_IsFirstBlock(currentblock)) authenticated = false;

                // If the sector hasn't been authenticated, do so first
                if (!authenticated)
                {
                    // Starting of a new sector ... try to to authenticate
                    Serial.print("------------------------Sector ");Serial.print(currentblock / 4, DEC); Serial.println("-------------------------");
                    success = nfc.mifareclassic_AuthenticateBlock (uid, uidLength, currentblock, 1, keyuniversal);
                    if (success) authenticated = true;
                    else Serial.println("Authentication error");
                }
                // If we're still not authenticated just skip the block
                if (!authenticated)
                {
                    Serial.print("Block ");Serial.print(currentblock, DEC);Serial.println(" unable to authenticate");
                }
                else
                {
                    // Authenticated ... we should be able to read the block now
                    // Dump the data into the 'data' array
                    success = nfc.mifareclassic_ReadDataBlock(currentblock, block);
                    if (success)
                    {
                        for (currentbyte = 0; currentbyte < 16; currentbyte++)
                        {
                            sector[(currentblock - 4) * 16 + currentbyte] = block[currentbyte];
                        }
                        // Read successful
                        Serial.print("Block "); Serial.print(currentblock, DEC);
                        Serial.print("    ");
                        // Dump the raw data
                        nfc.PrintHexChar(block, 16);
                    }
                    else
                    {
                        // Oops ... something happened
                        Serial.print("Block "); Serial.print(currentblock, DEC);
                        Serial.println(" unable to read this block");
                    }
                }
            }
            Serial.println("-----------------------Decoded IP------------------------");
            Serial.println(ndef.GetIP(sector));
            Serial.flush();
        }
        else
        {
            Serial.println("Ooops ... this doesn't seem to be a Mifare Classic card!");
        }
    }
    Serial.flush();
    delay(5000);
    Serial.flush();
    // Wait a bit before trying again
    /*Serial.println("\nCheck again? [y/n]\n\n");
    Serial.flush();
    while (!Serial.available());
    while (Serial.available()) Serial.read();
    Serial.flush();*/
}
