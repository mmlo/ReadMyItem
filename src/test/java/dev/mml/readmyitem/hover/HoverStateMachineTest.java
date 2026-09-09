package dev.mml.readmyitem.hover;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HoverStateMachineTest {
	@Test
	void delayThenSpeakNameThenDetails() {
		HoverStateMachine machine = new HoverStateMachine();
		long t = 1000;
		assertEquals(HoverStateMachine.Event.NONE, machine.onFrame("sword@0", false, false, t, 450, 900, 150));
		assertEquals(HoverStateMachine.Event.NONE, machine.onFrame("sword@0", false, false, t + 400, 450, 900, 150));
		assertEquals(HoverStateMachine.Event.SPEAK_NAME, machine.onFrame("sword@0", false, false, t + 450, 450, 900, 150));
		assertEquals(HoverStateMachine.Event.NONE, machine.onFrame("sword@0", false, false, t + 500, 450, 900, 150));
		assertEquals(HoverStateMachine.Event.SPEAK_DETAILS, machine.onFrame("sword@0", false, false, t + 500 + 900, 450, 900, 150));
	}

	@Test
	void leavingBeforeDelayCancels() {
		HoverStateMachine machine = new HoverStateMachine();
		long t = 0;
		machine.onFrame("sword@0", false, false, t, 450, 900, 150);
		assertEquals(HoverStateMachine.Event.NONE, machine.onFrame("", true, false, t + 100, 450, 900, 150));
		assertEquals(HoverStateMachine.Event.STOP, machine.onFrame("", true, false, t + 200, 450, 900, 150));
		assertEquals(HoverStateMachine.Phase.IDLE, machine.phase());
	}

	@Test
	void sameSlotDoesNotRestart() {
		HoverStateMachine machine = new HoverStateMachine();
		long t = 0;
		machine.onFrame("sword@0", false, false, t, 450, 900, 150);
		machine.onFrame("sword@0", false, false, t + 450, 450, 900, 150);
		HoverStateMachine.Event again = machine.onFrame("sword@0", false, false, t + 460, 450, 900, 150);
		assertEquals(HoverStateMachine.Event.NONE, again);
	}
}
