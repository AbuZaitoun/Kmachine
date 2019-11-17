package com.zaitoun.kmachine.Controllers;

import android.os.Handler;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;

public class GPIO implements Gpio {
    private Gpio gpio;

    public GPIO(String pin){
        PeripheralManager manager = PeripheralManager.getInstance();
        try {
            gpio = manager.openGpio(pin);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setUp(int direction){

    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void setDirection(int i) throws IOException {

    }

    @Override
    public void setEdgeTriggerType(int i) throws IOException {

    }

    @Override
    public void setActiveType(int i) throws IOException {

    }

    @Override
    public void setValue(boolean b) throws IOException {

    }

    @Override
    public boolean getValue() throws IOException {
        return false;
    }

    @Override
    public void registerGpioCallback(Handler handler, GpioCallback gpioCallback) throws IOException {

    }

    @Override
    public void unregisterGpioCallback(GpioCallback gpioCallback) {

    }
}
