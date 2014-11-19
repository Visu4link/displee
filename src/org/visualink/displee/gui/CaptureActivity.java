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

import java.io.ByteArrayOutputStream;

import org.visualink.displee.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera.Size;
import android.view.View;
import android.widget.ImageButton;

public class CaptureActivity extends Activity {

    private Preview mPreview;

    public static byte[] imageBytes;
    public static float mImageRatio;

    @Override
    public void onResume() {
        super.onResume();

        setContentView(R.layout.activity_capture);
        mPreview = (Preview)this.findViewById(R.id.cameraView);
        ImageButton photoButton = (ImageButton) this.findViewById(R.id.button1);
        ImageButton helpButton = (ImageButton) this.findViewById(R.id.help);

        mPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPreview.autoFocus();
            }
        });

        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                YuvImage image = mPreview.getPreviewImage();
                Size size = mPreview.getPreviewSize();

                // Set all the chrominance to 0.
                int yPlaneSize = size.width * size.height;
                byte data[] = image.getYuvData();
                for (int i = yPlaneSize; i < yPlaneSize * 3 / 2; ++i)
                    data[i] = -128;

                Rect rectangle = new Rect();
                rectangle.bottom = size.height;
                rectangle.top = 0;
                rectangle.left = 0;
                rectangle.right = size.width;
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                image.compressToJpeg(rectangle, 100, outStream);
                imageBytes = outStream.toByteArray();
                mImageRatio = (float)size.width / size.height;
                Intent intent = new Intent(CaptureActivity.this, ResultActivity.class);
                startActivity(intent);
            }
        });

        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHelpDialog();
            }
        });

        displayHelpAtFirstRun();

        mPreview.startCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        mPreview.stopCamera();
    }

    private void showHelpDialog() {
        final Dialog helpDialog = new Dialog(this, R.style.Dialog);
        helpDialog.setContentView(R.layout.capture_help_dialog);
        helpDialog.show();
    }

    private void displayHelpAtFirstRun() {
        boolean firstrun = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("firstrun", true);
        if (firstrun){
            showHelpDialog();
            // Save the state
            getSharedPreferences("PREFERENCE", MODE_PRIVATE)
            .edit()
            .putBoolean("firstrun", false)
            .commit();
        }
    }
}