import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

final class HttpRequest extends HttpMessage {
    private HashMap<String, Object> requestHeaders;
    private RequestLine requestLine;
    private String requestBody;
    private boolean isBadRequest;

    // Constructor
    public HttpRequest() {
        this.requestHeaders = new HashMap<>();
    }

    public void readFrom(DataInputStream inputStream) throws IOException {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String rawRequestLine = reader.readLine();
            try {
                this.requestLine = new RequestLine(rawRequestLine);

                // Parse the request headers.
                String headerLine;
                while ((headerLine = reader.readLine()).length() != 0) {
                    String[] tokens = headerLine.split(":", 2);
                    if (tokens.length == 2) {
                        // Parse cookies (if any).
                        if (tokens[0].equals("cookie")) {
                            requestHeaders.put(tokens[0], new CookieParser(tokens[1].trim()).table);
                        } else {
                            requestHeaders.put(tokens[0], tokens[1].trim());
                        }
                    } else {
                        throw new HttpBadRequestException("The request contains malformed headers.");
                    }
                }

                // Enforce that every HTTP/1.1 request includes a "Host" header.
                if (this.requestLine.getHttpVersion().equals(EHttpVersions.HTTP_1_1.value())) {
                    if (!this.requestHeaders.containsKey("Host")) {
                        this.isBadRequest = true;
                        return;
                    }
                } else {
                    // Get the body of this request (if any).
                    char[] buffer = new char[0];
                    if (requestHeaders.containsKey("Content-Length")) {
                        int length;
                        try {
                            // the length is expressed in DECIMAL format;
                            length = Integer.parseInt(String.valueOf(requestHeaders.get("Content-Length")));
                        } catch (NumberFormatException e) {
                            // the length is expressed in OCTET format;
                            length = Integer.parseInt(String.valueOf(requestHeaders.get("Content-Length")), 16);
                        }
                        buffer = new char[length];
                        reader.read(buffer);
                    }
                    this.requestBody = new String(buffer);
                }
            } catch (HttpBadRequestException e) {
                this.isBadRequest = true;
            }
        } catch (IOException e) {
            String msg = String.format("[%s]: Couldn't read from socket input stream", e.getClass().getTypeName());
            throw new IOException(msg);
        }
    }

    public String getRequestedResource() {
        return requestLine.getResource();
    }

    public EHttpMethods getRequestedMethod() throws HttpNotImplementedException {
        try {
            switch (Enum.valueOf(EHttpMethods.class, requestLine.getMethod())) {
                case GET:
                    return EHttpMethods.GET;
                case POST:
                    return EHttpMethods.POST;
                case TRACE:
                    return EHttpMethods.TRACE;
                case OPTIONS:
                    return EHttpMethods.OPTIONS;
                case HEAD:
                    return EHttpMethods.HEAD;
                default:
                    throw new HttpNotImplementedException();
            }
        } catch (IllegalArgumentException e) {
            throw new HttpNotImplementedException();
        }
    }

    public ProtocolVersion getHttpVersion() {
        return requestLine.getHttpVersion();
    }

    public boolean isBadRequest() {
        return isBadRequest;
    }

    public boolean isPersistent() {
        if (requestLine == null) return true;
        if (requestLine.getHttpVersion().equals(new ProtocolVersion("HTTP", 1, 1))) {
            if (requestHeaders.containsKey("Connection"))
                return !(String.valueOf(requestHeaders.get("Connection")).equalsIgnoreCase("Close"));
            return true;
        } else if (requestHeaders.containsKey("Connection")){
            if ((String.valueOf(requestHeaders.get("Connection")).equalsIgnoreCase("Keep-Alive")))
                    return true;
        }
        return false;
    }

    /**
     * Bonus - Support Keep-Alive header with "timeout" and "max" keep-alive-info parameters.
     * This method looks up for a header named 'Keep-Alive' that MIGHT be provided by the client.
     * If such header exists then it parses its value and returns it. Otherwise WebServer.DEFAULT_TIMEOUT is used.
     * If the keep-alive-info parameters have a non-numeric value then we consider this request message as a bad
     * request despite WebServer.DEFAULT_TIMEOUT time will be returned by this method.
     *
     * @return The time out specified in this request or WebServer.DEFAULT_TIMEOUT if it isn't specified in the headers.
     * @see <a href="http://tools.ietf.org/id/draft-thomson-hybi-http-timeout-01.html#rfc.section.2">Keep Alive Header Specification.</a>
     */
    public int getTimeOut() {
        int timeOut = WebServer.DEFAULT_TIMEOUT;
        if (requestHeaders.containsKey("Keep-Alive")) {
            String keepAliveValue = String.valueOf(requestHeaders.get("Keep-Alive"));
            String[] keepAliveValueInfo = keepAliveValue.split("=");
            if (keepAliveValueInfo[0].equalsIgnoreCase("\"timeout\"") ||
                    keepAliveValueInfo[0].equalsIgnoreCase("\"max\"")) {
                try {
                    timeOut = Integer.parseInt(keepAliveValueInfo[1]);
                } catch (NumberFormatException e) {
                    this.isBadRequest = true;
                }
            }
        }
        return timeOut;
    }

    public String getBody() {
        return requestBody;
    }

    public HashMap<String, Object> getHeaders() {
        return requestHeaders;
    }

    public RequestLine getRequestLine() {
        return requestLine;
    }
}