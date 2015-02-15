/*
  Program for the Adafruit Pro Trinket 5V 16Mhz
  This code allows the trinket to get data from an Adafruit 10dof sensor,
  convert it to a quaternion and send it over serial.
  It is encoded as a string with format:
  q0w[w value]x[x value]y[y value]z[z value]e
*/

////////////////////////////////// Includes ///////////////////////////////

#include <Adafruit_Sensor.h>
#include <Adafruit_LSM303_U.h>
#include <Adafruit_L3GD20_U.h>
#include<stdlib.h>
#include "Wire.h"
#include "Adafruit10dofSensor.h"

////////////////////////////////// Fields /////////////////////////////////

// Array for the sensor result
float sensorResult[4];
// Adafruit sensor
Adafruit10dofSensor* sensor;

// pin for led (status led, should just be blinking)
int led = 13;
// led state (on/off)
bool ledState = false;
// Timer variable (just for led)
long oldTime = 0;

//////////////////////////////////  Setup /////////////////////////////////

void setup() {
  // Begin serial communication
  Serial.begin(115200);
  
  // Join I2C
  Wire.begin();
  
  // Initialize sensor
  sensor = new Adafruit10dofSensor();
  sensor->initialize();
}

//////////////////////////////////  Output  ////////////////////////////////

void output(float sensorResult[4])
{
  // buffer for floats
  char floatBuffer[16];
  
  // string variable
  String stringToSend = "";
  
  // build string
  stringToSend = String(stringToSend + "q0" + 
    "w" + dtostrf(sensorResult[0], 1, 6, floatBuffer) + 
    "x" + dtostrf(sensorResult[1], 1, 6, floatBuffer) + 
    "y" + dtostrf(sensorResult[2], 1, 6, floatBuffer) +
    "z" + dtostrf(sensorResult[3], 1, 6, floatBuffer) + "e");
  
  // Send over serial  
  Serial.print(stringToSend);
}

///////////////////////////// Blink status led  //////////////////////////

void blinkLed() {
  if(millis() - oldTime > 500) {
    oldTime = millis();
    ledState = !ledState;  
    if(ledState) digitalWrite(led, HIGH);
    else digitalWrite(led, LOW);
  }
}

//////////////////////////////////  Loop  ////////////////////////////////

void loop() {
  // If the initialization worked
  if(sensor->isReady()) {
    // Measure
    sensor->measure();
    
    // get quaternion data
    sensor->getQuaternion(sensorResult);
    
    // write data to serial
    output(sensorResult);
    
    // blink with status led
    blinkLed();
  } else {
    // status led should not blink
    digitalWrite(led, LOW);
    
    // We can't do anything
    while(1){}
  }
}



