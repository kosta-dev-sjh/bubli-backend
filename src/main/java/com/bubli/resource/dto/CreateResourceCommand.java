package com.bubli.resource.dto;

import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceVisibility;

import java.util.UUID;

public record CreateResourceCommand(
		String title,
		ResourceKind kind,
		ResourceVisibility visibility,
		UUID roomId
) {
}
