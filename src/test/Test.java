package test;

import main.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 * @author Amel
 */
public class Test {

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        String server = "127.0.0.1", user = "user", pass = "pass";
        String[] files = null;
        
        for (int i = 0; i < args.length - 1; i++) {
            switch (args[i]) {
                case "-u":
                    user = args[i + 1];
                    break;
                case "-p":
                    pass = args[i + 1];
                    break;
                case "–server":
                    server = "" + args[i + 1];
                    break;
                case "–files":
                    files = args[i + 1].split(";");
                    break;
                default:
                    break;
            }
        }
        
        try {
            if (files == null) {
                throw new IOException("No files selected for upload.");
            }
            
            long startTime = System.currentTimeMillis();
            System.out.println("Uploading...");
            
            ExecutorService executorService = Executors.newFixedThreadPool(5);
            Set<Callable<FileUploadResult>> callables = new HashSet<Callable<FileUploadResult>>();
            for (int i = 0; i < files.length; i++) {
                callables.add(new FileUploadTask(server, user, pass, files[i]));
            }

            List<Future<FileUploadResult>> futures = executorService.invokeAll(callables);

            double fileUploadSpeeds = 0;
            int numberOfUploadedFiles = 0;

            for (Future<FileUploadResult> future : futures) {
                if(future.get() != null) {
                    fileUploadSpeeds += future.get().getUploadSpeed();
                    numberOfUploadedFiles++;
                    System.out.println("Uploaded " + future.get().getFileName() + ", upload time: " + future.get().getUploadTime() + " s, upload speed: " + String.format("%.2f", future.get().getUploadSpeed()) + " KB/s");
                }
                }

            executorService.shutdown();

            long stopTime = System.currentTimeMillis();
            double elapsedTime = (double) (stopTime - startTime) / 1000;

            System.out.println("Total upload time: " + elapsedTime + " s, average file upload speed: " + String.format("%.2f", fileUploadSpeeds / numberOfUploadedFiles) + " KB/s");
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}