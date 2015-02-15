#ifndef SERIAL_SENSOR_H
#define SERIAL_SENSOR_H

#include "QuaternionSensor.h"

String received = "";

// Max length of the received buffer
#define MAX_RECEIVED_LENGTH 50

/* 
 	Implementation of a serial input as 'sensor'.
 	Can be used to communicate with another device with real sensors
 	and Serial Output. Multiple SerialInputSensors can be used,
        as each 'sensor' just has a specific quaternion-id 
        (it will ignore other quaternions)Serial1
 */
class SerialInputSensor : 
public QuaternionSensor
{
public:
  SerialInputSensor(byte quaternionIndex)
  {
    isInitialized = false; 
    quatIndx = quaternionIndex;
  }
  virtual void initialize()
  {
    Serial1.begin(115200);
    data = new float[4];
    data[0] = 1; 
    data[1] = 0; 
    data[2] = 0;
    data[3] = 0;
    isInitialized = true;
  }
  virtual void measure()
  {
    readSerialInput();
  }
  virtual bool isReady()
  {
    return isInitialized; 
  }
  virtual void getQuaternion(float q[4])
  {
    q[0] = data[0];
    q[1] = data[1];
    q[2] = data[2];
    q[3] = data[3];
  }
private:
  bool isInitialized;
  float* data;
  byte quatIndx;

  ////////////////////////////  Parsing functionality ////////////////////////////  

  // Tries to parse the message
  bool tryToParse() {
    // Invalid input
    if(received[0] != 'q') return true;

    // get all indices
    int qIndx = 0;
    int wIndx = received.indexOf("w");
    int xIndx = received.indexOf("x");
    int yIndx = received.indexOf("y");
    int zIndx = received.indexOf("z");

    // if there is one value missing - ignore message
    if(wIndx == -1 || xIndx == -1 || yIndx == -1 || zIndx == -1) return true;

    // Retrieve values
    String q = received.substring(qIndx + 1, wIndx);
    int q_int = q.toInt();

    if(q_int == quatIndx) {
      // Convert values to numbers
      String w = received.substring(wIndx + 1, xIndx);
      String x = received.substring(xIndx + 1, yIndx);
      String y = received.substring(yIndx + 1, zIndx);
      String z = received.substring(zIndx + 1, received.length() - 1);
      data[0] = atof(w.c_str());
      data[1] = atof(x.c_str());
      data[2] = atof(y.c_str());
      data[3] = atof(z.c_str());
      return true;
    }
    return false;
  }

  // Reads from serial input
  void readSerialInput() {
    // if the received message does not start with q or is too long
    if((received.length() > 0 && received[0] != 'q') 
      || (received.length() > MAX_RECEIVED_LENGTH)) {
      received = "";
      return;  
    }

    // If there is no data - nothing to do
    if(!Serial1.available() && received.length() == 0) return;

    if(received.length() > 0 && received[received.length() - 1] == 'e') {
      bool delMsg = tryToParse();
      if(delMsg) received = "";
    }

    // Reading in...
    while(Serial1.available()) {
      char c = Serial1.read();
      received += c;
      if(c == 'e') {
        bool delMsg = tryToParse();
        if(delMsg) received = "";
		else return;
      }  
    }
  }
};

#endif //SERIAL_SENSOR_H





