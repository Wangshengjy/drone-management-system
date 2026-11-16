package com.drone.collab.domain;

public final class Statuses {

    public static final String DRAFT = "DRAFT";
    public static final String PENDING_REVIEW = "PENDING_REVIEW";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String FROZEN = "FROZEN";

    public static final String AUTO_PASS = "PASS";
    public static final String AUTO_NEED_MANUAL = "NEED_MANUAL";
    public static final String AUTO_REJECT = "REJECT";

    private Statuses() {
    }
}
