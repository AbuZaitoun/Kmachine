package com.zaitoun.kmachine.Controllers;

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.Pwm;

import java.io.IOException;

public class Servo {
    private Pwm pwm;

    public Servo(String pin, int frequency){
        PeripheralManager manager = PeripheralManager.getInstance();

        try {
            pwm = manager.openPwm(pin);
            pwm.setPwmFrequencyHz(frequency);
            pwm.setEnabled(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void move180Degrees() throws IOException {
        pwm.setPwmDutyCycle(12.5);
    }

    public void move0Degrees() throws IOException{
        pwm.setPwmDutyCycle(2.5);
    }
}
