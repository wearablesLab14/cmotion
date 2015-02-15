#ifndef DEBUG_OUTPUT_H
#define DEBUG_OUTPUT_H

#include "Output.h"

/*
  Simple debug output (prints a debug message every 1000ms).
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
      Serial.begin(9600);
      Serial.println("Using debug output");
    }
    virtual bool isReady()
    {
      return true;
    }
    virtual void output(float sensorResult[4], byte numOfQuaternions)
    {
      timer += millis() - oldTime;
      oldTime = millis();
      loops++;

      if (timer >= 1000) {
        Serial.print("Loops/s: \t");
        Serial.println(loops);
        loops = 0;
        timer = 0;
        Serial.print("quaternion ");
        Serial.print(": \t");
        Serial.print(sensorResult[0]);
        Serial.print("\t");
        Serial.print(sensorResult[1]);
        Serial.print("\t");
        Serial.print(sensorResult[2]);
        Serial.print("\t");
        Serial.println(sensorResult[3]);
      }
    }
  private:
    int loops;
    int timer;
    long oldTime;
};

#endif //DEBUG_OUTPUT_H
