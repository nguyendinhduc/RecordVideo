
package com.jmolsmobile.landscapevideocapture.view;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jmolsmobile.landscapevideocapture.R;
import com.jmolsmobile.landscapevideocapture.R.id;
import com.jmolsmobile.landscapevideocapture.preview.CapturePreview;

public class VideoCaptureView extends FrameLayout implements OnClickListener {

    private ImageView mDeclineBtnIv;
    private ImageView mAcceptBtnIv;
    private ImageView mRecordBtnIv;
    private SurfaceCamera mSurfaceView;
    private ImageView mThumbnailIv;
    private LinearLayout llConfirm;
    private TextView txtLimitDuration;
    private ImageView btnFacing;
    private boolean isFacingFront = true;
    private boolean hasCameraFront = false;
    private boolean hasCameraBack = false;

    private RecordingButtonInterface mRecordingInterface;

    public VideoCaptureView(Context context) {
        super(context);
        checkNumberCamera();
        initialize(context);
    }

    public VideoCaptureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        checkNumberCamera();
        initialize(context);
    }

    public VideoCaptureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        checkNumberCamera();
        initialize(context);
    }

    private void checkNumberCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                hasCameraFront = true;
            }

            if ( cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                hasCameraBack = true;
            }
        }
    }

    private void initialize(Context context) {
        final View videoCapture = View.inflate(context, R.layout.view_videocapture, this);

        mRecordBtnIv = (ImageView) videoCapture.findViewById(id.videocapture_recordbtn_iv);
        mAcceptBtnIv = (ImageView) videoCapture.findViewById(id.videocapture_acceptbtn_iv);
        mDeclineBtnIv = (ImageView) videoCapture.findViewById(id.videocapture_declinebtn_iv);
        btnFacing = (ImageView) videoCapture.findViewById(id.btnFacing);
        txtLimitDuration = (TextView) videoCapture.findViewById(id.txtLimitDuration);
        llConfirm = (LinearLayout)videoCapture.findViewById(id.llConfirm);

        mRecordBtnIv.setOnClickListener(this);
        mAcceptBtnIv.setOnClickListener(this);
        mDeclineBtnIv.setOnClickListener(this);
        btnFacing.setOnClickListener(this);

        mThumbnailIv = (ImageView) videoCapture.findViewById(R.id.videocapture_preview_iv);
        mSurfaceView = (SurfaceCamera) videoCapture.findViewById(R.id.videocapture_preview_sv);

    }

    public CapturePreview.ChangeSizeView getChangeSize() {
        return mSurfaceView;
    }

    public void setRecordingButtonInterface(RecordingButtonInterface mBtnInterface) {
        this.mRecordingInterface = mBtnInterface;
    }

    public TextureView getPreviewSurfaceTexture() {
        return mSurfaceView;
    }

    public void updateUINotRecording() {
        mRecordBtnIv.setSelected(false);
        mRecordBtnIv.setVisibility(View.VISIBLE);
//        mAcceptBtnIv.setVisibility(View.GONE);
//        mDeclineBtnIv.setVisibility(View.GONE);
        mThumbnailIv.setVisibility(View.GONE);
        mSurfaceView.setVisibility(View.VISIBLE);
    }

    public void updateUIRecordingOngoing() {
        mRecordBtnIv.setSelected(true);
        mRecordBtnIv.setVisibility(View.VISIBLE);
//        mAcceptBtnIv.setVisibility(View.GONE);
//        mDeclineBtnIv.setVisibility(View.GONE);
        mThumbnailIv.setVisibility(View.GONE);
        mSurfaceView.setVisibility(View.VISIBLE);
    }

    public void updateUIRecordingFinished(Bitmap videoThumbnail) {
        mRecordBtnIv.setVisibility(View.INVISIBLE);
//        mAcceptBtnIv.setVisibility(View.VISIBLE);
//        mDeclineBtnIv.setVisibility(View.VISIBLE);

        mThumbnailIv.setVisibility(View.VISIBLE);
        mSurfaceView.setVisibility(View.GONE);
        final Bitmap thumbnail = videoThumbnail;
        if (thumbnail != null) {
            mThumbnailIv.setScaleType(ScaleType.CENTER_CROP);
            mThumbnailIv.setImageBitmap(videoThumbnail);
        }
        llConfirm.setVisibility(VISIBLE);
        startAnimationLlConfirm();
    }

    private void startAnimationLlConfirm() {
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                ObjectAnimator.ofFloat(llConfirm, "alpha", 0.0f, 1.0f, 1).setDuration(400),
                ObjectAnimator.ofFloat(llConfirm, "y", -llConfirm.getHeight(), 0.0f, 1).setDuration(400)
                );
        animatorSet.start();

    }

    @Override
    public void onClick(View v) {
        if (mRecordingInterface == null) return;

        if (v.getId() == mRecordBtnIv.getId()) {
            btnFacing.setVisibility(GONE);
            mRecordingInterface.onRecordButtonClicked();
        } else if (v.getId() == mAcceptBtnIv.getId()) {
            mRecordingInterface.onAcceptButtonClicked();
        } else if (v.getId() == mDeclineBtnIv.getId()) {
            mRecordingInterface.onDeclineButtonClicked();
        }else if ( v.getId() == btnFacing.getId()) {
            if ( isFacingFront ) {
                if ( hasCameraBack ) {
                    mRecordingInterface.changeCameraFacing(false);
                    isFacingFront = false;
                    btnFacing.setImageResource(R.drawable.ic_camera_back);
                }else {
                    Toast.makeText(getContext(), "Can not open front camera", Toast.LENGTH_LONG).show();
                }
            }else {
                if ( hasCameraFront ) {
                    mRecordingInterface.changeCameraFacing(true);
                    isFacingFront = true;
                    btnFacing.setImageResource(R.drawable.ic_camera_front);
                }else {
                    Toast.makeText(getContext(), "Can not open back camera", Toast.LENGTH_LONG).show();
                }
            }
        }

    }

    public boolean isFacingFront() {
        return isFacingFront;
    }

}
