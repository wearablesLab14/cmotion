#ifndef QUATERNION_SENSOR_H
#define QUATERNION_SENSOR_H

/*
  Class for quaternion sensors.
  They have to implement the following methods:
  - initialize: initialize everything it needs to work
  - measure: is called when the sensor should collect it's data
             The data will be requested later on, so the sensor
             class has to store it.
  - ready: true, if measure can be called/will do something
  - getQuaternion: should give the result as float[4]
*/
class QuaternionSensor
{
  public:
    virtual void initialize() = 0;
    virtual void measure() = 0;
    virtual bool isReady() = 0;
    virtual void getQuaternion(float q[4]) = 0;
};

#endif //QUATERNION_SENSOR_H
