package com.don.notesapp.controller;

import com.don.notesapp.entity.Note;
import com.don.notesapp.entity.User;
import com.don.notesapp.service.CollaborationService;
import com.don.notesapp.service.NoteService;
import com.don.notesapp.service.UserService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(NoteViewController.class)
@AutoConfigureMockMvc
class NoteViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NoteService noteService;

    @MockBean
    private UserService userService;

    @MockBean
    private CollaborationService collaborationService;

    @Test
    void showNoteDetailPage() throws Exception {

        Note note = new Note();

        note.setId(7L);
        note.setTitle("My note");
        note.setContent("Hello world");
        note.setTagsInput("work, ideas");
        User currentUser = new User();
        currentUser.setId(1L);

        when(noteService.getNoteById(7L))
                .thenReturn(note);
        when(userService.findByUsername("alice"))
                .thenReturn(currentUser);
        when(collaborationService.canView(note, currentUser)).thenReturn(true);
        when(collaborationService.canEdit(note, currentUser)).thenReturn(true);
        when(collaborationService.canDelete(note, currentUser)).thenReturn(true);
        when(collaborationService.canShare(note, currentUser)).thenReturn(true);
        when(collaborationService.canChangeRole(note, currentUser)).thenReturn(true);

        mockMvc.perform(
                get("/my-notes/7").with(user("alice"))
        )
        .andExpect(status().isOk())
        .andExpect(
                view().name("note-detail")
        )
        .andExpect(
                model().attributeExists("note")
        )
        .andExpect(
                model().attribute(
                        "note",
                        note
                )
        )
        .andExpect(
                model().attribute("canView", true)
        )
        .andExpect(
                model().attribute("canEdit", true)
        )
        .andExpect(
                model().attribute("canDelete", true)
        )
        .andExpect(
                model().attribute("canShare", true)
        )
        .andExpect(
                model().attribute("canChangeRole", true)
        );
    }

    @Test
    void rejectsDirectEditAccessWhenTheUserCannotEdit() throws Exception {
        Note note = new Note();
        note.setId(7L);
        User currentUser = new User();
        currentUser.setId(2L);

        when(noteService.getNoteById(7L)).thenReturn(note);
        when(userService.findByUsername("viewer")).thenReturn(currentUser);
        when(collaborationService.canEdit(note, currentUser)).thenReturn(false);

        mockMvc.perform(get("/my-notes/edit/7").with(user("viewer")))
                .andExpect(status().isOk())
                .andExpect(view().name("error"));
    }
}
