package com.don.notesapp.repository;

import com.don.notesapp.entity.Note;
import com.don.notesapp.entity.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByUser(User user, Sort sort);

    List<Note> findByUserAndTitleContainingIgnoreCase(User user, String keyword, Sort sort);

    Optional<Note> findByIdAndUser(Long id, User user);
}
