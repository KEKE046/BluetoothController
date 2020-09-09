# BluetoothController

[中文README](README.zh.md)

A simple bluetooth remote controller for bluetooth car.

see app/release to download apk.

*English translation is also available.* This picture is in Chinese because the locale in my phone is set to Chinese.

![](fig.png)

# Quick Start

The application has two big discs. Every time, the program read the x coordinate of the left disc and y coordinate of the right disc. Then concatenate them together, and send through bluetooth serial. 

The message format looks like `-23,45\r\n`. When controlling a car, you can convert it to the speed of two wheels simply.

![](https://render.githubusercontent.com/render/math?math=%5Cbegin%7Bcases%7Dv_%5Ctext%7Bleft%7D%20-%20v_%5Ctext%7Bright%7D%20%3D%202%20x%5C%5Cv_%5Ctext%7Bleft%7D%20%2B%20v_%5Ctext%7Bright%7D%20%3D%202%20y%5Cend%7Bcases%7D)

![](https://render.githubusercontent.com/render/math?math=%5Cbegin%7Bcases%7Dv_%5Ctext%7Bleft%7D%20%3D%20x%20%2B%20y%5C%5Cv_%5Ctext%7Bright%7D%20%3D%20y%20-%20x%5Cend%7Bcases%7D)

If you are use Arduino, you can use this code to drive you bluetooth car:
```c++
void loop() {
    if(Serial.available()) {
        int x = Serial.parseInt();
        int y = Serial.parseInt();
        int left = x + y, right  = y - x;
        drive(left, right); // your car driving function
    }
}
```

The command form, quantification, sending frequency, and control method can be customized is the Settings.


# HC-05 Bluetooth Module Connection
```
AT+CMODE=1          // enable connection by arbitrary device
AT+NAME=KEKE046     // set your bluetooth module name
AT+ROLE=0           // Android phone seems to be always Master Mode
AT+PSWD=123456      // set password
```

First pair with the Bluetooth module in the Android Bluetooth settings, and then connect in the software.

# Fast Command
The L1, L2, R1, R2 buttons at the top can send predefined command conveniently. You can change the default command in the Settings.

# Tricks

Command generation is based on string format.
```
String cmd = String.format(format, x, y);
```

Special format may make programming quick:

`"%+04d, %+04d"` completed the leading zeros, then you can use:```100 * buf[0] + 10 * buf[1] + buf[2]``` to convert string to integers.

`"%2$d, %1$d"` can exchange x and y coordinate, sometimes helps.

`"C%+04d%+04d"` can create a character 'C' in each coordinate message. You can distinguish coordinate message from other command easily.