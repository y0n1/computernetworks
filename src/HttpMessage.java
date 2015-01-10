import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created by Yoni on 3/1/2015.
 */
public abstract class HttpMessage {
    private static final String CRLF = "\r\n";

    public String getDebugInfo(Class<? extends HttpMessage> httpMessageClass,
                               IHttpStartLine startLine,
                               HashMap<String, Object> headers,
                               String body) {

        String httpMessageClassName = httpMessageClass.toString().substring(10).toUpperCase();
        String output = null;
        if (body == null || body.isEmpty()) {
            output =
                    "----BEGIN-OF--%1$s-------------------------------------------------------------" + CRLF +
                    "%2$s" + CRLF +
                    "%3$s" +
                    "-----END--OF--%1$s-------------------------------------------------------------" + CRLF;
            output = String.format(output, httpMessageClassName, startLine, prettyPrintHeaders(headers));
        } else if (!body.isEmpty()) {
            output =
                    "----BEGIN-OF--%1$s-------------------------------------------------------------" + CRLF +
                    "%2$s" + CRLF +
                    "%3$s" +
                    "%4$s" + CRLF +
                    "-----END--OF--%1$s-------------------------------------------------------------" + CRLF;
            output = String.format(output, httpMessageClassName, startLine, prettyPrintHeaders(headers), body);
        }

        return output;
    }

    protected String prettyPrintHeaders(HashMap<String, Object> headers) {
        StringBuilder sb = new StringBuilder();
        if (headers != null) {
            for (Map.Entry<String, Object> header : headers.entrySet()) {
                String key = header.getKey();
                String value = null;
                if (header.getValue() instanceof Hashtable) {
                    value = new CookieParser((Hashtable<String, String>) header.getValue()).toString();
                } else {
                    value = String.valueOf(header.getValue()).trim();
                }
                String headerLine = String.format("%1$s: %2$s", key, value);
                sb.append(headerLine + CRLF);
            }
        }
        return sb.toString() + CRLF;
    }
}
