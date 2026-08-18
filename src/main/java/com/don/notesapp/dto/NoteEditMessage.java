package com.don.notesapp.dto;

public class NoteEditMessage {

    private Long noteId;
    private String title;
    private String content;
    private String editedBy;
    private String type; // "EDIT" or "JOIN" or "LEAVE"

    public NoteEditMessage() {}

    public NoteEditMessage(Long noteId, String title,
                           String content, String editedBy, String type) {
        this.noteId = noteId;
        this.title = title;
        this.content = content;
        this.editedBy = editedBy;
        this.type = type;
    }

    public Long getNoteId() { return noteId; }
    public void setNoteId(Long noteId) { this.noteId = noteId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getEditedBy() { return editedBy; }
    public void setEditedBy(String editedBy) { this.editedBy = editedBy; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
