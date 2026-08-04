package com.don.notesapp.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "note_versions", uniqueConstraints = @UniqueConstraint(columnNames = {"note_id", "version_number"}))
public class NoteVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "tags_text", length = 1000)
    private String tagsText;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime savedAt;

    public NoteVersion() { }

    public NoteVersion(Note note, int versionNumber) {
        this.note = note;
        this.versionNumber = versionNumber;
        this.title = note.getTitle();
        this.content = note.getContent();
        this.tagsText = String.join(", ", note.getTags());
    }

    public Long getId() { return id; }
    public Note getNote() { return note; }
    public int getVersionNumber() { return versionNumber; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getTagsText() { return tagsText; }
    public LocalDateTime getSavedAt() { return savedAt; }
}
