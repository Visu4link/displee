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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.visualink.displee.ResultEntry;
import org.visualink.displee.R;
import org.visualink.displee.Util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class ResultActivity extends Activity {

    private LinearLayout mNoResultLayout;
    private LinearLayout mResultLayout;
    private ImageButton mRebegin;
    private ImageButton mBackButton;
    private ImageButton mNoGoodResultsButton;
    private TextView mErrorTextView;
    private ListView mResults;
    private ResultAdapter mResultAdapter;

    private String mReqId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        mNoResultLayout = (LinearLayout)findViewById(R.id.no_result_layout);
        mResultLayout = (LinearLayout)findViewById(R.id.result_layout);
        mRebegin = (ImageButton)findViewById(R.id.rebegin);
        mBackButton = (ImageButton)findViewById(R.id.back);
        mNoGoodResultsButton = (ImageButton)findViewById(R.id.no_good_results);
        mErrorTextView = (TextView)findViewById(R.id.error);
        mResults = (ListView)findViewById(R.id.result);
        mResultAdapter = new ResultAdapter(this);
        mResults.setAdapter(mResultAdapter);

        mResults.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                ResultEntry r = (ResultEntry)mResultAdapter.getItem(position);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(r.urlPage));
                startActivity(i);
            }
        });

        mRebegin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mBackButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ResultActivity.this.finish();
            }
        });

        mNoGoodResultsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Util.informNoGoodResults(mReqId);
                ResultActivity.this.finish();
            }
        });

        mHandler.sendEmptyMessage(SHOW_WAITING_DIALOG);
        mSendingThread = new SendingThread();
        mSendingThread.start();
    }

    private SendingThread mSendingThread;
    private ProgressDialog mProgressDialog;

    private class SendingThread extends Thread {

        public void run() {
            byte[] imageBytes = CaptureActivity.imageBytes;
            Bitmap img = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

            int newWidth;
            int newHeight;
            if (img.getHeight() > img.getWidth()) {
                newWidth = 500;
                newHeight = (int)((float)newWidth / CaptureActivity.mImageRatio);
            }
            else {
                newHeight = 500;
                newWidth = (int)((float)newHeight * CaptureActivity.mImageRatio);
            }

            Bitmap scaledImg = Bitmap.createScaledBitmap(img, newWidth, newHeight, false);

            ByteArrayOutputStream imgByteStream = new ByteArrayOutputStream();
            scaledImg.compress(Bitmap.CompressFormat.JPEG, 75, imgByteStream);

            Log.d("Pastec", "Image file size: " + imgByteStream.size());

            /* Create the JSON. */
            JSONObject json = new JSONObject();
            try {
                json.put("api_version", Util.API_VERSION);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            /* Prepare the request to the server. */
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(Util.SERVER + "results");
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            ByteArrayBody file = new ByteArrayBody(imgByteStream.toByteArray(), "userfile.jpg");
            builder.addPart("userfile", file);
            builder.addTextBody("json", json.toString());

            httppost.setEntity(builder.build());
            try {
                HttpResponse response = httpclient.execute(httppost);

                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                StringBuilder strBuilder = new StringBuilder();
                for (String line = null; (line = reader.readLine()) != null;) {
                    strBuilder.append(line);
                }

                JSONTokener tokener = new JSONTokener(strBuilder.toString());
                try {
                    JSONObject res = new JSONObject(tokener);
                    boolean success = res.getBoolean("success");
                    mReqId = res.getString("req_id");
                    if (success == true) {
                        JSONArray results = res.getJSONArray("results");
                        for (int i = 0; i < results.length(); ++i) {
                            JSONObject result = results.getJSONObject(i);
                            ResultEntry r = new ResultEntry();
                            r.id = result.getInt("id");
                            r.name = result.getString("name");
                            r.urlImage = result.getString("url_image");
                            r.urlPage = result.getString("url_page");
                            r.description = result.getString("description");
                            mResultAdapter.addItem(r);
                        }
                    }
                    if (mResultAdapter.getCount() > 0)
                        mHandler.sendEmptyMessage(RESULT_RECEIVED);
                    else
                        mHandler.sendEmptyMessage(NO_RESULT);
                } catch (JSONException e) {
                    mHandler.sendEmptyMessage(SERVICE_DOWN);
                    e.printStackTrace();
                }
            } catch (ClientProtocolException e) {
                mHandler.sendEmptyMessage(SERVICE_DOWN);
                e.printStackTrace();
            } catch (IOException e) {
                mHandler.sendEmptyMessage(SERVICE_DOWN);
                e.printStackTrace();
            }

            mHandler.sendEmptyMessage(HIDE_WAITING_DIALOG);
        }
    }

    public byte[] requestResponse;

    private static final int RESULT_RECEIVED = 1;
    private static final int NO_RESULT = 11;
    private static final int SERVICE_DOWN = 12;
    private static final int SHOW_WAITING_DIALOG = 2;
    private static final int HIDE_WAITING_DIALOG = 3;

    private final Handler mHandler = new MyHandler();

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case RESULT_RECEIVED:
                mResultAdapter.getImages();
                mResultAdapter.notifyDataSetChanged();
                mNoResultLayout.setVisibility(View.GONE);
                mResultLayout.setVisibility(View.VISIBLE);
                break;
            case NO_RESULT:
                mNoResultLayout.setVisibility(View.VISIBLE);
                mResultLayout.setVisibility(View.GONE);
                mErrorTextView.setText(ResultActivity.this.getText(R.string.no_result));
                break;
            case SERVICE_DOWN:
                mNoResultLayout.setVisibility(View.VISIBLE);
                mResultLayout.setVisibility(View.GONE);
                mErrorTextView.setText(ResultActivity.this.getText(R.string.service_down));
                break;
            case SHOW_WAITING_DIALOG:
                mProgressDialog = new ProgressDialog(ResultActivity.this, R.style.ProgressDialog);
                mProgressDialog.setMessage(ResultActivity.this.getText(R.string.retrieving));
                mProgressDialog.setCanceledOnTouchOutside(false);
                mProgressDialog.setCancelable(true);
                mProgressDialog.setOnCancelListener(new MyCancelListener());
                mProgressDialog.show();
                break;
            case HIDE_WAITING_DIALOG:
                mProgressDialog.setOnCancelListener(null);
                mProgressDialog.cancel();
                break;
            }
        }
    };

    private class MyCancelListener implements ProgressDialog.OnCancelListener {
        @Override
        public void onCancel(DialogInterface dialog) {
            mSendingThread.interrupt();
            finish();
        }
    };
}
