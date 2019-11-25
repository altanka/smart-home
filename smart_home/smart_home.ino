const int RES_PINS[] = {4, 5, 6, 7};
char veri;
String command;
int resistors[] = {0, 0, 0, 0};
int intervals[] = {0, 0, 0, 0};
int states[] = {0, 0, 0, 0};
int loadStatus[] = {0, 0, 0, 0};
float loadPower[] = {0, 0, 0, 0};
float startTime = 0;

void setup() {
  Serial.begin(9600);
  for(int i = 0; i < 4; i++){
    pinMode(RES_PINS[i], OUTPUT);
  }
  Serial.println("HC-06 Basladi");
}

void loop() {
  if (Serial.available() > 0) {
    veri = Serial.read();
    if (veri == '?') {
      executeCommand(command);
      command = "";
    } else {
      command += veri;
    }

  }
  //delay(50);
  String stateCommand = "#";
  stateCommand += states[0];
  stateCommand += "#";
  stateCommand += states[1];
  stateCommand += "#";
  stateCommand += states[2];
  stateCommand += "#";
  stateCommand += states[3];

  Serial.println(stateCommand);
  //delay(1);

  String powerCommand = "?";
  powerCommand += loadPower[0];
  powerCommand += "?";
  powerCommand += loadPower[1];
  powerCommand += "?";
  powerCommand += loadPower[2];
  powerCommand += "?";
  powerCommand += loadPower[3];
  //Serial.println(powerCommand);
  
  for(int i = 0; i < 4; i++){
    digitalWrite(RES_PINS[i], !states[i]);
    if(intervals[i] <= millis() - startTime){
      states[i] = 0;
    }
  }
}

void executeCommand(String command) {

  resistors[0] = getValue(command, '#', 0).toInt();
  intervals[0] = getValue(command, '#', 1).toInt();
  states[0] = getValue(command, '#', 2).toInt();

  resistors[1] = getValue(command, '#', 3).toInt();
  intervals[1] = getValue(command, '#', 4).toInt();
  states[1] = getValue(command, '#', 5).toInt();

  resistors[2] = getValue(command, '#', 6).toInt();
  intervals[2] = getValue(command, '#', 7).toInt();
  states[2] = getValue(command, '#', 8).toInt();

  resistors[3] = getValue(command, '#', 9).toInt();
  intervals[3] = getValue(command, '#', 10).toInt();
  states[3] = getValue(command, '#', 11).toInt();

  startTime = millis();

}

String getValue(String data, char separator, int index)
{
    int found = 0;
    int strIndex[] = { 0, -1 };
    int maxIndex = data.length() - 1;

    for (int i = 0; i <= maxIndex && found <= index; i++) {
        if (data.charAt(i) == separator || i == maxIndex) {
            found++;
            strIndex[0] = strIndex[1] + 1;
            strIndex[1] = (i == maxIndex) ? i+1 : i;
        }
    }
    return found > index ? data.substring(strIndex[0], strIndex[1]) : "";
}
