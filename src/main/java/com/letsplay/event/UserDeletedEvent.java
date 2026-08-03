package com.letsplay.event;

public class UserDeletedEvent {
    private final String userId;

    public UserDeletedEvent(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
