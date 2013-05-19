#include "NDEFlib.h"
#include <String.h>

NDEF::NDEF(){
    ip = 9;
}

String NDEF::GetIP(uint8_t * payload){
    String ip = "";
    for (int i = 11; i < 11 + payload[3]; i++)
    {
        if (payload[i] == 0) break;
        ip += DecodeIPByte(payload[i]);
    }
    return ip;
}

String NDEF::DecodeIPByte(uint8_t byte){
    switch(byte)
    {
        case 46: return "."; break;
        case 48: return "0"; break;
        case 49: return "1"; break;
        case 50: return "2"; break;
        case 51: return "3"; break;
        case 52: return "4"; break;
        case 53: return "5"; break;
        case 54: return "6"; break;
        case 55: return "7"; break;
        case 56: return "8"; break;
        case 57: return "9"; break;
        default: return "" ; break;
    }
}
