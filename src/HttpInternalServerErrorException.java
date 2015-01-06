/**
 * Created by Yoni on 1/1/2015.
 */
public class HttpInternalServerErrorException extends HttpException {

    public HttpInternalServerErrorException() {
        super(EStatusCodes.INTERNAL_SERVER_ERROR);
    }
}
