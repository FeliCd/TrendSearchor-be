package com.fpt.swp.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class CloudinaryService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_FORMATS = {"jpg", "jpeg", "png", "webp"};

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Upload a file to Cloudinary with validation and auto-cropping.
     *
     * @param file     the multipart file to upload
     * @param folder   target folder in Cloudinary (e.g. "trendsearchor/avatars")
     * @param publicId optional public ID; pass null for auto-generated ID
     * @return a map containing "secure_url", "public_id", "width", "height", "bytes", "format"
     * @throws IllegalArgumentException if validation fails
     */
    public Map<String, Object> upload(MultipartFile file, String folder, String publicId) throws IOException {
        validateFile(file);

        Map<String, Object> params = new HashMap<>();
        params.put("folder", folder);
        params.put("resource_type", "image");

        // Apply transformation: crop to square 500x500 with face detection during upload
        Transformation transformation = new Transformation()
                .width(500)
                .height(500)
                .crop("fill")
                .gravity("face");
        params.put("transformation", transformation);

        if (publicId != null && !publicId.isBlank()) {
            params.put("public_id", publicId);
        }

        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), params);

        Map<String, Object> response = new HashMap<>();
        response.put("secure_url", result.get("secure_url"));
        response.put("public_id", result.get("public_id"));
        response.put("width", result.get("width"));
        response.put("height", result.get("height"));
        response.put("bytes", result.get("bytes"));
        response.put("format", result.get("format"));
        return response;
    }

    /**
     * Validate file type and size.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or not provided.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds the maximum limit of 5MB.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !hasAllowedExtension(filename)) {
            throw new IllegalArgumentException(
                    "Invalid file type. Allowed formats: " + Arrays.toString(ALLOWED_FORMATS));
        }
    }

    private boolean hasAllowedExtension(String filename) {
        String lower = filename.toLowerCase();
        for (String ext : ALLOWED_FORMATS) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Delete an asset from Cloudinary.
     *
     * @param publicId the public ID of the asset to delete
     * @return true if deletion succeeded
     */
    public boolean delete(String publicId) throws IOException {
        Map<?, ?> result = cloudinary.uploader().destroy(publicId, Map.of("resource_type", "image"));
        return "ok".equals(result.get("result"));
    }
}
