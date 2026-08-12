package com.don.notesapp.service;

import com.don.notesapp.entity.Note;
import com.don.notesapp.entity.NoteCollaborator;
import com.don.notesapp.entity.CollaboratorRole;
import com.don.notesapp.entity.User;
import com.don.notesapp.repository.NoteCollaboratorRepository;
import com.don.notesapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollaborationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NoteCollaboratorRepository collaboratorRepository;

    @Test
    void returnsTheCollaboratorsFoundForTheNote() {
        CollaborationService service = new CollaborationService(
                userRepository,
                collaboratorRepository
        );
        Note note = new Note();
        List<NoteCollaborator> expectedCollaborators = List.of(
                new NoteCollaborator()
        );

        when(collaboratorRepository.findByNote(note))
                .thenReturn(expectedCollaborators);

        List<NoteCollaborator> actualCollaborators = service.getCollaborators(note);

        assertSame(expectedCollaborators, actualCollaborators);
        verify(collaboratorRepository).findByNote(note);
    }

    @Test
    void removesAnExistingCollaboratorWithoutDeletingTheUserOrNote() {
        CollaborationService service = new CollaborationService(
                userRepository,
                collaboratorRepository
        );
        Note note = noteWithOwner(1L);
        User collaboratorUser = user(2L);
        NoteCollaborator collaborator = new NoteCollaborator();

        when(collaboratorRepository.findByNoteAndUser(note, collaboratorUser))
                .thenReturn(Optional.of(collaborator));

        service.removeCollaborator(note, collaboratorUser);

        verify(collaboratorRepository).delete(collaborator);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void rejectsRemovalWhenTheUserIsNotACollaborator() {
        CollaborationService service = new CollaborationService(
                userRepository,
                collaboratorRepository
        );
        Note note = noteWithOwner(1L);
        User user = user(2L);

        when(collaboratorRepository.findByNoteAndUser(note, user))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.removeCollaborator(note, user)
        );

        verify(collaboratorRepository, never()).delete(any());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void rejectsRemovalOfTheNoteOwner() {
        CollaborationService service = new CollaborationService(
                userRepository,
                collaboratorRepository
        );
        Note note = noteWithOwner(1L);
        User owner = note.getOwner();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.removeCollaborator(note, owner)
        );

        verify(collaboratorRepository, never()).delete(any());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void changesAnExistingCollaboratorsRoleAndReturnsTheSavedCollaborator() {
        CollaborationService service = new CollaborationService(
                userRepository,
                collaboratorRepository
        );
        Note note = noteWithOwner(1L);
        User collaboratorUser = user(2L);
        NoteCollaborator collaborator = new NoteCollaborator();
        collaborator.setRole(CollaboratorRole.EDITOR);

        when(collaboratorRepository.findByNoteAndUser(note, collaboratorUser))
                .thenReturn(Optional.of(collaborator));
        when(collaboratorRepository.save(collaborator))
                .thenReturn(collaborator);

        NoteCollaborator savedCollaborator = service.changeRole(
                note,
                collaboratorUser,
                CollaboratorRole.VIEWER
        );

        assertSame(collaborator, savedCollaborator);
        assertSame(CollaboratorRole.VIEWER, collaborator.getRole());
        verify(collaboratorRepository).findByNoteAndUser(note, collaboratorUser);
        verify(collaboratorRepository).save(collaborator);
    }

    @Test
    void rejectsRoleChangeWhenTheUserIsNotACollaborator() {
        CollaborationService service = new CollaborationService(
                userRepository,
                collaboratorRepository
        );
        Note note = noteWithOwner(1L);
        User user = user(2L);

        when(collaboratorRepository.findByNoteAndUser(note, user))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.changeRole(note, user, CollaboratorRole.VIEWER)
        );

        verify(collaboratorRepository, never()).save(any());
    }

    @Test
    void rejectsRoleChangeForTheNoteOwnerBeforeLookingUpACollaborator() {
        CollaborationService service = new CollaborationService(
                userRepository,
                collaboratorRepository
        );
        Note note = noteWithOwner(1L);
        User owner = note.getOwner();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.changeRole(note, owner, CollaboratorRole.VIEWER)
        );

        verify(collaboratorRepository, never()).findByNoteAndUser(any(), any());
        verify(collaboratorRepository, never()).save(any());
    }

    private Note noteWithOwner(Long ownerId) {
        Note note = new Note();
        note.setOwner(user(ownerId));
        return note;
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
