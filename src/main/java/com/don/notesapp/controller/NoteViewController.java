package com.don.notesapp.controller;

import com.don.notesapp.entity.Note;
import com.don.notesapp.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/my-notes")
public class NoteViewController {

    private final NoteService noteService;
    public NoteViewController(NoteService noteService) {
        this.noteService = noteService;
    }

    // Show all notes for logged-in user
    @GetMapping
public String showNotes(
        @RequestParam(required = false) String keyword,
        Model model) {

    List<Note> notes;

    if (keyword != null && !keyword.trim().isEmpty()) {
        notes = noteService.searchNotes(keyword);
        model.addAttribute("keyword", keyword);
    } else {
        notes = noteService.getAllNotes();
    }

    model.addAttribute("notes", notes);
    return "notes";
}

    // Show create note form
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("note", new Note());
        return "create-note";
    }

    // Handle create note form submission
    @PostMapping("/create")
    public String createNote(
            @Valid @ModelAttribute("note") Note note,
            BindingResult result,
            Authentication authentication
    ) {
        // If validation fails, go back to form and show errors
        if (result.hasErrors()) {
            return "create-note";
        }

        noteService.createNote(note);
        return "redirect:/my-notes";
    }

    // Show edit note form
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Note note = noteService.getNoteById(id);
        model.addAttribute("note", note);
        return "edit-note";
    }

    // Handle edit note form submission
    @PostMapping("/edit/{id}")
    public String updateNote(
            @PathVariable Long id,
            @Valid @ModelAttribute("note") Note note,
            BindingResult result
    ) {
        // If validation fails, go back to form and show errors
        if (result.hasErrors()) {
            note.setId(id);
            return "edit-note";
        }

        noteService.updateNote(id, note);
        return "redirect:/my-notes";
    }

    // Delete a note
    @PostMapping("/delete/{id}")
    public String deleteNote(@PathVariable Long id) {
        noteService.deleteNote(id);
        return "redirect:/my-notes";
    }

    @GetMapping("/{id}/history")
    public String showHistory(@PathVariable Long id, Model model) {
        Note note = noteService.getNoteById(id);
        model.addAttribute("note", note);
        model.addAttribute("versions", noteService.getVersionHistory(id));
        return "note-history";
    }

    @PostMapping("/{noteId}/history/{versionId}/restore")
    public String restoreVersion(@PathVariable Long noteId, @PathVariable Long versionId) {
        noteService.restoreVersion(noteId, versionId);
        return "redirect:/my-notes/edit/" + noteId;
    }
}
