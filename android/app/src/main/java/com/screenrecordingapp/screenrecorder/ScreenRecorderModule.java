package com.screenrecordingapp.screenrecorder;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;

import java.io.File;
import java.io.IOException;

public class ScreenRecorderModule extends ReactContextBaseJavaModule implements ActivityEventListener, LifecycleEventListener {
    private static final int REQUEST_CODE = 1000;
    private static final String TAG = "ScreenRecorderModule";
    private static final int REQUEST_PERMISSIONS = 101;
    private static final int NOTIFICATION_ID = 1; // Unique ID for the notification
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private ReactApplicationContext reactContext;
    private Promise recordingPromise;
    private String videoFilePath;

    public ScreenRecorderModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        projectionManager = (MediaProjectionManager) reactContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        reactContext.addActivityEventListener(this);
        reactContext.addLifecycleEventListener(this);
    }

    @Override
    public String getName() {
        return "ScreenRecorder";
    }

    @ReactMethod
    public void startRecording(Promise promise) {
        this.recordingPromise = promise;
        Activity activity = getCurrentActivity();
        if (activity != null) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
            } else {
                // Start the foreground service before requesting the media projection
                Intent serviceIntent = new Intent(reactContext, MediaProjectionService.class);
                ContextCompat.startForegroundService(reactContext, serviceIntent);
            }
        } else {
            promise.reject("ACTIVITY_NULL", "Activity is null");
        }
    }

    @ReactMethod
    public void stopRecording(Promise promise) {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder = null;
                mediaProjection.stop();
                mediaProjection = null;
                promise.resolve(videoFilePath);
            } catch (RuntimeException e) {
                Log.e(TAG, "stopRecording: Failed to stop MediaRecorder", e);
                promise.reject("STOP_ERROR", "Failed to stop MediaRecorder", e);
            }
        } else {
            promise.reject("RECORDER_NULL", "MediaRecorder is null");
        }
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                mediaProjection = projectionManager.getMediaProjection(resultCode, data);
                startMediaRecorder();
                recordingPromise.resolve(null);
            } else {
                recordingPromise.reject("PERMISSION_DENIED", "Permission denied");
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // Not implemented
    }

    private void startMediaRecorder() {
        mediaRecorder = new MediaRecorder();

        // Set up the file path for the video
        File videoFile = new File(reactContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "screen_recording.mp4");
        videoFilePath = videoFile.getAbsolutePath();

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(videoFilePath);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setVideoEncodingBitRate(512 * 1000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(1280, 720);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "startMediaRecorder: Failed to prepare MediaRecorder", e);
            recordingPromise.reject("PREPARE_ERROR", "Failed to prepare MediaRecorder", e);
            return;
        }

        mediaProjection.createVirtualDisplay("ScreenRecorder",
                1280, 720, 1,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(), null, null);

        mediaRecorder.start();
    }

    @Override
    public void onHostResume() {
        // Not implemented
    }

    @Override
    public void onHostPause() {
        // Not implemented
    }

    @Override
    public void onHostDestroy() {
        if (mediaRecorder != null) {
            mediaRecorder.release();
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
    }
}
