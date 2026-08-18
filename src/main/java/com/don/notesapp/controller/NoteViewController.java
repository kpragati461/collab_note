package com.don.notesapp.controller;

import com.don.notesapp.entity.CollaboratorRole;
import com.don.notesapp.entity.Note;
import com.don.notesapp.entity.User;
import com.don.notesapp.service.CollaborationService;
import com.don.notesapp.service.NoteAttachmentService;
import com.don.notesapp.service.NoteService;
import com.don.notesapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/my-notes")
public class NoteViewController {

    private final NoteService noteService;
    private final NoteAttachmentService noteAttachmentService;
    private final UserService userService;
    private final CollaborationService collaborationService;

    public NoteViewController(
            NoteService noteService,
            NoteAttachmentService noteAttachmentService,
            UserService userService,
            CollaborationService collaborationService
    ) {
        this.noteService = noteService;
        this.noteAttachmentService = noteAttachmentService;
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
        model.addAttribute("greeting", getGreeting());
        return "notes";
    }

    private String getGreeting() {
        int hour = LocalTime.now().getHour();

        if (hour < 5) {
            return "Good night!";
        }
        if (hour < 12) {
            return "Good morning!";
        }
        if (hour < 18) {
            return "Good afternoon!";
        }
        if (hour < 22) {
            return "Good evening!";
        }
        return "Good night!";
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
            @RequestParam(name = "attachments", required = false) List<MultipartFile> attachments) {

        if (result.hasErrors()) {
            return "create-note";
        }

        Note savedNote = noteService.createNote(note);
        noteAttachmentService.addAttachments(savedNote, attachments);
        return "redirect:/my-notes";
    }

    // Show note detail
    @GetMapping("/{id}")
    public String showNoteDetail(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {

        Note note = noteService.getNoteById(id);
        User currentUser = getCurrentUser(authentication);

        model.addAttribute("note", note);
        model.addAttribute("collaborators", collaborationService.getCollaborators(note));
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
        model.addAttribute("currentUsername", authentication.getName());

        return "edit-note";
    }

    // Handle edit
    @PostMapping("/edit/{id}")
    public String updateNote(
            @PathVariable Long id,
            @Valid @ModelAttribute("note") Note note,
            BindingResult result,
            Authentication authentication,
            @RequestParam(name = "attachments", required = false) List<MultipartFile> attachments) {

        ensureCanEdit(noteService.getNoteById(id), authentication);

        if (result.hasErrors()) {
            note.setId(id);
            return "edit-note";
        }

        Note savedNote = noteService.updateNote(id, note);
        noteAttachmentService.addAttachments(savedNote, attachments);
        return "redirect:/my-notes";
    }

    // Delete note
    @PostMapping("/delete/{id}")
    public String deleteNote(@PathVariable Long id) {
        Note note = noteService.getNoteById(id);
        noteAttachmentService.deleteFiles(note);
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
        model.addAttribute("versions", noteService.getVersionHistory(id));

        return "note-history";
    }

    // Restore version
    @PostMapping("/{noteId}/history/{versionId}/restore")
    public String restoreVersion(
            @PathVariable Long noteId,
            @PathVariable Long versionId) {

        noteService.restoreVersion(noteId, versionId);
        return "redirect:/my-notes/edit/" + noteId;
    }

    // ─── Collaborator endpoints ───────────────────────────────────────────────

    // Add collaborator
    @PostMapping("/{id}/collaborators/add")
    public String addCollaborator(
            @PathVariable Long id,
            @RequestParam String username,
            @RequestParam CollaboratorRole role,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Note note = noteService.getNoteById(id);
        User currentUser = getCurrentUser(authentication);

        if (!collaborationService.canShare(note, currentUser)) {
            redirectAttributes.addFlashAttribute("shareError",
                    "You don't have permission to share this note.");
            return "redirect:/my-notes/" + id;
        }

        User targetUser = userService.findByUsername(username);

        if (targetUser == null) {
            redirectAttributes.addFlashAttribute("shareError",
                    "User '" + username + "' not found.");
            return "redirect:/my-notes/" + id;
        }

        if (targetUser.getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("shareError",
                    "You cannot add yourself as a collaborator.");
            return "redirect:/my-notes/" + id;
        }

        collaborationService.shareNote(note, targetUser, role);
        redirectAttributes.addFlashAttribute("shareSuccess",
                "Added " + username + " as " + role.name().toLowerCase() + ".");

        return "redirect:/my-notes/" + id;
    }

    // Change collaborator role
    @PostMapping("/{id}/collaborators/{userId}/role")
    public String changeCollaboratorRole(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestParam CollaboratorRole role,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Note note = noteService.getNoteById(id);
        User currentUser = getCurrentUser(authentication);

        if (!collaborationService.canChangeRole(note, currentUser)) {
            redirectAttributes.addFlashAttribute("shareError",
                    "You don't have permission to change roles.");
            return "redirect:/my-notes/" + id;
        }

        User targetUser = userService.findById(userId);

        if (targetUser == null) {
            redirectAttributes.addFlashAttribute("shareError", "User not found.");
            return "redirect:/my-notes/" + id;
        }

        collaborationService.changeRole(note, targetUser, role);
        redirectAttributes.addFlashAttribute("shareSuccess", "Role updated.");

        return "redirect:/my-notes/" + id;
    }

    // Remove collaborator
    @PostMapping("/{id}/collaborators/{userId}/remove")
    public String removeCollaborator(
            @PathVariable Long id,
            @PathVariable Long userId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Note note = noteService.getNoteById(id);
        User currentUser = getCurrentUser(authentication);

        if (!collaborationService.canShare(note, currentUser)) {
            redirectAttributes.addFlashAttribute("shareError",
                    "You don't have permission to remove collaborators.");
            return "redirect:/my-notes/" + id;
        }

        User targetUser = userService.findById(userId);

        if (targetUser == null) {
            redirectAttributes.addFlashAttribute("shareError", "User not found.");
            return "redirect:/my-notes/" + id;
        }

        collaborationService.removeCollaborator(note, targetUser);
        redirectAttributes.addFlashAttribute("shareSuccess", "Collaborator removed.");

        return "redirect:/my-notes/" + id;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void addPermissions(
            Model model,
            Note note,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);

        model.addAttribute("canView", collaborationService.canView(note, currentUser));
        model.addAttribute("canEdit", collaborationService.canEdit(note, currentUser));
        model.addAttribute("canDelete", collaborationService.canDelete(note, currentUser));
        model.addAttribute("canShare", collaborationService.canShare(note, currentUser));
        model.addAttribute("canChangeRole", collaborationService.canChangeRole(note, currentUser));
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