package it.sandroni.mysimpleflashlight;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;


public class MainFlashActivity extends Activity {

    Integer prevBright;
    SurfaceView preview;
    SurfaceHolder mHolder;
    LinearLayout flashControl;
    TextView tipText;
    Camera mCamera;
    Parameters parameters;
    Boolean flashlightStatus = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Retrieve the brightness value for future use
        try {
            prevBright = Settings.System.getInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        setContentView(R.layout.activity_main_flash);

        flashControl = (LinearLayout) findViewById(R.id.flashcontrol);
        preview = (SurfaceView) findViewById(R.id.preview);
        mHolder = preview.getHolder();
        tipText = (TextView) findViewById(R.id.tipText);


        flashControl.setOnClickListener(new LinearLayout.OnClickListener(){

            @Override
            public void onClick(View arg0) {
                toggleFlashLight();
            }
        });
    }

    /**
     * Revert to original brightness
     * Also turn off the flashlight if api level < 14
     * And turn off the cam if we're not using it
     */
    @Override
    public void onStop() {
        super.onStop();

        // Revert to original brightness
        setBrightness(prevBright);

        // Turn off the flashlight if api level < 14 as leaving it on would result in a FC
        if (Integer.valueOf(Build.VERSION.SDK_INT) < 14 || flashlightStatus == false) {
            turnOffFlashLight();

            // Turn off the cam if it is on
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
            }
        }
    }

    /**
     * Check if the device has a flashlight
     * @return True if the device has a flashlight, false if not
     */
    public Boolean deviceHasFlashlight() {
        Context context = this;
        PackageManager packageManager = context.getPackageManager();

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Set brightness to a desired value
     * @param brightness
     */
    private void setBrightness(int brightness) {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = brightness/100.0f;
        getWindow().setAttributes(layoutParams);
    }

    /**
     * Toggle the flashlight on/off status
     */
    public void toggleFlashLight() {
        if (flashlightStatus == false) { // Off, turn it on
            turnOnFlashLight();
        } else { // On, turn it off
            turnOffFlashLight();
        }
    }

    /**
     * Turn on the flashlight if the device has one.
     * Also set the background colour to white and brightness to max.
     */
    public void turnOnFlashLight() {
        // Safety measure if it's already on
        turnOffFlashLight();

        // Turn on the flash if the device has one
        if (deviceHasFlashlight()) {

            // Switch on the cam for app's life
            if (mCamera == null) {
                // Turn on Cam
                mCamera = Camera.open();
                try {
                    mCamera.setPreviewDisplay(mHolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mCamera.startPreview();
            }

            // Turn on LED
            parameters = mCamera.getParameters();
            parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(parameters);
        }

        // Set background color
        flashControl.setBackgroundColor(Color.WHITE);
        tipText.setTextColor(Color.BLACK);
        tipText.setText(R.string.flashToggleOn);

        // Set brightness to max
        setBrightness(100);

        // Self awareness
        flashlightStatus = true;
    }

    /**
     * Turn off the flashlight if we find it to be on.
     * Also set the background to black and revert to original brightness
     */
    public void turnOffFlashLight() {
        // Turn off flashlight
        if (mCamera != null) {
            parameters = mCamera.getParameters();
            if (parameters.getFlashMode().equals(Parameters.FLASH_MODE_TORCH)) {
                parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(parameters);
            }
        }

        // Set background color
        flashControl.setBackgroundColor(Color.BLACK);
        tipText.setTextColor(Color.WHITE);
        tipText.setText(R.string.flashToggleOff);

        // Revert to original brightness
        setBrightness(prevBright);

        // Self awareness
        flashlightStatus = false;
    }

}
