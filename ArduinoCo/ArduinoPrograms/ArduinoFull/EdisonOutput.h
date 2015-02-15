#ifndef EDISON_OUTPUT_H
#define EDISON_OUTPUT_H

#include "Output.h"

#define REQUEST_SYMBOL 'x'

/*
  class for sending quaternions to Intel Edison.
  It is assumed that a request symbol is sent to this device.
  When the symbol is received, it sends all available quaternions
*/
class EdisonOutput : 
public Output
{
public:
  EdisonOutput()
  {
  }
  virtual void initialize()
  {
    Serial1.begin(115200);
  }
  virtual bool isReady()
  {
    if(!Serial1) return false;
    bool hasSymbol = false;
    while(Serial1.available() > 0){
      char c = Serial1.read();
      if(c == REQUEST_SYMBOL) hasSymbol = true;
    }
    return hasSymbol;
  }
  virtual void output(float * sensorResult[4], byte numOfQuaternions)
  {
    char floatBuffer[16];
    String stringToSend = "";
    for(byte i = 0; i < numOfQuaternions; ++i) {
      stringToSend = String(stringToSend + "q" + i + 
        "w" + floatToString(floatBuffer, sensorResult[i][0], 6) + 
        "x" + floatToString(floatBuffer, sensorResult[i][1], 6) + 
        "y" + floatToString(floatBuffer, sensorResult[i][2], 6) +
        "z" + floatToString(floatBuffer, sensorResult[i][3], 6) + "e");
    }
    Serial1.print(stringToSend);
  }
};

#endif //SERIAL_OUTPUT_H


