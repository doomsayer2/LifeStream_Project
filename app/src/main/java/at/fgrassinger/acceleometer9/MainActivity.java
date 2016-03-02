package at.fgrassinger.acceleometer9;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.support.v7.widget.Toolbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.util.ArrayList;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import static android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;


public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {

    private static final String TAG = "SensorData";    //Tag to debug the application
    private boolean started = false;                   //Variable to check activation
    private boolean sensorInitialized;

    //Calculation___________________________________________________________________________________
    private static final double force = 9.80665;       //Earth force
    protected final double alpha = 0.8;                //A smaller value means more smoothing Range 0 < a < 1
    private float[] accelVals;                         //Field containing the smoothed values

    private double x, y, z, magnitude;                 //Variables for x,y,z and the magnitude
    private double xOld, yOld, zOld, magnitudeOld;     //Variables for the old sensor values
    private long time;                                 //Variable for the current time or frequency
    private long timeOld;                              //Initial time on the start
    private int timeCount = -1;                        //Counting up the milliseconds to seconds
    private long measuredTime = 0;                     //Time we measure the frequency
    private int frequency = 0;                         //Count the number of pushes in total
    private int frequencyReal = 0;                     //Should count the number of pushes which are right or deep enough

    //Server and Transmission_______________________________________________________________________
    //private String dataS = "";                       //Variable for the sent data
    private String IP = "91.219.68.209";               //Variable for the IP address
    //private String IP = "10.2.1.150";               //Variable for the IP address LOCAL
    private String PORT = "80";                        //Variable for the port
    //private String PORT = "8080";                        //Variable for the port LOCAL
    private SocketIO socket = null;                    //Create the socket object

    private Button btnStart, btnStop;                  //References for the button objects
    private LinearLayout linearLayout;                 //Reference for the linear layout
    private SensorManager sensorManager;               //Reference for the sensor manager
    private ImageView img;                             //Object for the image that is shown in the background

    private AccelManager data;                         //Object which stores the data values
    private ArrayList<AccelManager> sensorData;        //List for the data
    private AccelManager dataCal;                      //Object for the calculation data
    private ArrayList<AccelManager> sensorCalc;        //List for the data calculation


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        linearLayout = (LinearLayout) findViewById(R.id.linLayout); //Get Linear Layout object
        btnStart = (Button) findViewById(R.id.btnStart);    //Get the object for start button
        btnStop = (Button) findViewById(R.id.btnStop);      //Get the object for stop button
        btnStart.setOnClickListener(this);                  //Listener when the button is clicked else make it clickable
        btnStop.setOnClickListener(this);                   //Listener when the button is clicked else make it clickable
        btnStart.setEnabled(true);                          //Enable from default
        btnStop.setEnabled(true);                           //Enable from default

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);     //Creating the toolbar instance from the toolbar object
        setSupportActionBar(toolbar);                               //Set the Toolbar as Actionbar
        getSupportActionBar().setDisplayShowTitleEnabled(false);    //Remove the title from the Toolbar
        toolbar.getBackground().setAlpha(0);                        //Make the background transparent

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); //Get the sensor Service
        sensorData = new ArrayList<>();                     //Create a new ArrayList object for the data points
        sensorCalc = new ArrayList<>();                     //Create a new ArrayList for data calculation
        sensorInitialized = false;                          //Sensor is not started when the app is launched only when START is pressed

        img = (ImageView) findViewById(R.id.imageView);   //Get the image object
        img.setImageResource(R.drawable.push_here);       //Set the image
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (started) {
            sensorManager.unregisterListener(this);         //Disable the senors after closing the app
            sensorInitialized = false;                      //Indicated that the sensor isn't running anymore
        }
    }

    /**
     * Depending on which button is clicked the application reacts.
     *
     * @param v - the View object
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnStart:
                //timeOld = SystemClock.uptimeMillis();   //Save the time when the application starts.
                timeOld = System.currentTimeMillis();
                dataStream();                           //Open the socket connection if possible
                btnStart.setEnabled(false);             //Disable the start button
                btnStop.setEnabled(true);               //Enable the stop button
                sensorData = new ArrayList<>();         //Save the previous data
                started = true;                         //Checking if the app is running
                Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);      //Chose sensor type
                sensorManager.registerListener(this, accel,
                        SensorManager.SENSOR_DELAY_FASTEST);                                   //Activate the sensor

                img.setVisibility(View.VISIBLE);        //Show the image
                break;

            case R.id.btnStop:
                btnStart.setEnabled(true);              //Enable the start button again
                btnStop.setEnabled(false);              //Disable the stop button
                started = false;                        //Checking if the app is stopped
                sensorManager.unregisterListener(this); //Disable the sensor

                //Log.d(TAG, sensorData.toString());
                sensorInitialized = false;
                frequency = 0;
                frequencyReal = 0;
                measuredTime = 0;
                img.setVisibility(View.INVISIBLE);      //Remove the image

                stopPassData();                         //Show the result screen
                break;
            default:
                break;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.connect:
                //Call the method for the input dialog
                optionIPInput();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (started) {
            //double x = event.values[0];             //Save x-Axis values
            //double y = event.values[1];             //Save y-Axis values
            //double z = event.values[2];             //Save z-Axis values
            //--------------------------------------------------------------------------------------

            /*accelVals = lowPass(event.values.clone(), accelVals);         //Apply the low Pass filter to the measured values
            x = accelVals[0];                         //Save x-Axis values
            y = accelVals[1];                         //Save y-Axis values
            z = accelVals[2];                         //Save z-Axis values*/

            //time = SystemClock.uptimeMillis();                              //Returns the System Time since boot
            time = System.currentTimeMillis();

            if(time - timeOld > 5) {                                        //Send the data only every 5 milliseconds or more and do calculations
                long diffTime = (time - timeOld);                           //Store the time difference since last send and actual send
                timeOld = time;                                             //Store new time as previous time
                timeCount++;                                                //Count up to seconds

                x = event.values[0];
                y = event.values[1];
                z = event.values[2];
                magnitude = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));    //Calculation of the magnitude
                //Log.i(TAG, String.valueOf(magnitude));

                double speed = Math.abs(x + y + z - xOld - yOld - zOld) / diffTime * 10000;         //Calculation of force or speed on the phone
                double accelDiff = Math.abs(magnitude - magnitudeOld);                              //Calculation of the magnitude change
                magnitudeOld = magnitude;

                if(!sensorInitialized) {                                    //If the sensor is in use
                    xOld = x;                                               //Last x value is old value
                    yOld = y;                                               //Last y value is old value
                    zOld = z;                                               //Last z value is old value
                    magnitudeOld = magnitude;                               //Last magnitude is old one
                    sensorInitialized = true;
                } else {                                                    //If sensor is already initialized and we have values
                    if(accelDiff > 5)                                      //If value change is greater than specific one
                    {
                        frequency += 1;
                    }
                    if(speed > 700) {                                      //If push was hard enough
                        frequencyReal += 1;
                        if(frequencyReal > 30) {
                            Vibrator mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            mVibrator.vibrate(500);
                            frequencyReal = 0;
                        }
                    }
                    xOld = x;
                    yOld = y;
                    zOld = z;

                    //This clause is used to count the seconds that passed.
                    //As we send every 5ms or greater we count each time so 200 X 5ms is one second
                    //ATTENTION: this has to be changed if the send time gets increased or decreased
                    if (timeCount == 200) {                              //After 1 second
                        measuredTime += 1;
                        timeCount = 0;
                    } else if(measuredTime == 10) {                      //After 10 seconds reset the timer
                        measuredTime = 0;
                    }

                    //Save the data for the transmission to the result screen
                    //-----------------------------------------------------------------------------------
                    data = new AccelManager(time, x, y, z, magnitude);    //Call the constructor to make object
                    sensorData.add(data);                                 //Add the data to the ArrayList

                    //Here the data gets sent over the socket to the server
                    //------------------------------------------------------------------------------
                    try {
                        socket.emit("SensorData", new JSONObject().put("time", time).put("magnitude", magnitude));
                        socket.emit("AdditionalInformation", new JSONObject().put("freq", frequency).put("freqR", frequencyReal).put("timeMeasure", measuredTime));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    //Handling the Socket connection and Data transfer
    //----------------------------------------------------------------------------------------------
    /**
     * This function should start a stream or open a socket in order to send the data to the server.
     */
    private void dataStream() {
        try {
            String address = "http://" + IP + ":" + PORT;
            socket = new SocketIO(address);
            socket.connect(new IOCallback() {
                @Override
                public void onDisconnect() {
                    System.out.println("Connection disabled.");
                }

                @Override
                public void onConnect() {
                    System.out.println("Connection established.");
                    try {
                        socket.emit("storeClientInfo", new JSONObject().put("customId", "Android_Client"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onMessage(String s, IOAcknowledge ioAcknowledge) {
                    System.out.println("Server said: " + s);
                }

                @Override
                public void onMessage(JSONObject jsonObject, IOAcknowledge ioAcknowledge) {
                    try {
                        System.out.println("Server said: " + jsonObject.toString(2));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void on(String s, IOAcknowledge ioAcknowledge, Object... objects) {
                    System.out.println("Server triggered event '" + s + "'");
                }

                @Override
                public void onError(SocketIOException e) {
                    System.out.println("An error occured.");
                    e.printStackTrace();
                }
            });
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * This Method is used in order to pass the sensor data to a separate Result activity
     */
    public void stopPassData() {
        Intent passResultIntent = new Intent(this, ResultViewActivity.class);       //The intent to the second class
        final int result = 1;       //The return variable for the Intent with return
        passResultIntent.putExtra("sensorValues", new AccelManagerWrapper(sensorData));

        startActivityForResult(passResultIntent, result);                           //Start the Activity and wait for callback
    }


    //React on menu events and custom buttons
    //----------------------------------------------------------------------------------------------
    /**
     * This function is used when specific buttons are pressed in order to block their usage or
     * block the accidental close of the application
     */
    public void buttonsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("CLOSE APPLICATION?");               //Set a title for the dialog
        alertDialog.setMessage("Are you sure you want to close the application?");  //Set a message
        alertDialog.setIcon(R.drawable.alert);

        //Create the Yes and No button for the dialog
        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();                         //Show the alert dialog
    }

    /**
     * This function is used in order to get the IP address which the user has to add. Currently
     * only available till a static IP will be generated.
     */
    public void optionIPInput() {
        final EditText input = new EditText(MainActivity.this);

        new AlertDialog.Builder(MainActivity.this)
                .setTitle("IP CONFIGURATION")
                .setMessage("Enter a valid IP Address of the server")
                .setIcon(R.drawable.ip_adress)
                .setView(input)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        IP = String.valueOf(input.getText());
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }

    /**
     * This function is used in order to override the back button with a custom event.
     */
    @Override
    public void onBackPressed() {
        buttonsAlert();
    }


    //Helper functions and other stuff
    //----------------------------------------------------------------------------------------------
    /**
     * This function is used in order to simulate a full screen mode. The user has no acces to the
     * notification bar or the buttons depending on the device.
     * @param hasFocus Checks if window is focused
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            findViewById(R.id.mainLayout).setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }

    /**
     * This method waits for the result of the started activity in order to execute. Here it will
     * simply will wait for the answer in order to check if the BACK worked from the other activity.
     * @param requestCode Who asked for the intent
     * @param resultCode Who responded
     * @param data Whats the data result
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        int result = data.getIntExtra("finishedView", 1);
        if(result == -1) {
            String resultS = "The intent worked as intended.";
        }
        else {
            String resultW = "The intent didn't work as intended.";
        }
    }
}