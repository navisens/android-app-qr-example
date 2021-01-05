/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navisens.qrcodereader;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.navisens.motiondnaapi.MotionDna;
import com.navisens.motiondnaapi.MotionDnaHeaderAttributes;
import com.navisens.motiondnaapi.MotionDnaSDK;
import com.navisens.motiondnaapi.MotionDnaSDKListener;
import com.navisens.qrcodereader.barcode.BarcodeCaptureActivity;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import static com.google.android.gms.vision.barcode.Barcode.GEO;

/**
 * Main activity demonstrating how to pass extra parameters to an activity that
 * reads barcodes.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener, MotionDnaSDKListener {

    // use a compound button so either checkbox or switch widgets work.
    private CompoundButton autoFocus;
    private CompoundButton useFlash;
    private TextView statusTextView;
    private TextView barcodeTextView;
    private TextView statisticsTextView;
    private Button readQrButton;

    private static final int RC_BARCODE_CAPTURE = 9001;
    private static final int REQUEST_MDNA_PERMISSIONS = 1;
    private final String TAG = getClass().getSimpleName();
    private MotionDnaSDK motionDnaSDK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityCompat.requestPermissions(this, MotionDnaSDK.getRequiredPermissions(), REQUEST_MDNA_PERMISSIONS);
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.status_message);
        barcodeTextView = findViewById(R.id.barcode_value);
        statisticsTextView = findViewById(R.id.statistics);

        autoFocus = findViewById(R.id.auto_focus);
        useFlash = findViewById(R.id.use_flash);

        readQrButton = findViewById(R.id.read_barcode);
        readQrButton.setOnClickListener(this);
    }

    private Intent motionDnaServiceIntent;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(MotionDnaSDK.checkMotionDnaPermissions(this)) { // permissions already requested
            // Starts a foreground service to ensure that the
            // App continues to sample the sensors in background
//            motionDnaServiceIntent = new Intent(getApplicationContext(), MotionDnaForegroundService.class);
//            getApplicationContext().startService(motionDnaServiceIntent);

            // Start the MotionDna Core
            startMotionDna();
        }
        else {
            Log.e(this.getClass().getSimpleName(),"MotionDnaPermissions not granted");
        }
    }

    public void startMotionDna() {
        Log.v(TAG, "startMotionDna");
        String devKey = "<--DEVELOPER-KEY-HERE-->";
        motionDnaSDK = new MotionDnaSDK(getApplicationContext(), this);
        motionDnaSDK.start(devKey);

        statisticsTextView.setText("Initializing, please ensure SDK key is valid");
    }

    @Override
    public void receiveMotionDna(MotionDna motionDna) {
        if(!readQrButton.isEnabled()) {
            readQrButton.setEnabled(true);
        }
        statisticsTextView.setText(
                String.format(Locale.US,
                        "x: %f \n" +
                                "y: %f \n" +
                                "z: %f",
                        motionDna.getLocation().cartesian.x,
                        motionDna.getLocation().cartesian.y,
                        motionDna.getLocation().cartesian.z));
    }


    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.read_barcode) {

            Log.d(TAG, "read qr code pressed");
            // launch barcode activity.
            Intent intent = new Intent(this, BarcodeCaptureActivity.class);
            intent.putExtra(BarcodeCaptureActivity.AutoFocus, autoFocus.isChecked());
            intent.putExtra(BarcodeCaptureActivity.UseFlash, useFlash.isChecked());

            startActivityForResult(intent, RC_BARCODE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    statusTextView.setText(R.string.qr_code_success);
                    barcodeTextView.setText(barcode.displayValue);

                    Log.d(TAG, "Barcode read: " + barcode.displayValue);

                    int identifier = barcode.hashCode();
                    motionDnaSDK.recordObservation(identifier, 1.0);

                } else {
                    statusTextView.setText(R.string.qr_code_failure);
                    Log.d(TAG, "No barcode captured, intent data is null");
                }
            } else {
                statusTextView.setText(String.format(getString(R.string.qr_code_error),
                        CommonStatusCodes.getStatusCodeString(resultCode)));
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void reportStatus(MotionDnaSDK.Status status, String s) {
        switch (status) {
            case AuthenticationFailure:
                System.out.println("Error: Authentication Failed " + s);
                break;
            case AuthenticationSuccess:
                System.out.println("Status: Authentication Successful " + s);
                break;
            case ExpiredSDK:
                System.out.println("Status: SDK expired " + s);
                break;
            case PermissionsFailure:
                System.out.println("Status: permissions not granted " + s);
                break;
            case MissingSensor:
                System.out.println("Status: sensor missing " + s);
                break;
            case SensorTimingIssue:
                System.out.println("Status: sensor timing " + s);
                break;
            case Configuration:
                System.out.println("Status: configuration " + s);
                break;
            case None:
                System.out.println("Status: None " + s);
                break;
            default:
                System.out.println("Status: Unknown " + s);
        }
    }
}
