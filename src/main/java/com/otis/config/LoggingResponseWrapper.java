package com.otis.config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UncheckedIOException;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

public class LoggingResponseWrapper extends HttpServletResponseWrapper {
	private final ByteArrayOutputStream capture = new ByteArrayOutputStream();
	private ServletOutputStream outputStream;
	private PrintWriter writer;

	public LoggingResponseWrapper(HttpServletResponse response) {
		super(response);
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (writer != null) {
			throw new IllegalStateException("getWriter() has already been called");
		}
		if (outputStream == null) {
			outputStream = new ServletOutputStream() {
				@Override
				public void write(int b) throws IOException {
					capture.write(b); // capture the byte
					getResponse().getOutputStream().write(b); // forward to original
				}

				@Override
				public boolean isReady() {
					return true;
				}

				@Override
				public void setWriteListener(WriteListener listener) {
					// Not used - async writes are handled by Jetty's non-blocking API
					// The capture is already done synchronously in write(int b)
				}
			};
		}
		return outputStream;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (outputStream != null) {
			throw new IllegalStateException("getOutputStream() has already been called");
		}

		if (writer == null) {
			writer = new PrintWriter(new OutputStreamWriter(capture, getCharacterEncoding())) {
				@Override
				public void write(String s) {
					super.write(s);
					try {
						getResponse().getWriter().write(s); // forward to original
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}
			};
		}
		return writer;
	}

	public byte[] getCapturedBody() {
		return capture.toByteArray();
	}
}
