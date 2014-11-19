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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.visualink.displee.R;
import org.visualink.displee.ResultEntry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ResultAdapter extends BaseAdapter {

    private final ArrayList<ResultEntry> mResults;
    private final Map<String, Bitmap> mImages;
    private final Semaphore mImageSemaphore = new Semaphore(1);
    private LayoutInflater mInflater;

    private static final int IMAGE_RECEIVED = 0;

    public ResultAdapter(Context context) {
        mResults = new ArrayList<ResultEntry>();
        mImages = new HashMap<String, Bitmap>();
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mResults.size();
    }

    @Override
    public Object getItem(int position) {
        return mResults.get(position);
    }

    public Object addItem(ResultEntry r) {
        return mResults.add(r);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    private class ViewHolder {
        TextView name;
        TextView description;
        ImageView image;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        View v = convertView;

        /* If view not created */
        if (v == null) {
            v = mInflater.inflate(R.layout.result_item, parent, false);
            holder = new ViewHolder();
            holder.name = (TextView) v.findViewById(R.id.name);
            holder.description = (TextView) v.findViewById(R.id.description);
            holder.image = (ImageView) v.findViewById(R.id.image);
            v.setTag(holder);
        } else
            holder = (ViewHolder) v.getTag();

        final ResultEntry r = (ResultEntry)getItem(position);
        holder.name.setText(r.name);
        holder.description.setText(r.description);

        try {
            mImageSemaphore.acquire();
            final Bitmap bitmap = mImages.get(r.urlImage);
            mImageSemaphore.release();
            holder.image.setImageBitmap(bitmap);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return v;
    }

    public void getImages() {
        new GetImageTask().execute(mResults.toArray(new ResultEntry[mResults.size()]));
    }

    private class GetImageTask extends AsyncTask<ResultEntry, Void, Void> {

        protected Void doInBackground(ResultEntry... entries) {

            for (int i = 0; i < entries.length; ++i) {

                String url = entries[i].urlImage;
                ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                try {
                    InputStream in = new java.net.URL(url).openStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) != -1)
                        byteBuffer.write(buffer, 0, len);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                entries[i].imageData = byteBuffer.toByteArray();

                try {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(entries[i].imageData,
                            0, entries[i].imageData.length);
                    mImageSemaphore.acquire();
                    mImages.put(entries[i].urlImage, bitmap);
                    mImageSemaphore.release();
                    mHandler.sendEmptyMessage(IMAGE_RECEIVED);
                } catch (NullPointerException e) {
                    Log.e("ResultAdapter", "Image " + i + " could not be loaded.");
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case IMAGE_RECEIVED:
                ResultAdapter.this.notifyDataSetChanged();
                break;
            }
        }
    };

}
