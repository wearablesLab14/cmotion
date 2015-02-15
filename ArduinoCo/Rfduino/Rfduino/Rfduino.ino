/*
  RFduino program for sending quaternion data from one
  IMU (Seeedstudio 9dof). It should be easy to use another IMU,
  however.
  The data is sent over BLE.
*/

// ================================================================
// ===                      INCLUDES                            ===
// ================================================================
#include <RFduinoBLE.h>
#include "Wire.h"
#include "I2Cdev.h"
#include "MPU6050_6Axis_MotionApps20.h" 
// 6axis is better if there are interfering materials (magnetometer)
#include "MPU6050Sensor.h"
#include "BLEOutput.h"
#include "DebugOutput.h"

// ================================================================
// ===                      DEFINITIONS                         ===
// ================================================================

// Sensor
QuaternionSensor * sensor;
float *sensorResult;

// Outputs
#define numOfOutputs 2 // set to number of outputs
Output** outputs;

// ================================================================
// ===                      INITIAL SETUP                       ===
// ================================================================
void setup() {      
    // -- This section needs to be changed to use different outputs 
    // Initialize output arrays
    outputs = new Output*[numOfOutputs];
    outputs[0] = new BLEOutput("BLEuino");
    outputs[1] = new DebugOutput();
    // put all outputs into the output array...
  
    // Initialize outputs
    for(byte i = 0; i < numOfOutputs; i++)
      outputs[i]->initialize();
   
    // -- This section needs to be changed to use different sensors 
    // join I2C bus
    Wire.begin();
    Wire.write(127);
    
    // Initialize sensor
    sensorResult = new float[4];
    sensor = new MPU6050Sensor(0x68);
    // put all sensors into the sensor array...
    
    // Initialize sensor
    sensor->initialize();
}

// ================================================================
// ===                    MAIN PROGRAM LOOP                     ===
// ================================================================

void loop() {
    // Get Sensor data
    if(sensor->isReady()){
      sensor->measure();
      sensor->getQuaternion(sensorResult);
    }
    
    // output data
    for(byte i = 0; i < numOfOutputs; i++) {
      if(outputs[i]->isReady()) {
        outputs[i]->output(sensorResult, 1); 
      }
    }
}



