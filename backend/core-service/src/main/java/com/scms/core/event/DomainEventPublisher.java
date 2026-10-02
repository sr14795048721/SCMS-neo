package com.scms.core.event;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
