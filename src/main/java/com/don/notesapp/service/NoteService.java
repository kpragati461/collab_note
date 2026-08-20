package com.don.notesapp.service;

import com.don.notesapp.entity.Note;
import com.don.notesapp.entity.NoteVersion;
import com.don.notesapp.entity.Tag;
import com.don.notesapp.entity.User;
import com.don.notesapp.exception.NoteNotFoundException;
import com.don.notesapp.repository.NoteRepository;
import com.don.notesapp.repository.NoteVersionRepository;
import com.don.notesapp.repository.TagRepository;
import com.don.notesapp.repository.UserRepository;

import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final NoteVersionRepository noteVersionRepository;
    private final TagRepository tagRepository;
    private final CollaborationService collaborationService;

    public NoteService(
            NoteRepository noteRepository,
            UserRepository userRepository,
            NoteVersionRepository noteVersionRepository,
            TagRepository tagRepository,
            CollaborationService collaborationService
    ) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.noteVersionRepository = noteVersionRepository;
        this.tagRepository = tagRepository;
        this.collaborationService = collaborationService;
    }

    /*
     * CREATE NOTE
     */
    @Transactional
    public Note createNote(Note note) {

        User currentUser = getCurrentUser();

        note.setOwner(currentUser);

        applyTags(note);

        Note savedNote = noteRepository.save(note);

        saveVersion(savedNote);

        return savedNote;
    }

    /*
 * GET ALL NOTES OWNED BY OR SHARED WITH CURRENT USER
 */
@Transactional(readOnly = true)
public List<Note> getAllNotes() {

    User currentUser = getCurrentUser();

    // Notes owned by user
    List<Note> ownedNotes = noteRepository.findByOwner(
            currentUser,
            Sort.by(Sort.Direction.DESC, "createdAt")
    );

    // Notes shared with user as collaborator
    List<Note> collaboratedNotes = noteRepository
            .findByCollaboratorsUser(
                    currentUser,
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );

    // Merge both lists — no duplicates
    List<Note> allNotes = new ArrayList<>(ownedNotes);

    collaboratedNotes.forEach(note -> {
        if (allNotes.stream().noneMatch(n -> n.getId().equals(note.getId()))) {
            allNotes.add(note);
        }
    });

    // Sort merged list by createdAt DESC
    allNotes.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

    return allNotes;
}

/*
 * SEARCH NOTES OWNED BY OR SHARED WITH CURRENT USER
 */
@Transactional(readOnly = true)
public List<Note> searchNotes(String keyword) {
    User currentUser = getCurrentUser();

    // Search by title
    List<Note> byTitle = noteRepository.findByOwnerAndTitleContainingIgnoreCase(
            currentUser, keyword, Sort.by(Sort.Direction.DESC, "createdAt"));

    // Search by tag name
    List<Note> byTag = noteRepository.findByOwnerAndTagsNameIgnoreCase(
            currentUser, keyword, Sort.by(Sort.Direction.DESC, "createdAt"));

    // Merge without duplicates
    List<Note> merged = new ArrayList<>(byTitle);
    byTag.forEach(note -> {
        if (merged.stream().noneMatch(n -> n.getId().equals(note.getId()))) {
            merged.add(note);
        }
    });

    merged.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
    return merged;
}
    /*
     * GET ONE NOTE
     *
     * Only the owner can currently access it.
     *
     * Later this will become:
     * owner OR collaborator.
     */
    @Transactional(readOnly = true)
    public Note getNoteById(Long id) {

        User currentUser = getCurrentUser();
        Note note = noteRepository.findById(id)
                .orElseThrow(
                        () -> new NoteNotFoundException(id)
                );

        if (!collaborationService.canView(note, currentUser)) {
            throw new IllegalArgumentException("You do not have access to this note");
        }

        return note;
    }

    /*
     * UPDATE NOTE
     */
    @Transactional
    public Note updateNote(
            Long id,
            Note updatedNote
    ) {

        Note existingNote = getNoteById(id);

        if (!collaborationService.canEdit(existingNote, getCurrentUser())) {
            throw new IllegalArgumentException("You do not have permission to edit this note");
        }

        existingNote.setTitle(
                updatedNote.getTitle()
        );

        existingNote.setContent(
                updatedNote.getContent()
        );

        /*
         * Convert the tagsInput from the form
         * into real Tag entities.
         */
        if (updatedNote.getTagsInput() != null) {

            existingNote.setTags(
                    resolveTags(
                            updatedNote.getTagsInput()
                    )
            );
        }

        Note savedNote = noteRepository.save(existingNote);

        saveVersion(savedNote);

        return savedNote;
    }

    /*
     * DELETE NOTE
     */
    @Transactional
    public void deleteNote(Long id) {

        Note note = getNoteById(id);

        if (!collaborationService.canDelete(note, getCurrentUser())) {
            throw new IllegalArgumentException("You do not have permission to delete this note");
        }

        noteVersionRepository.deleteByNote(note);

        noteRepository.delete(note);
    }

    /*
     * VERSION HISTORY
     */
    @Transactional(readOnly = true)
    public List<NoteVersion> getVersionHistory(
            Long noteId
    ) {

        Note note = getNoteById(noteId);

        return noteVersionRepository
                .findByNoteOrderByVersionNumberDesc(note);
    }

    /*
     * RESTORE VERSION
     */
    @Transactional
    public void restoreVersion(
            Long noteId,
            Long versionId
    ) {

        Note note = getNoteById(noteId);

        NoteVersion version =
                noteVersionRepository
                        .findByIdAndNote(
                                versionId,
                                note
                        )
                        .orElseThrow(
                                () -> new NoteNotFoundException(
                                        versionId
                                )
                        );

        note.setTitle(
                version.getTitle()
        );

        note.setContent(
                version.getContent()
        );

        /*
         * Restore the tags from the snapshot.
         */
        note.setTags(
                resolveTags(
                        version.getTagsText()
                )
        );

        noteRepository.save(note);

        /*
         * Restoring is itself a new version.
         */
        saveVersion(note);
    }

    /*
     * CREATE A VERSION SNAPSHOT
     */
    private void saveVersion(Note note) {

        int nextVersion =
                noteVersionRepository
                        .findTopByNoteOrderByVersionNumberDesc(note)
                        .map(
                                version ->
                                        version.getVersionNumber() + 1
                        )
                        .orElse(1);

        NoteVersion version =
                new NoteVersion(
                        note,
                        nextVersion,
                        getCurrentUser()
                );

        noteVersionRepository.save(version);
    }

    /*
     * Convert comma-separated tag input
     * into Tag entities.
     */
    private void applyTags(Note note) {

        String tagsInput = note.getTagsInput();

        if (tagsInput == null || tagsInput.isBlank()) {

            note.setTags(new ArrayList<>());

            return;
        }

        note.setTags(
                resolveTags(tagsInput)
        );
    }

    /*
     * Find existing tags or create new ones.
     */
    private List<Tag> resolveTags(
            String tagsInput
    ) {

        if (tagsInput == null || tagsInput.isBlank()) {

            return new ArrayList<>();
        }

        return Arrays.stream(
                        tagsInput.split(",")
                )
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .map(String::toLowerCase)
                .map(
                        tag ->
                                tag.length() > 50
                                        ? tag.substring(0, 50)
                                        : tag
                )
                .distinct()
                .map(
                        tagName ->
                                tagRepository
                                        .findByNameIgnoreCase(tagName)
                                        .orElseGet(
                                                () ->
                                                        tagRepository.save(
                                                                new Tag(tagName)
                                                        )
                                        )
                )
                .toList();
    }

    /*
     * CURRENT LOGGED-IN USER
     */
    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "No authenticated user found"
            );
        }

        User user =
                userRepository.findByUsername(
                        authentication.getName()
                );

        if (user == null) {

            throw new IllegalStateException(
                    "Authenticated user does not exist"
            );
        }

        return user;
    }
}
