package com.don.notesapp.service;

import com.don.notesapp.entity.Note;
import com.don.notesapp.entity.User;
import com.don.notesapp.exception.NoteNotFoundException;
import com.don.notesapp.repository.NoteRepository;
import com.don.notesapp.repository.NoteVersionRepository;
import com.don.notesapp.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteServiceOwnershipTest {

    @Mock private NoteRepository noteRepository;
    @Mock private UserRepository userRepository;
    @Mock private NoteVersionRepository noteVersionRepository;

    private NoteService noteService;
    private User signedInUser;

    @BeforeEach
    void setUp() {
        noteService = new NoteService(noteRepository, userRepository, noteVersionRepository);
        signedInUser = new User();
        signedInUser.setUsername("alice");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "password"));
        when(userRepository.findByUsername("alice")).thenReturn(signedInUser);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updatesOnlyTheSignedInUsersNoteAndCreatesANewVersion() {
        Note existingNote = note("Old title", "Old content");
        Note requestedChanges = note("New title", "New content");
        requestedChanges.setTags(Set.of("work", "spring"));

        when(noteRepository.findByIdAndUser(7L, signedInUser)).thenReturn(Optional.of(existingNote));
        when(noteRepository.save(existingNote)).thenReturn(existingNote);
        when(noteVersionRepository.findTopByNoteOrderByVersionNumberDesc(existingNote)).thenReturn(Optional.empty());

        noteService.updateNote(7L, requestedChanges);

        assertEquals("New title", existingNote.getTitle());
        assertEquals("New content", existingNote.getContent());
        assertEquals(Set.of("work", "spring"), existingNote.getTags());
        verify(noteRepository).findByIdAndUser(7L, signedInUser);
        verify(noteRepository).save(existingNote);
        verify(noteVersionRepository).save(any());
    }

    @Test
    void refusesToUpdateANoteTheSignedInUserDoesNotOwn() {
        when(noteRepository.findByIdAndUser(99L, signedInUser)).thenReturn(Optional.empty());

        assertThrows(NoteNotFoundException.class, () -> noteService.updateNote(99L, note("Changed", "Changed")));

        verify(noteRepository, never()).save(any());
        verify(noteVersionRepository, never()).save(any());
    }

    @Test
    void refusesToDeleteANoteTheSignedInUserDoesNotOwn() {
        when(noteRepository.findByIdAndUser(99L, signedInUser)).thenReturn(Optional.empty());

        assertThrows(NoteNotFoundException.class, () -> noteService.deleteNote(99L));

        verify(noteVersionRepository, never()).deleteByNote(any());
        verify(noteRepository, never()).delete(any());
    }

    private Note note(String title, String content) {
        Note note = new Note();
        note.setTitle(title);
        note.setContent(content);
        return note;
    }
}
