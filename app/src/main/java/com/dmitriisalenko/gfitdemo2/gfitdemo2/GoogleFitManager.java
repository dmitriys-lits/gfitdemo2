package com.dmitriisalenko.gfitdemo2.gfitdemo2;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.RecordingClient;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

final public class GoogleFitManager {
    private Activity mActivity;
    private GoogleFitFactory mGoogleFitFactory;

    private boolean processingConnect = false;

    public int PERMISSION_REQUEST_CODE = 11235;

    public GoogleFitManager(Activity activity) {
        mActivity = activity;
        mGoogleFitFactory = new GoogleFitFactory(this);
    }

    public Activity getActivity() {
        return mActivity;
    }

    Vector<DataType> getDataTypes() {
        Vector<DataType> dataTypes = new Vector<>();

        // STEPS
        dataTypes.add(DataType.TYPE_STEP_COUNT_CUMULATIVE);
        dataTypes.add(DataType.TYPE_STEP_COUNT_DELTA);
        dataTypes.add(DataType.TYPE_STEP_COUNT_CADENCE);
        dataTypes.add(DataType.AGGREGATE_STEP_COUNT_DELTA);

        // CYCLING
        dataTypes.add(DataType.TYPE_CYCLING_PEDALING_CADENCE);
        dataTypes.add(DataType.TYPE_CYCLING_PEDALING_CUMULATIVE);
        dataTypes.add(DataType.TYPE_CYCLING_WHEEL_REVOLUTION);
        dataTypes.add(DataType.TYPE_CYCLING_WHEEL_RPM);

        // DISTANCE
        dataTypes.add(DataType.TYPE_DISTANCE_CUMULATIVE);
        dataTypes.add(DataType.TYPE_DISTANCE_DELTA);
        dataTypes.add(DataType.AGGREGATE_DISTANCE_DELTA);

        // ACTIVITIES
        dataTypes.add(DataType.TYPE_ACTIVITY_SEGMENT);
        dataTypes.add(DataType.TYPE_ACTIVITY_SAMPLES);
        dataTypes.add(DataType.AGGREGATE_ACTIVITY_SUMMARY);

        return dataTypes;
    }

    private FitnessOptions getFitnessOptions() {
        FitnessOptions.Builder fitnessOptionsBuilder = FitnessOptions.builder();

        for (DataType dt : getDataTypes()) {
            fitnessOptionsBuilder.addDataType(dt);
        }

        return fitnessOptionsBuilder.build();
    }

    private void subscribeToFitnessData() {
        RecordingClient recordingClient = Fitness.getRecordingClient(mActivity, GoogleSignIn.getLastSignedInAccount(mActivity));

        for (final DataType dt : getDataTypes()) {
            recordingClient.subscribe(dt)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Logger.log("Subscribe to " + dt.getName() + " ok");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Logger.log("Subscribe to " + dt.getName() + " is failure");
                        }
                    });
        }
    }

    //
    // BLOCK START: public methods list
    //

    public boolean hasPermissions() {
        return GoogleSignIn.hasPermissions(
                GoogleSignIn.getLastSignedInAccount(mActivity),
                getFitnessOptions());
    }

    public void connectGoogleFit() {
        if (processingConnect || hasPermissions()) {
            return;
        }

        processingConnect = true;

        GoogleSignIn.requestPermissions(
                mActivity,
                PERMISSION_REQUEST_CODE,
                GoogleSignIn.getLastSignedInAccount(mActivity),
                getFitnessOptions());
    }

    public void connectGoogleFitCompleted() {
        processingConnect = false;

        if (hasPermissions()) {
            subscribeToFitnessData();
        }
    }

    public void disconnectGoogleFit() {
        if (processingConnect || !hasPermissions()) {
            return;
        }

        processingConnect = true;

        Fitness.getConfigClient(mActivity, GoogleSignIn.getLastSignedInAccount(mActivity))
                .disableFit()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Logger.log("Disconnect Google Fit is okay");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Logger.log("Disconnect Google Fit failure " + e.getMessage());
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        processingConnect = false;

                        GoogleSignInOptions signInOptions = (new GoogleSignInOptions.Builder()).addExtension(getFitnessOptions()).build();
                        GoogleSignInClient client = GoogleSignIn.getClient(mActivity, signInOptions);
                        client.revokeAccess().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Logger.log("Google Account revoked access");
                                ((MainActivity) mActivity).refreshMainContentLayout();
                            }
                        });
                    }
                });
    }

    public void readActivitiesSamples(OnSuccessListener<String> onSuccessListener, OnFailureListener onFailureListener) {
        mGoogleFitFactory.readData(
                DataType.TYPE_ACTIVITY_SEGMENT,
                DataType.AGGREGATE_ACTIVITY_SUMMARY,
                onSuccessListener,
                onFailureListener
        );
    }

    public void readDistancesData(OnSuccessListener<String> onSuccessListener, OnFailureListener onFailureListener) {
        mGoogleFitFactory.readData(
                DataType.TYPE_DISTANCE_DELTA,
                DataType.AGGREGATE_DISTANCE_DELTA,
                onSuccessListener,
                onFailureListener
        );
    }

    public void readData(String type, OnSuccessListener<String> onSuccessListener, OnFailureListener onFailureListener) {
        switch (type) {
            case "activities":
                readActivitiesSamples(onSuccessListener, onFailureListener);
                break;
            case "distances":
                readDistancesData(onSuccessListener, onFailureListener);
                break;
            default:
                break;
        }
    }

    // BLOCK END
}
