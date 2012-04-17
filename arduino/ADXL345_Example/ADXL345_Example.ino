//Arduino 1.0+ Only!
//Arduino 1.0+ Only!

#include <Wire.h>
#include <ADXL345.h>


ADXL345 adxl; //variable adxl is an instance of the ADXL345 library

void setup(){
  Serial.begin(9600);
  //Serial.println("something working");
  adxl.powerOn();
  delay(500);  // Slight delay for power up
  adxl.setRangeSetting(2);  // set range to +-2g
  adxl.setRate(200);        // set sampling rate to 200 Hz
  calibrate();

}

void loop(){
  double x,y,z;  
  adxl.get_Gxyz(&x, &y, &z); //read the accelerometer values and store them in variables  x,y,z

  // Output x,y,z values - Commented out
  Serial.print(x);
  Serial.print(", ");
  Serial.print(y);
  Serial.print(", ");
  Serial.println(z);

  delay(400);
}

void calibrate(){
    double xcal = 0.0;
    double ycal = 0.0;
    double zcal = 0.0;
    double xtest, ytest, ztest;
    char xoff, yoff, zoff;
    
    // Grab 30 samples from each axis and average them to remove offset
    for(int j = 0; j < 30; j++){
      adxl.get_Gxyz(&xtest, &ytest, &ztest);
      xcal += xtest;
      ycal += ytest;
      zcal += ztest;
      delay(20);      
    }
    xcal /= 30.0;
    ycal /= 30.0;
    zcal /= 30.0;
    
    // Calibration assumes that x = 0g, y = 0g, and z = 1g at rest
    zcal -= 1.0;
    
    // offsets are calculated using 0.0156 g/LSB scaling factor
    // and negated because offsets are added for the ADXL345 and values taken earlier are positive
    xoff = -(byte)(xcal / .0156);
    yoff = -(byte)(ycal / .0156);
    zoff = -(byte)(zcal / .0156);
    adxl.setAxisOffset(xoff, yoff, zoff);
}
