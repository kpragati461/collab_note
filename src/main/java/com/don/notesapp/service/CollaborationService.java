package com.don.notesapp.service;

import com.don.notesapp.entity.CollaboratorRole;
import com.don.notesapp.entity.Note;
import com.don.notesapp.entity.NoteCollaborator;
import com.don.notesapp.entity.User;
import com.don.notesapp.repository.NoteCollaboratorRepository;
import com.don.notesapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CollaborationService {

    private final UserRepository userRepository;
    private final NoteCollaboratorRepository noteCollaboratorRepository;

    public CollaborationService(
            UserRepository userRepository,
            NoteCollaboratorRepository noteCollaboratorRepository
    ) {
        this.userRepository = userRepository;
        this.noteCollaboratorRepository = noteCollaboratorRepository;
    }

    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<NoteCollaborator> findCollaborator(Note note, User user) {
        return noteCollaboratorRepository.findByNoteAndUser(note, user);
    }

    @Transactional(readOnly = true)
    public List<NoteCollaborator> getCollaborators(Note note) {
        return noteCollaboratorRepository.findByNote(note);
    }

    @Transactional
    public NoteCollaborator shareNote(
            Note note,
            String username,
            CollaboratorRole role
    ) {
        User targetUser = findUserByUsername(username);

        if (targetUser == null) {
            throw new IllegalArgumentException(
                    "User with username '" + username + "' does not exist"
            );
        }

        if (note.getOwner() != null
                && note.getOwner().getId().equals(targetUser.getId())) {
            throw new IllegalArgumentException(
                    "The note owner cannot be added as a collaborator"
            );
        }

        NoteCollaborator collaborator = findCollaborator(note, targetUser)
                .orElseGet(NoteCollaborator::new);

        collaborator.setNote(note);
        collaborator.setUser(targetUser);
        collaborator.setRole(role);

        return noteCollaboratorRepository.save(collaborator);
    }
}
