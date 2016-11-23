/*
    Android Asynchronous Http Client
    Copyright (c) 2011 James Smith <james@loopj.com>
    https://loopj.com

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

/*
    This code is taken from Rafael Sanches' blog. Link is no longer working (as of 17th July 2015)
    https://blog.rafaelsanches.com/2011/01/29/upload-using-multipart-post-using-httpclient-in-android/
*/

package com.loopj.android.http;

import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.message.BasicHeader;
import cz.msebera.android.httpclient.protocol.HTTP;

/**
 * Simplified chunked entity mainly used for sending one or more files.
 */
class SimpleChunkedEntity implements HttpEntity {

    private static final String LOG_TAG = "SimpleChunkedEntity";

    private static final String STR_CR_LF = "\r\n";
    private static final byte[] CR_LF = STR_CR_LF.getBytes();

    private final ResponseHandlerInterface progressHandler;
    private boolean isRepeatable;
    private long bytesWritten;

    private long totalSize;

    private RequestParams.FileWrapper file;

    public SimpleChunkedEntity(ResponseHandlerInterface progressHandler) {
        this.progressHandler = progressHandler;
    }

    public void setFile(String key, RequestParams.FileWrapper value) {
        this.file = value;
    }

    private void updateProgress(long count) {
        bytesWritten += count;
        progressHandler.sendProgressMessage(bytesWritten, totalSize);
    }

    @Override
    public long getContentLength() {
        return -1;
    }

    @Override
    public Header getContentType() {
        return new BasicHeader(
                AsyncHttpClient.HEADER_CONTENT_TYPE,
                file.contentType);
    }

    @Override
    public boolean isChunked() {
        return true;
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public boolean isStreaming() {
        return false;
    }

    @Override
    public void writeTo(final OutputStream outstream) throws IOException {
        bytesWritten = 0;
        totalSize = (int) getContentLength();

        FileInputStream inputStream = new FileInputStream(file.file);
        final byte[] tmp = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(tmp)) != -1) {
            outstream.write(tmp, 0, bytesRead);
            outstream.flush();
            updateProgress(bytesRead);
        }
        AsyncHttpClient.silentCloseInputStream(inputStream);
    }

    @Override
    public Header getContentEncoding() {
        return null;
    }

    @Override
    public void consumeContent() throws IOException, UnsupportedOperationException {
        if (isStreaming()) {
            throw new UnsupportedOperationException(
                    "Streaming entity does not implement #consumeContent()");
        }
    }

    @Override
    public InputStream getContent() throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "getContent() is not supported. Use writeTo() instead.");
    }
}
