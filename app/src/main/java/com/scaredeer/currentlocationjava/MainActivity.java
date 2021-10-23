package com.scaredeer.currentlocationjava;

import static com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.viewbinding.BuildConfig;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.snackbar.Snackbar;
import com.scaredeer.currentlocationjava.databinding.MainActivityBinding;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_FINE_LOCATION_PERMISSIONS_REQUEST_CODE = 34;
    private static final String SETTINGS = "Settings";
    private static final String FINE_LOCATION_PERMISSION_RATIONALE
            = "The fine location permission is needed for core functionality.";
    private static final String FINE_PERMISSION_DENIED_EXPLANATION
            = "Fine location permission was denied but is needed for core functionality.";
    private static final String PERMISSION_APPROVED_EXPLANATION
            = "You approved FINE location, carry (and click) on!";

    private MainActivityBinding mBinding;

    // Allows class to cancel the location request if it exits the activity.
    // Typically, you use one cancellation source per lifecycle.
    private final CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

    private FusedLocationProviderClient mFusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = MainActivityBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    protected void onStop() {
        // Cancels location request (if in flight).
        cancellationTokenSource.cancel();

        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionResult()");

        if (requestCode == REQUEST_FINE_LOCATION_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(
                        mBinding.container,
                        PERMISSION_APPROVED_EXPLANATION,
                        Snackbar.LENGTH_LONG
                ).show();
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                Snackbar.make(mBinding.container, FINE_PERMISSION_DENIED_EXPLANATION, Snackbar.LENGTH_LONG)
                        .setAction(SETTINGS, view -> {
                            // Build intent that displays the App settings screen.
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", BuildConfig.LIBRARY_PACKAGE_NAME, null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        })
                        .show();
            }
        }
    }

    public void locationRequestOnClick(View view) {
        Log.d(TAG, "locationRequestOnClick()");

        if (checkPermissions()) {
            requestCurrentLocation();
        } else {
            requestPermissionsWithRationale();
        }
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
        );
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionsWithRationale() {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.ACCESS_FINE_LOCATION
        );

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");

            Snackbar.make(mBinding.container, FINE_LOCATION_PERMISSION_RATIONALE, Snackbar.LENGTH_LONG)
                    .setAction(getString(android.R.string.ok), view -> {
                        // Request permissions
                        startLocationPermissionRequest();
                    })
                    .show();
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            startLocationPermissionRequest();
        }
    }


    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(
                MainActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_FINE_LOCATION_PERMISSIONS_REQUEST_CODE
        );
    }

    @SuppressLint({"MissingPermission", "DefaultLocale"})
    private void requestCurrentLocation() {
        // Returns a single current location fix on the device. Unlike getLastLocation() that
        // returns a cached location, this method could cause active location computation on the
        // device. A single fresh location will be returned if the device location can be
        // determined within reasonable time (tens of seconds), otherwise null will be returned.
        //
        // Both arguments are required.
        // PRIORITY type is self-explanatory. (Other options are PRIORITY_BALANCED_POWER_ACCURACY,
        // PRIORITY_LOW_POWER, and PRIORITY_NO_POWER.)
        // The second parameter, [CancellationToken] allows the activity to cancel the request
        // before completion.
        mFusedLocationClient
                .getCurrentLocation(PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Location result = task.getResult();
                        Log.d(TAG, String.format("getCurrentLocation() result: %s", result.toString()));
                        logOutputToScreen(String.format(
                                "Location (success): %f, %f",
                                result.getLatitude(), result.getLongitude()
                        ));
                    } else {
                        Log.e(TAG, String.format("Location (failure): %s", task.getException()));
                    }
                });
    }

    @SuppressLint("SetTextI18n")
    private void logOutputToScreen(String outputString) {
        mBinding.outputTextView.setText(mBinding.outputTextView.getText() + "\n" + outputString);
    }
}