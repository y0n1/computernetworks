/**
 * Created by Yoni on 31/12/2014.
 */
public abstract class HttpException extends Throwable {

    private static final String CRLF = "\r\n";

    public HttpException(EStatusCodes statusCodeEnum) {
        super(statusCodeEnum.toString());
    }

    public String getHtml() {
        String html =
                "<!DOCTYPE HTML>" + CRLF +
                "<HTML>" + CRLF +
                "   <HEAD>" + CRLF +
                "       <TITLE>Error %1$s</TITLE>" + CRLF +
                "   </HEAD>" + CRLF +
                "   <BODY>" + CRLF +
                "       <h1>%1$s %2$s</h1>" + CRLF +
                "   </BODY>" + CRLF +
                "</HTML>" + CRLF;
        String[] tokens = super.getMessage().split(" ", 2);
        return String.format(html, tokens[0], tokens[1]);
    }
}
