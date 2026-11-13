package com.nlu.shared.infrastructure.cache;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Binary encoder/decoder for storing HTTP responses in Redis without double JSON escaping
 * and with transparent Gzip compression for large payloads.
 */
@Component
public class HotCachePayloadCodec {

    private static final int MAX_CONTENT_TYPE_BYTES = 1_024;

    public byte[] encode(CachedHttpResponse response, int gzipThresholdBytes) {
        if (response == null) {
            throw new IllegalArgumentException("Cached response must not be null");
        }
        byte[] rawBody = response.body() != null ? response.body() : new byte[0];
        String contentType = (response.contentType() != null && !response.contentType().isBlank())
                ? response.contentType()
                : MediaType.APPLICATION_JSON_VALUE;
        byte[] contentTypeBytes = contentType.getBytes(StandardCharsets.UTF_8);
        if (contentTypeBytes.length > MAX_CONTENT_TYPE_BYTES) {
            throw new IllegalArgumentException("Content-Type exceeds max length: " + contentTypeBytes.length);
        }

        byte encoding = HotCacheConstants.ENCODING_IDENTITY;
        byte[] payloadToStore = rawBody;
        if (rawBody.length > gzipThresholdBytes) {
            byte[] compressed = gzip(rawBody);
            if (compressed.length < rawBody.length) {
                encoding = HotCacheConstants.ENCODING_GZIP;
                payloadToStore = compressed;
            }
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(7 + contentTypeBytes.length + payloadToStore.length);
            DataOutputStream dataOut = new DataOutputStream(out);
            dataOut.writeInt(response.httpStatus());
            dataOut.writeByte(encoding);
            dataOut.writeShort(contentTypeBytes.length);
            dataOut.write(contentTypeBytes);
            dataOut.write(payloadToStore);
            dataOut.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode cached HTTP response", e);
        }
    }

    public CachedHttpResponse decode(byte[] rawBytes) {
        if (rawBytes == null || rawBytes.length < 7) {
            throw new IllegalArgumentException("Invalid cached payload byte length");
        }

        try {
            DataInputStream dataIn = new DataInputStream(new ByteArrayInputStream(rawBytes));
            int httpStatus = dataIn.readInt();
            byte encoding = dataIn.readByte();
            int contentTypeLength = dataIn.readUnsignedShort();
            if (contentTypeLength > MAX_CONTENT_TYPE_BYTES || dataIn.available() < contentTypeLength) {
                throw new IllegalStateException("Corrupted cached content-type header");
            }

            byte[] contentTypeBytes = new byte[contentTypeLength];
            dataIn.readFully(contentTypeBytes);
            String contentType = new String(contentTypeBytes, StandardCharsets.UTF_8);

            byte[] storedBody = dataIn.readAllBytes();
            byte[] decodedBody = (encoding == HotCacheConstants.ENCODING_GZIP) ? gunzip(storedBody) : storedBody;
            return new CachedHttpResponse(httpStatus, contentType, decodedBody);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to decode cached HTTP response", e);
        }
    }

    private byte[] gzip(byte[] input) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(input.length / 2);
            try (GZIPOutputStream gzipOut = new GZIPOutputStream(out)) {
                gzipOut.write(input);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to Gzip-compress cached response body", e);
        }
    }

    private byte[] gunzip(byte[] compressed) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(compressed.length * 2);
            try (GZIPInputStream gzipIn = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
                gzipIn.transferTo(out);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to Gzip-decompress cached response body", e);
        }
    }
}

