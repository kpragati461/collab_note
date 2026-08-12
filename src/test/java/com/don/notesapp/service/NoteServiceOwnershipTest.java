package com.don.notesapp.service;

import com.don.notesapp.entity.Note;
import com.don.notesapp.entity.Tag;
import com.don.notesapp.entity.User;
import com.don.notesapp.exception.NoteNotFoundException;
import com.don.notesapp.repository.NoteRepository;
import com.don.notesapp.repository.NoteVersionRepository;
import com.don.notesapp.repository.TagRepository;
import com.don.notesapp.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteServiceOwnershipTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NoteVersionRepository noteVersionRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private CollaborationService collaborationService;

    private NoteService noteService;

    private User signedInUser;

    @BeforeEach
    void setUp() {

        noteService = new NoteService(
                noteRepository,
                userRepository,
                noteVersionRepository,
                tagRepository,
                collaborationService
        );

        signedInUser = new User();
        signedInUser.setUsername("alice");

        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                "alice",
                                "password",
                                List.of(
                                        new SimpleGrantedAuthority(
                                                "ROLE_USER"
                                        )
                                )
                        )
                );

        when(
                userRepository.findByUsername("alice")
        ).thenReturn(signedInUser);
    }

    @AfterEach
    void clearSecurityContext() {

        SecurityContextHolder.clearContext();
    }

    @Test
    void updatesOnlyTheSignedInUsersNoteAndCreatesANewVersion() {

        Note existingNote = note(
                "Old title",
                "Old content"
        );

        Note requestedChanges = note(
                "New title",
                "New content"
        );

        Tag workTag = new Tag("work");
        Tag springTag = new Tag("spring");

        requestedChanges.setTagsInput("work, spring");

        when(
                tagRepository.findByNameIgnoreCase("work")
        ).thenReturn(Optional.of(workTag));

        when(
                tagRepository.findByNameIgnoreCase("spring")
        ).thenReturn(Optional.of(springTag));

        when(noteRepository.findById(7L)).thenReturn(Optional.of(existingNote));
        when(collaborationService.canView(existingNote, signedInUser)).thenReturn(true);
        when(collaborationService.canEdit(existingNote, signedInUser)).thenReturn(true);

        when(
                noteRepository.save(existingNote)
        ).thenReturn(existingNote);

        when(
                noteVersionRepository
                        .findTopByNoteOrderByVersionNumberDesc(
                                existingNote
                        )
        ).thenReturn(
                Optional.empty()
        );

        noteService.updateNote(
                7L,
                requestedChanges
        );

        assertEquals(
                "New title",
                existingNote.getTitle()
        );

        assertEquals(
                "New content",
                existingNote.getContent()
        );

        assertEquals(
                List.of(workTag, springTag),
                existingNote.getTags()
        );

        verify(noteRepository).findById(7L);

        verify(
                noteRepository
        ).save(existingNote);

        verify(
                noteVersionRepository
        ).save(any());
    }

    @Test
    void refusesToUpdateANoteTheSignedInUserDoesNotOwn() {

        when(noteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                NoteNotFoundException.class,
                () ->
                        noteService.updateNote(
                                99L,
                                note(
                                        "Changed",
                                        "Changed"
                                )
                        )
        );

        verify(
                noteRepository,
                never()
        ).save(any());

        verify(
                noteVersionRepository,
                never()
        ).save(any());
    }

    @Test
    void refusesToDeleteANoteTheSignedInUserDoesNotOwn() {

        when(noteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                NoteNotFoundException.class,
                () ->
                        noteService.deleteNote(99L)
        );

        verify(
                noteVersionRepository,
                never()
        ).deleteByNote(any());

        verify(
                noteRepository,
                never()
        ).delete(any());
    }

    @Test
    void allowsAnEditorToViewASharedNote() {
        Note sharedNote = note("Shared", "Visible to editors");

        when(noteRepository.findById(15L)).thenReturn(Optional.of(sharedNote));
        when(collaborationService.canView(sharedNote, signedInUser)).thenReturn(true);

        Note foundNote = noteService.getNoteById(15L);

        assertEquals(sharedNote, foundNote);
        verify(collaborationService).canView(sharedNote, signedInUser);
    }

    @Test
    void refusesToUpdateWhenTheUserCanViewButCannotEdit() {
        Note sharedNote = note("Shared", "Viewer content");

        when(noteRepository.findById(16L)).thenReturn(Optional.of(sharedNote));
        when(collaborationService.canView(sharedNote, signedInUser)).thenReturn(true);
        when(collaborationService.canEdit(sharedNote, signedInUser)).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> noteService.updateNote(16L, note("Changed", "Changed"))
        );

        verify(noteRepository, never()).save(sharedNote);
    }

    @Test
    void refusesToDeleteWhenTheUserIsNotTheOwner() {
        Note sharedNote = note("Shared", "Editor content");

        when(noteRepository.findById(17L)).thenReturn(Optional.of(sharedNote));
        when(collaborationService.canView(sharedNote, signedInUser)).thenReturn(true);
        when(collaborationService.canDelete(sharedNote, signedInUser)).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> noteService.deleteNote(17L)
        );

        verify(noteRepository, never()).delete(sharedNote);
    }

    private Note note(
            String title,
            String content
    ) {

        Note note = new Note();

        note.setTitle(title);
        note.setContent(content);

        return note;
    }
}
