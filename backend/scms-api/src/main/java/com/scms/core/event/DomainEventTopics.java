package com.scms.core.event;

public final class DomainEventTopics {

    public static final String ACTIVITY_PUBLISHED = "activity.published";
    public static final String REGISTRATION_CREATED = "registration.created";
    public static final String REGISTRATION_CANCELED = "registration.canceled";
    public static final String NOTIFICATION_DISPATCH = "notification.dispatch";
    public static final String AUDIT_CREATED = "audit.created";

    private DomainEventTopics() {
    }
}
