package main;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 *
 * @author Amel
 */
public class FileUploadTask implements Callable<FileUploadResult> {

    private final String filePath;
    private final String server;
    private final String user;
    private final String pass;
    private final FTPClient ftpClient;
    
    public FileUploadTask(String server, String user,String pass, String filePath) throws IOException {
        this.server = server;
        this.user = user;
        this.pass = pass;
        this.filePath = filePath;
        this.ftpClient = new FTPClient();
    }

    @Override
    public FileUploadResult call() {
         try {
            ftpClient.login(server, user, pass);
            FileUploadResult fileUploadResult = ftpClient.sendFile(filePath);
            ftpClient.disconnect();
            return fileUploadResult;
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            return null;
        } 
    }
    
}