package com.don.notesapp.repository;

import com.don.notesapp.entity.Note;
import com.don.notesapp.entity.NoteCollaborator;
import com.don.notesapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteCollaboratorRepository
        extends JpaRepository<NoteCollaborator, Long> {

    List<NoteCollaborator> findByNote(Note note);

    Optional<NoteCollaborator> findByNoteAndUser(
            Note note,
            User user
    );

    boolean existsByNoteAndUser(
            Note note,
            User user
    );
}
