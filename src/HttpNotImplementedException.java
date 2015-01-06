/**
 * Created by Yoni on 31/12/2014.
 */
public class HttpNotImplementedException extends HttpException {

    public HttpNotImplementedException() {
        super(EStatusCodes.NOT_IMPLEMENTED);
    }
}
