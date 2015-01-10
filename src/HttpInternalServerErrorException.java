/**
 * This Class represents a HTTP Internal Server Error.
 */
public class HttpInternalServerErrorException extends HttpException {

    public HttpInternalServerErrorException() {
        super(EStatusCodes.INTERNAL_SERVER_ERROR);
    }

    public HttpInternalServerErrorException(String msg) {
        super(msg);
    }
}
