import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class WebServer {
    public static final EHttpVersions DEFAULT_HTTP_VERSION = EHttpVersions.HTTP_1_1;
    public static final int DEFAULT_TIMEOUT = 15 * 1000;
    HashMap<String, HttpConnection> connectionsTable;
    Properties settings;
    int maxThreads;
    int port;

    public WebServer() {
        settings = new Properties();

        // Load the configuration file.
        try (FileReader fr = new FileReader("config.ini")) {
            settings.load(fr);

            // Validate the number of entries in the configuration file.
            if (settings.size() != 4) {
                System.err.println("Invalid number of tokens in configuration file.");
                System.exit(2);
            } else {

                // Validate the key-value pairs after they had been loaded.
                String rootFolder = settings.getProperty("rootFolder");
                if (!Files.isDirectory(new File(rootFolder).toPath())) {
                    System.err.println("Server Root folder not found!");
                    System.exit(3);
                }
                try {
                    port = Integer.parseInt(settings.getProperty("port"));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number format for property 'port'.");
                    System.exit(4);
                }
                try {
                    maxThreads = Integer.parseInt(settings.getProperty("maxThreads"));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number format for property 'maxThreads'.");
                    System.exit(5);
                }

                System.out.println("The Configuration file settings were loaded successfully!");
                connectionsTable = new HashMap<>(maxThreads);
            }
        } catch (FileNotFoundException e) {

            // When no config.ini is found in the working directory, we create one for you with default settings.
            System.err.println("Couldn't find file: config.ini");
            System.out.println("Using default configuration settings.");
            settings.setProperty("port", "8080");
            settings.setProperty("maxThreads", "10");
            settings.setProperty("defaultPage", "index.html");
            settings.setProperty("rootFolder", "/serverroot/");

            // Store the config.ini file we just created in the working directory.
            try (FileWriter fw = new FileWriter("config.ini")) {
                settings.store(fw, null);
            } catch (IOException e1) {
                System.err.println("An unknown I/O error occurred while storing the configuration file.");
                System.exit(6);
            }
        } catch (IOException e1) {
            System.err.println("An unknown I/O error occurred while loading the configuration file.");
            System.exit(1);
        }
    }

    public static void main(String argv[]) throws Exception {
        System.setProperty("java.net.preferIPv4Stack", "true"); // Force JVM to use the IPv4 stack only.
        WebServer server = new WebServer();

        // Specify the port number where the server will listen.
        int port = server.getPort();

        // Specify the max. number of threads the server will use for handling incoming connections.
        int maxThreads = server.getMaxThreadsLimit();

        // Establish the listen socket.
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.err.printf("Failed to bind on port %d%n", port);
            System.exit(6);
        }

        // Process HTTP service requests in an infinite loop.
        int threadsInUse = 0;
        while (true) {
            while (threadsInUse < maxThreads) {
                // Listen for a TCP connection request.
                Socket currentSocket = socket.accept();
                String guest = currentSocket.getRemoteSocketAddress().toString().substring(1);
                System.out.printf("New incoming connection from: %s%n", guest);

                // Construct an object to process the HTTP request message.
                String rootFolder = server.getRootFolder();
                String defaultPage = server.getDefaultPage();
                HttpConnection connection = new HttpConnection(currentSocket, defaultPage, rootFolder);
                server.addConnection(connection);

                // Create a new thread to process the request.
                Thread thread = new Thread(connection);

                // Start the thread.
                thread.start();
                threadsInUse++;
            }
            System.out.println("The Server is working at full capacity...");
            Thread.sleep(DEFAULT_TIMEOUT);
            for (Map.Entry<String, HttpConnection> entry : server.getConnectionsTable()) {
                HttpConnection connection = entry.getValue();
                if (connection.getSocket().isClosed()) {
                    server.removeConnection(entry.getKey());
                    threadsInUse--;
                }
            }
        }
    }

    private void removeConnection(String remoteSocketAddress) {
        connectionsTable.remove(remoteSocketAddress);
    }

    private void addConnection(HttpConnection newHttpConnection) {
        if (newHttpConnection != null) {
            String socketId = newHttpConnection.getSocket().getRemoteSocketAddress().toString().substring(1);
            connectionsTable.put(socketId, newHttpConnection);
        }
    }

    public int getPort() {
        return port;
    }

    public int getMaxThreadsLimit() {
        return maxThreads;
    }

    public Set<Map.Entry<String, HttpConnection>> getConnectionsTable() {
        return connectionsTable.entrySet();
    }

    private String getRootFolder() {
        String rootFolder;
        if (System.getProperty("os.name").contains("Windows")) {
            rootFolder = System.getenv("SystemDrive") + settings.getProperty("rootFolder");
        } else {
            rootFolder = settings.getProperty("rootFolder");
        }

        return rootFolder;
    }

    private String getDefaultPage() {
        return settings.getProperty("defaultPage");
    }
}