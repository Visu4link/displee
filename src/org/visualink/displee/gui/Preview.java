/******************************************************************************
* The MIT License (MIT)
*
* Copyright (c) 2014 Visualink
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
******************************************************************************/

package org.visualink.displee.gui;

import java.io.IOException;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple wrapper around a Camera and a SurfaceView that renders a centered preview of the Camera
 * to the surface. We need to center the SurfaceView because not all devices have cameras that
 * support preview sizes at the same aspect ratio as the device's display.
 */
class Preview extends ViewGroup implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private final String TAG = "Preview";
    private Context mContext;
    private byte[] mImageData;
    private Size mPreviewSize;
    private String mPreviewFocusMode;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private List<Size> mSupportedPreviewSizes;
    private List<String> mSupportedFocusModes;
    private Camera mCamera;

    Preview(Context context) {
        super(context);
        init(context);
    }

    public Preview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
    }

    public void startCamera() {
        mCamera = Camera.open();
        mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
        mSupportedFocusModes = mCamera.getParameters().getSupportedFocusModes();
        requestLayout();
    }

    public void stopCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
        }
    }


    public void autoFocus() {
        if (mCamera != null)
            mCamera.autoFocus(null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);
        if (mSupportedPreviewSizes != null)
            mPreviewSize = getOptimalPreviewSize(width, height);
        if (mSupportedFocusModes != null)
            mPreviewFocusMode = getOptimalFocusMode();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);
            final int width = r - l;
            final int height = b - t;
            child.layout(0, 0, width, height);
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    private Size getOptimalPreviewSize(int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)1024 / 768;

        if (mSupportedPreviewSizes == null)
            return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : mSupportedPreviewSizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : mSupportedPreviewSizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @SuppressLint("InlinedApi")
    private String getOptimalFocusMode() {
        String mode;
        if (mSupportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
            mode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
        else if (mSupportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
            mode = Camera.Parameters.FOCUS_MODE_AUTO;
        else
            mode = Camera.Parameters.FOCUS_MODE_MACRO;
        return mode;

    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        parameters.setFocusMode(mPreviewFocusMode);

        setCameraDisplayOrientation(mContext, 0, mCamera);

        requestLayout();
        mCamera.setParameters(parameters);
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);
        //mCamera.autoFocus(null);
    }

    public int getCameraOrientation(Activity activity) {
        return activity.getWindowManager().getDefaultDisplay().getRotation();
    }

    public static void setCameraDisplayOrientation(Context context,
            int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = ((Activity)context).getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
        case Surface.ROTATION_0: degrees = 0; break;
        case Surface.ROTATION_90: degrees = 90; break;
        case Surface.ROTATION_180: degrees = 180; break;
        case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        mImageData = data;
    }

    public YuvImage getPreviewImage() {
        YuvImage image = new YuvImage(mImageData, ImageFormat.NV21, mPreviewSize.width, mPreviewSize.height, null);
        return image;
    }

    public Size getPreviewSize() {
        return mPreviewSize;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

}
