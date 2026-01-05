package com.gianmarco.wowcraft.core.event;

/**
 * Priority levels for event handlers.
 * Handlers are executed in order from HIGHEST to MONITOR.
 */
public enum EventPriority {
    /**
     * Executed first. Use for system-critical handlers that need first access.
     */
    HIGHEST(0),

    /**
     * Executed after HIGHEST. Use for important modifications.
     */
    HIGH(1),

    /**
     * Default priority. Use for standard event handling.
     */
    NORMAL(2),

    /**
     * Executed after NORMAL. Use for reactions to modifications.
     */
    LOW(3),

    /**
     * Executed after LOW. Use for cleanup or final reactions.
     */
    LOWEST(4),

    /**
     * Executed last. Use for monitoring/logging only.
     * Should NOT modify the event.
     */
    MONITOR(5);

    private final int order;

    EventPriority(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }
}
