package com.loopj.android.http.sample;

import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.ResponseHandlerInterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Random;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpEntity;

public class ChunkedSample extends PostSample {

    public static final String LOG_TAG = "ChunkedSample";

    @Override
    public int getSampleTitle() {
        return R.string.title_post_chunked;
    }

    @Override
    public boolean isRequestBodyAllowed() {
        return false;
    }

    @Override
    public RequestHandle executeSample(AsyncHttpClient client, String URL, Header[] headers, HttpEntity entity, ResponseHandlerInterface responseHandler) {
        try {
            RequestParams params = new RequestParams();
            final String contentType = RequestParams.APPLICATION_OCTET_STREAM;
            params.put("fileOne", createTempFile("fileOne", 8192), contentType);
            params.setUseJsonStreamer(false);
            params.setChunkedRequest(true);
            return client.post(this, URL, params, responseHandler);
        } catch (FileNotFoundException fnfException) {
            Log.e(LOG_TAG, "executeSample failed with FileNotFoundException", fnfException);
        }
        return null;
    }

    public File createTempFile(String namePart, int byteSize) {
        try {
            File f = File.createTempFile(namePart, "_handled", getCacheDir());
            FileOutputStream fos = new FileOutputStream(f);
            Random r = new Random();
            byte[] buffer = new byte[byteSize];
            r.nextBytes(buffer);
            fos.write(buffer);
            fos.flush();
            fos.close();
            return f;
        } catch (Throwable t) {
            Log.e(LOG_TAG, "createTempFile failed", t);
        }
        return null;
    }
}
