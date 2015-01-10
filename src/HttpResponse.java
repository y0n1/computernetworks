import java.io.*;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Yoni on 29/12/2014.
 */
public class HttpResponse extends HttpMessage {
    private static final String CRLF = "\r\n";
    private final HashMap<String, Object> responseHeaders;
    private final String serverDefaultPage;
    private final String serverRootFolder;
    private final HttpRequest request;
    private StatusLine statusLine;
    private String responseBody;
    private File resource;

    public HttpResponse(HttpRequest httpRequest) {
        serverDefaultPage = WebServer.getInstance().getDefaultPage();
        serverRootFolder = WebServer.getInstance().getRootFolder();
        responseHeaders = new HashMap<>();
        request = httpRequest;

        //Retrieve the date
        DateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
        Calendar calendar = Calendar.getInstance();

        // Retrieve the hostname
        String hostname = System.getProperty("os.name").contains("Windows") ? "COMPUTERNAME" : "HOSTNAME";
        hostname = System.getenv(hostname);

        // Add basic headers
        responseHeaders.put("Date", dateFormat.format(calendar.getTime()));
        responseHeaders.put("Server", String.format("%s/%s", hostname, System.getProperty("os.name")));
    }

    public void writeTo(DataOutputStream outputStream) throws IOException {
        try {
            // Make sure we got a good request first.
            if (request.isBadRequest()) throw new HttpBadRequestException();

            // Parse the resource...
            String path = request.getRequestedResource();
            resource = parseResource(path);

            // Setup the status-line...
            statusLine = new StatusLine(parseHttpVersion(), EStatusCodes.OK);
            outputStream.writeBytes(statusLine.toString() + CRLF);

            // Identify the HTTP method...
            switch (request.getRequestedMethod()) {
                case GET:
                    sendGetResponse(outputStream);
                    break;
                case POST:
                    sendPostResponse(outputStream);
                    break;
                case TRACE:
                    sendTraceResponse(outputStream);
                    break;
                case OPTIONS:
                    sendOptionsResponse(outputStream);
                    break;
                case HEAD:
                    sendHeadResponse(outputStream);
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

    private void sendPostResponse(DataOutputStream outputStream) throws IOException, HttpInternalServerErrorException {
        sendGetResponse(outputStream);
    }

    private void sendHeadResponse(DataOutputStream outputStream) throws IOException, HttpInternalServerErrorException {
        // Set response headers.
        responseHeaders.put("Content-Length", resource.length());
        writeHeaders(outputStream);
    }

    private void sendGetResponse(DataOutputStream outputStream) throws IOException, HttpInternalServerErrorException {
        if (chunkResponseRequired()) {
            // Set response headers.
            responseHeaders.put("Transfer-Encoding", "chunked");
            writeHeaders(outputStream);
            // Read chunks and send them.
            Reader reader = (responseBody != null) ? new StringReader(responseBody) : new FileReader(resource);
            BufferedReader bufferedReader = new BufferedReader(reader);
            StringBuilder stringBuilder = new StringBuilder();
            do {
                String chunk = bufferedReader.readLine();
                String size = Integer.toHexString(chunk.length());
                String output = String.format("%s%s", size + CRLF, chunk + CRLF);
                outputStream.writeBytes(output);
                stringBuilder.append(output);
            } while (bufferedReader.ready());

            // Update the body for later use when debugging only if this body was empty.
            if (responseBody == null) responseBody = stringBuilder.toString();

        } else {

            // Set response headers.
            responseHeaders.put("Content-Length", (responseBody == null) ? resource.length() : responseBody.length());
            writeHeaders(outputStream);

            // Set response body.
            byte[] bytes = new byte[0];
            if (responseBody == null) {
                bytes = readFile(resource);
                responseBody = new String(bytes);
            }

            if (isImage()) {
                outputStream.write(bytes);
            } else {
                outputStream.writeBytes(responseBody);
            }
            outputStream.writeBytes(CRLF);
        }
    }

    private void sendOptionsResponse(DataOutputStream outputStream) throws IOException, HttpInternalServerErrorException {
        // Set response headers.
        responseHeaders.remove("Content-Type");
        responseHeaders.put("Content-Length", 0);
        responseHeaders.put("Allow", EHttpMethods.asStringList());
        writeHeaders(outputStream);
    }

    private void sendTraceResponse(DataOutputStream outputStream) throws IOException, HttpInternalServerErrorException {
        // Set response body.
        String requestLine = request.getRequestLine().toString() + CRLF;
        String requestHeaders = prettyPrintHeaders(request.getHeaders());
        responseBody = requestLine + requestHeaders;

        // Set response headers.
        responseHeaders.put("Content-Type", "message/http");
        responseHeaders.put("Content-Length", responseBody.length());

        // Write the response.
        writeHeaders(outputStream);
        outputStream.writeBytes(responseBody);
    }

    private void sendHttpException(DataOutputStream outputStream, EStatusCodes statusCode, HttpException e) {
        statusLine = new StatusLine(statusCode);
        responseHeaders.put("Content-Type", parseContentType(".html"));
        responseHeaders.put("Content-Length", e.getHtml().length());
        responseBody = e.getHtml();
        try {
            outputStream.writeBytes(statusLine.toString() + CRLF);
            writeHeaders(outputStream);
            outputStream.writeBytes(responseBody);
        } catch (HttpInternalServerErrorException | IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    private boolean isImage() {
        return parseContentType(resource.getName()).startsWith("image");
    }

    private byte[] readFile(File file) throws HttpInternalServerErrorException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bFile = new byte[(int) file.length()];
            // read until the end of the stream.
            while (fis.available() != 0) {
                fis.read(bFile, 0, bFile.length);
            }
            return bFile;
        } catch (IOException e) {
            throw new HttpInternalServerErrorException("Something went wrong while fetching the resource.");
        }
    }

    private boolean chunkResponseRequired() {
        Object headerValue = request.getHeaders().get("chunked");
        return (headerValue instanceof String) && (headerValue.toString().equalsIgnoreCase("yes"));
    }

    private void writeHeaders(DataOutputStream outputStream) throws HttpInternalServerErrorException {
        try {
            outputStream.writeBytes(prettyPrintHeaders(responseHeaders));
        } catch (IOException e) {
            throw new HttpInternalServerErrorException();
        }
    }

    private ProtocolVersion parseHttpVersion() throws HttpBadRequestException {
        ProtocolVersion requestHttpVersion = request.getHttpVersion();
        if (!requestHttpVersion.protocol.equals("HTTP"))
            throw new HttpBadRequestException();
        switch ((requestHttpVersion.compareToVersion(EHttpVersions.HTTP_1_1.value()))) {
            case 1:
                return EHttpVersions.HTTP_1_1.value();
            case 0:
                break;
            case -1:
                if (requestHttpVersion.lessThan(EHttpVersions.HTTP_1_0.value()))
                    throw new HttpBadRequestException();
                break;
        }
        return requestHttpVersion;
    }

    private File parseResource(String requestedResource) throws HttpNotFoundException {
        if (requestedResource.equals("/"))
            requestedResource = serverDefaultPage;

        if (requestedResource.startsWith("/"))
            requestedResource = requestedResource.substring(1);

        String path = serverRootFolder + requestedResource;
        File resourceFile = new File(path);

        if (Files.notExists(resourceFile.toPath())) {
            if (path.endsWith("params_info.html")) {
                setupParamsInfoResponseBody();
            } else {
                throw new HttpNotFoundException();
            }
        }

        // Update the Content-Type header.
        this.responseHeaders.put("Content-Type", parseContentType(path));

        return resourceFile;
    }

    private void setupParamsInfoResponseBody() {
        HashMap<String, String> params = request.getRequestLine().getQueryStringParameters();
        StringBuilder stringBuilder = new StringBuilder();
        String head =
                "<!DOCTYPE html>\n" +
                "<html>\n" +
                "   <head>\n" +
                "   <style>\n" +
                "   table, th, td {\n" +
                "       border: 1px solid black;\n" +
                "       border-collapse: collapse;\n" +
                "   }\n" +
                "   th, td {\n" +
                "       padding: 5px;\n" +
                "       text-align: center;\n" +
                "   }\n" +
                "   </style>\n" +
                "   </head>\n";
        stringBuilder.append(head);
        String body =
                "<body>\n" +
                "<table style=\"width:100%\">\n" +
                "  <tr><th>Name</th><th>Value</th></tr>\n";
        stringBuilder.append(body);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue();
            try {
                value = URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                System.err.println(e.getMessage());
            }
            String line = String.format("  <tr><td>%s</td><td>%s</td></tr>%n", entry.getKey(), value);
            stringBuilder.append(line);
        }
        stringBuilder.append(
                "</table>\n" +
                "</body>\n" +
                "</html>\n"
        );
        responseBody = stringBuilder.toString();
    }

    private String parseContentType(String fileName) {
        if (fileName.endsWith(".htm") || fileName.endsWith(".html")) {
            return "text/html";
        } else if (fileName.endsWith(".bmp")) {
            return "image/bmp";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".jpg")) {
            return "image/jpg";
        } else if (fileName.endsWith(".ico")) {
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
