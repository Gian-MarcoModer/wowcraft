package com.gianmarco.wowcraft.core.event;

/**
 * Base class for events that can be cancelled.
 * When an event is cancelled, remaining handlers are skipped.
 */
public abstract class CancellableEvent implements WowEvent {

    private boolean cancelled = false;

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public boolean isCancellable() {
        return true;
    }
}
