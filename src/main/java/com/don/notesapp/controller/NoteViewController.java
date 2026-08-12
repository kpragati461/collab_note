package com.don.notesapp.controller;

import com.don.notesapp.entity.Note;
import com.don.notesapp.entity.User;
import com.don.notesapp.service.CollaborationService;
import com.don.notesapp.service.NoteService;
import com.don.notesapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@Controller
@RequestMapping("/my-notes")
public class NoteViewController {

    private final NoteService noteService;
    private final UserService userService;
    private final CollaborationService collaborationService;

    public NoteViewController(
            NoteService noteService,
            UserService userService,
            CollaborationService collaborationService
    ) {
        this.noteService = noteService;
        this.userService = userService;
        this.collaborationService = collaborationService;
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
            BindingResult result) {

        if (result.hasErrors()) {
            return "create-note";
        }

        noteService.createNote(note);

        return "redirect:/my-notes";
    }

    // Show note detail
    @GetMapping("/{id}")
    public String showNoteDetail(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {

        Note note = noteService.getNoteById(id);

        model.addAttribute("note", note);
        addPermissions(model, note, authentication);

        return "note-detail";
    }

    // Show edit form
    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {

        Note note = noteService.getNoteById(id);
        ensureCanEdit(note, authentication);

        model.addAttribute("note", note);
        model.addAttribute("canEdit", true);

        return "edit-note";
    }

    // Handle edit
    @PostMapping("/edit/{id}")
    public String updateNote(
            @PathVariable Long id,
            @Valid @ModelAttribute("note") Note note,
            BindingResult result,
            Authentication authentication) {

        ensureCanEdit(noteService.getNoteById(id), authentication);

        if (result.hasErrors()) {
            note.setId(id);
            return "edit-note";
        }

        noteService.updateNote(id, note);

        return "redirect:/my-notes";
    }

    // Delete note
    @PostMapping("/delete/{id}")
    public String deleteNote(
            @PathVariable Long id) {

        noteService.deleteNote(id);

        return "redirect:/my-notes";
    }

    // Version history
    @GetMapping("/{id}/history")
    public String showHistory(
            @PathVariable Long id,
            Model model) {

        Note note = noteService.getNoteById(id);

        model.addAttribute("note", note);
        model.addAttribute(
                "versions",
                noteService.getVersionHistory(id)
        );

        return "note-history";
    }

    // Restore version
    @PostMapping("/{noteId}/history/{versionId}/restore")
    public String restoreVersion(
            @PathVariable Long noteId,
            @PathVariable Long versionId) {

        noteService.restoreVersion(
                noteId,
                versionId
        );

        return "redirect:/my-notes/edit/" + noteId;
    }

    private void addPermissions(
            Model model,
            Note note,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);

        model.addAttribute("canView", collaborationService.canView(note, currentUser));
        model.addAttribute("canEdit", collaborationService.canEdit(note, currentUser));
        model.addAttribute("canDelete", collaborationService.canDelete(note, currentUser));
        model.addAttribute("canShare", collaborationService.canShare(note, currentUser));
        model.addAttribute(
                "canChangeRole",
                collaborationService.canChangeRole(note, currentUser)
        );
    }

    private User getCurrentUser(Authentication authentication) {
        return userService.findByUsername(authentication.getName());
    }

    private void ensureCanEdit(Note note, Authentication authentication) {
        if (!collaborationService.canEdit(note, getCurrentUser(authentication))) {
            throw new IllegalArgumentException(
                    "You do not have permission to edit this note"
            );
        }
    }
}
