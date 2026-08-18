package com.don.notesapp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notes")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title cannot be empty")
    @Size(
            min = 1,
            max = 100,
            message = "Title must be between 1 and 100 characters"
    )
    private String title;

    @NotBlank(message = "Content cannot be empty")
    @Size(
            min = 1,
            max = 5000,
            message = "Content must be between 1 and 5000 characters"
    )
    @Column(columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /*
     * Owner of the note.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /*
     * Users who have access to this note.
     */
    @OneToMany(
            mappedBy = "note",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<NoteCollaborator> collaborators = new ArrayList<>();

    /*
     * Version history.
     */
    @OneToMany(
            mappedBy = "note",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("versionNumber DESC")
    private List<NoteVersion> versions = new ArrayList<>();

        @OneToMany(
            mappedBy = "note",
            cascade = CascadeType.ALL,
            orphanRemoval = true
        )
        private List<NoteAttachment> attachments = new ArrayList<>();

    /*
     * Tags attached to this note.
     */
    @ManyToMany
    @JoinTable(
            name = "note_tags",
            joinColumns = @JoinColumn(name = "note_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags = new ArrayList<>();

    /*
     * Used by Thymeleaf forms.
     *
     * This is NOT stored in the database.
     * NoteService converts this text into Tag entities.
     */
    @Transient
    private String tagsInput;

    public Note() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public List<NoteCollaborator> getCollaborators() {
        return collaborators;
    }

    public void setCollaborators(List<NoteCollaborator> collaborators) {
        this.collaborators =
                collaborators == null ? new ArrayList<>() : collaborators;
    }

    public List<NoteVersion> getVersions() {
        return versions;
    }

    public void setVersions(List<NoteVersion> versions) {
        this.versions =
                versions == null ? new ArrayList<>() : versions;
    }

    public List<NoteAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<NoteAttachment> attachments) {
        this.attachments =
                attachments == null ? new ArrayList<>() : attachments;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags =
                tags == null ? new ArrayList<>() : new ArrayList<>(tags);
    }

    public String getTagsInput() {

        if (tagsInput != null) {
            return tagsInput;
        }

        if (tags == null || tags.isEmpty()) {
            return "";
        }

        return tags.stream()
                .map(Tag::getName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    public void setTagsInput(String tagsInput) {
        this.tagsInput = tagsInput;
    }
}