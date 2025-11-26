package org.saikumar.webfileexplorer.model;


import lombok.Builder;
import lombok.Data;
import org.saikumar.webfileexplorer.FileTypes;

@Data
@Builder
public class FileInformation {
    private String fileName;
    private String filePath;
    private long fileSize;
    private FileTypes fileType;
    private String fileExtension;
    private boolean directory;
}
