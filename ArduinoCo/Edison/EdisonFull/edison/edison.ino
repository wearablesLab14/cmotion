/*
  Edison WLAN sender
 It works with a mpu or with serial connected devices with sensors.
 
 The quaternion data can be requested by udp (the program will
 answer any message with this data)
 */

////////////////////////////  Definitionen ///////////////////////////// 

// Maximum number of quaternions (the last one is edisons own sensor)
#define NUM_OF_QUATERNIONS 2
// Time between debug prints (0 disables debug prints)
#define DEBUG_PRINT_RATE 0
// definition for wlan
#define usedWLAN
// Definitions for used sensors
#define usedSerial
#define usedMPU6050
//#define usedAdafruit


////////////////////////////  Includes //////////////////////////////// 

#include "Wire.h"

#include <SPI.h>
#include <WiFi.h>
#include <WiFiUdp.h>
#include "wlan.h"

#include "serial.h"

#include "I2Cdev.h"
#include "MPU6050_6Axis_MotionApps20.h"
#include "sensorMPU.h"

#include <Adafruit_SensorE.h>
#include <Adafruit_LSM303_UE.h>
#include <Adafruit_L3GD20_UE.h>
#include "Adafruit10dofSensor.h"

////////////////////////////  Adafruit ////////////////////////////////

#ifdef usedAdafruit
Adafruit10dofSensor* adaSensor;
#endif

////////////////////////////  Quaternions /////////////////////////////

// Array for quaternions (quaternions are float[4])
float** quaternions;
// Array with 1, 0 for the indices used (in case some sensor gets lost)
int *usedQuats;
// Initialize quaternion arrays and quaternions
void initQuaternions() {
  quaternions = new float*[NUM_OF_QUATERNIONS];
  usedQuats = new int[NUM_OF_QUATERNIONS];
  for(unsigned int i = 0; i < NUM_OF_QUATERNIONS; ++i) {
    usedQuats[i] = 0;
    quaternions[i] = new float[4];
    quaternions[i][0] = 1;
    quaternions[i][1] = 0;
    quaternions[i][2] = 0;
    quaternions[i][3] = 0;
  }
}

////////////////////////////  Timer variables ////////////////////////////

// timer for debug prints
long printTimer = 0;

////////////////////////////  Debug functions ////////////////////////////

// Prints the quaternions to serial (debug)
void printQuaternions() {
  // If disabled, return
  if(DEBUG_PRINT_RATE == 0) return;
  
  // if it is not time yet
  if(millis() - printTimer < DEBUG_PRINT_RATE) return;

  // setting timer
  printTimer = millis();

  // printing
  for(unsigned int i = 0; i < NUM_OF_QUATERNIONS; ++i) {
    if(usedQuats[i] > 0) {
      Serial.print("q: ");
      Serial.print(i);
      Serial.print("\tw: ");  
      Serial.print(quaternions[i][0]);
      Serial.print("\tx: ");  
      Serial.print(quaternions[i][1]);
      Serial.print("\ty: ");  
      Serial.print(quaternions[i][2]);
      Serial.print("\tz: ");  
      Serial.println(quaternions[i][3]);
    }
  }  
}

////////////////////////////  Setup and loop ////////////////////////////

void setup() {
  if(DEBUG_PRINT_RATE > 0) {
    // start debug serial
    Serial.begin(9600);
  }
  
  // join I2C bus (I2Cdev library doesn't do this automatically)
  Wire.begin();
    
  // init quaternion arrays
  initQuaternions();

#ifdef usedSerial
  // init serial polling timer
  initSerial();
#endif

#ifdef usedMPU6050
  // Init 0x69 sensor
  initializeMPU();
#endif

#ifdef usedAdafruit
  // Init adafruit
  adaSensor = new Adafruit10dofSensor();
  adaSensor->initialize();
#endif

#ifdef usedWLAN
  // initialize wifi connection
  boolean wifiSuccess = initalizeWifi();

  // if wifi failed, no need to carry on
  if(!wifiSuccess) {
    Serial.println("Wifi initialization failed");
    while(true);  
  }
#endif

}

void loop() {
#ifdef usedSerial
  // serial communication
  handleSerial(quaternions, usedQuats);
#endif

#ifdef usedMPU6050
  // mpu0x69
  handleMPU(quaternions, usedQuats, (NUM_OF_QUATERNIONS - 1));
#endif

#ifdef usedAdafruit
  adaSensor->handle(quaternions, usedQuats, (NUM_OF_QUATERNIONS - 1));        
#endif

  // wlan communication
  boolean trigger = false;
#ifdef usedWLAN
  trigger = handleWlan(quaternions, usedQuats);
#endif

#ifdef usedSerial
  // if the next serial polling should be triggered
  if(trigger) triggerPolling();
#endif

  // debug print
  printQuaternions();
  
}


