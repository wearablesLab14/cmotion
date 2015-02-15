#ifndef BLE_OUTPUT_H
#define BLE_OUTPUT_H
#include "Output.h"

/*
  Output for Bluetooth LE.
  For now, the data is sent per string (with fixed size).
*/
class BLEOutput : 
public Output
{
public:
  BLEOutput(String devName)
  {
    isInitialized = false;
    bleName = devName;
  }

  virtual void initialize()
  {
    RFduinoBLE.deviceName = bleName.cstr();
    RFduinoBLE.advertisementData = "quat";
    RFduinoBLE.advertisementInterval = MILLISECONDS(300);
    RFduinoBLE.txPowerLevel = 4;  // (-20dbM to +4 dBm)

    // start the BLE stack
    RFduinoBLE.begin();
    isInitialized = true;
  }
  virtual bool isReady()
  {
    if(!isInitialized) return false;
    
    return true;
  }
  virtual void output(float sensorResult[4], byte numOfQuaternions)
  {
    String stringToSend = "";
    stringToSend += floatToString(sensorResult[0], 5);
    stringToSend += floatToString(sensorResult[1], 5);
    stringToSend += floatToString(sensorResult[2], 5);
    stringToSend += floatToString(sensorResult[3], 5);
    RFduinoBLE.send(stringToSend.cstr(), stringToSend.length());
  }
  
private:
  bool isInitialized;
  String bleName;
};

#endif //BLUETOOTH_OUTPUT_H

