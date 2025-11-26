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

    @GetMapping("/stream")
    public Mono<ResponseEntity<InputStreamResource>> streamVideo(
            @RequestParam String path,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {

        return Mono.fromCallable(() -> {

            File file = new File(path);
            long fileLength = file.length();

            long start = 0;
            long end = fileLength - 1;

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

            if (end >= fileLength) {
                end = fileLength - 1;
            }

            long rangeLength = end - start + 1;

            InputStream is = new FileInputStream(file);
            if (start > 0) {
                // skip to start position
                long skipped = is.skip(start);
            }

            InputStreamResource resource = new InputStreamResource(is);

            // pick MIME type based on extension
            String name = file.getName().toLowerCase();
            String contentType = "video/mp4";
            if (name.endsWith(".webm")) contentType = "video/webm";
            else if (name.endsWith(".ogg")) contentType = "video/ogg";

            return ResponseEntity
                    .status(rangeHeader == null ? 200 : 206)
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength)
                    .contentLength(rangeLength)
                    .body(resource);
        }).subscribeOn(Schedulers.boundedElastic());
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
