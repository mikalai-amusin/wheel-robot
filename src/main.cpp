#include <Arduino.h>
#include <Wire.h>
#include <WiFi.h>
#include <SD.h>
#include <SPI.h>
#include <ESP.h>
#include <ArduinoJson.h>
#include <NTPClient.h>

#include "driver/adc.h"
#include <esp_wifi.h>
#include <esp_bt.h>
#include "user-variables.h"
#include <18B20_class.h>
#include <Adafruit_BME280.h>
#include "ESPAsyncWebServer.h"

#include <iostream>
#include <list>
#include <iterator>
using namespace std;

// Logfile on SPIFFS
#include "SPIFFS.h"

// Set your access point network credentials
const char* wifi_name = "K_5";
const char* wifi_password = "v7007410";

// mqtt constants
WiFiClient wifiClient;
AsyncWebServer server(80);

// Reboot counters
RTC_DATA_ATTR int bootCount = 0;
RTC_DATA_ATTR int sleep5no = 0;

//Sensor bools
bool bme_found = false;

//json construct setup
struct Config
{
  String date;
  String time;
  String temp;
  String humid;
  String pressure;
};
Config config;


WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP);
String formattedDate;
String dayStamp;
String timeStamp1;

const int led = 13;

#define I2C_SDA 25
#define I2C_SCL 26
#define DHT_PIN 16
#define BAT_ADC 33
#define SALT_PIN 34
#define SOIL_PIN 32
#define BOOT_PIN 0
#define POWER_CTRL 4
#define USER_BUTTON 35
#define DS18B20_PIN 21

Adafruit_BME280 bmp;     //0x77

#include <file-management.h>
#include <go-to-deep-sleep.h>
#include <connect-to-network.h>

String readTemp() {
  return String(bmp.readTemperature());
  //return String(1.8 * bme.readTemperature() + 32);
}

String readHumi() {
  return String(bmp.readHumidity());
}

String readPres() {
  return String(bmp.readPressure() / 100.0F);
}

StaticJsonDocument<20000> doc;
JsonArray data;

void setup()
{
  // Set the values in the document
  // Device changes according to device placement
  data = doc.to<JsonArray>();
  Serial.begin(115200);
  Serial.println("Void Setup");

  connectToNetwork();
  Serial.println(" ");
  Serial.println("Connected to network");

  Serial.println(WiFi.macAddress());
  Serial.println(WiFi.localIP());

  // timeClient.setTimeOffset(gmtOffset_sec);
  // while (!timeClient.update())
  // {
  //   timeClient.forceUpdate();
  // }

  #include <time-management.h>

  //! Sensor power control pin , use deteced must set high
  pinMode(POWER_CTRL, OUTPUT);
  digitalWrite(POWER_CTRL, 1);
  delay(1000);

  bool wireOk = Wire.begin(I2C_SDA, I2C_SCL); // wire can not be initialized at beginng, the bus is busy
  if (wireOk)
  {
    Serial.println(F("Wire ok"));
    if (logging)
    {
      writeFile(SPIFFS, "/error.log", "Wire Begin OK! \n");
    }
  }
  else
  {
    Serial.println(F("Wire NOK"));
  }

  if (!bmp.begin())
  {
    Serial.println(F("Could not find a valid BMP280 sensor, check wiring!"));
    bme_found = false;
  }
  else
  {
    bme_found = true;
  }

  // Go to sleep
  //Increment boot number and print it every reboot
  ++bootCount;
  Serial.println("Boot number: " + String(bootCount));

  server.on("/temperature", HTTP_GET, [](AsyncWebServerRequest *request){
    request->send_P(200, "text/plain", readTemp().c_str());
  });
  server.on("/humidity", HTTP_GET, [](AsyncWebServerRequest *request){
    request->send_P(200, "text/plain", readHumi().c_str());
  });
  server.on("/pressure", HTTP_GET, [](AsyncWebServerRequest *request){
    request->send_P(200, "text/plain", readPres().c_str());
  });

  server.on("/alive", HTTP_GET, [](AsyncWebServerRequest *request){
    request->send_P(200, "text/plain", "I'm alive");
  });

  server.on("/data", HTTP_GET, [](AsyncWebServerRequest *request){
    
    char buffer[1536];
    serializeJson(doc, buffer);
    serializeJson(doc, Serial);

    data = doc.to<JsonArray>();
    
    request->send_P(200, "application/json", buffer);
  });

  // Start server
  server.begin();
}

void loop()
{
  JsonObject root = data.createNestedObject();

  root["date"] = timeClient.getFormattedDate();
  root["temp"] = readTemp();
  root["humid"] = readHumi();
  root["pressure"] = readPres();

  char buffer[1555];
  serializeJson(doc, buffer);

  delay(30000);
}