// ================================================================
// ===                      INCLUDES                            ===
// ================================================================
#include "Wire.h"
#include "I2Cdev.h"
#include <SPI.h>
#include <WiFi.h>
#include <WiFiUdp.h>
#include "MPU6050_6Axis_MotionApps20.h" 
// does work with 9axis (0x69 won't work)
#include "MPU6050Sensor.h"
#include "wlanOutput.h"

// ================================================================
// ===                      DEFINITIONS                         ===
// ================================================================

// Sensor array
#define numOfSensors 1 // set to number of connected sensors
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
  outputs[0] = new WlanOutput();
  // put all outputs into the output array...

  // Initialize outputs
  for(byte i = 0; i < numOfOutputs; i++)
    outputs[i]->initialize();

  // -- This section needs to be changed to use different sensors 
  // NOTE: sensors have to be initialized AFTER the outputs,
  // because otherwise the serial bluetooth won't be inquirable
  // join I2C bus
  Wire.begin();
  //Wire.write(127); first write to init I2C

  // Initialize sensor array
  sensors = new QuaternionSensor*[numOfSensors];
  sensors[0] = new MPU6050Sensor(0x68);
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




