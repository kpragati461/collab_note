package com.don.notesapp.repository;

import com.don.notesapp.entity.NoteAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteAttachmentRepository extends JpaRepository<NoteAttachment, Long> {
}
