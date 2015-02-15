#ifndef EDISON_WLAN_SENDER_H
#define EDISON_WLAN_SENDER_H

/* This is the WLAN code for sending quaternions/handling requests.
   It assumes a message from the computer running ROS (it will
   answer with the actual quaternions).
*/

// if 1, wlan triggers polling serial
#define WLAN_TRIGGERS_POLLING 0

////////////////////////////  WLAN/UDP variables ////////////////////////

// Wifi-status
int status = WL_IDLE_STATUS;
// ssid (name) of the network (it is assumed to be open)
char ssid[] = "mocap";
// the port to receive requests for sensor data
int portToReceive = 6060;
// the port to send data
int portToSend = 5050;
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
  if(DEBUG_PRINT_RATE == 0) return;
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
    if(DEBUG_PRINT_RATE > 0)
      Serial.println("WiFi shield not present"); 
    return false;
  }
 
  // firmware check
  String fv = WiFi.firmwareVersion();
  
  // attempt to connect to Wifi network:
  while ( status != WL_CONNECTED) { 
    if(DEBUG_PRINT_RATE > 0) {
      Serial.print("Attempting to connect to SSID: ");
      Serial.println(ssid); 
    }
    status = WiFi.begin(ssid);
    // wait 10 seconds for connection:
    delay(10000);
  } 
  if(DEBUG_PRINT_RATE > 0) {
    Serial.println("Connected to wifi");
    printWifiStatus();
    Serial.println("\nStarting listening...");
  }
  Udp.begin(portToReceive);
  
  return true;
}

////////////////////////////  Helper functions ////////////////////////////

// converts a float to a byte array
void convertToBytes(float val, byte* bytes){
  union {
    float inputFloat;
    byte tempArray[4];
  } u;
  u.inputFloat = val;
  memcpy(bytes, u.tempArray, 4);
}

// converts an integer to a byte array
void convertToBytes(uint32_t val, byte* bytes){
  union {
    uint32_t inputInt;
    byte tempArray[4];
  } u;
  u.inputInt = val;
  memcpy(bytes, u.tempArray, 4);
}

////////////////////////////  Main functionality ////////////////////////////  

// handles the udp-requests for sensor data. it is assumed that sensor data exists 
// otherwise no data will be send
boolean handleWlan(float** quaternions, int *usedQuats) {
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
    
    // check if the actual quat exists
    unsigned int num = 0;
    while(usedQuats[sendIndx] == 0 && num < NUM_OF_QUATERNIONS) {
      sendIndx = (sendIndx + 1)% NUM_OF_QUATERNIONS;
      num++;
    }
    
    // send a reply, to the IP address and port that sent us the packet we received
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
    sendIndx = (sendIndx + 1) % NUM_OF_QUATERNIONS;
    
    // Send next request (by manipulating polling timer)
    if(WLAN_TRIGGERS_POLLING)
      return true;
  }
  return false;
}

#endif
