/**
 * Copyright 2014 Jeroen Mols
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ducnd.recordvideo.camera;

import android.annotation.TargetApi;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.Surface;

import com.ducnd.CLog;
import com.ducnd.recordvideo.camera.OpenCameraException.OpenType;

import java.io.IOException;
import java.util.List;

@SuppressWarnings("deprecation")
public class CameraWrapper {

    private final int mDisplayRotation;
    private NativeCamera mNativeCamera = null;
    private Parameters   mParameters   = null;
    private int idCamera;
    private CameraSize previewSize;

    public CameraWrapper(NativeCamera nativeCamera, int displayRotation, int idCamera) {
        this.idCamera = idCamera;
        mNativeCamera = nativeCamera;
        mDisplayRotation = displayRotation;
    }

    public Camera getCamera() {
        return mNativeCamera.getNativeCamera();
    }

    public void openCamera() throws OpenCameraException {
        try {
            mNativeCamera.openNativeCamera(idCamera);
        } catch (final RuntimeException e) {
            e.printStackTrace();
            throw new OpenCameraException(OpenType.INUSE);
        }

        if (mNativeCamera.getNativeCamera() == null) throw new OpenCameraException(OpenType.NOCAMERA);
    }

    public void setIdCamera( int idCamera ) {
        this.idCamera = idCamera;
    }

    public int getIdCamera() {
        return idCamera;
    }

    public void prepareCameraForRecording() throws PrepareCameraException {
        try {
            mNativeCamera.unlockNativeCamera();
        } catch (final RuntimeException e) {
            e.printStackTrace();
            throw new PrepareCameraException();
        }
    }

    public void releaseCamera() {
        if (getCamera() == null) return;
        mNativeCamera.releaseNativeCamera();
    }

    public void startPreview(final SurfaceTexture holder) throws IOException {
        mNativeCamera.setNativePreviewDisplay(holder);
        mNativeCamera.startNativePreview();
    }

    public void stopPreview() throws Exception {
        mNativeCamera.stopNativePreview();
        mNativeCamera.clearNativePreviewCallback();
    }

    public RecordingSize getSupportedRecordingSize(int width, int height) {
        CameraSize recordingSize = getOptimalSize(getSupportedVideoSizes(VERSION.SDK_INT), width, height);
        if (recordingSize == null) {
            CLog.e(CLog.CAMERA, "Failed to find supported recording size - falling back to requested: " + width + "x" + height);
            return new RecordingSize(width, height);
        }
        CLog.d(CLog.CAMERA, "Recording size: " + recordingSize.getWidth() + "x" + recordingSize.getHeight());
        return new RecordingSize(recordingSize.getWidth(), recordingSize.getHeight());
    }

    public CamcorderProfile getBaseRecordingProfile() {
        CamcorderProfile returnProfile;
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            returnProfile = getDefaultRecordingProfile();
        } else if (CamcorderProfile.hasProfile(mNativeCamera.getIdCamera(), CamcorderProfile.QUALITY_720P)) {
            returnProfile = CamcorderProfile.get(mNativeCamera.getIdCamera(), CamcorderProfile.QUALITY_720P);
        } else if (CamcorderProfile.hasProfile(mNativeCamera.getIdCamera(), CamcorderProfile.QUALITY_480P)) {
            returnProfile = CamcorderProfile.get(mNativeCamera.getIdCamera(), CamcorderProfile.QUALITY_480P);
        } else {
            returnProfile = getDefaultRecordingProfile();
        }
        return returnProfile;
    }

    private CamcorderProfile getDefaultRecordingProfile() {
        CamcorderProfile highProfile = CamcorderProfile.get(mNativeCamera.getIdCamera(), CamcorderProfile.QUALITY_HIGH);
        if (highProfile != null) {
            return highProfile;
        }
        CamcorderProfile lowProfile = CamcorderProfile.get(mNativeCamera.getIdCamera(), CamcorderProfile.QUALITY_LOW);
        if (lowProfile != null) {
            return lowProfile;
        }
        throw new RuntimeException("No quality level found");
    }

    public void configureForPreview(int viewWidth, int viewHeight) {
        final Parameters params = mNativeCamera.getNativeCameraParameters();
        previewSize = getOptimalSize(params.getSupportedPreviewSizes(), viewWidth, viewHeight);

        params.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
        params.setPreviewFormat(ImageFormat.NV21);
        mNativeCamera.updateNativeCameraParameters(params);
        mNativeCamera.setDisplayOrientation(getRotationCorrection());
        CLog.d(CLog.CAMERA, "Preview size: " + previewSize.getWidth() + "x" + previewSize.getHeight());

    }

    public CameraSize getPreviewSize() {
        return previewSize;
    }

    public void enableAutoFocus() {
        final Parameters params = mNativeCamera.getNativeCameraParameters();
        params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        mNativeCamera.updateNativeCameraParameters(params);
    }

    public int getRotationCorrection() {
//        if ( idCamera == Camera.CameraInfo.CAMERA_FACING_FRONT ) {
//            result = (mNativeCamera.getCameraOrientation() + degrees) % 360;
//            result = (360 - result) % 360;
//        }
//        int displayRotation = mDisplayRotation * 90;
//        return (mNativeCamera.getCameraOrientation() - displayRotation + 360) % 360;
        int degrees = 0;
        switch (mDisplayRotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (idCamera == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (mNativeCamera.getCameraOrientation() + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (mNativeCamera.getCameraOrientation() - degrees + 360) % 360;
        }
        return result;
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    protected List<Size> getSupportedVideoSizes(int currentSdkInt) {
        Parameters params = mNativeCamera.getNativeCameraParameters();

        List<Size> supportedVideoSizes;
        if (currentSdkInt < Build.VERSION_CODES.HONEYCOMB) {
            CLog.e(CLog.CAMERA, "Using supportedPreviewSizes iso supportedVideoSizes due to API restriction");
            supportedVideoSizes = params.getSupportedPreviewSizes();
        } else if (params.getSupportedVideoSizes() == null) {
            CLog.e(CLog.CAMERA, "Using supportedPreviewSizes because supportedVideoSizes is null");
            supportedVideoSizes = params.getSupportedPreviewSizes();
        } else {
            supportedVideoSizes = params.getSupportedVideoSizes();
        }

        return supportedVideoSizes;
    }

    /**
     * Copyright (C) 2013 The Android Open Source Project
     * <p/>
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     * <p/>
     * http://www.apache.org/licenses/LICENSE-2.0
     * <p/>
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
    public CameraSize getOptimalSize(List<Camera.Size> sizes, int w, int h) {
        // Use a very small tolerance because we want an exact match.
        final double ASPECT_TOLERANCE = 0.1;
        final double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;

        // Start with max value and refine as we iterate over available preview sizes. This is the
        // minimum difference between view and camera height.
        double minDiff = Double.MAX_VALUE;

        // Target view height
        final int targetHeight = h;

        // Try to find a preview size that matches aspect ratio and the target view size.
        // Iterate over all available sizes and pick the largest size that can fit in the view and
        // still maintain the aspect ratio.
        for (final Camera.Size size : sizes) {
            final double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find preview size that matches the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (final Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return new CameraSize(optimalSize.width, optimalSize.height);
    }
}
