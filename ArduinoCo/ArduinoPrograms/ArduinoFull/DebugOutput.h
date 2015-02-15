#ifndef DEBUG_OUTPUT_H
#define DEBUG_OUTPUT_H

#include "Output.h"

/*
  Simple debug output (prints a debug message every 1000ms).
  This starts the Serial, so care when using with SerialOutput
*/
class DebugOutput : public Output
{
  public:
    DebugOutput()
    {
      loops = 0;
      timer = 0;
      oldTime = 0;
    }
    virtual void initialize()
    {
      if(!Serial) Serial.begin(9600);
      Serial.println("Using debug output");
    }
    virtual bool isReady()
    {
      return true;
    }
    virtual void output(float * sensorResult[4], byte numOfQuaternions)
    {
      timer += millis() - oldTime;
      oldTime = millis();
      loops++;
      
      if(timer >= 1000) {
        Serial.print("Loops/s: \t");
        Serial.println(loops);
        loops = 0;
        timer = 0;
        char floatBuffer[16];
        for(byte i = 0; i < numOfQuaternions; ++i) {
          Serial.print("quaternion ");
          Serial.print(i);
          Serial.print(": \t");
          floatToString(floatBuffer, sensorResult[i][0], 6);
          Serial.print(floatBuffer);
          Serial.print("\t");
          floatToString(floatBuffer, sensorResult[i][1], 6);
          Serial.print(floatBuffer);
          Serial.print("\t");
          floatToString(floatBuffer, sensorResult[i][2], 6);
          Serial.print(floatBuffer);
          Serial.print("\t");
          floatToString(floatBuffer, sensorResult[i][3], 6);
          Serial.println(floatBuffer);
        }
      }
    }
  private:
    int loops;
    int timer;
    long oldTime;
};

#endif //DEBUG_OUTPUT_H
