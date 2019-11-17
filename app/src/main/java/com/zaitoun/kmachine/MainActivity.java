package com.zaitoun.kmachine;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.Pwm;
import com.zaitoun.kmachine.Controllers.Servo;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final String GPIO_HEAT = "GPIO6_IO14";
    private static final String GPIO_PUMP = "GPIO6_IO15";
    private static final String GPIO_TRIGGER = "GPIO2_IO00";
    private static final String GPIO_ECHO = "GPIO2_IO05";
    private static final String PWM_PIN = "PWM1";

    private static final int INTERVAL_BETWEEN_TRIGGERS = 300;
    private static DecimalFormat df2 = new DecimalFormat("#.##");

    private Button toggle_heat, toggle_pump;
    private TextView distance;
    private Gpio gpio_heat, gpio_pump, gpio_trigger, gpio_echo;

    private boolean heat_state = false;
    private boolean pump_state = false;
    long time1, time2;

    private Handler mCallbackHandler;
    private Handler ultrasonicTriggerHandler;

    private Servo servo;
    int i;
    Timer t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Basically tied java object to xml object, android things
        toggle_heat = findViewById(R.id.heat_toggle);
        toggle_pump = findViewById(R.id.pump_toggle);
        distance = findViewById(R.id.distance);

        // The next couple of lines are used for ultrasound sensor, yet I'm not sure if it's the best way to do it, gotta search more
        // Prepare handler for GPIO callback
        HandlerThread handlerThread = new HandlerThread("callbackHandlerThread");
        handlerThread.start();
        mCallbackHandler = new Handler(handlerThread.getLooper());

        // Prepare handler to send triggers
        HandlerThread triggerHandlerThread = new HandlerThread("triggerHandlerThread");
        triggerHandlerThread.start();
        ultrasonicTriggerHandler = new Handler(triggerHandlerThread.getLooper());


        try {
            PeripheralManager manager = PeripheralManager.getInstance();
            servo = new Servo(PWM_PIN, 50);

            gpio_heat = manager.openGpio(GPIO_HEAT);
            gpio_pump = manager.openGpio(GPIO_PUMP);
            gpio_echo = manager.openGpio(GPIO_ECHO);
            gpio_trigger = manager.openGpio(GPIO_TRIGGER);

            configureInput();
        } catch (IOException e) {
            Log.w("TAG", "Unable to access GPIO", e);
        }

        ultrasonicTriggerHandler.post(triggerRunnable);

        toggle_heat.setOnClickListener(v->toggleIO(gpio_heat));
        toggle_pump.setOnClickListener(b->toggleIO(gpio_pump));

        // The next three lines are not useful for our app, they're just added to test the servo, will be replaced with something useful later
        i = 0;
        t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                NextMove();
            }
        }, 0, 500);

    }
    private void NextMove() {
        switch (i % 2) {
            case 0:
                try {
                    servo.move0Degrees();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 1:
                try {
                    servo.move180Degrees();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
        i++;
    }

    /**
     * Function used to initialize IO pins
     * Don't forget to add whatever pin you add here
     * @throws IOException when wrong IO pin is given, shouldn't happen if we stick to the map
     */
    public void configureInput() throws IOException {

        // Initialize directions for IO pins
        gpio_heat.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
        gpio_pump.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
        gpio_trigger.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        gpio_echo.setDirection(Gpio.DIRECTION_IN);

        // When waiting signal from input port, use callback
        gpio_echo.setEdgeTriggerType(Gpio.EDGE_BOTH);
        gpio_echo.registerGpioCallback(mCallbackHandler, mCallback);

        // Set Active type for IO pins
        gpio_echo.setActiveType(Gpio.ACTIVE_HIGH);
        gpio_heat.setActiveType(Gpio.ACTIVE_LOW);
        gpio_pump.setActiveType(Gpio.ACTIVE_LOW);

        // Set initial value for output IO pins
        gpio_heat.setValue(heat_state);
        gpio_pump.setValue(pump_state);
    }

    /**
     * This function toggles the IO pin, useful for turning on and off pump and heat
     * @param gpio gpio is the general purpose IO you want to toggle
     */
    private void toggleIO(Gpio gpio){
        try {
            gpio.setValue(!gpio.getValue());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //TODO: Figure out how to get accurate reading from ultrasound sensor
    private Runnable triggerRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                readDistanceAsnyc();
                ultrasonicTriggerHandler.postDelayed(triggerRunnable, INTERVAL_BETWEEN_TRIGGERS);
            } catch (IOException e) {
                Log.e("TAG", "Error on PeripheralIO API", e);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    protected void readDistanceAsnyc() throws IOException, InterruptedException {
        // Just to be sure, set the trigger first to false
        gpio_trigger.setValue(false);
        Thread.sleep(0,2000);

        // Hold the trigger pin high for at least 10 us
        gpio_trigger.setValue(true);
        Thread.sleep(0,10000); //10 microsec

        // Reset the trigger pin
        gpio_trigger.setValue(false);

    }

    private GpioCallback mCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            try {

                if (!gpio.getValue()){
                    // The end of the pulse on the ECHO pin

                    time2 = System.nanoTime();

                    long pluseWidth = time2 - time1;
                    Log.d("TAG", "pluseWidth: " + pluseWidth);
                    double distance = (pluseWidth / 1000000000.0 ) * 34300/200;
                    Log.i("TAG", "distance: " + distance + " cm");
                    runOnUiThread(() -> MainActivity.this.distance.setText(df2.format(distance)));

                } else {
                    // The pulse arrived on ECHO pin
                    time1 = System.nanoTime();

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }

        @Override
        public void onGpioError(Gpio gpio, int error) {
            Log.e("TAG", "error: " + error);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (gpio_heat != null && gpio_pump != null) {
            try {
                gpio_heat.close();
                gpio_pump.close();

                gpio_heat = null;
                gpio_pump = null;
            } catch (IOException e) {
                Log.w("TAG", "Unable to close GPIO", e);
            }
        }
    }
}
