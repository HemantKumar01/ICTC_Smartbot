const byte numChars = 32;
char receivedChars[numChars]; 

boolean newData = false;

void setup() {
  // initialize serial communication at 9600 bits per second:
   pinMode(LED_BUILTIN, OUTPUT);
  Serial.begin(9600);
}
void loop() {
    recvWithEndMarker();
    showNewData();
}

void recvWithEndMarker() {
    static byte ndx = 0;
    char endMarker = '\n';
    char rc;
    
    while (Serial.available() > 0 && newData == false) {
        rc = Serial.read();

        if (rc != endMarker) {
            receivedChars[ndx] = rc;
            ndx++;
            if (ndx >= numChars) {
                ndx = numChars - 1;
            }
        }
        else {
            receivedChars[ndx] = '\0'; // terminate the string
            ndx = 0;
            newData = true;
        }
    }
}
void showNewData() {
    if (newData == true) {
        Serial.print("Recieved: ");
        Serial.println(receivedChars);
        int num = atoi(receivedChars);
        if(num > 0){
          digitalWrite(LED_BUILTIN, HIGH);
        }else{
          digitalWrite(LED_BUILTIN, LOW);
        }
        newData = false;
    }
}