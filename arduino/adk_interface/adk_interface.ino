/*
John Sherohman
Team ZapCraft

Some ELM interface code taken from OBDuino project, open source

Hardware interface driver
*/

typedef uint8_t byte; // compatability with broken libraries. Arduino 1.0 is very broken.

#include <stdio.h>
#include <ch9.h>
#include <Max3421e.h>
#include <Max3421e_constants.h>
#include <Max_LCD.h>
#include <Usb.h>
#include <AndroidAccessory.h>
#include <SPI.h>
#include <Wire.h>
#include <ADXL345.h>

#define STRLEN  40
#define NUL     '\0'
#define CR      '\r' // carriage return = 0x0d = 13
#define PROMPT  '>'  // end character from ELM
#define DATA    1    // data with no cr/prompt

// Constants for calculating fuel economy
#define R_AIR 287 // [J/kg/K]
#define ENG_DIS 0.001901   // Engine displacement in m^3
#define VOL_EFF 0.8  // volumetric efficiency of engine, say 80%

int i = 0;

// some globals, for trip calculation and others
unsigned long old_time;
byte has_rpm=0;
float instantfuel=0.0;
byte vss=0;  // speed
long maf=0;  // MAF
long engineRPM=0; // RPM
//unsigned long engine_on, engine_off; //used to track time of trip.\


// Android Accessory stuff
// accessory descriptor. It's how Arduino identifies itself to Android
char applicationName[] = "FuelBehavior"; // the app on your phone
char accessoryName[] = "FuelBehavior"; // your Arduino board
char companyName[] = "Team ZapCraft";

// make up anything you want for these
char versionNumber[] = "1.0";
char serialNumber[] = "1";
char url[] = "google.com"; // the URL of your app online

/********** Function prototypes **********/
// ELM chip interface stuff
void elm_write(char *str);
byte elm_read(char *str, byte size);
int elm_init();
byte elm_check_response(byte *cmd, char *str);
void establishContact();

// OBDII data getters
int get_rpm();
float get_airflow();
int get_throttle();
int get_speed();
int get_iat();
unsigned char get_fuel_status();
boolean uses_maf();
int get_map();

// Fuel consumption accumulator function
void accu_trip();

// initialize ADK as an Android accessory:
AndroidAccessory accessory(companyName, applicationName,
                           accessoryName,versionNumber,url,serialNumber);

// Initialize accelerometer object
ADXL345 accel;

void setup()
{
  // initialize serial ports 0 (serial 1 initialized in ELM function
  Serial.begin(9600);
  // Start USB connection to Android device
  accessory.powerOn();

  // Start connection to accelerometer
  /*
  accel.powerOn();
  delay(500);  // Slight delay for power up
  accel.setRangeSetting(2);  // set range to +-2g
  accel.setRate(100);        // set sampling rate to 100 Hz
  calibrate_accel();
  */

  // Initialize ELM chip
  elm_init();
}

void loop()
{
  char msg[40];
  double x, y, z;

  accu_trip();

  static long timer = millis();

  // sprintf_P(msg,"%f\n",instantfuel);

  if (millis() - timer > 50) { // send 20 times per second
    if(accessory.isConnected()) {
      sprintf(msg, "RPM %d", get_rpm());
      accessory.print(msg);

      sprintf(msg, "MPG %f", instantfuel);
      accessory.println(msg);

      sprintf(msg, "THROTTLE %d", get_throttle());
      accessory.println(msg);

      sprintf(msg, "SPEED %d", get_speed());
      accessory.println(msg);

      /*
      accel.get_Gxyz(&x, &y, &z);
      sprintf(msg, "ACCEL %f, %f, %f", x, y, z);
      accessory.println(msg);
      */
    }
  }
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

// Function to write to ELM device
void elm_write(char *str)
{
  while(*str!=NUL) {
    Serial1.write(*str++);
  }
}

/* each ELM response ends with '\r' followed at the end by the prompt
 so read com port until we find a prompt */
byte elm_read(char *str, byte size)
{
  int b;
  byte i;
  char str2[8];
  // wait for something on com port
  i=0;
  while((b=Serial1.read()) != PROMPT && i<size) {
    if( b>=' ') {
      str[i++]=b;
    }
  }

  if(i!=size) { // we got a prompt
    str[i]=NUL;  // replace CR by NUL
    return PROMPT;
  } else {
    return DATA;
  }
}

// Initialization function to reset ELM chip and check operation
int elm_init()
{
  char str[STRLEN];
  // Begin serial communications
  Serial1.begin(9600);
  // Clear serial input buffer
  Serial1.flush();
  // reset, wait for something and display it
  elm_command(str, PSTR("ATWS\r"));
  // Set starting protocol to ISO 15765-4 CAN
  elm_command(str, PSTR("ATSPA6\r"));
  // Request current protocol
  elm_command(str, PSTR("ATDP\r"));
  // turn echo off
  elm_command(str,PSTR("ATE0\r"));
  // initialize connection with car
  elm_command(str,PSTR("0100\r"));
  return 0;
}

// Function for sending a command to the ELM and reading response
byte elm_command(char *str, char *cmd)
{
  // sprintf_P uses program memory instead of RAM
  sprintf_P(str, cmd);
  elm_write(str);
  return elm_read(str, STRLEN);
}

// Function to remove non-useful data from data string from ELM chip
byte elm_compact_response(byte *buf, char *str)
{
  byte i;
  // start at 6 which is the first hex byte after header
  // ex: "41 0C 1A F8"
  // return buf: 0x1AF8
  i=0;
  str+=6;
  while(*str!=NUL) {
    buf[i++]=strtoul(str, &str, 16);
  }
  return i;
}

// Returns engine RPM
int get_rpm()
{
  char str[STRLEN];
  byte buf[10];
  int ret;
  elm_command(str,PSTR("010C\r"));
  //str = "41 0C 12 F2 "; // for testing
  elm_compact_response(buf,str);
  ret=(buf[0]*256+buf[1])/4;
  return ret;
}

// Returns airflow in grams/sec
float get_airflow()
{
  char str[STRLEN];
  byte buf[10];
  elm_command(str,PSTR("0110\r"));
  elm_compact_response(buf,str);
  return ((float)(buf[0]*256+buf[1]))/100.0;
}

// Returns throttle percentage
int get_throttle()
{
  char str[STRLEN];
  byte buf[10];
  elm_command(str,PSTR("0111\r"));
  elm_compact_response(buf,str);
  return buf[0]*100/255;
}

// Returns speed in kph
int get_speed()
{
  char str[STRLEN];
  byte buf[10];
  elm_command(str,PSTR("010D\r"));
  elm_compact_response(buf,str);
  return buf[0];
}

// Returns manifold absolute pressure in kPA
int get_map()
{
  char str[STRLEN];
  byte buf[10];
  elm_command(str,PSTR("010B\r"));
  elm_compact_response(buf,str);
  return buf[0];
}

// Returns intake air temp in degrees C
int get_iat()
{
  char str[STRLEN];
  byte buf[10];
  elm_command(str,PSTR("010F\r"));
  elm_compact_response(buf,str);
  return buf[0] - 40;
}

// Function to connect to computer
void establishContact()
{
  while (Serial.available() <= 0) {
    Serial.print('A');   // send a capital A
    delay(300);
  }
}

// Function which gets the fuel-metering mode of the car
// closed loop, normal open-loop, open-loop because of
// problems, etc
unsigned char get_fuel_status()
{
  char str[STRLEN];
  byte buf[10];
  elm_command(str,PSTR("0103\r"));
  elm_compact_response(buf,str);
  return buf[0];
}

// Function to determine if a car uses a MAF for fuel metering
// If it doesn't, we will assume that it uses a MAP
boolean uses_maf()
{
  char str[STRLEN];
  byte buf[10];
  elm_command(str,PSTR("0100\r"));
  elm_compact_response(buf,str);
  return (buf[1] & 0x01);
}

// Function to accumulate fuel consumption data
void accu_trip()
{
  static byte min_throttle_pos=255;   // idle throttle position, start high
  byte throttle_pos;   // current throttle position
  byte fuel_status;    // byte-encoded fuel system status
  byte open_loop;
  unsigned long time_now, delta_time;
  unsigned long delta_dist, delta_fuel;
  unsigned long fuel_flowrate;

  // time elapsed
  time_now = millis();
  delta_time = (long)(time_now - old_time); //must take care of rollover
  old_time = time_now;

  // Get distance traveled in this loop in cm
  vss = get_speed();
  delta_dist=((long)vss*delta_time)/36;

  // Check throttle position to see if car is coasting
  throttle_pos = (byte)get_throttle();
  if(throttle_pos<min_throttle_pos && throttle_pos != 0) { //And make sure its not '0' returned by no response in read byte function
    min_throttle_pos=throttle_pos;
  }

  // Check if car is in open loop fuel mode
  fuel_status = get_fuel_status();
  open_loop = (fuel_status & 0x04) ? 1 : 0;

  // check to see if throttle position is within a certain bound of minimum throttle
  // if it is, assume we are coasting
  if(throttle_pos<(min_throttle_pos+4) && open_loop) {
    maf=0;  // decelerate fuel cut-off, fake the MAF as 0
  }

  else {
    if(uses_maf()) {
      maf = get_airflow();
    } else {
      // No MAF found if we end up here
      // Declare Variable Inputs
      long imap, rpm, manp, iat;

      // Get OBD-II Variable Inputs
      // Manifold Ambient Pressure
      manp = get_map()*1000;// [Pa]
      // Revolutions Per Minute
      rpm = get_rpm();// [Rev/min]
      // Intake Ambient Temperature
      iat = get_iat() + 273;  // [K]

      // Calculate Mass Air Flow, uses scaling factor of 0.12
      maf=(rpm*manp*VOL_EFF*ENG_DIS)/(0.12*iat*R_AIR); //[g/sec]

      /*
      No MAF found if we go here
      rpm - engine RPM
      manp - manifold pressure in pascals
      VOL_EFF - volumetric efficiency of engine as a ratio
      ENG_DIS - engine displacement in m^3
      iat - intake air temperature in Kelvin
      R_AIR - ideal gas constant for air
      */

    }
  }

  // Get fuel flow rate in gallons/sec
  fuel_flowrate = maf*0.000092502;// [gallons/sec]

  // Get instantaneous fuel economy in MPG
  instantfuel = (delta_dist)/(fuel_flowrate*delta_time*160.9344);// [mpg]

  /*
  delta_dist - distance traveled in last loop in cm
  fuel_flowrate - current fuel flow rate in gallons/sec
  delta_time - time for last loop in ms
  */
}

