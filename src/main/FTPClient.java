package main;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.StringTokenizer;

/**
 *
 * @author Amel
 */
public class FTPClient {

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private static final String EXTENSION_SPLIT_REGEX = "\\.(?=[^\\.]+$)";
    private static final boolean DEBUG = false;

    public FTPClient() {

    }

    /**
     * Connects to an FTP server with supplied ip address/hostname logs in with
     * the supplied username and password.
     *
     * @param server ip address/hostname
     * @param user username
     * @param pass password
     * @throws java.io.IOException if login fails
     */
    public void login(String server, String user, String pass) throws IOException {
        login(server, 21, user, pass);
    }

    /**
     * Connects to an FTP server with supplied ip address/hostname and port logs
     * in with the supplied username and password.
     *
     * @param server ip address/hostname
     * @param port ftp server port
     * @param user username
     * @param pass password
     * @throws java.io.IOException if login fails
     */
    public void login(String server, int port, String user,
            String pass) throws IOException {
        if (socket != null) {
            throw new IOException("FTPClient is already connected. Disconnect first.");
        }

        socket = new Socket(server, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream()));

        String response = readLine();
        if (!response.startsWith("220 ")) {
            throw new IOException(
                    "FTPClient received an unknown response when connecting to the FTP server: "
                    + response);
        }

        sendUser(user);

        sendPassword(pass);

    }

    /**
     * Disconnects from the FTP server.
     *
     * @throws java.io.IOException if ftp client fail to disconnect
     */
    public synchronized void disconnect() throws IOException {
        try {
            sendLine("QUIT");
        } finally {
            socket.close();
        }
    }

    /**
     * Sends a file to be stored on the FTP server. Returns true if the file
     * transfer was successful. The file is sent in passive mode to avoid NAT or
     * firewall problems at the client end.
     *
     * @param filePath path to file for upload
     * @return FileUploadResult class object with upload results of uploaded
     * file
     * @throws java.io.IOException if file doesn't exist
     */
    public FileUploadResult sendFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("File " + filePath + " doesn't exist. Check file path.");
        }

        return sendFile(file);
    }

    /**
     * Sends a file to be stored on the FTP server. Returns true if the file
     * transfer was successful. The file is sent in passive mode to avoid NAT or
     * firewall problems at the client end.
     *
     * @param file file for upload
     * @return FileUploadResult class object with upload results of uploaded
     * file
     * @throws java.io.IOException if error occurs while uploading file
     */
    public FileUploadResult sendFile(File file) throws IOException {
        if (file.isDirectory()) {
            throw new IOException("FTPClient cannot upload a directory.");
        }

        long startTime = System.currentTimeMillis();

        String fileName = file.getName();
        String[] parts = fileName.split(EXTENSION_SPLIT_REGEX);
        String extension = parts[1];

        if (extension.equals("txt")) {
            enterAsciiMode();
        } else {
            enterBinaryMode();
        }

        double fileSize = (double) file.length() / 1024; // size in KB  

        BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));

        //System.out.println("Uploading... " + fileName);
        String response = enterPassiveMode();

        String ip = null;
        int port = -1;

        int opening = response.indexOf('(');
        int closing = response.indexOf(')', opening + 1);
        if (closing > 0) {
            String dataLink = response.substring(opening + 1, closing);
            StringTokenizer tokenizer = new StringTokenizer(dataLink, ",");
            ip = getIp(tokenizer);
            port = getPort(tokenizer);
        }

        sendLine("STOR " + fileName);

        Socket dataSocket = new Socket(ip, port);

        response = readLine();
        if (!response.startsWith("150 ")) {
            throw new IOException("FTPClient was not allowed to send the file: "
                    + response);
        }

        BufferedOutputStream output = new BufferedOutputStream(dataSocket
                .getOutputStream());
        byte[] buffer = new byte[4096];
        int bytesRead = 0;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        output.flush();
        output.close();
        input.close();

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        double uploadTime = (double) elapsedTime / 1000;

        return new FileUploadResult(fileName, fileSize, uploadTime, fileSize / uploadTime);
    }

    /**
     * Enter binary mode for sending binary files.
     *
     * @return true - if enters binary mode successfully, false - if fails to
     * enter binary mode
     * @throws java.io.IOException if error ocurrs during entering binary mode
     */
    public boolean enterBinaryMode() throws IOException {
        sendLine("TYPE I");
        String response = readLine();
        return (response.startsWith("200 "));
    }

    /**
     * Enter ASCII mode for sending text files.
     *
     * @return true - if successful, false - if unsuccessful
     * @throws java.io.IOException if error ocurrs during entering ascii mode
     */
    public boolean enterAsciiMode() throws IOException {
        sendLine("TYPE A");
        String response = readLine();
        return (response.startsWith("200 "));
    }

    /**
     * Changes the working directory
     *
     * @param directoryName name of new working directory
     * @return true if successful, false if unsuccessful
     * @throws java.io.IOException if error occurs
     */
    public boolean changeWorkingDirectory(String directoryName) throws IOException {
        sendLine("CWD " + directoryName);
        String response = readLine();
        return (response.startsWith("250 "));
    }

    /**
     * Sends a raw command to the FTP server.
     * 
     * @param line command to send to ftp server
     * @throws java.io.IOException if error occurs
     */
    private void sendLine(String line) throws IOException {
        if (socket == null) {
            throw new IOException("FTPClient is not connected.");
        }
        try {
            writer.write(line + "\r\n");
            writer.flush();
            if (DEBUG) {
                System.out.println("> " + line);
            }
        } catch (IOException e) {
            socket = null;
            throw e;
        }
    }

    /**
     * Reads response from FTP server.
     * 
     * @throws java.io.IOException if error occurs
     */
    private String readLine() throws IOException {
        String line = reader.readLine();
        if (DEBUG) {
            System.out.println("< " + line);
        }

        return line;
    }

    /**
     * Sends user data to FTP server.
     * 
     * @throws java.io.IOException if error occurs
     */
    private void sendUser(String user) throws IOException {
        String response;

        sendLine("USER " + user);

        response = readLine();
        if (!response.startsWith("331 ")) {
            throw new IOException(
                    "FTPClient received an unknown response after sending the user: "
                    + response);
        }
    }

    /**
     * Sends password data to FTP server.
     * 
     * @throws java.io.IOException if error occurs
     */
    private void sendPassword(String pass) throws IOException {
        String response;

        sendLine("PASS " + pass);

        response = readLine();
        if (!response.startsWith("230 ")) {
            throw new IOException(
                    "FTPClient was unable to log in with the supplied password: "
                    + response);
        }
    }

    /**
     * Enters passive mode.
     * 
     * @throws java.io.IOException if error occurs
     */
    private String enterPassiveMode() throws IOException {

        sendLine("PASV");

        String response = readLine();
        if (!response.startsWith("227 ")) {
            throw new IOException("FTPClient could not request passive mode: "
                    + response);
        }
        return response;
    }

    /**
     * Extracts ip address from ftp server response.
     * 
     * @param stringTokenizer contains data about ip address and port
     * @throws java.io.IOException if error occurs
     */
    private String getIp(StringTokenizer stringTokenizer) throws IOException {
        try {
            return stringTokenizer.nextToken() + "." + stringTokenizer.nextToken() + "."
                    + stringTokenizer.nextToken() + "." + stringTokenizer.nextToken();
        } catch (Exception e) {
            throw new IOException("FTPClient received bad data link information.");
        }
    }

    /**
     * Extracts port from ftp server response.
     * 
     * @param stringTokenizer contains data about ip address and port
     * @throws java.io.IOException if error occurs
     */
    private int getPort(StringTokenizer stringTokenizer) throws IOException {
        try {
            return Integer.parseInt(stringTokenizer.nextToken()) * 256
                    + Integer.parseInt(stringTokenizer.nextToken());
        } catch (Exception e) {
            throw new IOException("FTPClient received bad data link information.");
        }
    }

}