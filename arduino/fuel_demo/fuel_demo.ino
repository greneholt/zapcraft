/*******************************************************************************
 * Copyright (C) 2012 Team ZapCraft, Colorado School of Mines
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/

/*
Demo hardware interface driver. Continuously sends a series of test values over
serial, simulating how the device would work in a car.
*/

typedef uint8_t byte; // compatability with broken libraries. Arduino 1.0 is very broken.

#include <stdio.h>
#include <ch9.h>
#include <Max_LCD.h>
#include <Usb.h>
#include <SPI.h>
#include <Wire.h>
#include <ADXL345.h>

// OBDII data getters
unsigned int get_rpm();
unsigned int get_throttle();
unsigned int get_speed();
unsigned int get_map();
unsigned int get_iat();
float get_mpg();
float get_airflow();

// Outputs a serial msg with a checksum appended. Assumes that msg is large enough to contain the checksum.
void checksum_println(char* msg);

// Initialize accelerometer object
ADXL345 accel;

#define DEMO_LENGTH 46;
uint8_t demo[] = { 10, 11, 13, 15, 15, 15, 18, 20, 20, 19, 18, 20, 22, 23, 24, 25, 27, 23, 18, 16, 16, 15, 12, 13, 12, 11, 10, 7, 5, 5, 4, 5, 7, 9, 9, 5, 4, 2, 0, 0, 0, 0, 0, 0, 0, 0};

void setup()
{
  // initialize serial ports 0 (serial 1 initialized in ELM function
  Serial.begin(115200);

  // Start connection to accelerometer

  accel.powerOn();
  delay(500);  // Slight delay for power up
  accel.setRangeSetting(2);	// set range to +-2g
  accel.setRate(100);      	// set sampling rate to 100 Hz
  calibrate_accel();		// Run accelerometer calibration routine
}

void loop()
{
  char tempfloat[10];
  char tmpx[10];
  char tmpy[10];
  char tmpz[10];
  char msg[100];
  double x, y, z;

  static long timer = millis();

  // sprintf_P(msg,"%f\n",instantfuel);

  if (millis() - timer > 200) { // send 20 times per second
    sprintf(msg, "RPM %d", get_rpm());
    checksum_println(msg);

    dtostrf(get_mpg(),6,5,tempfloat);
    sprintf(msg, "MPG %s", tempfloat);
    checksum_println(msg);

    sprintf(msg, "MAP %d", get_map());
    checksum_println(msg);

    sprintf(msg, "TEMP %d", get_iat());
    checksum_println(msg);

    sprintf(msg, "THROTTLE %d", get_throttle());
    checksum_println(msg);

    sprintf(msg, "SPEED %d", get_speed());
    checksum_println(msg);

    dtostrf(get_airflow(),6,5,tempfloat);
    sprintf(msg, "MAF %s", tempfloat);
    checksum_println(msg);

    accel.get_Gxyz(&x, &y, &z);
    dtostrf(x,6,5,tmpx);	// dtostrf used instead of sprintf because sprintf not fully implemented in Arduino language
    dtostrf(y,6,5,tmpy);
    dtostrf(z,6,5,tmpz);
    sprintf(msg, "ACCEL %s %s %s", tmpx, tmpy, tmpz);
    checksum_println(msg);

    timer = millis();
  }
}

void checksum_println(char* msg) {
  char checksum[8];
  int i = 0;
  uint8_t crc = 0;

  while (msg[i] != '\0') {
    crc ^= msg[i++];
  }

  sprintf(checksum, "*%02X", crc);
  strcat(msg, checksum);

  Serial.println(msg);
}

void calibrate_accel()
{
  double xcal = 0.0;
  double ycal = 0.0;
  double zcal = 0.0;
  double xtest, ytest, ztest;
  char xoff, yoff, zoff;

  // Grab 30 samples from each axis and average them to remove offset
  for(int j = 0; j < 30; j++) {
    accel.get_Gxyz(&xtest, &ytest, &ztest);
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
  accel.setAxisOffset(xoff, yoff, zoff);
}

float get_mpg()
{
  static int i = 0;
  i = (i + 1) % DEMO_LENGTH;
  return demo[i];
}

float get_airflow()
{
  static int i = 0;
  i = (i + 1) % DEMO_LENGTH;
  return demo[i];
}

// Returns engine RPM
unsigned int get_rpm()
{
  static int i = 0;
  i = (i + 1) % DEMO_LENGTH;
  return demo[i]*136;
}

// Returns throttle percentage
unsigned int get_throttle()
{
  static int i = 10;
  i = (i + 1) % DEMO_LENGTH;
  return demo[i]*2;
}

// Returns speed in kph
unsigned int get_speed()
{
  static int i = 5;
  i = (i + 1) % DEMO_LENGTH;
  return demo[i]*8/3;
}

unsigned int get_map()
{
  static int i = 0;
  i = (i + 1) % DEMO_LENGTH;
  return demo[i];
}

unsigned int get_iat()
{
  static int i = 0;
  i = (i + 1) % DEMO_LENGTH;
  return demo[i];
}
