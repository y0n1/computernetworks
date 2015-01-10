import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class WebServer {
    private static final WebServer INSTANCE = new WebServer();
    int port, maxThreads;
    Properties settings;


    private WebServer() {
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
        WebServer server = WebServer.getInstance();

        // Specify the port number where the server will listen.
        int port = server.getPort();

        // Specify the max. number of threads the server will use for handling incoming connections.
        int maxThreads = server.getMaxThreadsLimit();

        // Establish the listen socket.
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            System.out.printf("Listening on port: %s%n", port);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.err.printf("Failed to bind on port %d%n", port);
            System.exit(6);
        }

        ExecutorService pool = Executors.newFixedThreadPool(maxThreads);
        // Process HTTP service requests in an infinite loop.
        while (true) {
            // Listen for a TCP connection request.
            Socket socket = serverSocket.accept();
            int guestPort = socket.getPort();
            String guestIp = socket.getInetAddress().getHostAddress();
            System.out.printf("New incoming connection from: %s:%s%n", guestIp, guestPort);

            // Construct an object to process the HTTP request message.
            pool.submit(new HttpConnection(socket));
            //HttpConnection connection = new HttpConnection(socket);

            // Create a new thread to process the request.
            //Thread thread = new Thread(connection);

            // Start the thread.
            //thread.start();
        }
    }

    public static WebServer getInstance() {
        return INSTANCE;
    }

    public int getPort() {
        return port;
    }

    public int getMaxThreadsLimit() {
        return maxThreads;
    }

    public String getRootFolder() {
        String rootFolder;
        if (System.getProperty("os.name").contains("Windows")) {
            rootFolder = System.getenv("SystemDrive") + settings.getProperty("rootFolder");
        } else {
            rootFolder = settings.getProperty("rootFolder");
        }

        return rootFolder;
    }

    public String getDefaultPage() {
        return settings.getProperty("defaultPage");
    }
}