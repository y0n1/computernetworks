import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by Yoni on 29/12/2014.
 */
public class HttpResponse extends HttpMessage {
    private static final String CRLF = "\r\n";
    private final String serverDefaultPage;
    private final String serverRootFolder;
    private final HttpRequest httpRequest;
    private HashMap<String, Object> responseHeaders;
    private StatusLine statusLine;
    private String responseBody;

    public HttpResponse(HttpRequest httpRequest, String serverRootFolder, String serverDefaultPage) {
        this.serverDefaultPage = serverDefaultPage;
        this.serverRootFolder = serverRootFolder;
        this.httpRequest = httpRequest;
        this.responseHeaders = new HashMap<>();

        //Retrieve the date
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Calendar calendar = Calendar.getInstance();

        // Retrieve the hostname
        String hostname = System.getProperty("os.name").contains("Windows") ? "COMPUTERNAME" : "HOSTNAME";
        hostname = System.getenv(hostname);

        // Add basic headers
        this.responseHeaders.put("Date", dateFormat.format(calendar.getTime()));
        this.responseHeaders.put("Server", String.format("%s, %s", hostname, System.getenv("os.name")));
        this.responseHeaders.put("Content-Length", null);
        this.responseHeaders.put("Content-Type", null);
        if (this.httpRequest.getHeaders().containsKey("chunked")
                && ((String) this.httpRequest.getHeaders().get("chunked")).equalsIgnoreCase("yes")) {
            this.responseHeaders.put("Transfer-Encoding", "Chunked");
            this.responseHeaders.remove("Content-Length");
        }
    }

    public void writeTo(DataOutputStream outputStream) throws IOException {
        // TODO: Complete this method...
        try {
            // Make sure we got a good request first.
            if (httpRequest.isBadRequest()) throw new HttpBadRequestException();

            // Determine which HTTP Protocol should be used...
            this.statusLine = new StatusLine(determineHttpVersion());

            // Locate the resource...
            File requestedResourceFile = parseResourcePath(httpRequest.getRequestedResource());

            // Now we can be certain that the status code is OK
            this.statusLine.setStatusCode(EStatusCodes.OK);
            super.setStartLine(this.statusLine);

            // Identify the HTTP method...
            switch (httpRequest.getRequestedMethod()) {
                case GET:
                    // TODO: Handle GET...
                    break;
                case POST:
                    // TODO: Handle POST...
                    break;
                case TRACE:
                    // TODO: Handle TRACE...
                    outputStream.writeBytes(statusLine.toString() + CRLF);
                    responseHeaders.put("Content-Type", "message/http");
                    sendHeaders(outputStream, responseHeaders);
                    outputStream.writeBytes(CRLF);
                    // TODO: Add support for "Transfer-Encoding: Chunked"
                    outputStream.writeBytes(httpRequest.getRequestLine().toString());
                    sendHeaders(outputStream, httpRequest.getHeaders());
                    break;
                case OPTIONS:
                    // TODO: Handle OPTIONS...
                    break;
                case HEAD:
                    // TODO: Handle HEAD...
                    break;
            }
        } catch (HttpNotFoundException e) {
            handleHttpException(outputStream, EStatusCodes.NOT_FOUND, e);
        } catch (HttpBadRequestException e) {
            handleHttpException(outputStream, EStatusCodes.BAD_REQUEST, e);
        } catch (HttpNotImplementedException e) {
            handleHttpException(outputStream, EStatusCodes.NOT_IMPLEMENTED, e);
        } catch (HttpInternalServerErrorException e) {
            handleHttpException(outputStream, EStatusCodes.INTERNAL_SERVER_ERROR, e);
        }
    }

    private void handleHttpException(DataOutputStream outputStream,  EStatusCodes statusCode, HttpException e) {
        super.setStartLine(new StatusLine(statusCode));
        super.setMessageBody(e.getHtml());
        this.responseHeaders.replace("Content-Type", getContentType(".html"));
        this.responseHeaders.replace("Content-Length", String.valueOf(e.getHtml().length()));
        super.setMessageHeaders(this.responseHeaders);
        try {
            outputStream.writeBytes(statusLine.toString() + CRLF);
            sendHeaders(outputStream, responseHeaders);
            outputStream.writeBytes(CRLF);
            outputStream.writeBytes(responseBody);
        } catch (HttpInternalServerErrorException | IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    private void sendHeaders(DataOutputStream outputStream, HashMap<String, Object> headers)
            throws HttpInternalServerErrorException {
        try {
            outputStream.writeBytes(super.prettyPrintHeaders() + CRLF);
        } catch (IOException e) {
            throw new HttpInternalServerErrorException();
        }
    }

    private ProtocolVersion determineHttpVersion() throws HttpBadRequestException {
        ProtocolVersion requestHttpVersion = httpRequest.getHttpVersion();
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
        this.responseHeaders.put("Content-Type", getContentType(path));

        return resourceFile;
    }

    private String getContentType(String fileName) {
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

}
