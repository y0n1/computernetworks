import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by Yoni on 29/12/2014.
 */
public class HttpConnection implements Runnable {
    private final String serverRootFolder;
    private final String serverDefaultPage;
    private Socket socket;

    public HttpConnection(Socket socket, String serverDefaultPage, String serverRootFolder) {
        this.serverDefaultPage = serverDefaultPage;
        this.serverRootFolder = serverRootFolder;
        this.socket = socket;
    }

    @Override
    public void run() {
        DataInputStream is;
        DataOutputStream os;
        try {
            // Get a reference to the socket's input and output streams.
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());

            // Prepare the request.
            HttpRequest req = new HttpRequest();
            req.readFrom(is);

            System.out.print(req.getDebugInfo(className, req.getRequestLine(), req.getBody(), req.getHeaders()));

            // Prepare the response.
            HttpResponse res = new HttpResponse(req, serverRootFolder, serverDefaultPage);
            res.writeTo(os);
            className = res.getClass().getSimpleName().substring(4);
            System.out.print(res.getDebugInfo(className, res.getResponseLine(), res.getBody(), res.getHeaders()));

            try {
                if (req.isPersistent()) {
                    this.socket.setKeepAlive(true);
                } else {
                    if (is != null) is.close();
                    if (os != null) is.close();
                }
            } catch (SocketException e) {
                System.err.println("Fail to set keep alive");
            }

        } catch (IOException exceptionFromDataInOutStream) {
            System.err.println(exceptionFromDataInOutStream.getMessage());
        }
    }

    public Socket getSocket() {
        return socket;
    }
}
