#ifndef EDISON_SENSORMPU_H
#define EDISON_SENSORMPU_H

/*
  Contains functions to receive data from mpu with address 0x69
*/

MPU6050 mpu(0x69);        // 0x69 sensor does not work on any other device, so edison has to do this...
Quaternion quaternion;    // storage Quaternion
uint16_t packetSize;      // packet size
uint8_t mpuIntStatus;     // holds actual interrupt status byte from MPU
uint8_t devStatus;        // return status after each device operation (0 = success, !0 = error)
uint16_t fifoCount;       // count of all bytes currently in FIFO
uint8_t fifoBuffer[64];   // FIFO storage buffer
boolean mpuIsInitialized; // flag for testing if the init worked

void initializeMPU() {
    // initialize device
    mpu.initialize();

    // verify connection
    bool testResult = mpu.testConnection();
    if(!testResult) {
      if(DEBUG_PRINT_RATE > 0)
        Serial.println("Connection to MPU failed!");
      delay(1000);
      if(DEBUG_PRINT_RATE > 0)
        Serial.println("Retry!");
      mpu.initialize();
      testResult = mpu.testConnection();
      if(!testResult) {
        if(DEBUG_PRINT_RATE > 0)
          Serial.println("No connection possible");
        return;  
      }
    }

    // load and configure the DMP
    devStatus = mpu.dmpInitialize();

    // make sure it worked (returns 0 if so)
    if (devStatus == 0) {
      // turn on the DMP, now that it's ready
      mpu.setDMPEnabled(true);
      mpuIntStatus = mpu.getIntStatus();
      mpuIsInitialized = true;

      // get expected DMP packet size for later comparison
      packetSize = mpu.dmpGetFIFOPacketSize();
    } 
}

void measure() {
    if(!mpuIsInitialized) return;
  
    // Get status
    mpuIntStatus = mpu.getIntStatus();

    // get current FIFO count
    fifoCount = mpu.getFIFOCount();

    // check for overflow (this should never happen unless our code is too inefficient)
    if ((mpuIntStatus & 0x10) || fifoCount == 1024) {
      // reset so we can continue cleanly
      mpu.resetFIFO();
      // otherwise, check for DMP data ready interrupt (this should happen frequently)
    } 
    else if (mpuIntStatus & 0x02) {
      // wait for correct available data length, should be a VERY short wait
      while (fifoCount < packetSize) fifoCount = mpu.getFIFOCount();

      // read a packet from FIFO
      mpu.getFIFOBytes(fifoBuffer, packetSize);

      // track FIFO count here in case there is > 1 packet available
      // (this lets us immediately read more without waiting for an interrupt)
      fifoCount -= packetSize;

      // Get quaternion
      mpu.dmpGetQuaternion(&quaternion, fifoBuffer);
    }
}

void setQuaternion(float** quaternions, int *usedQuats, int quatIndex) {
  usedQuats[quatIndex] = 1;
  quaternions[quatIndex][0] = quaternion.w;
  quaternions[quatIndex][1] = quaternion.x;
  quaternions[quatIndex][2] = quaternion.y;
  quaternions[quatIndex][3] = quaternion.z;
}

void handleMPU(float** quaternions, int *usedQuats, int quatIndex) {
    if(!mpuIsInitialized) {
      if(DEBUG_PRINT_RATE > 0)
        Serial.println("MPU not initialized");
      return;  
    }
    measure();
    setQuaternion(quaternions, usedQuats, quatIndex);
}

#endif
