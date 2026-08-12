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

    public boolean canView(Note note, User user) {
        if (isOwner(note, user)) {
            return true;
        }

        return findCollaborator(note, user)
                .map(collaborator -> collaborator.getRole() == CollaboratorRole.EDITOR
                        || collaborator.getRole() == CollaboratorRole.VIEWER)
                .orElse(false);
    }

    public boolean canEdit(Note note, User user) {
        if (isOwner(note, user)) {
            return true;
        }

        return findCollaborator(note, user)
                .map(collaborator -> collaborator.getRole() == CollaboratorRole.EDITOR)
                .orElse(false);
    }

    public boolean canDelete(Note note, User user) {
        if (isOwner(note, user)) {
            return true;
        }

        findCollaborator(note, user);
        return false;
    }

    public boolean canShare(Note note, User user) {
        if (isOwner(note, user)) {
            return true;
        }

        findCollaborator(note, user);
        return false;
    }

    public boolean canChangeRole(Note note, User user) {
        if (isOwner(note, user)) {
            return true;
        }

        findCollaborator(note, user);
        return false;
    }

    @Transactional(readOnly = true)
    public List<NoteCollaborator> getCollaborators(Note note) {
        return noteCollaboratorRepository.findByNote(note);
    }

    @Transactional
    public void removeCollaborator(
            Note note,
            User user
    ) {
        if (note.getOwner() != null
                && note.getOwner().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "The note owner cannot be removed as a collaborator"
            );
        }

        NoteCollaborator collaborator = noteCollaboratorRepository
                .findByNoteAndUser(note, user)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User is not a collaborator on this note"
                ));

        noteCollaboratorRepository.delete(collaborator);
    }

    @Transactional
    public NoteCollaborator changeRole(
            Note note,
            User user,
            CollaboratorRole newRole
    ) {
        if (note.getOwner() != null
                && note.getOwner().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "The note owner's role cannot be changed"
            );
        }

        NoteCollaborator collaborator = noteCollaboratorRepository
                .findByNoteAndUser(note, user)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User is not a collaborator on this note"
                ));

        collaborator.setRole(newRole);

        return noteCollaboratorRepository.save(collaborator);
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

    private boolean isOwner(Note note, User user) {
        return note != null
                && note.getOwner() != null
                && note.getOwner().getId() != null
                && user != null
                && note.getOwner().getId().equals(user.getId());
    }
}
