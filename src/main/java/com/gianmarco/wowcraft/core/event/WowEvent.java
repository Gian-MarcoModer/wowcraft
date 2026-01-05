package com.gianmarco.wowcraft.core.event;

/**
 * Base interface for all WowCraft events.
 * Events are used for cross-system communication without tight coupling.
 */
public interface WowEvent {

    /**
     * Check if this event has been cancelled.
     * Cancelled events will not propagate to remaining handlers.
     * 
     * @return true if cancelled
     */
    default boolean isCancelled() {
        return false;
    }

    /**
     * Set the cancelled state of this event.
     * Not all events support cancellation - check isCancellable() first.
     * 
     * @param cancelled true to cancel
     */
    default void setCancelled(boolean cancelled) {
        // Default implementation does nothing
        // Override in CancellableEvent
    }

    /**
     * Check if this event type supports cancellation.
     * 
     * @return true if can be cancelled
     */
    default boolean isCancellable() {
        return false;
    }
}
