import java.io.*;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by Yoni on 29/12/2014.
 */
public class HttpResponse extends HttpMessage {
    private static final String CRLF = "\r\n";
    private final String serverDefaultPage;
    private final String serverRootFolder;
    private final HttpRequest request;
    private HashMap<String, Object> responseHeaders;
    private StatusLine statusLine;
    private String responseBody;

    public HttpResponse(HttpRequest httpRequest, String serverRootFolder, String serverDefaultPage) {
            this.serverDefaultPage = serverDefaultPage;
            this.serverRootFolder = serverRootFolder;
            this.request = httpRequest;
            this.responseHeaders = new HashMap<>();

        //Retrieve the date
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Calendar calendar = Calendar.getInstance();

        // Retrieve the hostname
        String hostname = System.getProperty("os.name").contains("Windows") ? "COMPUTERNAME" : "HOSTNAME";
        hostname = System.getenv(hostname);

        // Add basic headers
        this.responseHeaders.put("Date", dateFormat.format(calendar.getTime()));
        this.responseHeaders.put("Server", String.format("%s, %s", hostname, System.getProperty("os.name")));
        if (transferEncodingChunkedRequired()) {
            this.responseHeaders.put("Transfer-Encoding", "Chunked");
        } else {
            this.responseHeaders.put("Content-Length", null);
        }
    }

    public void writeTo(DataOutputStream outputStream) throws IOException {
        // TODO: Complete this method...
        try {
            // Make sure we got a good request first.
            if (request.isBadRequest()) throw new HttpBadRequestException();

            // Determine which HTTP Protocol should be used...
            this.statusLine = new StatusLine(parseHttpVersion());

            // Locate the resource...
            File requestedResourceFile = parseResourcePath(request.getRequestedResource());

            // Now we can be certain that the status code is OK
            this.statusLine.setStatusCode(EStatusCodes.OK);

            // Identify the HTTP method...
            switch (request.getRequestedMethod()) {
                case GET:
                    statusLine.setStatusCode(EStatusCodes.OK);
                    if (transferEncodingChunkedRequired()) {
                        // Send status line.
                        outputStream.writeBytes(statusLine.toString());
                        // Send response headers.
                        writeHeaders(outputStream,responseHeaders);
                        // Send \r\n separator
                        outputStream.writeBytes(CRLF);
                        // Send the resource as the body of this message


                    } else {
                        this.
                    }
                    break;
                case POST:
                    sendPostResponse();
                    break;
                case TRACE:
                    sendTraceResponse(outputStream);
                    break;
                case OPTIONS:
                    sendOptionsResponse(outputStream);
                    break;
                case HEAD:
                    // TODO: Handle HEAD...
                    break;
            }
        } catch (HttpNotFoundException e) {
            sendHttpException(outputStream, EStatusCodes.NOT_FOUND, e);
        } catch (HttpBadRequestException e) {
            sendHttpException(outputStream, EStatusCodes.BAD_REQUEST, e);
        } catch (HttpNotImplementedException e) {
            sendHttpException(outputStream, EStatusCodes.NOT_IMPLEMENTED, e);
        } catch (HttpInternalServerErrorException e) {
            sendHttpException(outputStream, EStatusCodes.INTERNAL_SERVER_ERROR, e);
        }
    }

    private void sendPostResponse() {
        // TODO: Implement this method...
    }

    private void sendOptionsResponse(DataOutputStream outputStream) throws IOException, HttpInternalServerErrorException {
        // Update response headers.
        responseHeaders.replace("Content-Length", String.valueOf(0));
        responseHeaders.put("Allow", EHttpMethods.asListString());

        // Write the status line.
        statusLine.setStatusCode(EStatusCodes.OK);
        outputStream.writeBytes(statusLine.toString() + CRLF);

        // Update the length of this message.
        writeHeaders(outputStream, responseHeaders);
        outputStream.writeBytes(CRLF);
    }

    private void sendTraceResponse(DataOutputStream outputStream) throws IOException, HttpInternalServerErrorException {
        // Setup the response body.
        String requestLine = request.getRequestLine().toString();
        String requestHeaders = prettyPrintHeaders(request.getHeaders());
        responseBody = requestLine + CRLF + requestHeaders;

        // Update response headers.
        responseHeaders.put("Content-Type", "message/http");

        // Write the status line.
        outputStream.writeBytes(statusLine.toString() + CRLF);

        // Decide whether this response message should be returned in chunks or not.
        if (transferEncodingChunkedRequired()) {
            writeHeaders(outputStream, responseHeaders);
            outputStream.writeBytes(CRLF);

            // Split the body and send it in chunks.
            String currentChunkLength = Integer.toString(responseBody.length());
            outputStream.writeBytes(currentChunkLength + CRLF);
            outputStream.writeBytes(responseBody + CRLF);
        } else {
            // Update the length of this message.
            responseHeaders.putIfAbsent("Content-Length", Integer.toString(responseBody.length()));
            writeHeaders(outputStream, responseHeaders);
            outputStream.writeBytes(CRLF);
            outputStream.writeBytes(responseBody);
        }
    }

    private boolean transferEncodingChunkedRequired() {
        boolean requestContainsHeaderNamedChunked = this.request.getHeaders().containsKey("chunked");
        if (requestContainsHeaderNamedChunked) {
            return ((String) this.request.getHeaders().get("chunked")).equalsIgnoreCase("yes");
        } else {
            return false;
        }
    }

    private void sendHttpException(DataOutputStream outputStream, EStatusCodes statusCode, HttpException e) {
        this.statusLine = new StatusLine(statusCode);
        this.responseBody = e.getHtml();
        this.responseHeaders.replace("Content-Type", parseContentType(".html"));
        this.responseHeaders.replace("Content-Length", String.valueOf(e.getHtml().length()));
        try {
            outputStream.writeBytes(statusLine.toString() + CRLF);
            writeHeaders(outputStream, responseHeaders);
            outputStream.writeBytes(CRLF);
            outputStream.writeBytes(responseBody);
        } catch (HttpInternalServerErrorException | IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    private void writeHeaders(DataOutputStream outputStream, HashMap<String, Object> headers)
            throws HttpInternalServerErrorException {
        try {
            outputStream.writeBytes(prettyPrintHeaders(headers));
        } catch (IOException e) {
            throw new HttpInternalServerErrorException();
        }
    }

    private ProtocolVersion parseHttpVersion() throws HttpBadRequestException {
        ProtocolVersion requestHttpVersion = request.getHttpVersion();
        if (!requestHttpVersion.protocol.equals("HTTP"))
            throw new HttpBadRequestException();
        switch ((requestHttpVersion.compareToVersion(WebServer.DEFAULT_HTTP_VERSION.value()))) {
            case 1:
                return WebServer.DEFAULT_HTTP_VERSION.value();
            case 0:
                break;
            case -1:
                if (requestHttpVersion.lessThan(EHttpVersions.HTTP_1_0.value()))
                    throw new HttpBadRequestException();
                break;
        }
        return requestHttpVersion;
    }

    private File parseResourcePath(String requestedResource) throws HttpNotFoundException {
        String path;
        if (requestedResource.equals("/"))
            requestedResource =  serverDefaultPage;

        if (requestedResource.startsWith("/"))
            requestedResource = requestedResource.substring(1);

        path = serverRootFolder + requestedResource;
        File resourceFile = new File(path);

        if (Files.notExists(resourceFile.toPath()))
            throw new HttpNotFoundException();

        // Update the Content-Type header.
        this.responseHeaders.put("Content-Type", parseContentType(path));

        return resourceFile;
    }

    private String parseContentType(String fileName) {
        if (fileName.endsWith(".htm") || fileName.endsWith(".html")) {
            return "text/html";
        } else if (fileName.endsWith(".bmp")) {
            return "image/bmp";
        } else if (fileName.endsWith(".gif")) {
            return  "image/gif";
        } else if (fileName.endsWith(".png")) {
            return  "image/png";
        } else if (fileName.endsWith(".jpg")) {
            return "image/jpg";
        } else if (fileName.endsWith(".ico")){
            return "image/x-icon";
        } else {
            return "application/octet-stream";
        }
    }

    public StatusLine getStatusLine() {
        return statusLine;
    }

    public String getBody() {
        return responseBody;
    }

    public HashMap<String, Object> getHeaders() {
        return responseHeaders;
    }
}
