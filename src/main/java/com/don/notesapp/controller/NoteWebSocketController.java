package com.don.notesapp.controller;

import com.don.notesapp.dto.NoteEditMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class NoteWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public NoteWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Client sends to /app/notes/{noteId}/edit
    // Server broadcasts to /topic/notes/{noteId}
    @MessageMapping("/notes/{noteId}/edit")
    public void handleNoteEdit(
            @DestinationVariable Long noteId,
            @Payload NoteEditMessage message,
            Authentication authentication) {

        // Stamp who made the edit
        message.setNoteId(noteId);
        message.setEditedBy(authentication.getName());
        message.setType("EDIT");

        // Broadcast to all users on this note
        messagingTemplate.convertAndSend(
                "/topic/notes/" + noteId,
                message
        );
    }

    // Notify others when a user joins the note
    @MessageMapping("/notes/{noteId}/join")
    public void handleJoin(
            @DestinationVariable Long noteId,
            Authentication authentication) {

        NoteEditMessage message = new NoteEditMessage();
        message.setNoteId(noteId);
        message.setEditedBy(authentication.getName());
        message.setType("JOIN");

        messagingTemplate.convertAndSend(
                "/topic/notes/" + noteId,
                message
        );
    }

    // Notify others when a user leaves
    @MessageMapping("/notes/{noteId}/leave")
    public void handleLeave(
            @DestinationVariable Long noteId,
            Authentication authentication) {

        NoteEditMessage message = new NoteEditMessage();
        message.setNoteId(noteId);
        message.setEditedBy(authentication.getName());
        message.setType("LEAVE");

        messagingTemplate.convertAndSend(
                "/topic/notes/" + noteId,
                message
        );
    }
}