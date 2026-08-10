package com.don.notesapp.service;

import com.don.notesapp.entity.Note;
import com.don.notesapp.entity.NoteCollaborator;
import com.don.notesapp.repository.NoteCollaboratorRepository;
import com.don.notesapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
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
}
