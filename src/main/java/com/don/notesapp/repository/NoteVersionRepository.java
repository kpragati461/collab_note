package com.don.notesapp.repository;

import com.don.notesapp.entity.Note;
import com.don.notesapp.entity.NoteVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteVersionRepository extends JpaRepository<NoteVersion, Long> {
    List<NoteVersion> findByNoteOrderByVersionNumberDesc(Note note);
    Optional<NoteVersion> findByIdAndNote(Long id, Note note);
    Optional<NoteVersion> findTopByNoteOrderByVersionNumberDesc(Note note);
    void deleteByNote(Note note);
}
