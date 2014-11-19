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

package org.visualink.displee;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Log;

public class Util {

    static public final String SERVER = "http://visualink.io/displeeBE/engine/";
    //static public final String SERVER = "http://pastec4.visualink.io:8000/engine/";
    static public final int API_VERSION = 1;

    static public void informNoGoodResults(final String reqId) {

        new Thread(new Runnable() {
            public void run() {
                // Create the JSON.
                JSONObject json = new JSONObject();
                try {
                    json.put("api_version", API_VERSION);
                    json.put("req_id", reqId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(SERVER + "noGoodResults");
                    StringEntity se = new StringEntity(json.toString());
                    se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                    httpPost.setEntity(se);
                    HttpResponse response = httpClient.execute(httpPost);

                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    StringBuilder strBuilder = new StringBuilder();
                    for (String line = null; (line = reader.readLine()) != null;) {
                        strBuilder.append(line);
                    }
                    JSONTokener tokener = new JSONTokener(strBuilder.toString());
                    try {
                        JSONObject res = new JSONObject(tokener);
                        boolean success = res.getBoolean("success");
                        if (success == true) {
                            Log.i("Displee", "No good results information sent.");
                        }
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
