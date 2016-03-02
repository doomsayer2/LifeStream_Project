package at.fgrassinger.acceleometer9;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * This class is used to wrap the custom class AccelManager and pass it with an Intent around
 * It only implements the constructor as well as a simple getter.
 * Created by admin on 03.10.2015.
 */
public class AccelManagerWrapper implements Serializable {
    private static final long serialVersionUID = 1L;
    private ArrayList<AccelManager> accelData;

    public AccelManagerWrapper(ArrayList<AccelManager> accelData) {
        this.accelData = accelData;
    }

    public ArrayList<AccelManager> getAccelData() {
        return accelData;
    }
}
