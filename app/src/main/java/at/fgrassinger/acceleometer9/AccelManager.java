package at.fgrassinger.acceleometer9;

import java.io.Serializable;

/**
 * This is the Accelerometer manager for the Accelerometer program. The class stores acceleration data
 * as well as various other functions. For showing the data and visualize it.
 * Created by admin on 02.03.2015.
 */
public class AccelManager implements Serializable {
    private static final long serialVersionUID = 1L;

    private long time;              //How long the measurement is taking
    private double x;               //x-Axis
    private double y;               //y-Axis
    private double z;               //z-Axis
    private double magnitude;       //sqrt(x^2+y^2+z^2)

    private long time2;
    private double magnitude2;

    /**
     * Constructor for the AccelManager class in order to set the given parameters
     */
    public AccelManager(long time, double x, double y, double z, double magnitude) {
        this.time = time;
        this.x = x;
        this.y = y;
        this.z = z;
        this.magnitude = magnitude;
    }

    public AccelManager(long time2, double magnitude2) {
        this.time2 = time2;
        this.magnitude2 = magnitude2;
    }

    //Getter and Setter methods for time variable
    public long getTime() {
        return time;
    }
    public void setTime(long time) {
        this.time = time;
    }

    //Getter and Setter methods for x variable
    public double getX() {
        return x;
    }
    public void setX(double x) {
        this.x = x;
    }

    //Getter and Setter methods for y variable
    public double getY() {
        return y;
    }
    public void setY(double y) {
        this.y = y;
    }

    //Getter and Setter methods for z variable
    public double getZ() {
        return z;
    }
    public void setZ(double z) {
        this.z = z;
    }

    //Getter and Setter methods for magnitude variable
    public double getMagnitude() {
        return magnitude;
    }
    public void setMagnitude(double magnitude) {
        this.magnitude = magnitude;
    }

    public long getTime2() {
        return time2;
    }

    public double getMagnitude2() {
        return magnitude2;
    }

    /**
     * Converts the Objects to string to display it
     */
    @Override
    public String toString() {
        return "AccelManager{" +
                "time=" + time +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", Mag=" + magnitude +
                '}';
    }

    public String convertData() {
        return time + ";" + x + ";" + y + ";" + z + ";" + magnitude;
    }
}
