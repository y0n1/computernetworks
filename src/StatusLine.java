/**
 * Created by Yoni on 24-Dec-14.
 */
public class StatusLine implements IHttpStartLine {
    private ProtocolVersion httpVersion;
    private String reasonPhrase;
    private int statusCode;

    public StatusLine(ProtocolVersion httpVersion) {
        this.httpVersion = httpVersion;
    }

    public StatusLine(ProtocolVersion httpVersion, int statusCode, String reasonPhrase) {
        this(httpVersion);
        this.reasonPhrase = reasonPhrase;
        this.statusCode = statusCode;
    }

    public StatusLine(ProtocolVersion httpVersion, EStatusCodes statusCodeEnum) {
        this(httpVersion, statusCodeEnum.getStatusCode(), statusCodeEnum.getReasonPhrase());
    }

    public StatusLine(EStatusCodes statusCodeEnum) {
        this(WebServer.DEFAULT_HTTP_VERSION.value(), statusCodeEnum);
    }

    public ProtocolVersion getHttpVersion() {
        return httpVersion;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public void setHttpVersion(ProtocolVersion httpVersion) {
        this.httpVersion = httpVersion;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setStatusCode(EStatusCodes statusCodeEnum) {
        this.setStatusCode(statusCodeEnum.getStatusCode());
        this.setReasonPhrase(statusCodeEnum.getReasonPhrase());
    }

    public void setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", httpVersion, statusCode, reasonPhrase);
    }
}
