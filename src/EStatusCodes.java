/**
 * Created by Yoni on 24-Dec-14.
 */
public enum EStatusCodes {
    OK (200, "OK"),
    BAD_REQUEST (400, "Bad Request"),
    NOT_FOUND (404, "Not Found"),
    INTERNAL_SERVER_ERROR (500, "Internal Server Error"),
    NOT_IMPLEMENTED (501, "Not Implemented");

    protected int statusCode;
    protected String reasonPhrase;

    EStatusCodes(int i_statusCode, String i_reasonPhrase) {
        this.statusCode = i_statusCode;
        this.reasonPhrase = i_reasonPhrase;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String toString() {
        return String.format("%s %s", this.statusCode, this.reasonPhrase);
    }
}
