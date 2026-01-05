package com.gianmarco.wowcraft.core.event;

/**
 * Functional interface for event handlers.
 * 
 * @param <T> The event type this handler processes
 */
@FunctionalInterface
public interface EventHandler<T extends WowEvent> {

    /**
     * Handle the event.
     * 
     * @param event The event to process
     */
    void handle(T event);
}
