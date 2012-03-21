/*
John Sherohman
Team ZapCraft

Some ELM interface code taken from OBDuino project, open source

Hardware interface driver
*/

#include <ch9.h>
#include <Max3421e.h>
#include <Max3421e_constants.h>
#include <Max_LCD.h>
#include <Usb.h>
#include <AndroidAccessory.h>
#include <SPI.h>
#include <stdio.h>

#define STRLEN  40
#define NUL     '\0'
#define CR      '\r' // carriage return = 0x0d = 13
#define PROMPT  '>'  // end character from ELM
#define DATA    1    // data with no cr/prompt

#define PID_SUPPORT00 0x00
#define MIL_CODE      0x01
#define FREEZE_DTC    0x02
#define FUEL_STATUS   0x03
#define LOAD_VALUE    0x04
#define COOLANT_TEMP  0x05
#define STFT_BANK1    0x06
#define LTFT_BANK1    0x07
#define STFT_BANK2    0x08
#define LTFT_BANK2    0x09
#define FUEL_PRESSURE 0x0A
#define MAN_PRESSURE  0x0B
#define ENGINE_RPM    0x0C
#define VEHICLE_SPEED 0x0D
#define TIMING_ADV    0x0E
#define INT_AIR_TEMP  0x0F
#define MAF_AIR_FLOW  0x10
#define THROTTLE_POS  0x11
#define SEC_AIR_STAT  0x12
#define OXY_SENSORS1  0x13
#define B1S1_O2_V     0x14
#define B1S2_O2_V     0x15
#define B1S3_O2_V     0x16
#define B1S4_O2_V     0x17
#define B2S1_O2_V     0x18
#define B2S2_O2_V     0x19
#define B2S3_O2_V     0x1A
#define B2S4_O2_V     0x1B
#define OBD_STD       0x1C
#define OXY_SENSORS2  0x1D
#define AUX_INPUT     0x1E
#define RUNTIME_START 0x1F
#define PID_SUPPORT20 0x20
#define DIST_MIL_ON   0x21
#define FUEL_RAIL_P   0x22
#define FUEL_RAIL_DIESEL 0x23
#define O2S1_WR_V     0x24
#define O2S2_WR_V     0x25
#define O2S3_WR_V     0x26
#define O2S4_WR_V     0x27
#define O2S5_WR_V     0x28
#define O2S6_WR_V     0x29
#define O2S7_WR_V     0x2A
#define O2S8_WR_V     0x2B
#define EGR           0x2C
#define EGR_ERROR     0x2D
#define EVAP_PURGE    0x2E
#define FUEL_LEVEL    0x2F
#define WARM_UPS      0x30
#define DIST_MIL_CLR  0x31
#define EVAP_PRESSURE 0x32
#define BARO_PRESSURE 0x33
#define O2S1_WR_C     0x34
#define O2S2_WR_C     0x35
#define O2S3_WR_C     0x36
#define O2S4_WR_C     0x37
#define O2S5_WR_C     0x38
#define O2S6_WR_C     0x39
#define O2S7_WR_C     0x3A
#define O2S8_WR_C     0x3B
#define CAT_TEMP_B1S1 0x3C
#define CAT_TEMP_B2S1 0x3D
#define CAT_TEMP_B1S2 0x3E
#define CAT_TEMP_B2S2 0x3F
#define PID_SUPPORT40 0x40
#define MONITOR_STAT  0x41
#define CTRL_MOD_V    0x42
#define ABS_LOAD_VAL  0x43
#define CMD_EQUIV_R   0x44
#define REL_THR_POS   0x45
#define AMBIENT_TEMP  0x46
#define ABS_THR_POS_B 0x47
#define ABS_THR_POS_C 0x48
#define ACCEL_PEDAL_D 0x49
#define ACCEL_PEDAL_E 0x4A
#define ACCEL_PEDAL_F 0x4B
#define CMD_THR_ACTU  0x4C
#define TIME_MIL_ON   0x4D
#define TIME_MIL_CLR  0x4E
//
//
#define FUEL_TYPE     0x51
#define ETHYL_FUEL    0x52

#define LAST_PID      0x52  // same as the last one defined above


int incomingByte = 0;
char datastring[20];
int i = 0;

// some globals, for trip calculation and others
unsigned long old_time;
byte has_rpm=0;
long vss=0;  // speed
long maf=0;  // MAF
long engineRPM=0; // RPM
unsigned long engine_on, engine_off; //used to track time of trip.

char initialize[] = "ATSP0";

// Function prototypes
void elm_write(char *str);
byte elm_read(char *str, byte size);
int elm_init();
byte elm_check_response(byte *cmd, char *str);
int get_rpm();
void establishContact();
float get_airflow();
int get_throttle();
int get_speed();

void setup() {
  // initialize serial ports 0 and 1:
  Serial.begin(9600);
  //Serial1.begin(9600);  
  //Serial.println("Initializing...");
  
  // Initialize ELM chip
  elm_init();
  
  // Print an 'A' to let computer know that arduino is ready
  //Serial.println("A");
  //establishContact();
}

void loop()
{
  int rpm = 0;
  int throttle = 0;
  float airflow = 0.0;
  if(Serial.available() > 0){
    switch (Serial.read()){
      case 'R':
        rpm = get_rpm();
        //rpm = 1200;  // Test value
        Serial.print(rpm);
        //Serial.write(rpm);
        Serial.write(' ');
        break;
        
      case 'M':
        airflow = get_airflow();
        Serial.print(airflow);
        Serial.write(' ');
        break;
        
      case 'T':
        throttle = get_throttle();
        //throttle = 30;
        Serial.print(throttle);
        Serial.write(' ');
        break;
    }
  }
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
	if(/*b!=-1 &&*/ b>=' ')
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
  // sprintf_P uses program memory instead of flash
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

// Function to grab RPM data from a vehicle and convert to meaningful numbers
int get_rpm()
{
  char str[STRLEN];
  byte buf[10];
  int ret;
/* Sample output:
41 0C 13 24 
19
36
41 0C 13 24 
19
36
41 0C 12 F2 
18
242
41 0C 13 24 
19
36
41 0C 12 F2 
18
242
  */
  elm_command(str,PSTR("010C\r"));
  //str = "41 0C 12 F2 "
  elm_compact_response(buf,str);
  ret=(buf[0]*256+buf[1])/4;
  return ret;
}

float get_airflow()
{
  char str[STRLEN];
  byte buf[10];
  elm_command(str,PSTR("0110\r"));
  elm_compact_response(buf,str);
  return ((float)(buf[0]*256+buf[1]))/100.0;
}

int get_throttle()
{
  char str[STRLEN];
  byte buf[10];
  elm_command(str,PSTR("0111\r"));
  elm_compact_response(buf,str);
  return buf[0]*100/255;
}

int get_speed()
{
  char str[STRLEN];
  byte buf[10];
  elm_command(str,PSTR("010D\r"));
  elm_compact_response(buf,str);
  return buf[0];
}

// Function to connect to computer
void establishContact() {
  while (Serial.available() <= 0) {
    Serial.print('A');   // send a capital A
    delay(300);
  }
}

