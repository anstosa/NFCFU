/*
 * Based on the HTTP Server Example found at:
 * http://www.docjar.org/html/api/org/apache/http/examples/ElementalHttpServer.java.html
 */

package com.nfcfu.android.httpserver;

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
import java.util.Locale;

public class HttpServer {
    Thread requestListener;

    public HttpServer() throws IOException {
        Thread t = new RequestListenerThread(8080);
        t.setDaemon(false);
        t.start();
    }

    public void stop() {
        if (requestListener != null) {
            requestListener.interrupt();
        }
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

                    body = new StringEntity(new String(entityContent));
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

    static class RequestListenerThread extends Thread {

        private final ServerSocket serversocket;
        private final HttpParams params;
        private final HttpService httpService;

        public RequestListenerThread(int port) throws IOException {
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
        }

        public void run() {
            while (!Thread.interrupted()) {
                try {
                    // Set up HTTP connection
                    Socket socket = this.serversocket.accept();
                    DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
                    conn.bind(socket, this.params);

                    // Start worker thread
                    Thread t = new WorkerThread(this.httpService, conn);
                    t.setDaemon(true);
                    t.start();
                } catch (InterruptedIOException ex) {
                    break;
                } catch (IOException e) {
                    System.err.println("I/O error initialising connection thread: "
                            + e.getMessage());
                    break;
                }
            }
        }
    }

    static class WorkerThread extends Thread {

        private final HttpService httpservice;
        private final HttpServerConnection conn;

        public WorkerThread(
                final HttpService httpservice,
                final HttpServerConnection conn) {
            super();
            this.httpservice = httpservice;
            this.conn = conn;
        }

        public void run() {
            HttpContext context = new BasicHttpContext(null);
            try {
                while (!Thread.interrupted() && this.conn.isOpen()) {
                    this.httpservice.handleRequest(this.conn, context);
                }
            } catch (ConnectionClosedException ex) {
                System.err.println("Client closed connection");
            } catch (IOException ex) {
                System.err.println("I/O error: " + ex.getMessage());
            } catch (HttpException ex) {
                System.err.println("Unrecoverable HTTP protocol violation: " + ex.getMessage());
            } finally {
                try {
                    this.conn.shutdown();
                } catch (IOException ignore) {
                }
            }
        }
    }
}