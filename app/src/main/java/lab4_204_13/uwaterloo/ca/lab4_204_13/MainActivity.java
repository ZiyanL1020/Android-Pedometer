package lab4_204_13.uwaterloo.ca.lab4_204_13;


import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.hardware.SensorEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import android.os.Bundle;



public class MainActivity extends AppCompatActivity {

    MapView mv;
    NavigationalMap map;
    double angleInRadian = 0;
    float[] accelerometerData = new float[3];

    int stepNorth = 0; // steps in north direction component
    int stepEast = 0; // steps in east direction component

    float displacementNorth = 0;
    float displacementEast = 0;
    static final float STEPSIZE = 1f;

    boolean enable = true; // trigger for calibration dialog
    int steps;

    PointF startPoint;
    PointF destinationPoint;
    PointF userLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        /* Get reference to LinearLayout and set orientation */
        LinearLayout ll = (LinearLayout)findViewById(R.id.ll);
        ll.setOrientation(LinearLayout.VERTICAL);


        /* Create a text view for Accelerometer */
        TextView acceleroMeterTV = new TextView(getApplicationContext());
        acceleroMeterTV.setTextColor(Color.BLACK);
        ll.addView(acceleroMeterTV);


        /*Create a text view for calibration note*/
        TextView calibrationTV = new TextView(getApplicationContext());
        calibrationTV.setTextColor(Color.BLACK);
        ll.addView(calibrationTV);


        /*Design map width and height according to device*/
        int width = this.getBaseContext().getResources().getDisplayMetrics().widthPixels;
        int height = this.getBaseContext().getResources().getDisplayMetrics().heightPixels;

        /*Create a map view and map*/
        mv = new MapView(getApplicationContext(),width - 50,(int)height/3,40,30);
        registerForContextMenu (mv);

        map = MapLoader.loadMap(getExternalFilesDir(null),"E2-3344.svg");
        mv.setMap(map);

        ll.addView(mv);
        mv.setVisibility(View.VISIBLE);


        /*Implements the positionListener interface, pass a object as listener to the map*/
        MyPositionListener pListener = new MyPositionListener();
        mv.addListener(pListener);


        /*Create and register sensors*/
        SensorManager mySensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        Sensor accelerometer = mySensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor orientationSensor = mySensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        final SensorEventListener accelerometerListener = new AcceleroMeterEventListener(acceleroMeterTV);
        SensorEventListener orientationListener = new orientationSensorEventListener();

        mySensorManager.registerListener(accelerometerListener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mySensorManager.registerListener(orientationListener, orientationSensor,SensorManager.SENSOR_DELAY_NORMAL);


        /*Create a reset button*/
        Button resetButton = (Button) findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                ((AcceleroMeterEventListener)accelerometerListener).ResetSteps();
            }
        });

        /*Create a calibration button*/
        Button calibrationButton = (Button) findViewById(R.id.calibrationButton);
        calibrationButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                enable = false;
                AlertDialog calibrationDialog = new AlertDialog.Builder(MainActivity.this).create();
                calibrationDialog.setTitle("Calibration");
                calibrationDialog.setMessage("Calibrate your phone by rotating it 1-2 times along each axis\nAfter finished, click OK to resume");
                calibrationDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                enable = true;
                                dialog.dismiss();
                            }
                        });
                calibrationDialog.show();
            }
        });

    }

    /*Map setting*/
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        mv.onCreateContextMenu(menu, v, menuInfo);
    }

    /*Map setting*/
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return super.onContextItemSelected(item) || mv.onContextItemSelected(item);
    }

    /*Implement the positionlistener interface*/
    class MyPositionListener implements PositionListener{

        public MyPositionListener(){
            startPoint = new PointF(0,0);
            destinationPoint = new PointF(0,0);
        }

        @Override
        public void destinationChanged(MapView source, PointF dest) {
            // set user destination point
            source.setDestinationPoint(dest);
            destinationPoint = dest;
        }

        @Override
        public void originChanged(MapView source, PointF loc) {
            // set user original point
            source.setUserPoint(loc);
            startPoint = loc;
        }
    }

    class AcceleroMeterEventListener implements SensorEventListener {

        TextView output;
        float smoothedAccel;
        int C;
        int state;

        public AcceleroMeterEventListener(TextView OutputView){
            output = OutputView;
            steps = 0;
            smoothedAccel = 0;
            C = 15;
            state = 1;
        }

        @Override
        public void onAccuracyChanged(Sensor s, int i){
            // do nothing here
        }

        /*step counter state machine*/
        public int CountStep(float in,int step) {

            if (enable) {
                switch (state) {
                    case 1:
                        if (in >= 7.25) {
                            state = 2; // if the value is larger than 7.25, switch to the next stage
                        } else {
                            state = 1; // if the value is less than 7.25, stay in this stage
                        }
                        break;
                    case 2:
                        if (in < 6.75) {
                            state = 3; // if the value is less than 6.75, switch to next stage
                        } else {
                            state = 2;
                        }
                        break;
                    case 3:
                        double degreeTemp = Math.toDegrees(angleInRadian);
                        if(degreeTemp < 0){
                            degreeTemp += 360;
                        }
                        Integer stepNorthTemp = new Integer(stepNorth);
                        Integer stepEastTemp = new Integer(stepEast);

                        /*determine the steps in north and east direction according to degrees*/
                        if(degreeTemp >= 315 || degreeTemp < 45){
                            stepNorthTemp++;
                        }else if (degreeTemp >= 45 && degreeTemp < 135){
                            stepEastTemp++;
                        }else if(degreeTemp >= 135 && degreeTemp < 225){
                            stepNorthTemp--;
                        }else if (degreeTemp >= 225 && degreeTemp < 315){
                            stepEastTemp--;
                        }

                        /*check if there is a wall between user and location after next step, if yes. silently ignore this step; if not, increase the step*/
                        if(map.calculateIntersections(userLocation, new PointF(startPoint.x + (stepEastTemp *  STEPSIZE), startPoint.y - (stepNorthTemp * STEPSIZE))).isEmpty()){
                            stepEast = new Integer(stepEastTemp);
                            stepNorth = new Integer(stepNorthTemp);
                            step++;  // in stage 3, increase the step
                        }
                        state = 1; // switch back to state 1
                        break;
                }

            }
            return step;
        }

        @Override
        public void onSensorChanged(SensorEvent se){

            accelerometerData = se.values; // collect accelerometer data for further usage

            smoothedAccel += (se.values[2] - smoothedAccel)/C;//filter the values on Z axis
            se.values[2] = smoothedAccel; // insert the filtered value back to the array

            steps = CountStep(se.values[2],steps); // go through the state machine


            /*case the # of steps into float for other usage*/
            displacementNorth = (float) stepNorth;
            displacementEast = (float) stepEast;

            /*update the real-time location of user*/
            userLocation = new PointF(displacementEast * STEPSIZE + startPoint.x, -displacementNorth * STEPSIZE + startPoint.y);

            String value = ""; // string for output textview
            double distance = VectorUtils.distance(userLocation, destinationPoint); // distance between user location and destination point
            String orientation="";// string for output textview

            double degree = 0;// placeholder for angel in degree
            degree = Math.toDegrees(angleInRadian);

            /*determine the orientation based on the angel*/
            if (degree >=  337.5 || degree < 22.5 )
                orientation = "North";
            else if(degree >= 22.5 && degree < 67.5)
                orientation = "NorthEast";
            else if(degree >= 67.5 && degree < 112.5)
                orientation = "East";
            else if(degree >= 112.5 && degree < 157.5)
                orientation = "SouthEast";
            else if(degree >= 157.5 && degree < 202.5)
                orientation = "South";
            else if(degree >= 202.5 && degree < 247.5)
                orientation = "SouthWest";
            else if(degree >= 247.5 && degree < 292.5)
                orientation = "West";
            else if(degree >= 292.5 && degree < 337.5)
                orientation = "NorthWest";

            /*output information*/
            value = String.format("# of steps: %d\n" +
                            "Displacement in North: %.4f\n" +
                            "Displacement in East: %.4f\n" +
                            "Orientation: %s\n" +
                            "Heading direction: %.4f\n" +
                            "Distance: %.4f\n",
                    steps, displacementNorth, displacementEast, orientation, degree, distance);

            /*calculate the path to from user location to destination*/
            PointF pointHead = new PointF(userLocation.x, userLocation.y);// initialize the heading vector to user current location
            PointF pointNext = destinationPoint;// initialize the next vector to user destination location
            List<PointF> route = new ArrayList<PointF>();// route array for storing a number of points to draw a path
            route.add(userLocation);// add the starting point to the route array

            /*if there is no wall between start point and the destination point, simply add the destination point to the array*/
            if(map.calculateIntersections(userLocation, destinationPoint).isEmpty()){
                pointNext = new PointF(destinationPoint.x, destinationPoint.y);
                route.add(pointNext);
            }
            /*If there is wall between start point and destination point, but there is a perpendicular path to take, take the path*/
            else if(map.calculateIntersections(userLocation, new PointF(userLocation.x, destinationPoint.y)).isEmpty()
                    && map.calculateIntersections(destinationPoint, new PointF(userLocation.x, destinationPoint.y)).isEmpty()){
                pointNext = new PointF(userLocation.x, destinationPoint.y);
                route.add(pointNext);
                route.add(destinationPoint);
            }
            /*If there is wall between start point and destination point, but there is a perpendicular path to take, take the path*/
            else if(map.calculateIntersections(userLocation, new PointF(destinationPoint.x, userLocation.y)).isEmpty()
                    && map.calculateIntersections(destinationPoint, new PointF(destinationPoint.x, userLocation.y)).isEmpty()){
                pointNext = new PointF(destinationPoint.x, userLocation.y);
                route.add(pointNext);
                route.add(destinationPoint);
            }
            /*If the situation is not included above, find the nearest wall*/
            else{
                List<InterceptPoint> myIntercepts = new ArrayList<InterceptPoint>();// placeholder for intercepts of walls and direct route, line segment contains a point and a line
                myIntercepts = map.calculateIntersections(userLocation, destinationPoint);

                LineSegment wall = myIntercepts.get(0).getLine(); // get the nearest line of wall from the array

                float[] difference;// difference between one end of wall and
                float[] uV = wall.findUnitVector(); // unit vector

                // Determine which point to use as next point

                /*if the start point is on the right side of destination*/
                if(userLocation.x > destinationPoint.x){
                    difference = VectorUtils.difference(wall.start, myIntercepts.get(0).getPoint());

                    if(wall.start.y > userLocation.y){
                        pointNext = new PointF(userLocation.x, myIntercepts.get(0).getPoint().y - uV[1]*difference[1]*1.1f);
                    }else{
                        pointNext = new PointF(myIntercepts.get(0).getPoint().y - uV[1]*difference[1]*1.2f, myIntercepts.get(0).getPoint().y - uV[1]*difference[1]*1.2f);
                    }
                }else{
                    difference = VectorUtils.difference(wall.end, myIntercepts.get(0).getPoint());

                    if(wall.end.y > userLocation.y){
                        pointNext = new PointF(userLocation.x, myIntercepts.get(0).getPoint().y + uV[1]*difference[1]*1.1f);
                    }else{
                        pointNext = new PointF(myIntercepts.get(0).getPoint().x + uV[1]*difference[1]*1.2f, myIntercepts.get(0).getPoint().y - uV[1]*difference[1]*1.2f);
                    }
                }

                // Add the next point
                route.add(pointNext);
                PointF a = new PointF(pointNext.x, pointNext.y);

                PointF b;

                // Get perpendicular path from next path to destination
                if(map.calculateIntersections(a, new PointF(a.x, destinationPoint.y)).isEmpty()
                        && map.calculateIntersections(destinationPoint, new PointF(a.x, destinationPoint.y)).isEmpty()){
                    b = new PointF(a.x, destinationPoint.y);
                    route.add(b);
                }else if(map.calculateIntersections(a, new PointF(destinationPoint.x, a.y)).isEmpty()
                        && map.calculateIntersections(destinationPoint, new PointF(destinationPoint.x, a.y)).isEmpty()){
                    b = new PointF(destinationPoint.x, a.y);
                    route.add(b);
                }

                route.add(destinationPoint);
            }

            if (degree >=  337.5 || degree < 22.5 ){
                pointHead.y -= 1;
            }else if(degree >= 22.5 && degree < 67.5){
                pointHead.y -= 1;
                pointHead.x += 1;
            }else if(degree >= 67.5 && degree < 112.5){
                pointHead.x += 1;
            }else if(degree >= 112.5 && degree < 157.5){
                pointHead.y += 1;
                pointHead.x += 1;
            }else if(degree >= 157.5 && degree < 202.5){
                pointHead.y += 1;
            }else if(degree >= 202.5 && degree < 247.5){
                pointHead.y += 1;
                pointHead.x -= 1;
            }else if(degree >= 247.5 && degree < 292.5){
                pointHead.x -= 1;
            }else if(degree >= 292.5 && degree < 337.5){
                pointHead.y -= 1;
                pointHead.x -= 1;
            }

            float angle = (float) ( VectorUtils.angleBetween(userLocation, pointHead, pointNext) * 180 / Math.PI);

            mv.setUserPath(route);
            mv.setUserPoint(route.get(0));//update the real-time location of user location

            // Check distance from destination
            if(distance < 1f){
                value += "You've reached your destination\n";
            }else if(Math.abs(angle) > 25){
                if(angle > 0)
                    value += String.format("Turn Right %.3f degrees\n", angle);
                else
                    value += String.format("Turn Left %.3f degrees\n", Math.abs(angle));
            }else{
                value += "Proceed Forward\n";
            }

            output.setText(value);

        }

        public void ResetSteps(){
            steps = 0; // reset the steps
            stepNorth = 0; // reset the step component in north
            stepEast = 0;// reset the step component in east
            displacementNorth = 0;
            displacementEast = 0;
        }

    }

    class orientationSensorEventListener implements SensorEventListener{
        double degree = 0;

        @Override
        public void onSensorChanged(SensorEvent se) {

            degree = (se.values[0] ) % 360;


            if(degree < 0) {
                degree += 360;
            }
            angleInRadian = Math.toRadians(degree);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }
}








