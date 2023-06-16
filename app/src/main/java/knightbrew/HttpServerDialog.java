// Created by chat gpt lol
package knightbrew;

import com.sun.net.httpserver.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.*;
import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;

public class HttpServerDialog extends JDialog
{
    private JButton startButton;
    private JLabel directoryLabel;
    boolean serverRunning = false;

    public HttpServerDialog(Frame parent)
    {
        super(parent, "HTTP Server Configuration", true);

        directoryLabel = new JLabel("Directory:");
        startButton = new JButton("Start Server");

        // Add components to the dialog
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.add(directoryLabel);
        panel.add(startButton);
        add(panel);

        // Set button click listener
        startButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e)
            {
                if (serverRunning == false)
                {
                    AppDirs appDirs = AppDirsFactory.getInstance();
                    String path = appDirs.getUserDataDir("Knightbrew", null, "Knightwatch");
                    String directory = path + File.separator + "Source";

                    directoryLabel.setText("Serving Knightwatch at http://0.0.0.0:8080...");
                    startHttpServer(directory);
                    startButton.setText("Click to open homepage");
                }

                // Create a URI object with the webpage address
                URI webpage;
                try
                {
                    webpage = new URI("http://0.0.0.0:8080");

                    // Get the desktop instance
                    Desktop desktop = Desktop.getDesktop();

                    // Check if the desktop supports the browse action
                    if (desktop.isSupported(Desktop.Action.BROWSE))
                    {
                        // Browse the webpage
                        desktop.browse(webpage);
                    }
                }
                catch (Exception e1)
                {
                    e1.printStackTrace();
                }
            }
        });

        // Set dialog properties
        pack();
        setLocationRelativeTo(parent);
    }

    private void startHttpServer(String directory)
    {
        HttpServer server;
        try
        {
            server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/", new HttpHandler() {
                @Override public void handle(HttpExchange exchange) throws IOException
                {
                    String requestPath = exchange.getRequestURI().getPath();
                    File file = new File(directory + requestPath);

                    if (file.isDirectory())
                    {
                        file = new File(directory + requestPath + "/index.html");
                    }

                    if (file.exists() && file.isFile())
                    {
                        // Serve file contents
                        exchange.sendResponseHeaders(200, file.length());
                        try (InputStream fileStream = new FileInputStream(file))
                        {
                            OutputStream responseBody = exchange.getResponseBody();
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = fileStream.read(buffer)) != -1)
                            {
                                responseBody.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                    else
                    {
                        // File not found
                        String response = "File not found";
                        exchange.sendResponseHeaders(404, response.length());
                        exchange.getResponseBody().write(response.getBytes());
                    }
                    exchange.close();
                }
            });
            server.setExecutor(null);
            server.start();
            serverRunning = true;
            JOptionPane.showMessageDialog(this, "HTTP Server started successfully. You can now close this dialog.");
        }
        catch (IOException e)
        {
            JOptionPane.showMessageDialog(this, "Failed to start HTTP Server: " + e.getMessage(), "Error",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
}
