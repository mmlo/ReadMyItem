package dev.mml.readmyitem.hover;

/**
 * Pure timing/debounce logic so unit tests do not need Minecraft.
 */
public final class HoverStateMachine {
	public enum Phase {
		IDLE,
		WAITING_NAME,
		SPEAK_NAME,
		WAITING_DETAILS,
		SPEAK_DETAILS
	}

	public static final int GRACE_MS = 150;
	public static final int ANTI_SPAM_MS = 50;

	private Phase phase = Phase.IDLE;
	private String currentId = "";
	private long phaseStartedAt;
	private long lastLeaveAt;
	private long lastSeenAt;
	private long lastIdentityChangeAt;
	private int identityChangesInWindow;
	private long windowStartAt;

	public Phase phase() {
		return phase;
	}

	public String currentId() {
		return currentId;
	}

	public void reset(long now) {
		phase = Phase.IDLE;
		currentId = "";
		phaseStartedAt = now;
	}

	public Event onFrame(String identity, boolean empty, boolean speakEmpty, long now,
			int hoverDelayMs, int dwellMs, int graceMs) {
		if (identity == null) {
			identity = "";
		}

		if (!identity.equals(currentId) && !identity.isEmpty() && !currentId.isEmpty()) {
			if (now - windowStartAt > 1000) {
				windowStartAt = now;
				identityChangesInWindow = 0;
			}
			identityChangesInWindow++;
			lastIdentityChangeAt = now;
			if (identityChangesInWindow > 20 || now - lastIdentityChangeAt < ANTI_SPAM_MS && identityChangesInWindow > 8) {
				return Event.NONE;
			}
		}

		boolean same = identity.equals(currentId);
		if (identity.isEmpty() || (empty && !speakEmpty)) {
			if (phase == Phase.IDLE) {
				currentId = "";
				return Event.NONE;
			}
			if (now - lastSeenAt < graceMs) {
				return Event.NONE;
			}
			lastLeaveAt = now;
			phase = Phase.IDLE;
			currentId = "";
			return Event.STOP;
		}
		lastSeenAt = now;

		if (!same) {
			boolean wasActive = phase != Phase.IDLE;
			currentId = identity;
			phase = Phase.WAITING_NAME;
			phaseStartedAt = now;
			return wasActive ? Event.STOP_THEN_WAIT : Event.NONE;
		}

		return switch (phase) {
			case IDLE -> {
				phase = Phase.WAITING_NAME;
				phaseStartedAt = now;
				yield Event.NONE;
			}
			case WAITING_NAME -> {
				if (now - phaseStartedAt >= hoverDelayMs) {
					phase = Phase.SPEAK_NAME;
					phaseStartedAt = now;
					yield Event.SPEAK_NAME;
				}
				yield Event.NONE;
			}
			case SPEAK_NAME -> {
				phase = Phase.WAITING_DETAILS;
				phaseStartedAt = now;
				yield Event.NONE;
			}
			case WAITING_DETAILS -> {
				if (now - phaseStartedAt >= dwellMs) {
					phase = Phase.SPEAK_DETAILS;
					phaseStartedAt = now;
					yield Event.SPEAK_DETAILS;
				}
				yield Event.NONE;
			}
			case SPEAK_DETAILS -> Event.NONE;
		};
	}

	public enum Event {
		NONE,
		SPEAK_NAME,
		SPEAK_DETAILS,
		STOP,
		STOP_THEN_WAIT
	}
}
