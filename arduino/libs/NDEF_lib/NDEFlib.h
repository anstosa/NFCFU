#include "Arduino.h"
class NDEF{
    public:
        NDEF();

        String GetIP(uint8_t* sector);
        
    private:
        int ip;
        String DecodeIPByte(uint8_t byte);
};
