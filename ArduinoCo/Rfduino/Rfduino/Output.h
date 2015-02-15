#ifndef OUTPUT_H
#define OUTPUT_H

/*  
  abstract class for quaternion data outputs 
  outputs have to implement 3 methods:
  - an initialization that is called before sending the first quaternion
  - a method ready() that tells the main program if the output can be used
  - a method to send a quaternion
*/
class Output
{
public:
  virtual void initialize() = 0;
  virtual bool isReady() = 0;
  virtual void output(float sensorResult[4], byte numOfQuaternions) = 0;
  protected:
  String floatToString(float value, byte outLength){
      String result = "";
      if (value < 0){
        result += "-";
        value *= -1.0;
      }
      int integer = (int)value;
      result += integer;
      result += ".";
      while(result.length() < outLength){
        value -= (float)integer;
        value *= 10;
        integer = (int)value;
        result += integer;
        }
      return result;
    }
};

#endif //OUTPUT_H


