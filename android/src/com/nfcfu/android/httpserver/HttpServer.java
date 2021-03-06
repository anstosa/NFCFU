/*
 * Based on the HTTP Server Example found at:
 * http://www.docjar.org/html/api/org/apache/http/examples/ElementalHttpServer.java.html
 */

package com.nfcfu.android.httpserver;

import android.util.Log;
import com.nfcfu.android.FileAccessor;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.*;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpServer {
    private RequestListener listener;

    public HttpServer() throws IOException {
        Log.v(this.getClass().getSimpleName(), "Creating new HTTP Server");

        listener = new RequestListener(8080);
        Thread t = new Thread(listener);
        t.start();
    }

    public void stop() {
        Log.v(this.getClass().getSimpleName(), "Stopping HTTP Server");
        listener.stop();
    }

    static class HttpFileHandler implements HttpRequestHandler {
        @Override
        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {

            String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
            if (method.equals("GET")) {
                // Display an informational message

                response.setStatusCode(HttpStatus.SC_OK);
                StringEntity body = new StringEntity("This web page is intended only for POST request containing multipart file uploads");
                response.setEntity(body);
            } else if (method.equals("POST")) {
                // Check that the request is multipart/form-data and handle

                StringEntity body = null;
                String contentType = getContentType(request);
                if (contentType != null && contentType.equals("multipart/form-data") && request instanceof HttpEntityEnclosingRequest) {
                    HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                    byte[] entityContent = EntityUtils.toByteArray(entity);

                    MultipartParser parser = new MultipartParser(entityContent);

                    File writeTo = new File(FileAccessor.getRootFile(), parser.getFileName());
                    FileOutputStream output = new FileOutputStream(writeTo);
                    IOUtils.write(parser.getFileBytes(), output);

                    body = new StringEntity("File uploaded");
                } else {
                    body = new StringEntity("Received a POST request!");
                }

                response.setStatusCode(HttpStatus.SC_OK);
                response.setEntity(body);
            } else {
                throw new MethodNotSupportedException(method + " method not supported");
            }
        }

        private String getContentType(HttpRequest request) {
            if (request.containsHeader("Content-Type")) {
                Header[] headers = request.getHeaders("Content-Type");

                if (headers.length == 0) {
                    return null;
                }

                if (headers[0].getValue().contains("multipart/form-data")) {
                    return "multipart/form-data";
                } else {
                    return headers[0].getValue();
                }
            }
            return null;
        }
    }

    static class RequestListener implements Runnable {
        private final ServerSocket serversocket;
        private final HttpParams params;
        private final HttpService httpService;

        private AtomicBoolean running;
        private List<Worker> workers;

        public RequestListener(int port) throws IOException {
            this.serversocket = new ServerSocket(port);
            this.params = new BasicHttpParams();
            this.params
                    .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
                    .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
                    .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
                    .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
                    .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1");

            // Set up the HTTP protocol processor
            BasicHttpProcessor httpproc = new BasicHttpProcessor();
            httpproc.addInterceptor(new ResponseDate());
            httpproc.addInterceptor(new ResponseServer());
            httpproc.addInterceptor(new ResponseContent());
            httpproc.addInterceptor(new ResponseConnControl());

            // Set up request handlers
            HttpRequestHandlerRegistry reqistry = new HttpRequestHandlerRegistry();
            reqistry.register("*", new HttpFileHandler());

            // Set up the HTTP service
            this.httpService = new HttpService(
                    httpproc,
                    new DefaultConnectionReuseStrategy(),
                    new DefaultHttpResponseFactory());
            this.httpService.setParams(this.params);
            this.httpService.setHandlerResolver(reqistry);

            workers = new ArrayList<Worker>();
        }

        @Override
        public void run() {
            running = new AtomicBoolean(true);

            while (running.get()) {
                try {
                    // Set up HTTP connection
                    Socket socket = this.serversocket.accept();
                    DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
                    conn.bind(socket, this.params);

                    // Start worker thread
                    Worker w = new Worker(this.httpService, conn);
                    Thread t = new Thread(w);
                    t.setDaemon(true);
                    t.start();

                    workers.add(w);
                } catch (InterruptedIOException ex) {
                    break;
                } catch (IOException e) {
                    Log.e(this.getClass().getSimpleName(), "I/O error initialising connection thread: " + e.getMessage());
                    break;
                }
            }
        }

        public void stop() {
            running.set(false);

            for(Worker worker : workers) {
                if (worker != null) {
                    worker.stop();
                }
            }

            try {
                this.serversocket.close();
            } catch (IOException e) {
                Log.e(this.getClass().getSimpleName(), "Could not close socket");
            }
        }
    }

    static class Worker implements Runnable {
        private final HttpService httpservice;
        private final HttpServerConnection conn;

        private AtomicBoolean running;

        public Worker(
                final HttpService httpservice,
                final HttpServerConnection conn) {
            super();
            this.httpservice = httpservice;
            this.conn = conn;

            Log.v(this.getClass().getSimpleName(), "Worker thread starting");
        }

        public void run() {
            running = new AtomicBoolean(true);

            HttpContext context = new BasicHttpContext(null);
            try {
                while (running.get() && this.conn.isOpen()) {
                    this.httpservice.handleRequest(this.conn, context);
                }
            } catch (ConnectionClosedException ex) {
                Log.e(this.getClass().getSimpleName(), "Client closed connection");
            } catch (IOException ex) {
                Log.e(this.getClass().getSimpleName(), "I/O error: " + ex.getMessage());
            } catch (HttpException ex) {
                Log.e(this.getClass().getSimpleName(), "Unrecoverable HTTP protocol violation: " + ex.getMessage());
            } finally {
                try {
                    this.conn.shutdown();
                } catch (IOException ignore) {
                }
            }
        }

        public void stop() {
            running.set(false);
        }
    }
}