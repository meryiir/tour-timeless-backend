package com.tourisme.service;

import com.tourisme.entity.Activity;
import com.tourisme.entity.Destination;
import com.tourisme.entity.DestinationPageCard;
import com.tourisme.repository.ActivityRepository;
import com.tourisme.repository.DestinationPageCardRepository;
import com.tourisme.repository.DestinationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageOptimizationService {

    private static final int MAX_DIMENSION = 1200;
    private static final double JPEG_QUALITY = 0.72;
    private static final long MIN_BYTES_TO_PROCESS = 80_000;

    private final ActivityRepository activityRepository;
    private final DestinationRepository destinationRepository;
    private final DestinationPageCardRepository destinationPageCardRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDirPath;

    public record OptimizationResult(
            int filesProcessed,
            int filesOptimized,
            int referencesUpdated,
            long bytesBefore,
            long bytesAfter
    ) {
        public long bytesSaved() {
            return Math.max(0, bytesBefore - bytesAfter);
        }
    }

    private Path uploadDir() {
        return Paths.get(uploadDirPath).toAbsolutePath().normalize();
    }

    /** Returns the filename that should be stored in the database (may change .png/.webp -> .jpg). */
    public String optimizeUploadedFile(Path path) {
        OptimizeOutcome outcome = optimizeFile(path);
        if (outcome.renamed()) {
            updateImageReferences(Map.of(outcome.oldFilename(), outcome.newFilename()));
        }
        return outcome.newFilename();
    }

    @Transactional
    public OptimizationResult optimizeAllUploads() {
        Path uploadDir = uploadDir();
        cleanupTempFiles(uploadDir);

        int processed = 0;
        int optimized = 0;
        long beforeTotal = 0;
        long afterTotal = 0;
        Map<String, String> renames = new HashMap<>();

        if (!Files.isDirectory(uploadDir)) {
            return new OptimizationResult(0, 0, 0, 0, 0);
        }

        try (Stream<Path> files = Files.list(uploadDir)) {
            for (Path path : files.filter(Files::isRegularFile).toList()) {
                String name = path.getFileName().toString();
                if (name.startsWith("img-opt-") || name.startsWith("opt-")) {
                    continue;
                }
                processed++;
                long before = sizeOf(path);
                beforeTotal += before;

                OptimizeOutcome outcome = optimizeFile(path);
                if (outcome.optimized()) {
                    optimized++;
                }
                if (outcome.renamed()) {
                    renames.put(outcome.oldFilename(), outcome.newFilename());
                }
                afterTotal += sizeOf(uploadDir.resolve(outcome.newFilename()));
            }
        } catch (IOException e) {
            log.error("Failed to scan upload directory {}", uploadDir, e);
        }

        int referencesUpdated = updateImageReferences(renames);
        referencesUpdated += repairConvertedImageReferences();
        cleanupTempFiles(uploadDir);
        return new OptimizationResult(processed, optimized, referencesUpdated, beforeTotal, afterTotal);
    }

    private record OptimizeOutcome(
            String oldFilename,
            String newFilename,
            boolean optimized,
            boolean renamed
    ) {
    }

    private OptimizeOutcome optimizeFile(Path path) {
        String oldFilename = path.getFileName().toString();
        String lowerName = oldFilename.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".gif")) {
            return new OptimizeOutcome(oldFilename, oldFilename, false, false);
        }

        long before;
        try {
            before = Files.size(path);
        } catch (IOException e) {
            return new OptimizeOutcome(oldFilename, oldFilename, false, false);
        }

        if (before < MIN_BYTES_TO_PROCESS) {
            return new OptimizeOutcome(oldFilename, oldFilename, false, false);
        }

        Path temp = null;
        try {
            temp = Files.createTempFile(path.getParent(), "opt-", ".jpg");
            Thumbnails.of(path.toFile())
                    .size(MAX_DIMENSION, MAX_DIMENSION)
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .outputQuality(JPEG_QUALITY)
                    .toFile(temp.toFile());

            long after = Files.size(temp);
            if (after <= 0 || after >= before) {
                return new OptimizeOutcome(oldFilename, oldFilename, false, false);
            }

            boolean convertToJpg = lowerName.endsWith(".png") || lowerName.endsWith(".webp");
            if (convertToJpg) {
                String baseName = oldFilename.replaceAll("(?i)\\.(png|webp)$", "");
                Path jpgPath = path.getParent().resolve(baseName + ".jpg");
                Files.move(temp, jpgPath, StandardCopyOption.REPLACE_EXISTING);
                temp = null;
                Files.deleteIfExists(path);
                makeWorldReadable(jpgPath);
                log.info("Optimized {} -> {} ({} -> {} bytes)", oldFilename, jpgPath.getFileName(), before, after);
                return new OptimizeOutcome(oldFilename, jpgPath.getFileName().toString(), true, true);
            }

            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            temp = null;
            makeWorldReadable(path);
            log.info("Optimized {} ({} -> {} bytes)", oldFilename, before, after);
            return new OptimizeOutcome(oldFilename, oldFilename, true, false);
        } catch (Exception e) {
            log.warn("Could not optimize {}: {}", oldFilename, e.getMessage());
            return new OptimizeOutcome(oldFilename, oldFilename, false, false);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // no-op
                }
            }
        }
    }

    private int updateImageReferences(Map<String, String> renames) {
        if (renames.isEmpty()) {
            return 0;
        }
        int updated = 0;

        for (Activity activity : activityRepository.findAll()) {
            boolean changed = false;
            String imageUrl = replaceFilename(activity.getImageUrl(), renames);
            if (imageUrl != null && !imageUrl.equals(activity.getImageUrl())) {
                activity.setImageUrl(imageUrl);
                changed = true;
            }
            List<String> gallery = activity.getGalleryImages();
            if (gallery != null && !gallery.isEmpty()) {
                List<String> nextGallery = gallery.stream().map(url -> replaceFilename(url, renames)).toList();
                if (!nextGallery.equals(gallery)) {
                    activity.setGalleryImages(nextGallery);
                    changed = true;
                }
            }
            if (changed) {
                activityRepository.save(activity);
                updated++;
            }
        }

        for (Destination destination : destinationRepository.findAll()) {
            String imageUrl = replaceFilename(destination.getImageUrl(), renames);
            if (imageUrl != null && !imageUrl.equals(destination.getImageUrl())) {
                destination.setImageUrl(imageUrl);
                destinationRepository.save(destination);
                updated++;
            }
        }

        for (DestinationPageCard card : destinationPageCardRepository.findAll()) {
            String imageUrl = replaceFilename(card.getImageUrl(), renames);
            if (imageUrl != null && !imageUrl.equals(card.getImageUrl())) {
                card.setImageUrl(imageUrl);
                destinationPageCardRepository.save(card);
                updated++;
            }
        }

        return updated;
    }

    private int repairConvertedImageReferences() {
        Map<String, String> renames = new HashMap<>();
        Path uploadDir = uploadDir();
        if (!Files.isDirectory(uploadDir)) {
            return 0;
        }

        try (Stream<Path> files = Files.list(uploadDir)) {
            for (Path jpg : files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jpg"))
                    .toList()) {
                String jpgName = jpg.getFileName().toString();
                String base = jpgName.substring(0, jpgName.length() - 4);
                Path png = uploadDir.resolve(base + ".png");
                Path webp = uploadDir.resolve(base + ".webp");
                if (!Files.exists(png)) {
                    renames.put(base + ".png", jpgName);
                }
                if (!Files.exists(webp)) {
                    renames.put(base + ".webp", jpgName);
                }
            }
        } catch (IOException e) {
            log.warn("Could not repair converted image references: {}", e.getMessage());
            return 0;
        }

        return updateImageReferences(renames);
    }

    private static void makeWorldReadable(Path path) {
        try {
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-r--r--");
            Files.setPosixFilePermissions(path, perms);
        } catch (UnsupportedOperationException | IOException ignored) {
            // no-op on non-posix filesystems
        }
    }

    private static String replaceFilename(String url, Map<String, String> renames) {
        if (url == null || url.isBlank()) {
            return url;
        }
        for (Map.Entry<String, String> entry : renames.entrySet()) {
            if (url.contains(entry.getKey())) {
                return url.replace(entry.getKey(), entry.getValue());
            }
        }
        return url;
    }

    private static long sizeOf(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0;
        }
    }

    private static void cleanupTempFiles(Path uploadDir) {
        try (Stream<Path> files = Files.list(uploadDir)) {
            files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith("img-opt-") || name.startsWith("opt-"))
                    .forEach(name -> {
                        try {
                            Files.deleteIfExists(uploadDir.resolve(name));
                        } catch (IOException e) {
                            log.warn("Could not delete temp upload file {}", name);
                        }
                    });
        } catch (IOException e) {
            log.warn("Could not cleanup temp upload files: {}", e.getMessage());
        }
    }
}
