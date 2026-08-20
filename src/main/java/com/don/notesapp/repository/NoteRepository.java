package com.don.notesapp.repository;

import com.don.notesapp.entity.Note;
import com.don.notesapp.entity.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByOwner(User owner, Sort sort);

    List<Note> findByOwnerAndTitleContainingIgnoreCase(
            User owner,
            String keyword,
            Sort sort
    );

    Optional<Note> findByIdAndOwner(
            Long id,
            User owner
    );
    // Notes shared with a user as collaborator
List<Note> findByCollaboratorsUser(User user, Sort sort);
List<Note> findByOwnerAndTagsNameIgnoreCase(User owner, String tagName, Sort sort);

// Search in collaborated notes
List<Note> findByCollaboratorsUserAndTitleContainingIgnoreCase(
        User user, String keyword, Sort sort);
        
}
