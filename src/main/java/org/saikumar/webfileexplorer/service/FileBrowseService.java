package org.saikumar.webfileexplorer.service;

import org.saikumar.webfileexplorer.model.FileInformation;
import org.springframework.core.io.InputStreamResource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;

public interface FileBrowseService {

    Flux<FileInformation> browseFiles();
    Flux<FileInformation> browseFiles(String path);

    Flux<FileInformation> listDrives();
    Flux<FileInformation> listExternalDrives();
    Mono<InputStreamResource> downloadFile(String path) throws IOException;
}
