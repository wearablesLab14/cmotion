#ifndef EDISON_WLAN_SENDER_H
#define EDISON_WLAN_SENDER_H

#include "Output.h"

/* This is the WLAN code for sending quaternions/handling requests.
 It assumes a message from the computer running ROS (it will
 answer with the actual quaternions).
 */
// ssid (name) of the network (it is assumed to be open)
char ssid[] = "mocap";
// the port to receive requests for sensor data
int portToReceive = 6060;
// the port to send data
int portToSend = 5050;


class WlanOutput : 
public Output
{
public:
  WlanOutput() 
  { 
    isInitialized = false; 
    status = WL_IDLE_STATUS;
  }
  virtual void initialize()
  {
    isInitialized = initalizeWifi();
  }
  virtual bool isReady()
  {
    return isInitialized;  
  }
  virtual void output(float * sensorResult[4], byte numOfQuaternions) 
  {
    handleWlan(sensorResult, numOfQuaternions);
  }
private:
  bool isInitialized;  
  ////////////////////////////  WLAN/UDP variables ////////////////////////
  int status;
  // Wifi UDP object
  WiFiUDP Udp;
  // buffer for receiving messages
  char packetBuffer[255];
  // byte arrays for sending
  byte quatIndex[4];
  byte quatW[4];
  byte quatX[4];
  byte quatY[4];
  byte quatZ[4];
  // Variable with the last sent index
  unsigned int sendIndx = 0;
  ////////////////////////////  Debug functions ////////////////////////////

  // prints the wifi status (just for debug)
  void printWifiStatus() {
    // print the SSID of the network you're attached to:
    Serial.print("SSID: ");
    Serial.println(WiFi.SSID());

    // print your WiFi shield's IP address:
    IPAddress ip = WiFi.localIP();
    Serial.print("IP Address: ");
    Serial.println(ip);

    // print the received signal strength:
    long rssi = WiFi.RSSI();
    Serial.print("signal strength (RSSI):");
    Serial.print(rssi);
    Serial.println(" dBm");
  }

  ////////////////////////////  Wifi Initialization ////////////////////////////  

  // initialize wifi connection
  boolean initalizeWifi() {
    // check for the presence of the shield:
    if (WiFi.status() == WL_NO_SHIELD) {
      Serial.println("WiFi shield not present"); 
      return false;
    }

    // firmware check
    String fv = WiFi.firmwareVersion();
    if( fv != "1.1.0" )
      Serial.println("Please upgrade the firmware");

    // attempt to connect to Wifi network:
    while ( status != WL_CONNECTED) { 
      Serial.print("Attempting to connect to SSID: ");
      Serial.println(ssid); 
      status = WiFi.begin(ssid);
      // wait 10 seconds for connection:
      delay(10000);
    } 
    Serial.println("Connected to wifi");
    printWifiStatus();

    Serial.println("\nStarting listening...");
    Udp.begin(portToReceive);

    return true;
  }
  ////////////////////////////  Helper functions ////////////////////////////

  // converts a float to a byte array
  void convertToBytes(float val, byte* bytes){
    union {
      float inputFloat;
      byte tempArray[4];
    } 
    u;
    u.inputFloat = val;
    memcpy(bytes, u.tempArray, 4);
  }

  // converts an integer to a byte array
  void convertToBytes(uint32_t val, byte* bytes){
    union {
      uint32_t inputInt;
      byte tempArray[4];
    } 
    u;
    u.inputInt = val;
    memcpy(bytes, u.tempArray, 4);
  }
  ////////////////////////////  Sending functionality ////////////////////////////  
  // handles the udp-requests for sensor data. it is assumed that sensor data exists 
  // otherwise no data will be send
  void handleWlan(float** quaternions, byte numOfQuaternions) {
    // Receive packet
    int packetSize = Udp.parsePacket();
    if(packetSize)
    {   
      // Retrieve IP and port
      IPAddress remoteIp = Udp.remoteIP();
      int port = Udp.remotePort();

      // read the packet into packetBufffer
      int len = Udp.read(packetBuffer,255);
      if (len >0) packetBuffer[len]=0;

      // send a reply, to the IP address and port that sent us the packet we received
      // NOTE: we go round robin, as we expect a single message per quaternions
      Udp.beginPacket(Udp.remoteIP(), portToSend);
      // Conversions
      convertToBytes(sendIndx, &quatIndex[0]);
      convertToBytes(quaternions[sendIndx][0], &quatW[0]);
      convertToBytes(quaternions[sendIndx][1], &quatX[0]);
      convertToBytes(quaternions[sendIndx][2], &quatY[0]);
      convertToBytes(quaternions[sendIndx][3], &quatZ[0]);

      // Little/big endian:
      /*
        byte tmp =quatIndex[3];
       quatIndex[3] = quatIndex[0];
       quatIndex[0] = tmp;
       tmp = quatIndex[2];
       quatIndex[2] = quatIndex[1];
       quatIndex[1] = tmp;
       */
      Udp.write(quatIndex, 4);
      Udp.write(quatW, 4);
      Udp.write(quatX, 4);
      Udp.write(quatY, 4);
      Udp.write(quatZ, 4);

      // End packet (and send)
      Udp.endPacket();

      // change sendIndx
      sendIndx = (sendIndx + 1) % numOfQuaternions;
    }
  }

};

#endif



