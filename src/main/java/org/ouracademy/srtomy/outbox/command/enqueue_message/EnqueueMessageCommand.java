package org.ouracademy.srtomy.outbox.command.enqueue_message;

public record EnqueueMessageCommand(String type, String payload) {}
