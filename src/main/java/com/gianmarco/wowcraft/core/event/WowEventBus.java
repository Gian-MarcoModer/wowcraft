package com.gianmarco.wowcraft.core.event;

import com.gianmarco.wowcraft.WowCraft;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central event bus for WowCraft.
 * Provides thread-safe event registration and posting.
 * 
 * Usage:
 * 
 * <pre>
 * // Register a handler
 * WowEventBus.register(PreDamageEvent.class, event -> {
 *     if (event.getTarget().hasShield()) {
 *         event.setDamage(event.getDamage() * 0.5f);
 *     }
 * });
 * 
 * // Post an event
 * PreDamageEvent event = WowEventBus.post(new PreDamageEvent(source, target, damage));
 * if (!event.isCancelled()) {
 *     // Apply damage
 * }
 * </pre>
 */
public final class WowEventBus {

    private WowEventBus() {
    } // Prevent instantiation

    // Map of event type -> list of handlers sorted by priority
    private static final Map<Class<?>, List<RegisteredHandler<?>>> handlers = new ConcurrentHashMap<>();

    /**
     * Register an event handler with NORMAL priority.
     * 
     * @param eventType The class of the event to listen for
     * @param handler   The handler to invoke when the event is posted
     * @param <T>       The event type
     */
    public static <T extends WowEvent> void register(Class<T> eventType, EventHandler<T> handler) {
        register(eventType, EventPriority.NORMAL, handler);
    }

    /**
     * Register an event handler with a specific priority.
     * 
     * @param eventType The class of the event to listen for
     * @param priority  The priority level for this handler
     * @param handler   The handler to invoke when the event is posted
     * @param <T>       The event type
     */
    public static <T extends WowEvent> void register(
            Class<T> eventType,
            EventPriority priority,
            EventHandler<T> handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>())
                .add(new RegisteredHandler<>(priority, handler));

        // Re-sort by priority after adding
        handlers.get(eventType).sort(Comparator.comparingInt(h -> h.priority().getOrder()));

        WowCraft.LOGGER.debug("Registered {} handler for {} with priority {}",
                eventType.getSimpleName(), handler.getClass().getSimpleName(), priority);
    }

    /**
     * Post an event to all registered handlers.
     * Handlers are invoked in priority order.
     * If the event is cancellable and gets cancelled, remaining handlers are
     * skipped.
     * 
     * @param event The event to post
     * @param <T>   The event type
     * @return The event (possibly modified by handlers)
     */
    @SuppressWarnings("unchecked")
    public static <T extends WowEvent> T post(T event) {
        List<RegisteredHandler<?>> handlerList = handlers.get(event.getClass());

        if (handlerList == null || handlerList.isEmpty()) {
            return event;
        }

        for (RegisteredHandler<?> registered : handlerList) {
            try {
                ((EventHandler<T>) registered.handler()).handle(event);

                // Stop if event was cancelled (except MONITOR handlers)
                if (event.isCancelled() && registered.priority() != EventPriority.MONITOR) {
                    break;
                }
            } catch (Exception e) {
                WowCraft.LOGGER.error("Error handling event {} in handler {}",
                        event.getClass().getSimpleName(),
                        registered.handler().getClass().getSimpleName(),
                        e);
            }
        }

        return event;
    }

    /**
     * Check if an event type has any registered handlers.
     * 
     * @param eventType The event class to check
     * @return true if handlers exist
     */
    public static boolean hasHandlers(Class<? extends WowEvent> eventType) {
        List<RegisteredHandler<?>> list = handlers.get(eventType);
        return list != null && !list.isEmpty();
    }

    /**
     * Remove all handlers for an event type.
     * Mainly useful for testing.
     * 
     * @param eventType The event class to clear
     */
    public static void clearHandlers(Class<? extends WowEvent> eventType) {
        handlers.remove(eventType);
    }

    /**
     * Remove all registered handlers.
     * Mainly useful for testing or mod reload.
     */
    public static void clearAll() {
        handlers.clear();
    }

    /**
     * Internal record for tracking handlers with their priority.
     */
    private record RegisteredHandler<T extends WowEvent>(
            EventPriority priority,
            EventHandler<T> handler) {
    }
}
