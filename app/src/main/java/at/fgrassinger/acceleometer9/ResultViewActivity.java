package at.fgrassinger.acceleometer9;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import static android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

/**
 * This is the Result View activity, where the user can view the results from the main activity.
 * The reanimation or acceleration curve will be plotted, as well as saved if the user wants to.
 * Created by admin on 01.10.2015.
 */
public class ResultViewActivity extends Activity implements View.OnClickListener {

    private AccelManagerWrapper sensorWrapper;        //Custom Wrapper for the serializable
    private ArrayList<AccelManager> sensorData;       //List for the sensor values
    private Button btnSave, btnBack;                  //Variables for the button instances

    private boolean graphPlotted = false;             //Check if the graph is plotted and also result for Intent
    private View mChart;                              //Variable for the chart view object
    private LinearLayout linearLayout;                //Reference for the linear layout

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result_view_layout);

        btnSave = (Button) findViewById(R.id.btnSave);              //Get the Save Button object
        btnSave.setOnClickListener(this);                           //Attach the Listener Event
        btnBack = (Button) findViewById(R.id.btnBack);              //Get the Back Button object
        btnBack.setOnClickListener(this);                           //Attach the Listener Event
        linearLayout = (LinearLayout) findViewById(R.id.linLayout); //Get Linear Layout object


        Intent activityCalling = getIntent();                       //The intent will be retrieved
        sensorWrapper = (AccelManagerWrapper) activityCalling.getSerializableExtra("sensorValues"); //The values are stored
        sensorData = sensorWrapper.getAccelData();                  //The data gets extracted from the wrapper

        openChart();                                                //Draw the curve and show it
    }

    /**
     * This method reacts on the various button presses and executes different functions respectively.
     * @param v The view object clicked
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSave:
                //Save the data to phone
                //----------------------------------------------------------------------------------s
                File file;                             //Object for the Filename
                FileOutputStream outputStream;         //Object for the stream

                try {
                    file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Accelerometer.csv");
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btnBack:
                onBackIntentResult();
                break;
            default:
                break;
        }
    }

    /**
     * This method is executed by the BACK Button and generally brings the user back to the main
     * screen. It sends the result back to the starting intent from the main activity.
     */
    public void onBackIntentResult() {
        if(graphPlotted) {
            Intent backResult = new Intent();           //Create a new Intent to the main activity
            backResult.putExtra("finishedView", -1);    //Create a message for the main activity
            setResult(RESULT_OK, backResult);           //Pass the message back to it
            graphPlotted = false;
            finish();                                   //Close the current activity

        }
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
            graphPlotted = true;
        }
    }

    /**
     * This function is used in order to simulate a full screen mode. The user has no acces to the
     * notification bar or the buttons depending on the device.
     * @param hasFocus Checks if window is focused
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            findViewById(R.id.resultLayout).setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }
}
