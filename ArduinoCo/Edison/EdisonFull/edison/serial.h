#ifndef EDISON_SERIAL_RECEIVER_H
#define EDISON_SERIAL_RECEIVER_H

/*
  Contains functions to receive data from serial
  (mainly parsing, but also polling).
  It sends a request (a single character like defined below) and
  expects one ore multiple quaternion(s) with format:
  q<int>w<float>x<float>y<float>z<float>e
  example: q0w1.0x0y0z0eq1w0.5x-0.7y0.3z0.6e
*/

// use polling
//#define NEED_POLLING
// Max length of the received buffer
#define MAX_RECEIVED_LENGTH 50
// Time between requests (after each UDP-request may be a request as well)
#define POLLING_RATE 15
// symbol that is used for polling
#define REQUEST_SYMBOL 'x'

////////////////////////////  Timer variables ////////////////////////////

// timer for polling
long pollingTimer = 0;
// used for counting time
long oldPollingTime = 0;

////////////////////////////  Initialization /////////////////////////////

void initSerial() {
  oldPollingTime = millis();  
  
   // start serial connection
  Serial1.begin(115200);
}

////////////////////////////  Serial variable ////////////////////////////

// stores the received string up to the moment it can be parsed
String received = "";

////////////////////////////  Main functionality ////////////////////////////  

// Tries to parse the message
void tryToParse(float** quaternions, int *usedQuats) {
  // Invalid input
  if(received[0] != 'q') return;
  
  // get all indices
  int qIndx = 0;
  int wIndx = received.indexOf("w");
  int xIndx = received.indexOf("x");
  int yIndx = received.indexOf("y");
  int zIndx = received.indexOf("z");
  
  // if there is one value missing - ignore message
  if(wIndx == -1 || xIndx == -1 || yIndx == -1 || zIndx == -1) return;
  
  // Retrieve values
  String q = received.substring(qIndx + 1, wIndx);
  String w = received.substring(wIndx + 1, xIndx);
  String x = received.substring(xIndx + 1, yIndx);
  String y = received.substring(yIndx + 1, zIndx);
  String z = received.substring(zIndx + 1, received.length() - 1);
  
  // Convert valuesto numbers
  int q_int = q.toInt();
  if(q_int < NUM_OF_QUATERNIONS) {
    usedQuats[q_int] = 1;
    quaternions[q_int][0] = atof(w.buffer);
    quaternions[q_int][1] = atof(x.buffer);
    quaternions[q_int][2] = atof(y.buffer);
    quaternions[q_int][3] = atof(z.buffer);
  }
}

// Reads from serial input
void readSerialInput(float** quaternions, int *usedQuats) {
  // if the received message does not start with q or is too long
  if((received.length() > 0 && received[0] != 'q') 
  || (received.length() > MAX_RECEIVED_LENGTH)) {
    received = "";
    return;  
  }
  
  // If there is no data - nothing to do
  if(!Serial1.available()) return;  
  
  // Reading in...
  while(Serial1.available()) {
    char c = Serial1.read();
    received += c;
    if(c == 'e') {
      tryToParse(quaternions, usedQuats);
      received = "";
    }  
  }
}

// Checks if a request should be sent
void pollingSensors() {
  // Setting timers
  long actTime = millis();
  pollingTimer += actTime - oldPollingTime;
  oldPollingTime = actTime;
  
  // send requests
  if(pollingTimer >= POLLING_RATE) {
    Serial1.print(REQUEST_SYMBOL);
    pollingTimer = 0;
  }
}

// Handles the serial communication
void handleSerial(float** quaternions, int *usedQuats) {
  #ifdef NEED_POLLING
  // polling sensors
  pollingSensors();
  #endif
  
  // read serial input
  readSerialInput(quaternions, usedQuats);
}

// triggers polling
void triggerPolling() {
  pollingTimer = POLLING_RATE;  
}

#endif
