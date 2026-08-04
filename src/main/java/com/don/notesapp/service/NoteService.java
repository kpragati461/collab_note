package com.don.notesapp.service;

import com.don.notesapp.entity.Note;
import com.don.notesapp.entity.User;
import com.don.notesapp.entity.NoteVersion;
import com.don.notesapp.exception.NoteNotFoundException;
import com.don.notesapp.repository.NoteRepository;
import com.don.notesapp.repository.NoteVersionRepository;
import com.don.notesapp.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final NoteVersionRepository noteVersionRepository;

    public NoteService(NoteRepository noteRepository,
                       UserRepository userRepository,
                       NoteVersionRepository noteVersionRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.noteVersionRepository = noteVersionRepository;
    }

    @Transactional
    public Note createNote(Note note) {
        note.setUser(getCurrentUser());
        Note savedNote = noteRepository.save(note);
        saveVersion(savedNote);
        return savedNote;
    }

    // ← now sorted by newest first
    public List<Note> getAllNotes() {
        User user = getCurrentUser();
        return noteRepository.findByUser(user,
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    // ← new search method
    public List<Note> searchNotes(String keyword) {
        User user = getCurrentUser();
        return noteRepository.findByUserAndTitleContainingIgnoreCase(
                user, keyword, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public Note getNoteById(Long id) {
        return noteRepository.findByIdAndUser(id, getCurrentUser())
                .orElseThrow(() -> new NoteNotFoundException(id));
    }

    @Transactional
    public Note updateNote(Long id, Note updatedNote) {
        Note existingNote = getNoteById(id);
        existingNote.setTitle(updatedNote.getTitle());
        existingNote.setContent(updatedNote.getContent());
        existingNote.setTags(updatedNote.getTags());
        Note savedNote = noteRepository.save(existingNote);
        saveVersion(savedNote);
        return savedNote;
    }

    @Transactional
    public void deleteNote(Long id) {
        Note note = getNoteById(id);
        noteVersionRepository.deleteByNote(note);
        noteRepository.delete(note);
    }

    public List<NoteVersion> getVersionHistory(Long noteId) {
        return noteVersionRepository.findByNoteOrderByVersionNumberDesc(getNoteById(noteId));
    }

    @Transactional
    public void restoreVersion(Long noteId, Long versionId) {
        Note note = getNoteById(noteId);
        NoteVersion version = noteVersionRepository.findByIdAndNote(versionId, note)
                .orElseThrow(() -> new NoteNotFoundException(versionId));
        note.setTitle(version.getTitle());
        note.setContent(version.getContent());
        note.setTagsInput(version.getTagsText());
        noteRepository.save(note);
        saveVersion(note);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(authentication.getName());
    }

    private void saveVersion(Note note) {
        int nextVersion = noteVersionRepository.findTopByNoteOrderByVersionNumberDesc(note)
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);
        noteVersionRepository.save(new NoteVersion(note, nextVersion));
    }
}
