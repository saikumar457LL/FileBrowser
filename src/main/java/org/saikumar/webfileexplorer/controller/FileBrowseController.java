package org.saikumar.webfileexplorer.controller;


import lombok.extern.slf4j.Slf4j;
import org.saikumar.webfileexplorer.model.FileInformation;
import org.saikumar.webfileexplorer.service.FileBrowseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;

@RestController
@Slf4j
public class FileBrowseController {

    @Autowired
    private FileBrowseService fileBrowseService;

    @GetMapping()
    public Flux<FileInformation> fileBrowser () {
        return fileBrowseService.browseFiles();
    }

    @GetMapping("/browse")
    public Flux<FileInformation> fileBrowser (@RequestParam(required = false,name = "path",defaultValue = "/") String path) {
        return fileBrowseService.browseFiles(path);
    }

    @GetMapping("/download")
    public Mono<ResponseEntity<InputStreamResource>> downloadFile(@RequestParam String path) throws IOException {

        return fileBrowseService.downloadFile(path)
                .map(stream -> {

                    File file = new File(path);
                    long size = file.length();

                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=\"" + file.getName() + "\"")
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .contentLength(size)
                            .body(stream);
                });
    }


    @GetMapping("/drives")
    public Flux<FileInformation> listDrives() {
        return fileBrowseService.listDrives();
    }

    @GetMapping("/removables")
    public Flux<FileInformation> listExternalDrives() {
        return fileBrowseService.listExternalDrives();
    }


}
