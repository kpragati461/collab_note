package com.don.notesapp.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.don.notesapp.entity.Note;
import com.don.notesapp.entity.NoteAttachment;
import com.don.notesapp.repository.NoteAttachmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class NoteAttachmentServiceTest {

    @Test
    void storesImagesAndVideosWithTheirMediaTypes() throws Exception {
        NoteAttachmentRepository repository = mock(NoteAttachmentRepository.class);
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap()))
                .thenReturn(
                        Map.of("secure_url", "https://res.cloudinary.com/test/image/upload/photo", "public_id", "notesapp/photo"),
                        Map.of("secure_url", "https://res.cloudinary.com/test/video/upload/clip", "public_id", "notesapp/clip")
                );

        NoteAttachmentService service = new NoteAttachmentService(repository, cloudinary);
        Note note = new Note();

        MockMultipartFile image = new MockMultipartFile(
                "attachments", "photo.png", "image/png", "image".getBytes()
        );
        MockMultipartFile video = new MockMultipartFile(
                "attachments", "clip.mp4", "video/mp4", "video".getBytes()
        );

        service.addAttachments(note, List.of(image, video));

        assertEquals(2, note.getAttachments().size());
        NoteAttachment imageAttachment = note.getAttachments().get(0);
        NoteAttachment videoAttachment = note.getAttachments().get(1);
        assertEquals("IMAGE", imageAttachment.getMediaType());
        assertEquals("VIDEO", videoAttachment.getMediaType());
        assertEquals("notesapp/photo", imageAttachment.getStoredFilename());
        assertEquals("notesapp/clip", videoAttachment.getStoredFilename());
        verify(repository, org.mockito.Mockito.times(2)).save(org.mockito.ArgumentMatchers.any());
    }
}
