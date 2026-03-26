package com.example.link;

enum LeaveReason {
	KICKED,
	CLOSED,
	LEFT;

	static LeaveReason fromString(String value) {
		if (value == null) {
			return null;
		}
		try {
			return valueOf(value);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
