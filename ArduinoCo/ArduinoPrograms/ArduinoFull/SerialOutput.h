#ifndef SERIAL_OUTPUT_H
#define SERIAL_OUTPUT_H

#include "Output.h"
/* Serial output - sends the data when data is received
   This starts the Serial, so care when using with DebugOutput
*/
class SerialOutput : 
public Output
{
public:
  SerialOutput()
  {
  }
  virtual void initialize()
  {
    if(!Serial) Serial.begin(115200);
  }
  virtual bool isReady()
  {
    bool hasSerial = Serial.available();
    while(Serial.available()) Serial.read();
    return hasSerial;
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
    Serial.print(stringToSend);
  }
};

#endif //SERIAL_OUTPUT_H

