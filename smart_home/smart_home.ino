const int LED_PIN = 9;
const int SERVO_PIN = 10;
const int X_PIN = 11;
const int Y_PIN = 12;

char veri;
String command;
int loadStatus[] = {0, 0, 0, 0};
float loadPower[] = {0, 0, 0, 0};

void setup() {
  Serial.begin(9600);
  pinMode(LED_PIN, OUTPUT);
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
  delay(50);
  String statusCommand = "#";
  statusCommand += loadStatus[0];
  statusCommand += "#";
  statusCommand += loadStatus[1];
  statusCommand += "#";
  statusCommand += loadStatus[2];
  statusCommand += "#";
  statusCommand += loadStatus[3];

  Serial.println(statusCommand);
  delay(50);

  String powerCommand = "?";
  powerCommand += loadPower[0];
  powerCommand += "?";
  powerCommand += loadPower[1];
  powerCommand += "?";
  powerCommand += loadPower[2];
  powerCommand += "?";
  powerCommand += loadPower[3];
  Serial.println(powerCommand);
}

void executeCommand(String command) {
  if (command == "kapat1") {
    digitalWrite(LED_PIN, LOW);
    loadStatus[0] = 0;
  }
  if (command == "kapat2") {
    digitalWrite(SERVO_PIN, LOW);
    loadStatus[1] = 0;
  }
  if (command == "kapat3") {
    digitalWrite(X_PIN, LOW);
    loadStatus[2] = 0;
  }
  if (command == "kapat4") {
    digitalWrite(Y_PIN, LOW);
    loadStatus[3] = 0;
  }

  if (command == "ac1") {
    digitalWrite(LED_PIN, HIGH);
    loadStatus[0] = 1;
  }
  if (command == "ac2") {
    digitalWrite(SERVO_PIN, HIGH);
    loadStatus[1] = 1;
  }
  if (command == "ac3") {
    digitalWrite(X_PIN, HIGH);
    loadStatus[2] = 1;
  }
  if (command == "ac4") {
    digitalWrite(Y_PIN, HIGH);
    loadStatus[3] = 1;
  }

}
