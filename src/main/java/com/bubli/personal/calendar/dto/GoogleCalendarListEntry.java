package com.bubli.personal.calendar.dto;

public record GoogleCalendarListEntry(
		String id,
		String summary,
		Boolean primary,
		String accessRole,
		Boolean selected,
		String backgroundColor
) {
	public String displayName() {
		return summary == null || summary.isBlank() ? id : summary;
	}
}
