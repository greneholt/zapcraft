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

#define STRLEN  40
#define NUL     '\0'
#define CR      '\r' // carriage return = 0x0d = 13
#define PROMPT  '>'  // end character from ELM
#define DATA    1    // data with no cr/prompt
#define ENG_DIS 19   // Engine displacement in deciliters, here 1.9l

// [CONFIRMED] For gas car use 3355 (1/14.7/730*3600)*10000 (from OBDuino project)
#define GasConst 3355 
#define GasMafConst 107310 // 14.7*730*10
#define FuelAdjust 100
#define InstantConst 0.6533737  // 3785411/(3600*1000*1.609344)

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

char initialize[] = "ATSP0";

// Function prototypes

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

void setup() {
  // initialize serial ports 0 (serial 1 initialized in ELM function
  //Serial.begin(9600);
  // Start USB connection to Android device
  accessory.powerOn();
  // Initialize ELM chip
  elm_init();
}

void loop()
{
  static uint8_t k = 0;
  static long timer = millis();
  
  if(millis()-timer>100) { // sending 10 times per second
    if (accessory.isConnected()) { // isConnected makes sure the USB connection is open
      int rpm = get_rpm();
      uint8_t msg[3];
      msg[0] = 0x1;
      decompose_int(rpm, msg[1], msg[2], msg[3]);
      accessory.write(msg, 3);
    }
    timer = millis();
  }
}

void decompose_int(int val, uint8_t &high, uint8_t &low, uint8_t &checksum)
{
  high = val >> 8;
  low = val & 0xff;
  checksum = high ^ low;
}

// Function to write to ELM device
void elm_write(char *str)
{
  while(*str!=NUL)
    Serial1.write(*str++);
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
  while((b=Serial1.read())!=PROMPT && i<size)
	if( b>=' ')
		str[i++]=b;
  if(i!=size)  // we got a prompt
  {
    str[i]=NUL;  // replace CR by NUL
    return PROMPT;
  }
  else
    return DATA;
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
  while(*str!=NUL)
    buf[i++]=strtoul(str, &str, 16);
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
void establishContact() {
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
  
  // time elapsed
  time_now = millis();
  delta_time = (long)(time_now - old_time); //must take care of rollover
  old_time = time_now;

  // Get distance traveled in this loop in cm
  vss = get_speed();  
  delta_dist=((long)vss*delta_time)/36;

  // Check throttle position to see if car is coasting
  throttle_pos = (byte)get_throttle();
  if(throttle_pos<min_throttle_pos && throttle_pos != 0) //And make sure its not '0' returned by no response in read byte function
      min_throttle_pos=throttle_pos;
  
  // Check if car is in open loop fuel mode
  fuel_status = get_fuel_status();    
  open_loop = (fuel_status & 0x04) ? 1 : 0;
  
  if(throttle_pos<(min_throttle_pos+4) && open_loop)
  {
    //clear_icons_tmaf();
    maf=0;  // decelerate fuel cut-off, fake the MAF as 0
  }
  
  if(uses_maf()){
     maf = get_airflow();
  }
  else{
     /*
     No MAF found if we go here

     No MAF (Uses MAP and Absolute Temp to approximate MAF):
     IMAP = RPM * MAP / IAT
     MAF = (IMAP/120)*(VE/100)*(ED)*(MM)/(R)
     MAP - Manifold Absolute Pressure in kPa
     IAT - Intake Air Temperature in Kelvin
     R - Specific Gas Constant (8.314472 J/(mol.K)
     MM - Average molecular mass of air (28.9644 g/mol)
     VE - volumetric efficiency measured in percent, let's say 80%
     ED - Engine Displacement in liters
     This method requires tweaking of the VE for accuracy.
     */
     
     long imap, rpm, manp, iat;
     
     manp = get_map();
     rpm = get_rpm();
     iat = get_iat() + 273;  // In Kelvin
     
     imap=(rpm*manp)/(iat+273);
     // does not divide by 100 at the end because we use (MAF*100) in formula
     // but divide by 10 because engine displacement is in dL
     // imap * VE * ED * MM / (120 * 100 * R * 10) = 0.0020321
     // ex: VSS=80km/h, MAP=64kPa, RPM=1800, IAT=21C
     //     engine=2.2L, efficiency=70%
     // maf = ( (1800*64)/(21+273) * 22 * 20 ) / 100
     // maf = 17.24 g/s which is about right at 80km/h
     maf=(imap*ENG_DIS)/5;
  }
  
  // we want fuel used in uL
  // maf gives grams of air/s
  // divide by 100 because our MAF return is not divided!
  // divide by 14.7 (a/f ratio) to have grams of fuel/s
  // divide by 730 to have L/s
  // mul by 1000000 to have uL/s
  // divide by 1000 because delta_time is in ms
  delta_fuel=(maf*FuelAdjust*delta_time) / GasMafConst;
  
  // test function for instantaneous fuel economy
  // vss is in km/hour, delta_time is in ms, delta_fuel is in uL
  // divide by 1000 because delta_time is in ms, divide by 3600
  // because vss is in km/hour, divide by 1.609344 km/mi,
  // multiply by 3785411 microliters per gallon
  
  instantfuel = (vss*delta_time*InstantConst) / delta_fuel;
}

