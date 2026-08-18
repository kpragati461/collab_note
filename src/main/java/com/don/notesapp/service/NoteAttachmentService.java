package com.don.notesapp.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.don.notesapp.entity.Note;
import com.don.notesapp.entity.NoteAttachment;
import com.don.notesapp.repository.NoteAttachmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NoteAttachmentService {

    private final NoteAttachmentRepository attachmentRepository;
    private final Cloudinary cloudinary;

    public NoteAttachmentService(
            NoteAttachmentRepository attachmentRepository,
            Cloudinary cloudinary
    ) {
        this.attachmentRepository = attachmentRepository;
        this.cloudinary = cloudinary;
    }

    public void addAttachments(Note note, List<MultipartFile> files) {
        if (files == null) {
            return;
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            String contentType = file.getContentType();
            String mediaType = resolveMediaType(contentType);
            String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
            String publicId = "notesapp/" + UUID.randomUUID();

            try {
                // Determine resource type based on media type
                String resourceType = mediaType.equals("IMAGE") ? "image" : "video";
                
                // Upload to Cloudinary
                Map uploadResult = cloudinary.uploader().upload(
                        file.getInputStream(),
                        ObjectUtils.asMap(
                                "public_id", publicId,
                                "resource_type", resourceType,
                                "original_filename", originalFilename
                        )
                );

                String cloudinaryUrl = (String) uploadResult.get("secure_url");
                String cloudinaryPublicId = (String) uploadResult.get("public_id");

                NoteAttachment attachment = new NoteAttachment();
                attachment.setNote(note);
                attachment.setOriginalFilename(originalFilename);
                attachment.setStoredFilename(cloudinaryPublicId);
                attachment.setMediaType(mediaType);
                attachment.setContentType(contentType);
                attachment.setCloudinaryUrl(cloudinaryUrl);
                note.getAttachments().add(attachment);
                attachmentRepository.save(attachment);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not upload attachment to Cloudinary", exception);
            }
        }
    }

    public void deleteFiles(Note note) {
        note.getAttachments().forEach(attachment -> {
            try {
                String resourceType = attachment.getMediaType().equals("IMAGE") ? "image" : "video";
                cloudinary.uploader().destroy(
                        attachment.getStoredFilename(),
                        ObjectUtils.asMap("resource_type", resourceType)
                );
            } catch (IOException exception) {
                throw new IllegalStateException("Could not delete attachment from Cloudinary", exception);
            }
        });
    }

    private String resolveMediaType(String contentType) {
        if (contentType != null && contentType.startsWith("image/")) {
            return "IMAGE";
        }
        if (contentType != null && contentType.startsWith("video/")) {
            return "VIDEO";
        }
        throw new IllegalArgumentException("Only image and video attachments are supported");
    }

    private String sanitizeOriginalFilename(String filename) {
        String safeFilename = filename == null ? "attachment" : filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return safeFilename.isBlank() ? "attachment" : safeFilename;
    }
}
