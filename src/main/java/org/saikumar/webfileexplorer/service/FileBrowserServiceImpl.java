package org.saikumar.webfileexplorer.service;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.saikumar.webfileexplorer.FileTypes;
import org.saikumar.webfileexplorer.model.FileInformation;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Service
public class FileBrowserServiceImpl implements FileBrowseService {

    private ExtractFileInformation<File, FileInformation> extractFileInformation;

    @PostConstruct
    public void init() {
        extractFileInformation = file ->
                file.isDirectory()
                        ? getDirectoryInformation(file)
                        : getFileInformation(file);
    }

    @Override
    public Flux<FileInformation> browseFiles() {
        return getFileInformationFlux("/");
    }

    private Flux<FileInformation> getFileInformationFlux(String path) {
        return Flux.defer(() -> {
            File root = Paths.get(path).toFile();
            File[] files = root.listFiles();

            if (files == null) {
                return Flux.empty();
            }

            return Flux.fromArray(files)
                    .publishOn(Schedulers.boundedElastic())
                    .map(extractFileInformation);
        });
    }


    @Override
    public Flux<FileInformation> browseFiles(String path) {
        return getFileInformationFlux(path);
    }

    public Flux<FileInformation> listDrives() {

        return Flux.defer(() -> {
            File[] roots = File.listRoots();
            if (roots == null) return Flux.empty();

            return Flux.fromArray(roots)
                    .publishOn(Schedulers.boundedElastic())
                    .map(root -> FileInformation.builder()
                            .fileName(root.getAbsolutePath())
                            .filePath(root.getAbsolutePath())
                            .directory(true)
                            .fileType(FileTypes.drive)
                            .fileSize(0)
                            .build()
                    );
        });
    }

    public Flux<FileInformation> listExternalDrives() {

        return Flux.defer(() -> {

            List<File> usbDrives = new ArrayList<>();
            FileSystemView fsv = FileSystemView.getFileSystemView();

            // --- WINDOWS ---
            for (File root : File.listRoots()) {
                String type = fsv.getSystemTypeDescription(root);
                if (type != null && type.toLowerCase().contains("removable")) {
                    usbDrives.add(root);
                }
            }

            // --- LINUX / MAC ---
            File[] linuxPaths = {
                    new File("/media"),
                    new File("/run/media"),
                    new File("/Volumes")
            };

            for (File path : linuxPaths) {
                if (path.exists() && path.isDirectory()) {
                    File[] list = path.listFiles();
                    if (list != null) usbDrives.addAll(Arrays.asList(list));
                }
            }

            return Flux.fromIterable(usbDrives)
                    .publishOn(Schedulers.boundedElastic())
                    .map(drive ->
                            FileInformation.builder()
                                    .fileName(drive.getName())
                                    .filePath(drive.getAbsolutePath())
                                    .directory(true)
                                    .fileSize(drive.getTotalSpace())
                                    .fileType(FileTypes.drive)
                                    .build()
                    );
        });
    }

    public Mono<InputStreamResource> downloadFile(String path) throws IOException {
        Path fileAbsolutePath = Paths.get(path);
        if (fileAbsolutePath.toFile().isDirectory()) {
            throw new IOException("Not a File");
        }
        InputStreamResource inputStreamResource = new InputStreamResource(Files.newInputStream(fileAbsolutePath));
        return Mono.just(inputStreamResource);
    }



    public FileInformation getDirectoryInformation(File file) {
        return FileInformation.builder()
                .fileName(file.getName())
                .filePath(file.getAbsolutePath())
                .fileType(FileTypes.directory)
                .fileSize(0L)
                .directory(true)
                .build();
    }

    @SneakyThrows
    public FileInformation getFileInformation(File file) {

        String ext = getFileExtension(file);

        return FileInformation.builder()
                .fileName(file.getName())
                .fileExtension(ext)
                .filePath(file.getAbsolutePath())
                .fileSize(Files.size(file.toPath()))
                .fileType(getFileType(ext))
                .directory(false)
                .build();
    }

    public FileTypes getFileType(String extension) {
        return switch (extension.toLowerCase()) {
            case "txt" -> FileTypes.txt;
            case "jpeg" -> FileTypes.jpeg;
            case "jpg" -> FileTypes.jpg;
            case "zip" -> FileTypes.zip;
            case "tar" -> FileTypes.tar;
            case "mp3" -> FileTypes.mp3;
            case "json" -> FileTypes.json;
            case "png" -> FileTypes.png;
            case "mkv" -> FileTypes.mkv;
            case "mp4" -> FileTypes.mp4;
            case "img" -> FileTypes.img;
            case "pdf" -> FileTypes.pdf;
            case "doc" -> FileTypes.doc;
            case "docx" -> FileTypes.docx;
            case "gz" -> FileTypes.gz;
            case "part" -> FileTypes.part;
            case "deb" -> FileTypes.deb;
            case "exe" -> FileTypes.exe;
            case "xlsx" -> FileTypes.xlsx;
            case "xls" -> FileTypes.xls;
            case "csv" -> FileTypes.csv;
            case "xz" -> FileTypes.xz;
            case "tgz" -> FileTypes.tgz;
            case "appimage" -> FileTypes.AppImage;
            case "conf" -> FileTypes.conf;
            case "mov" -> FileTypes.mov;
            case "gif" -> FileTypes.gif;
            case "bmp" -> FileTypes.bmp;
            case "webm" -> FileTypes.webm;
            case "webp" -> FileTypes.webp;
            case "log" -> FileTypes.log;
            case  "md" -> FileTypes.md;
            case "" -> FileTypes.unknown;
            default -> FileTypes.unknown;
        };
    }

    public String getFileExtension(File file) {
        String name = file.getName();

        int lastDot = name.lastIndexOf('.');
        if (lastDot <= 0) return "";

        return name.substring(lastDot + 1);
    }

    @FunctionalInterface
    public interface ExtractFileInformation<T, R> extends Function<T, R> {}
}
