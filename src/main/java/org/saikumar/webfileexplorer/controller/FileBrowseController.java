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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

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
    public Mono<ResponseEntity<InputStreamResource>> downloadFile(@RequestParam String path) {

        return Mono.fromSupplier(() -> {

            File file = new File(path);

            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }

            try {
                // Detect MIME type — fallback to binary
                String mime = Files.probeContentType(file.toPath());
                if (mime == null) mime = "application/octet-stream";

                InputStreamResource resource =
                        new InputStreamResource(new FileInputStream(file));

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + file.getName() + "\"")
                        .header(HttpHeaders.CONTENT_TYPE, mime)
                        .header(HttpHeaders.CONTENT_LENGTH,
                                String.valueOf(file.length()))
                        .body(resource);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @GetMapping("/stream")
    public Mono<ResponseEntity<InputStreamResource>> streamVideo(
            @RequestParam String path,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {

        return Mono.fromSupplier(() -> {

            File file = new File(path);
            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }

            long fileLength = file.length();
            long start = 0;
            long end = fileLength - 1;

            // Detect MIME Type
            String mime;
            try {
                mime = Files.probeContentType(Paths.get(path));
                if (mime == null) mime = "application/octet-stream";
            } catch (IOException e) {
                mime = "application/octet-stream";
            }

            // Handle Range Request (video streaming needs this)
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                try {
                    String[] parts = rangeHeader.replace("bytes=", "").split("-");
                    start = Long.parseLong(parts[0]);
                    if (parts.length > 1 && !parts[1].isEmpty()) {
                        end = Long.parseLong(parts[1]);
                    }
                } catch (Exception e) {
                    start = 0;
                }
            }

            if (end >= fileLength) end = fileLength - 1;
            if (start > end) start = 0;

            long contentLength = end - start + 1;

            try {
                FileInputStream fis = new FileInputStream(file);
                if (start > 0) fis.skip(start);

                InputStreamResource resource = new InputStreamResource(fis);

                return ResponseEntity
                        .status(rangeHeader == null ? 200 : 206)
                        .header("Content-Type", mime)
                        .header("Accept-Ranges", "bytes")
                        .header("Content-Range", "bytes " + start + "-" + end + "/" + fileLength)
                        .contentLength(contentLength)
                        .body(resource);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
