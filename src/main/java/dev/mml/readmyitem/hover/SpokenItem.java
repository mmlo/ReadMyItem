package dev.mml.readmyitem.hover;

import java.util.List;

public record SpokenItem(
		String displayName,
		int count,
		List<String> detailLines,
		String stableId
) {
	public SpokenItem {
		displayName = displayName == null ? "" : displayName;
		detailLines = detailLines == null ? List.of() : List.copyOf(detailLines);
		stableId = stableId == null ? "" : stableId;
	}

	public static SpokenItem empty(int slotIndex) {
		return new SpokenItem("", 0, List.of(), "empty:" + slotIndex);
	}
}
