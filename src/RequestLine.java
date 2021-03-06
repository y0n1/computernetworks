import java.util.HashMap;

/**
 * Created by Yoni on 29/12/2014.
 */
public class RequestLine implements IHttpStartLine {
    private String requestedMethod;
    private String requestedResource;
    private String queryStringParameters;
    private ProtocolVersion requestedHttpVersion;

    public RequestLine(String rawRequestLine) throws HttpBadRequestException {
        // Parse the request line into tokens.
        String[] tokens = rawRequestLine.split(" ");
        if (tokens.length == 3) {
            setMethod(tokens[0]);
            setResource(tokens[1]);
            try {
                setHttpVersion(new ProtocolVersion(tokens[2]));
            } catch (AssertionError assertionError) {
                throw new HttpBadRequestException();
            }
        } else {
            throw new HttpBadRequestException();
        }
    }

    public String getMethod() {
        return requestedMethod;
    }

    public String getResource() {
        return requestedResource;
    }

    public ProtocolVersion getHttpVersion() {
        return requestedHttpVersion;
    }

    public void setMethod(String requestedMethod) {
        this.requestedMethod = requestedMethod;
    }

    public void setResource(String requestedResource) {
        // Parse query string parameters (if any).
        if (requestedResource.contains("\\?")) {
            String[] parsedRequestedResource = requestedResource.split("\\?");
            this.queryStringParameters = parsedRequestedResource[1];
            this.requestedResource = parsedRequestedResource[0];
        } else {
            this.requestedResource = requestedResource;
        }
    }

    public void setHttpVersion(ProtocolVersion requestedHttpVersion) {
        this.requestedHttpVersion = requestedHttpVersion;
    }

    public HashMap<String, String> getQueryStringParameters() {
        if (queryStringParameters == null) return null;
        HashMap<String, String> map = new HashMap<>();
        String[] tokens = queryStringParameters.split("&");
        for (String token : tokens) {
            String[] entry = token.split("=");
            String k = entry[0];
            String v = entry[1];
            map.put(k, v);
        }
        return map;
    }

    public void setQueryStringParameters(String queryParams) {
        if (queryStringParameters == null) {
            this.queryStringParameters = queryParams;
        } else {
            this.queryStringParameters += "&" + queryParams;
        }
    }

    @Override
    public String toString() {
        String text;
        if (queryStringParameters != null) {
            String fullUri = String.format("%s\\?%s", getResource(), getQueryStringParameters());
            text = String.format("%s %s %s", getMethod(), fullUri, getHttpVersion());
        } else {
            text = String.format("%s %s %s", getMethod(), requestedResource, getHttpVersion());
        }

        return text;
    }
}
