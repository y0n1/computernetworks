import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

final class HttpRequest extends HttpMessage {
    private HashMap<String, Object> requestHeaders;
    private HashMap<String, String> requestParams;
    private RequestLine requestLine;
    private String requestBody;
    private boolean isBadRequest;

    // Constructor
    public HttpRequest() {
        this.requestHeaders = new HashMap<>();
    }

    public void readFrom(InputStreamReader inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(inputStream);
        parseRequestLine(reader);
        parseHeaders(reader);
        parseBody(reader);
    }

    private void parseBody(BufferedReader reader) throws IOException {
        if (requestHeaders.containsKey("Content-Length")) {
            int length;
            try {
                // the length is expressed in DECIMAL format;
                length = Integer.parseInt(String.valueOf(requestHeaders.get("Content-Length")));
            } catch (NumberFormatException e) {
                // the length is expressed in OCTET format;
                length = Integer.parseInt(String.valueOf(requestHeaders.get("Content-Length")), 16);
            }
            char[] buffer = new char[length];
            reader.read(buffer);
            this.requestBody = new String(buffer);
        } else {
            while (reader.ready()) {
                requestBody += reader.readLine();
            }
        }

        // We assume that only a POST request has content in its body.
        if (requestBody != null) {
            requestLine.setQueryStringParameters(requestBody);
        }
    }

    private void parseRequestLine(BufferedReader reader) throws IOException {
        String rawRequestLine;
        while ((rawRequestLine = reader.readLine()) == null);
        try {
            this.requestLine = new RequestLine(rawRequestLine);
            this.requestParams = requestLine.getQueryStringParameters();
        } catch (HttpBadRequestException e) {
            this.isBadRequest = true;
        }
    }

    private void parseHeaders(BufferedReader reader) throws IOException {
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
                this.isBadRequest = true;
                System.err.println("The request contains bad headers.");
            }
        }

        // Enforce that every HTTP/1.1 request includes a "Host" header.
        this.isBadRequest = isPersistent() && !requestHeaders.containsKey("Host");
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
        boolean case1 = requestLine.getHttpVersion().equals(EHttpVersions.HTTP_1_1.value());
        boolean case2 = requestHeaders.containsValue("keep-alive");
        return case1 || case2;
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