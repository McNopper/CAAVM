package com.opencode.ide.client.model;

/**
 * One part of a chat message. Only the fields needed for rendering are mapped;
 * the {@code type} discriminates ({@code text}, {@code reasoning}, {@code tool},
 * {@code step-start}, {@code step-finish}, ...). {@code text} is present on
 * {@code text} and {@code reasoning} parts; other types are ignored by the
 * chat renderer.
 */
public record ChatPart(String type, String text) {

    public boolean isText() {
        return "text".equals(type);
    }

    public boolean isReasoning() {
        return "reasoning".equals(type);
    }
}
