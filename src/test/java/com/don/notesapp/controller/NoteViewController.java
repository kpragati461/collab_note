package com.don.notesapp.controller;

import com.don.notesapp.entity.Note;
import com.don.notesapp.service.NoteService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(NoteViewController.class)
@AutoConfigureMockMvc(addFilters = false)
class NoteViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NoteService noteService;

    @Test
    void showNoteDetailPage() throws Exception {

        Note note = new Note();

        note.setId(7L);
        note.setTitle("My note");
        note.setContent("Hello world");
        note.setTagsInput("work, ideas");

        when(noteService.getNoteById(7L))
                .thenReturn(note);

        mockMvc.perform(
                get("/my-notes/7")
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
        );
    }
}