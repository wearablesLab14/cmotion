/*
  Main program for retrieving and sending quaternions.
  It supports different outputs and sensors.
*/

// ================================================================
// ===                      INCLUDES                            ===
// ================================================================
#include <Adafruit_Sensor.h>
#include <Adafruit_LSM303_U.h>
#include <Adafruit_L3GD20_U.h>
#include "Wire.h"
#include "I2Cdev.h"
#include "MPU6050_6Axis_MotionApps20.h"
// does not work with 9axis (0x69 won't work, library bug)
#include "MPU6050Sensor.h"
#include "Adafruit10dofSensor.h"
#include "DebugOutput.h"
#include "SerialOutput.h"
#include "BluetoothOutput.h"
#include "EdisonOutput.h"
#include "SerialInputSensor.h"

// ================================================================
// ===                      DEFINITIONS                         ===
// ================================================================

// Sensor array
#define numOfSensors 2 // set to number of connected sensors
QuaternionSensor** sensors;
float ** sensorResults;

// Outputs
#define numOfOutputs 1 // set to number of outputs
Output** outputs;

// ================================================================
// ===                      INITIAL SETUP                       ===
// ================================================================
void setup() {
    // Init results buffer
    sensorResults = new float*[numOfSensors];
    for(byte i = 0; i < numOfSensors; i++)
      sensorResults[i] = new float[4];
    
// -- This section needs to be changed to use different outputs 
    // Initialize output arrays
    outputs = new Output*[numOfOutputs];
    outputs[0] = new DebugOutput();
    // put all outputs into the output array...
  
    // Initialize outputs
    for(byte i = 0; i < numOfOutputs; i++)
      outputs[i]->initialize();
   
// -- This section needs to be changed to use different sensors 
// NOTE: sensors have to be initialized AFTER the outputs,
// because otherwise the serial bluetooth won't be inquirable
    // join I2C bus
    Wire.begin();
    
    // Initialize sensor array
    sensors = new QuaternionSensor*[numOfSensors];
    sensors[0] = new SerialInputSensor(0);
    sensors[1] = new SerialInputSensor(1);
    //sensors[0] = new MPU6050Sensor(0x68);
    //sensors[1] = new MPU6050Sensor(0x69);
    // put all sensors into the sensor array...
    
    // Initialize sensors
    for(byte i = 0; i < numOfSensors; i++)
      sensors[i]->initialize();
}


// ================================================================
// ===                    MAIN PROGRAM LOOP                     ===
// ================================================================

void loop() {
    // Get Sensor data
    for(byte i = 0; i < numOfSensors; i++) {
      if(sensors[i]->isReady()){
        sensors[i]->measure();
        sensors[i]->getQuaternion(sensorResults[i]);
      }
    }
    
    // No sensors - no output
    if(numOfSensors == 0) return;
    
    // output data
    for(byte i = 0; i < numOfOutputs; i++) {
      if(outputs[i]->isReady()) {
        outputs[i]->output(sensorResults, numOfSensors); 
      }
    }
}



