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
            do {
                try {
                    // Get a reference to the socket's input and output streams.
                    is = new DataInputStream(socket.getInputStream());
                    os = new DataOutputStream(socket.getOutputStream());

                    // Prepare the request.
                    HttpRequest req = new HttpRequest();
                    req.readFrom(is);
                    System.out.print(req.getDebugInfo(req.getClass(), req.getRequestLine(), req.getHeaders(), req.getBody()));

                    // Prepare the response.
                    HttpResponse res = new HttpResponse(req, serverRootFolder, serverDefaultPage);
                    res.writeTo(os);
                    System.out.print(res.getDebugInfo(res.getClass(), res.getStatusLine(), res.getHeaders(), res.getBody()));

                    try {
                        if (req.isPersistent()) {
                            this.socket.setKeepAlive(true);
                        } else {
                            this.socket.setKeepAlive(false);
                            is.close();
                            os.close();
                            break;
                        }
                    } catch (SocketException e) {
                        System.err.println("Unknown Socket Error: Failed to set keep alive for this socket.");
                        break;
                    }

                } catch (IOException exceptionFromDataInOutStream) {
                    System.err.println(exceptionFromDataInOutStream.getMessage());
                    break;
                }
            } while (this.socket.getKeepAlive());
        } catch (SocketException e) {
            System.err.println("Unknown Socket Error: Failed to get keep alive status for this socket.");
        }
    }

    public Socket getSocket() {
        return socket;
    }
}
