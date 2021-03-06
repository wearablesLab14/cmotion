#ifndef ADAFRUIT_10DOF_SENSOR_H
#define ADAFRUIT_10DOF_SENSOR_H

/*
 Class for the Adafruit10dof sensor.
 Used the filter from
 https://github.com/sparkfun/MPU-9150_Breakout/blob/master/firmware/MPU6050/Examples/MPU9150_AHRS.ino 
 */

// global constants for 9 DoF fusion and AHRS (Attitude and Heading Reference System)
#define GyroMeasError PI * (40.0f / 180.0f) // gyroscope measurement error in rads/s (shown as 3 deg/s)
#define GyroMeasDrift PI * (0.0f / 180.0f) // gyroscope measurement drift in rad/s/s (shown as 0.0 deg/s/s)

#define beta sqrt(3.0f / 4.0f) * GyroMeasError // compute beta
#define zeta sqrt(3.0f / 4.0f) * GyroMeasDrift // compute zeta, the other free parameter in the Madgwick scheme usually set to a small or zero value


class Adafruit10dofSensor
{
public:
  Adafruit10dofSensor()
  {
    accel = new Adafruit_LSM303_Accel_Unified(30301);
    mag   = new Adafruit_LSM303_Mag_Unified(30302);
    gyro = new Adafruit_L3GD20_Unified(20);
    quaternion = new float[4];
    quaternion[0] = 1.0f; 
    quaternion[1] = 0; 
    quaternion[2] = 0; 
    quaternion[3] = 0;

    isInitialized = false;
  }
  void initialize()
  {
    if(!accel->begin()) 
      return;

    if(!mag->begin())
      return;

    if(!gyro->begin())
      return;

    isInitialized = true;
  }
  void handle(float** quaternions, int *usedQuats, int quatIndex) {
    if(isReady()) {
      measure();
      usedQuats[quatIndex] = 1;
      getQuaternion(quaternions[quatIndex]);
    }  
  }
private:
  Adafruit_LSM303_Accel_Unified* accel;
  Adafruit_LSM303_Mag_Unified* mag;
  Adafruit_L3GD20_Unified* gyro;
  bool isInitialized;
  float ax, ay, az, gx, gy, gz, mx, my, mz; // variables to hold latest sensor data values
  float* quaternion; // vector to hold quaternion
  float deltat;
  uint16_t lastUpdate;
  uint16_t now;
  void measure()
  {
    /* Get a new sensor event */
    sensors_event_t event;

    // acceleration measurements
    accel->getEvent(&event);
    ax = event.acceleration.x;
    ay = event.acceleration.y;
    az = event.acceleration.z;

    // gyro measurements
    gyro->getEvent(&event);
    gx = event.gyro.x;
    gy = event.gyro.y;
    gz = event.gyro.z;

    // magnetometer measurements
    mag->getEvent(&event);
    mx = event.magnetic.x;
    my = event.magnetic.y;
    mz = event.magnetic.z;

    // get time since last update
    now = millis();
    deltat = ((now - lastUpdate)/1000.0f);
    if(deltat < 0.0001) deltat = 0.000001;
    lastUpdate = now;

    MadgwickQuaternionUpdate(ax, ay, az, gx*PI/180.0f, gy*PI/180.0f, gz*PI/180.0f, my, mx, mz);
  }
  bool isReady()
  {
    return isInitialized;
  }
  void getQuaternion(float q[4])
  {
    q[0] = quaternion[0];
    q[1] = quaternion[1];
    q[2] = quaternion[2];
    q[3] = quaternion[3];
  }
  // Implementation of Sebastian Madgwick's "...efficient orientation filter for... inertial/magnetic sensor arrays"
  // (see http://www.x-io.co.uk/category/open-source/ for examples and more details)
  // which fuses acceleration, rotation rate, and magnetic moments to produce a quaternion-based estimate of absolute
  // device orientation -- which can be converted to yaw, pitch, and roll. Useful for stabilizing quadcopters, etc.
  // The performance of the orientation filter is at least as good as conventional Kalman-based filtering algorithms
  // but is much less computationally intensive---it can be performed on a 3.3 V Pro Mini operating at 8 MHz!
  void MadgwickQuaternionUpdate(float ax, float ay, float az, float gx, float gy, float gz, float mx, float my, float mz)
  {
    float q1 = quaternion[0], q2 = quaternion[1], q3 = quaternion[2], q4 = quaternion[3]; // short name local variable for readability
    float norm;
    float hx, hy, _2bx, _2bz;
    float s1, s2, s3, s4;
    float qDot1, qDot2, qDot3, qDot4;
    // Auxiliary variables to avoid repeated arithmetic
    float _2q1mx;
    float _2q1my;
    float _2q1mz;
    float _2q2mx;
    float _4bx;
    float _4bz;
    float _2q1 = 2.0f * q1;
    float _2q2 = 2.0f * q2;
    float _2q3 = 2.0f * q3;
    float _2q4 = 2.0f * q4;
    float _2q1q3 = 2.0f * q1 * q3;
    float _2q3q4 = 2.0f * q3 * q4;
    float q1q1 = q1 * q1;
    float q1q2 = q1 * q2;
    float q1q3 = q1 * q3;
    float q1q4 = q1 * q4;
    float q2q2 = q2 * q2;
    float q2q3 = q2 * q3;
    float q2q4 = q2 * q4;
    float q3q3 = q3 * q3;
    float q3q4 = q3 * q4;
    float q4q4 = q4 * q4;

    // Normalise accelerometer measurement
    norm = sqrt(ax * ax + ay * ay + az * az);
    if (norm == 0.0f) return; // handle NaN
    norm = 1.0f/norm;
    ax *= norm;
    ay *= norm;
    az *= norm;
    // Normalise magnetometer measurement
    norm = sqrt(mx * mx + my * my + mz * mz);
    if (norm == 0.0f) return; // handle NaN
    norm = 1.0f/norm;
    mx *= norm;
    my *= norm;
    mz *= norm;
    // Reference direction of Earth's magnetic field
    _2q1mx = 2.0f * q1 * mx;
    _2q1my = 2.0f * q1 * my;
    _2q1mz = 2.0f * q1 * mz;
    _2q2mx = 2.0f * q2 * mx;
    hx = mx * q1q1 - _2q1my * q4 + _2q1mz * q3 + mx * q2q2 + _2q2 * my * q3 + _2q2 * mz * q4 - mx * q3q3 - mx * q4q4;
    hy = _2q1mx * q4 + my * q1q1 - _2q1mz * q2 + _2q2mx * q3 - my * q2q2 + my * q3q3 + _2q3 * mz * q4 - my * q4q4;
    _2bx = sqrt(hx * hx + hy * hy);
    _2bz = -_2q1mx * q3 + _2q1my * q2 + mz * q1q1 + _2q2mx * q4 - mz * q2q2 + _2q3 * my * q4 - mz * q3q3 + mz * q4q4;
    _4bx = 2.0f * _2bx;
    _4bz = 2.0f * _2bz;
    // Gradient decent algorithm corrective step
    s1 = -_2q3 * (2.0f * q2q4 - _2q1q3 - ax) + _2q2 * (2.0f * q1q2 + _2q3q4 - ay) - _2bz * q3 * (_2bx * (0.5f - q3q3 - q4q4) + _2bz * (q2q4 - q1q3) - mx) + (-_2bx * q4 + _2bz * q2) * (_2bx * (q2q3 - q1q4) + _2bz * (q1q2 + q3q4) - my) + _2bx * q3 * (_2bx * (q1q3 + q2q4) + _2bz * (0.5f - q2q2 - q3q3) - mz);
    s2 = _2q4 * (2.0f * q2q4 - _2q1q3 - ax) + _2q1 * (2.0f * q1q2 + _2q3q4 - ay) - 4.0f * q2 * (1.0f - 2.0f * q2q2 - 2.0f * q3q3 - az) + _2bz * q4 * (_2bx * (0.5f - q3q3 - q4q4) + _2bz * (q2q4 - q1q3) - mx) + (_2bx * q3 + _2bz * q1) * (_2bx * (q2q3 - q1q4) + _2bz * (q1q2 + q3q4) - my) + (_2bx * q4 - _4bz * q2) * (_2bx * (q1q3 + q2q4) + _2bz * (0.5f - q2q2 - q3q3) - mz);
    s3 = -_2q1 * (2.0f * q2q4 - _2q1q3 - ax) + _2q4 * (2.0f * q1q2 + _2q3q4 - ay) - 4.0f * q3 * (1.0f - 2.0f * q2q2 - 2.0f * q3q3 - az) + (-_4bx * q3 - _2bz * q1) * (_2bx * (0.5f - q3q3 - q4q4) + _2bz * (q2q4 - q1q3) - mx) + (_2bx * q2 + _2bz * q4) * (_2bx * (q2q3 - q1q4) + _2bz * (q1q2 + q3q4) - my) + (_2bx * q1 - _4bz * q3) * (_2bx * (q1q3 + q2q4) + _2bz * (0.5f - q2q2 - q3q3) - mz);
    s4 = _2q2 * (2.0f * q2q4 - _2q1q3 - ax) + _2q3 * (2.0f * q1q2 + _2q3q4 - ay) + (-_4bx * q4 + _2bz * q2) * (_2bx * (0.5f - q3q3 - q4q4) + _2bz * (q2q4 - q1q3) - mx) + (-_2bx * q1 + _2bz * q3) * (_2bx * (q2q3 - q1q4) + _2bz * (q1q2 + q3q4) - my) + _2bx * q2 * (_2bx * (q1q3 + q2q4) + _2bz * (0.5f - q2q2 - q3q3) - mz);
    norm = sqrt(s1 * s1 + s2 * s2 + s3 * s3 + s4 * s4); // normalise step magnitude
    norm = 1.0f/norm;
    s1 *= norm;
    s2 *= norm;
    s3 *= norm;
    s4 *= norm;
    // Compute rate of change of quaternion
    qDot1 = 0.5f * (-q2 * gx - q3 * gy - q4 * gz) - beta * s1;
    qDot2 = 0.5f * (q1 * gx + q3 * gz - q4 * gy) - beta * s2;
    qDot3 = 0.5f * (q1 * gy - q2 * gz + q4 * gx) - beta * s3;
    qDot4 = 0.5f * (q1 * gz + q2 * gy - q3 * gx) - beta * s4;

    // Integrate to yield quaternion
    q1 += qDot1 * deltat;
    q2 += qDot2 * deltat;
    q3 += qDot3 * deltat;
    q4 += qDot4 * deltat;
    norm = sqrt(q1 * q1 + q2 * q2 + q3 * q3 + q4 * q4); // normalise quaternion
    norm = 1.0f/norm;
    quaternion[0] = q1 * norm;
    quaternion[1] = q2 * norm;
    quaternion[2] = q3 * norm;
    quaternion[3] = q4 * norm;
  }
};

#endif //ADAFRUIT_SENSOR_H





