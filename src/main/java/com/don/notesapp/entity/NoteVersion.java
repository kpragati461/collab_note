package com.don.notesapp.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "note_versions",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"note_id", "version_number"}
                )
        }
)
public class NoteVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /*
     * Snapshot of tags at the time this version was created.
     *
     * Example:
     * "spring, java, backend"
     */
    @Column(name = "tags_text", length = 1000)
    private String tagsText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saved_by")
    private User savedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime savedAt;

    public NoteVersion() {
    }

    public NoteVersion(
            Note note,
            int versionNumber,
            User savedBy
    ) {
        this.note = note;
        this.versionNumber = versionNumber;
        this.title = note.getTitle();
        this.content = note.getContent();
        this.savedBy = savedBy;

        this.tagsText = note.getTags()
                .stream()
                .map(Tag::getName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    public Long getId() {
        return id;
    }

    public Note getNote() {
        return note;
    }

    public void setNote(Note note) {
        this.note = note;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTagsText() {
        return tagsText;
    }

    public void setTagsText(String tagsText) {
        this.tagsText = tagsText;
    }

    public User getSavedBy() {
        return savedBy;
    }

    public void setSavedBy(User savedBy) {
        this.savedBy = savedBy;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }
}