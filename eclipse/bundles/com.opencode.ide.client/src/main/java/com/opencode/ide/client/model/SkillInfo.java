package com.opencode.ide.client.model;

/**
 * One skill exposed by the opencode server ({@code GET /skill}; loaded from the
 * working directory's {@code .opencode/skills/}). Nullable-tolerant.
 */
public record SkillInfo(String name, String description) {
}
