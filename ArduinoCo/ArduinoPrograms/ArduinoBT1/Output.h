#ifndef OUTPUT_H
#define OUTPUT_H
#include<stdlib.h>

/*  abstract class for quaternion data outputs 
  outputs have to implement 3 methods:
  - an initialization that is called before sending the first quaternion
  - a method ready() that tells the main program if the output can be used
  - a method to send an array of quaternions
*/
class Output
{
public:
  virtual void initialize() = 0;
  virtual bool isReady() = 0;
  virtual void output(float * sensorResult[4], byte numOfQuaternions) = 0;
protected:
  char * floatToString(char * out, float value, byte precision){
    return dtostrf(value, 1, precision, out);
  }
};

#endif //OUTPUT_H


