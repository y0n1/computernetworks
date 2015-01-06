/**
 * Created by Yoni on 29/12/2014.
 */
public class HttpBadRequestException extends HttpException {

    public HttpBadRequestException() {
        super(EStatusCodes.BAD_REQUEST);
    }
}
