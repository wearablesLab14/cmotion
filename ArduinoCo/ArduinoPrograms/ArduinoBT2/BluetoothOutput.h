#ifndef BLUETOOTH_OUTPUT_H
#define BLUETOOTH_OUTPUT_H
#include "Output.h"

#define REQUEST_SYMBOL 'x'

/*
  class for sending quaternions via serial bluetooth (grove serial bluetooth).
  It is assumed that a request symbol is sent to this device.
  When the symbol is received, it sends all available quaternions
*/
class BluetoothOutput : 
public Output
{
public:
  BluetoothOutput()
  {
    isInitialized = false;
    btName = "ArduinoBT";
    actDiscIndx = 0;
  }
  BluetoothOutput(String name)
  {
    isInitialized = false;
    btName = name;
    actDiscIndx = 0;
  }
  virtual void initialize()
  {
    isInitialized = true;
    setupBlueToothConnection();
  }
  virtual bool isReady()
  {
    if(!isInitialized) return false;

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
        "w" + floatToString(floatBuffer, sensorResult[i][0], 5) + 
        "x" + floatToString(floatBuffer, sensorResult[i][1], 5) + 
        "y" + floatToString(floatBuffer, sensorResult[i][2], 5) +
        "z" + floatToString(floatBuffer, sensorResult[i][3], 5) + "e");
    }
    Serial1.print(stringToSend);
  }
private:
  void setupBlueToothConnection()
  {
    Serial1.begin(38400); //Set BluetoothBee BaudRate to default baud rate 38400
    Serial1.print("\r\n+STWMOD=0\r\n"); //set the bluetooth work in slave mode
    Serial1.print("\r\n+STNA="+ btName +"\r\n");
    Serial1.print("\r\n+STPIN=0000\r\n");//Set SLAVE pincode"0000"
    Serial1.print("\r\n+STOAUT=1\r\n"); // Permit Paired device to connect me
    Serial1.print("\r\n+STAUTO=0\r\n"); // Auto-connection should be forbidden here
    delay(2000); // This delay is required.
    Serial1.print("\r\n+INQ=1\r\n"); //make the slave bluetooth inquirable 
    Serial.println("The slave bluetooth is inquirable!");
    delay(2000); // This delay is required.
    Serial1.flush();
  }

  bool isInitialized;
  String btName;
  int actDiscIndx;
};

#endif //BLUETOOTH_OUTPUT_H

