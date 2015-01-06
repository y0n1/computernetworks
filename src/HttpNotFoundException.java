/**
 * Created by Yoni on 1/1/2015.
 */
public class HttpNotFoundException extends HttpException {

    public HttpNotFoundException() {
        super(EStatusCodes.NOT_FOUND);
    }
}
