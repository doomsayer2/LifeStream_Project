package at.fgrassinger.acceleometer9;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import static android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;


public class MainActivity extends Activity implements SensorEventListener, View.OnClickListener {

    private static final String TAG = "SensorData";    //Tag to debug the application
    private static final double force = 9.80665;       //Earth force
    private boolean started = false;                   //Variable to check activation
    private boolean graph = false;                     //Variable to clear the graph on start

    private String dataS = "";                         //Variable for the sent data
    private String IP = "91.219.68.209";               //Variable for the IP address
    private String PORT = "80";                        //Variable for the port
    private SocketIO socket = null;                    //Create the socket object

    private Button btnStart, btnStop, btnSave;         //References for the button objects
    private LinearLayout linearLayout;                 //Reference for the linear layout
    private SensorManager sensorManager;               //Reference for the sensor manager
    private ArrayList<AccelManager> sensorData;        //List for the data
    private View mChart;                               //View for the plotted data
    private ImageView img;                             //Variable for the image
    private AccelManager data;                         //Object which stores the data values

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = (Button) findViewById(R.id.btnStart);    //Get the object for start button
        btnStop = (Button) findViewById(R.id.btnStop);      //Get the object for stop button
        btnSave = (Button) findViewById(R.id.btnSave);      //Get the object for save button
        btnStart.setOnClickListener(this);                  //Listener when the button is clicked else make it clickable
        btnStop.setOnClickListener(this);                   //Listener when the button is clicked else make it clickable
        btnSave.setOnClickListener(this);                   //Listener when the button is clicked else make it clickable
        btnStart.setEnabled(true);                          //Enable from default
        btnStop.setEnabled(true);                           //Enable from default

        linearLayout = (LinearLayout) findViewById(R.id.linLayout); //Get Linear Layout object
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); //Get the sensor Service
        sensorData = new ArrayList<>();                       //Create a new ArrayList object for the data points
        if (sensorData.isEmpty() || sensorData.size() == 0) {
            btnSave.setEnabled(false);                      //Disable the button if no data is there
        }

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
                dataStream();                           //Open the socket connection if possible
                btnStart.setEnabled(false);             //Disable the start button
                //btnStop.setEnabled(false);
                btnStop.setEnabled(true);               //Enable the stop button
                btnSave.setEnabled(false);              //Disable the save button meanwhile
                sensorData = new ArrayList<>();         //Save the previous data
                started = true;                         //Checking if the app is running
                Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);      //Chose sensor type
                sensorManager.registerListener(this, accel,
                        SensorManager.SENSOR_DELAY_FASTEST);                                   //Activate the sensor

                img.setVisibility(View.VISIBLE);        //Show the image

                if(graph) {                             //If there is a previous graph remove it
                    linearLayout.removeAllViews();
                    graph = false;
                }
                break;

            case R.id.btnStop:
                btnStart.setEnabled(true);              //Enable the start button again
                btnStop.setEnabled(false);              //Disable the stop button
                btnSave.setEnabled(true);               //Enable the save button
                started = false;                        //Checking if the app is stopped
                sensorManager.unregisterListener(this); //Disable the sensor
                linearLayout.removeAllViews();          //Clear the chart
                openChart();                            //Open the drawing function
                Log.d(TAG, sensorData.toString());
                img.setVisibility(View.INVISIBLE);      //Remove the image
                break;

            case R.id.btnSave:
                //Save the data to server

                //Save the data to phone
                //----------------------------------------------------------------------------------s
                File file;                             //Object for the Filename
                FileOutputStream outputStream;         //Object for the stream

                try {
                    file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Accelerometer.txt");
                    outputStream = new FileOutputStream(file);
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file, true /*append*/));

                    writer.write("Time;x;y;z;Magnitude");
                    writer.write("\r\n");
                    for(AccelManager mang : sensorData) {
                        writer.write(mang.convertData());
                        writer.write("\r\n");
                    }
                    //outputStream.write(data.getBytes());
                    outputStream.close();
                    Toast.makeText(getBaseContext(), "Data saved to phone: " + file.toString(), Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //In order to save the graph image, a bitmap is created, which will get the plotted
                //graph and saves it also to the phone in .jpg format with a compression.
                Bitmap bitmap1;
                mChart.setDrawingCacheEnabled(true);
                bitmap1 = Bitmap.createBitmap(mChart.getDrawingCache());
                mChart.setDrawingCacheEnabled(false);

                File file2;
                FileOutputStream outputStream1;
                file2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Accelerometer.jpg");
                try {
                    outputStream1 = new FileOutputStream(file2);
                    bitmap1.compress(Bitmap.CompressFormat.JPEG, 90, outputStream1);
                    outputStream1.flush();
                    outputStream1.close();
                    Toast.makeText(getBaseContext(), "Image saved to phone: " + file2.toString(), Toast.LENGTH_LONG).show();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
            final double alpha = 0.8;          //Alpha is calculated as t / (t + dT)
            //With t, the low-pass filter's time-constant and dT, the event delivery rate
            double[] gravity = {0,0,0};
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
            double x = event.values[0] - gravity[0];        //Acceleration in x-Axis without Force
            double y = event.values[1] - gravity[1];        //Acceleration in y-Axis without Force
            double z = event.values[2] - gravity[2];        //Acceleration in z-Axis without Force

            double magnitude = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));    //Calculation of the magnitude
            //Log.i(TAG, String.valueOf(magnitude));

            long time = System.currentTimeMillis();               //Get the current time
            data = new AccelManager(time, x, y, z, magnitude);    //Call the constructor to make object
            sensorData.add(data);                                 //Add the data to the ArrayList

            //Here the data gets sent over the socket to the server
            try {
                socket.emit("SensorData", new JSONObject().put("time", time).put("magnitude", magnitude));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * Function to draw the Sensor data in the Renderer Object
     */
    private void openChart() {
        if (sensorData != null || sensorData.size() > 0) {
            long t = sensorData.get(0).getTime();
            XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();        //Object for the chart

            //Adding the data points to the chart axis
            XYSeries xSeries = new XYSeries("X");
            XYSeries ySeries = new XYSeries("Y");
            XYSeries zSeries = new XYSeries("Z");

            for (AccelManager data : sensorData) {
                xSeries.add(data.getTime() - t, data.getX());
                ySeries.add(data.getTime() - t, data.getY());
                zSeries.add(data.getTime() - t, data.getZ());
            }

            dataset.addSeries(xSeries);
            dataset.addSeries(ySeries);
            dataset.addSeries(zSeries);

            //Change the data points and lines
            XYSeriesRenderer xRenderer = new XYSeriesRenderer();
            xRenderer.setColor(Color.RED);
            xRenderer.setPointStyle(PointStyle.CIRCLE);
            xRenderer.setFillPoints(true);
            xRenderer.setLineWidth(1);
            xRenderer.setDisplayChartValues(false);

            XYSeriesRenderer yRenderer = new XYSeriesRenderer();
            yRenderer.setColor(Color.GREEN);
            yRenderer.setPointStyle(PointStyle.CIRCLE);
            yRenderer.setFillPoints(true);
            yRenderer.setLineWidth(1);
            yRenderer.setDisplayChartValues(false);

            XYSeriesRenderer zRenderer = new XYSeriesRenderer();
            zRenderer.setColor(Color.BLUE);
            zRenderer.setPointStyle(PointStyle.CIRCLE);
            zRenderer.setFillPoints(true);
            zRenderer.setLineWidth(2);
            zRenderer.setDisplayChartValues(false);

            //Set the title of the graph as well as other things
            XYMultipleSeriesRenderer multiRenderer = new XYMultipleSeriesRenderer();
            multiRenderer.setXLabels(0);
            multiRenderer.setLabelsColor(Color.RED);
            multiRenderer.setChartTitle("t vs (x,y,z)");
            multiRenderer.setXTitle("Sensor Data");
            multiRenderer.setYTitle("Acceleration values");
            multiRenderer.setZoomButtonsVisible(true);
            for (int i = 0; i < sensorData.size(); i++) {

                multiRenderer.addXTextLabel(i + 1, ""
                        + (sensorData.get(i).getTime() - t));
            }
            for (int i = 0; i < 12; i++) {
                multiRenderer.addYTextLabel(i + 1, "" + i);
            }

            multiRenderer.addSeriesRenderer(xRenderer);
            multiRenderer.addSeriesRenderer(yRenderer);
            multiRenderer.addSeriesRenderer(zRenderer);

            // Creating a Line Chart
            mChart = ChartFactory.getLineChartView(getBaseContext(), dataset,
                    multiRenderer);

            // Adding the Line Chart to the LinearLayout
            linearLayout.addView(mChart);
            graph = true;
        }
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

    //React on various system buttons
    //----------------------------------------------------------------------------------------------
    /**
     * This function is used in order to override the back button with a custom event.
     */
    @Override
    public void onBackPressed() {
        buttonsAlert();
    }
}