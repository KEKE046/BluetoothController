# BluetoothController

A simple bluetooth remote controller for bluetooth car.

see app/release to download apk.

*English translation is also available.* This picture is in Chinese because the locale in my phone is set to Chinese.

![](fig.png)

# Quick Start

The application has two big discs. Every time, the program read the x coordinate of the left disc and y coordinate of the right disc. Then concatenate them together, and send through bluetooth serial. 

The message format looks like `-23,45\r\n`. When controlling a car, you can convert it to the speed of two wheels simply.
$$
\begin{cases}
v_\text{left} - v_\text{right} = 2 x\\
v_\text{left} + v_\text{right} = 2 y
\end{cases}
\quad
\Rightarrow
\quad
\begin{cases}
v_\text{left} = x + y\\
v_\text{right} = y - x
\end{cases}
$$

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

