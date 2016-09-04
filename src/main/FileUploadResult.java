package main;

/**
 *
 * @author Amel
 */
public class FileUploadResult {
    private final String fileName;
    private final double fileSize;
    private final double uploadTime;
    private final double uploadSpeed;

    public FileUploadResult(String fileName, double fileSize, double uploadTime, double uploadSpeed) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.uploadTime = uploadTime;
        this.uploadSpeed = uploadSpeed;
    }

    public String getFileName() {
        return fileName;
    }

    public double getFileSize() {
        return fileSize;
    }

    public double getUploadSpeed() {
        return uploadSpeed;
    }

    public double getUploadTime() {
        return uploadTime;
    }
    
}