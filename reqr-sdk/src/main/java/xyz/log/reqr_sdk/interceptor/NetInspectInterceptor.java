package xyz.log.reqr_sdk.interceptor;

import android.content.Context;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import xyz.log.reqr_sdk.database.NetworkLogDao;
import xyz.log.reqr_sdk.database.NetworkLogDatabase;
import xyz.log.reqr_sdk.model.NetworkLog;

public class NetInspectInterceptor implements Interceptor {

    private static final long MAX_BODY_SIZE = 1024 * 1024; // 1 MB limit to prevent OOM
    private final NetworkLogDao logDao;
    private final ExecutorService executor;

    public NetInspectInterceptor(Context context) {
        this.logDao = NetworkLogDatabase.getDatabase(context).networkLogDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        
        long startTime = System.currentTimeMillis();
        
        NetworkLog log = new NetworkLog();
        log.timestamp = startTime;
        log.method = request.method();
        log.url = request.url().toString();
        log.host = request.url().host();
        log.path = request.url().encodedPath();
        
        log.requestHeaders = headersToString(request.headers());
        log.requestBody = getRequestBody(request);

        Response response;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            log.durationMs = System.currentTimeMillis() - startTime;
            log.responseCode = 0; // Or some standard error code
            log.responseBody = "Error: " + e.getMessage();
            saveLogAsync(log);
            throw e;
        }

        log.durationMs = System.currentTimeMillis() - startTime;
        log.responseCode = response.code();
        log.responseHeaders = headersToString(response.headers());
        log.responseBody = getResponseBody(response);

        saveLogAsync(log);

        return response;
    }

    private void saveLogAsync(NetworkLog log) {
        executor.execute(() -> logDao.insert(log));
    }

    private String headersToString(Headers headers) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, size = headers.size(); i < size; i++) {
            sb.append(headers.name(i)).append(": ").append(headers.value(i)).append("\n");
        }
        return sb.toString();
    }

    private String getRequestBody(Request request) {
        RequestBody requestBody = request.body();
        if (requestBody == null) return null;
        
        try {
            Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);
            
            if (isPlaintext(buffer)) {
                if (buffer.size() > MAX_BODY_SIZE) {
                    return "[Body too large to inspect reliably (" + buffer.size() + " bytes)]";
                }
                Charset charset = StandardCharsets.UTF_8;
                MediaType contentType = requestBody.contentType();
                if (contentType != null) {
                    charset = contentType.charset(StandardCharsets.UTF_8);
                }
                return buffer.readString(charset);
            } else {
                return "[Binary data or unsupported format]";
            }
        } catch (Exception e) {
            return "Error reading request body: " + e.getMessage();
        }
    }

    private String getResponseBody(Response response) {
        ResponseBody responseBody = response.body();
        if (responseBody == null) return null;
        
        try {
            BufferedSource source = responseBody.source();
            source.request(Long.MAX_VALUE); // Buffer the entire body
            Buffer buffer = source.getBuffer();
            
            if (isPlaintext(buffer)) {
                if (buffer.size() > MAX_BODY_SIZE) {
                    return "[Body too large to inspect reliably (" + buffer.size() + " bytes)]";
                }
                Charset charset = StandardCharsets.UTF_8;
                MediaType contentType = responseBody.contentType();
                if (contentType != null) {
                    charset = contentType.charset(StandardCharsets.UTF_8);
                }
                if (responseBody.contentLength() != 0) {
                    return buffer.clone().readString(charset);
                }
            } else {
                return "[Binary data or unsupported format]";
            }
        } catch (Exception e) {
            return "Error reading response body: " + e.getMessage();
        }
        return null;
    }

    /**
     * Attempts to determine if the buffer contains mostly human-readable plaintext
     * instead of arbitrary binary data (like images or compiled blobs).
     */
    private boolean isPlaintext(Buffer buffer) {
        try {
            Buffer prefix = new Buffer();
            long byteCount = Math.min(buffer.size(), 64);
            buffer.copyTo(prefix, 0, byteCount);
            for (int i = 0; i < 16; i++) {
                if (prefix.exhausted()) {
                    break;
                }
                int codePoint = prefix.readUtf8CodePoint();
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
