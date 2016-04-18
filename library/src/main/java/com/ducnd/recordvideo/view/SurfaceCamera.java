package com.ducnd.recordvideo.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import com.ducnd.recordvideo.preview.CapturePreview;

/**
 * Created by ducnd on 16/04/2016.
 */
public class SurfaceCamera extends TextureView implements CapturePreview.ChangeSizeView{
    private static final String TAG = SurfaceCamera.class.getSimpleName();
    private int widht = 0, height = 0;

    public SurfaceCamera(Context context) {
        super(context);
    }

    public SurfaceCamera(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SurfaceCamera(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    public void chageSize(int widht, int height) {
        this.widht = widht;
        this.height = height;
        setMeasuredDimension(this.widht, this.height);
        Log.i(TAG, "chageSize width: " + widht + " , height: " + height);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if ( height > 0 ) {
            setMeasuredDimension(this.widht, this.height);
        }

    }
}
