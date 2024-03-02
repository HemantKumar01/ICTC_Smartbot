package com.example.smartbotjava;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

//import me.aflak.arduino.Arduino;
//import me.aflak.arduino.ArduinoListener;

public class SmartBotActivity extends CameraActivity implements CvCameraViewListener2  {
    /** Check if this device has a camera */
    private static final String TAG = "OCVSample::Activity";
    private CameraBridgeViewBase mOpenCvCameraView;
    public static double calculateDistance(Point point1, Point point2) {
        double dx = point1.x - point2.x;
        double dy = point1.y - point2.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    public SmartBotActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }
//    private Camera mCamera;
//    public void turnOffTheFlash() {
//        Camera.Parameters params = mCamera.getParameters();
//        params.setFlashMode(params.FLASH_MODE_OFF);
//        mCamera.setParameters(params);
//    }
//
//    public void turnOnTheFlash() {
//        Camera.Parameters params = mCamera.getParameters();
//        params.setFlashMode(params.FLASH_MODE_TORCH);
//        mCamera.setParameters(params);
//    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);


        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV loaded successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }

        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.CamView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
//        turnOnTheFlash();

    }

    public void sendToUSB(String str){

        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);

        if (availableDrivers.isEmpty()) {
            Log.e("hemant", "Drivers not present");
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());

        if (connection == null) {
            Log.e("hemant", "connection null");
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
//            UsbManager.requestPermission(driver.getDevice());
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, SmartBotActivity.class), PendingIntent.FLAG_IMMUTABLE);
            manager.requestPermission(driver.getPorts().get(0).getDriver().getDevice(), pendingIntent);
            return;
        }

        UsbSerialPort port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        try {
            port.open(connection);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] data = (str + '\n').getBytes();
        try {
            Log.e("hemant", "Data Transferred: "+String.valueOf(data.length)+"Byte");
            port.write(data, 2000);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] buffer = new byte[8192];
        try {
            int len = port.read(buffer, 2000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    @Override
    public void onResume()
    {
        super.onResume();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.enableView();
    }
    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    @Override
    public void onCameraViewStarted(int width, int height) {

    }
    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        Mat frame = inputFrame.rgba();
        Mat gray = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGBA2GRAY);
        Mat thresh = new Mat();
        Imgproc.threshold(gray, thresh, 127, 255, Imgproc.THRESH_BINARY);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        if (!contours.isEmpty()) {
            MatOfPoint largestContour = Collections.max(contours, new Comparator<MatOfPoint>() {
                @Override
                public int compare(MatOfPoint o1, MatOfPoint o2) {
                    return (int)(Imgproc.contourArea(o1) - Imgproc.contourArea(o2));
                }
            });

            Moments moments = Imgproc.moments(largestContour);
            if (moments.m00 != 0) {
                int cx = (int) (moments.m10 / moments.m00);
                int cy = (int) (moments.m01 / moments.m00);
                Imgproc.circle(frame, new Point(cx, cy), 5, new Scalar(0, 0, 255), -1);

                // Calculate distance from centroid to center of frame
                Point frameCenter = new Point(frame.width() / 2, frame.height() / 2);
                double distance = calculateDistance(new Point(cx, cy), frameCenter);
                if(cy < frame.height()/2){
                    distance *=-1;
                }
                Log.e("hemant", String.valueOf(distance));
                sendToUSB(String.valueOf(distance));

            }
        }

        return frame; // Display the processed frame
    }
}