/**
 * Created by Yoni on 3/1/2015.
 */
public interface IHttpStartLine {
    public ProtocolVersion getHttpVersion();

    public void setHttpVersion(ProtocolVersion httpVersion);
}
