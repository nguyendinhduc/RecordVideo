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

package com.jmolsmobile.landscapevideocapture.preview;

import android.graphics.SurfaceTexture;
import android.view.SurfaceHolder;
import android.view.TextureView;

import com.jmolsmobile.landscapevideocapture.CLog;
import com.jmolsmobile.landscapevideocapture.camera.CameraSize;
import com.jmolsmobile.landscapevideocapture.camera.CameraWrapper;

import java.io.IOException;

public class CapturePreview implements TextureView.SurfaceTextureListener {

    private boolean mPreviewRunning = false;
    private final CapturePreviewInterface mInterface;
    public final CameraWrapper mCameraWrapper;
//    private TextureView mHolder;
    private SurfaceTexture mHolder;
    private int width;
    private int height;
    private ChangeSizeView changeSizeView;
    private boolean isChangSize = false;

    public CapturePreview(CapturePreviewInterface capturePreviewInterface, CameraWrapper cameraWrapper,
                          TextureView holder, ChangeSizeView changeSizeView) {
        mInterface = capturePreviewInterface;
        mCameraWrapper = cameraWrapper;
        this.changeSizeView = changeSizeView;

        initalizeSurfaceHolder(holder);
    }

    @SuppressWarnings("deprecation")
    private void initalizeSurfaceHolder(final TextureView surfaceHolder) {
        surfaceHolder.setSurfaceTextureListener(this);
//        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // Necessary for older API's
    }

//    @Override
//    public void surfaceCreated(final SurfaceHolder holder) {
//        // NOP
//    }
//
//    @Override
//    public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
//        if (isChangSize && ( width == this.width && height == this.height && mHolder != null)) return;
//        isChangSize = true;
//        this.mHolder = holder;
//        this.height = height;
//        this.width = width;
//        referess();
//    }
//
//    @Override
//    public void surfaceDestroyed(final SurfaceHolder holder) {
//        // NOP
//    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (isChangSize && (width == this.width && height == this.height && mHolder != null))
            return;
        isChangSize = true;
        this.mHolder = surface;
        this.height = height;
        this.width = width;
        referess();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public void referess() {
        if (mPreviewRunning) {
            try {
                mCameraWrapper.stopPreview();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        try {
            mCameraWrapper.configureForPreview(height, width);
            CameraSize cameraSize = mCameraWrapper.getPreviewSize();
            updateSizeViewOrientPo(cameraSize);
            changeSizeView.chageSize(width, height);
            CLog.d(CLog.PREVIEW, "Configured camera for preview in surface of " + width + " by " + height);
        } catch (final RuntimeException e) {
            e.printStackTrace();
            CLog.d(CLog.PREVIEW, "Failed to show preview - invalid parameters set to camera preview");
            mInterface.onCapturePreviewFailed();
            return;
        }

        try {
            mCameraWrapper.enableAutoFocus();
        } catch (final RuntimeException e) {
            e.printStackTrace();
            CLog.d(CLog.PREVIEW, "AutoFocus not available for preview");
        }

        try {
            mCameraWrapper.startPreview(mHolder);
            setPreviewRunning(true);
        } catch (final IOException e) {
            e.printStackTrace();
            CLog.d(CLog.PREVIEW, "Failed to show preview - unable to connect camera to preview (IOException)");
            mInterface.onCapturePreviewFailed();
        } catch (final RuntimeException e) {
            e.printStackTrace();
            CLog.d(CLog.PREVIEW, "Failed to show preview - unable to start camera preview (RuntimeException)");
            mInterface.onCapturePreviewFailed();
        }
    }

    private void updateSizeViewOrientPo(CameraSize cameraSize) {
        int temWidth = height;
        int temHeight = temWidth * cameraSize.getHeight() / cameraSize.getWidth();
        if (temHeight < width) {
            temHeight = width;
            temWidth = temHeight * cameraSize.getWidth() / cameraSize.getHeight();
        }

        width = temHeight;
        height = temWidth;

    }

    public void releasePreviewResources() {
        if (mPreviewRunning) {
            try {
                mCameraWrapper.stopPreview();
                setPreviewRunning(false);
            } catch (final Exception e) {
                e.printStackTrace();
                CLog.e(CLog.PREVIEW, "Failed to clean up preview resources");
            }
        }
    }

    protected void setPreviewRunning(boolean running) {
        mPreviewRunning = running;
    }


    public interface ChangeSizeView {
        void chageSize(int widht, int height);
    }

}