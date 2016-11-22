/*  
 * The Bloop Library.
 * Copyright (c) 2013 Benjamin Billet.
 * http://benjaminbillet.fr/wiki/doku.php?id=bloop
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.loopj.android.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * An implementation of the HTTP {@code Transfer-Encoding: chunked} as an
 * {@link OutputStream}. <br>
 * Basically, when data are written into this stream, they are buffered in
 * memory until {@link ChunkedOutputStream#flush()} is invoked. Then, the
 * buffered data are written into the underlying stream as a new chunk.
 * <p>
 * When {@link ChunkedOutputStream#close()} is called, a new chunk is written if
 * there is pending data into the buffer, and then the termination chunk is
 * written before closing the underlying {@link OutputStream}.<br>
 * For application that would need to reuse the underlying {@link OutputStream}
 * elsewhere, {@link ChunkedOutputStream#finish()} can be called to write the
 * termination chunk without closing the underlying {@link OutputStream}. <br>
 * Once the termination chunk is written, any attempt to write data into this
 * stream will throw an {@link IllegalStateException}.
 * <p>
 * In any case, the termination chunk MUST be written, by calling either
 * {@link ChunkedOutputStream#finish()} or {@link ChunkedOutputStream#close()}.
 * Without this last chunk, the HTTP client/server will wait indefinitely and
 * fail.
 * <p>
 * Example of chunk encoding (where \r and \n are CR and LF):
 * <pre>
 * 1a\r\n
 * abcdefghijklmnopqrstuvwxyz\r\n
 * 5\r\n
 * abcde\r\n
 * a\r\n
 * abcdefghij\r\n
 * 0\r\n
 * \r\n
 * </pre>
 * See <a href="http://tools.ietf.org/html/rfc2616#section-3.6.1">RF2616</a> for
 * more details about chunk encoding.
 * 
 * @version 0.1
 * @author Benjamin Billet
 */
public class ChunkedOutputStream extends OutputStream
{
	private ByteArrayOutputStream buffer;
	private OutputStream output;
	private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);
	private static final byte[] DOUBLE_CRLF = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);
	private boolean finished = false;

	/**
	 * Creates a new {@code ChunkedOutputStream} that writes chunk-encoded
	 * blocks into an existing output stream.
	 * @param output the output stream.
	 */
	public ChunkedOutputStream(OutputStream output)
	{
		this(output, 32);
	}
	
	public ChunkedOutputStream(OutputStream output, int initialBufferCapacity)
	{
		this.buffer = new ByteArrayOutputStream(initialBufferCapacity);
		this.output = output;
	}
	
	@Override
	public void flush()	throws IOException
	{
		assertNotFinished();
		
		int chunkSize = buffer.size();
		if(chunkSize > 0)
		{
			output.write(Integer.toHexString(chunkSize).getBytes());
			output.write(CRLF);
			buffer.writeTo(output);
			output.write(CRLF);
			output.flush();
		}
		buffer.reset();
	}
	
	@Override
	public void close()	throws IOException
	{
		if(finished == false)
			finish();
		
		output.close();
	}
	
	public void finish() throws IOException
	{
		flush();
		output.write('0');
		output.write(DOUBLE_CRLF);
		output.flush();
		finished = true;
	}

	@Override
	public void write(int b) throws IOException
	{
		assertNotFinished();
		buffer.write(b);
	}
	
	@Override
	public void write(byte bytes[]) throws IOException
	{
		assertNotFinished();
		buffer.write(bytes);
	}

	@Override
	public void write(byte bytes[], int offset, int length) throws IOException
	{
		assertNotFinished();
		buffer.write(bytes, offset, length);
	}
	
	private void assertNotFinished() throws IllegalStateException
	{
		if(finished)
			throw new IllegalStateException("the termination chunk was sent");
	}
}

