#ifndef MPU6050_SENSOR_H
#define MPU6050_SENSOR_H

#include "QuaternionSensor.h"

/* Simple implementation for the MPU6050 Sensor
   It does not use interrupts (mainly because classes + interrupts don't fit that well)
   Extending it to use interrupts should be easy by changing the isReady() method.
   Uses the example code from https://github.com/Seeed-Studio/Grove_IMU_9DOF
*/
class MPU6050Sensor : public QuaternionSensor
{
public:
  MPU6050Sensor(uint8_t address)
  {
    mpu = MPU6050(address); 
    isInitialized = false; 
  }
  virtual void initialize()
  {
    // initialize device
    mpu.initialize();

    // verify connection
    bool testResult = mpu.testConnection();
    if(!testResult) return;

    // load and configure the DMP
    devStatus = mpu.dmpInitialize();

    // make sure it worked (returns 0 if so)
    if (devStatus == 0) {
      // turn on the DMP, now that it's ready
      mpu.setDMPEnabled(true);
      mpuIntStatus = mpu.getIntStatus();
      isInitialized = true;

      // get expected DMP packet size for later comparison
      packetSize = mpu.dmpGetFIFOPacketSize();
    } 
  }
  virtual void measure()
  {
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
  virtual bool isReady()
  {
    return isInitialized; 
  }
  virtual void getQuaternion(float q[4])
  {
    q[0] = quaternion.w;
    q[1] = quaternion.x;
    q[2] = quaternion.y;
    q[3] = quaternion.z;  
  }
private:
  MPU6050 mpu;
  bool isInitialized;
  Quaternion quaternion;
  uint16_t packetSize;
  uint8_t mpuIntStatus;   // holds actual interrupt status byte from MPU
  uint8_t devStatus;      // return status after each device operation (0 = success, !0 = error)
  uint16_t fifoCount;     // count of all bytes currently in FIFO
  uint8_t fifoBuffer[64]; // FIFO storage buffer
};

#endif //MPU6050_SENSOR_H



