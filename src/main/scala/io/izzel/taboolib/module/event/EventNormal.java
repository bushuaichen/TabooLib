package io.izzel.taboolib.module.event;

import io.izzel.taboolib.kotlin.Reflex;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 普通监听器简化接口
 *
 * @author sky
 * @since 2019-10-22 10:25
 */
@SuppressWarnings({"rawtypes"})
public abstract class EventNormal<T extends EventNormal> extends Event {

    protected static final HandlerList handlers = new HandlerList();

    public EventNormal() {
        this(false);
    }

    public EventNormal(boolean autoAsync) {
        if (autoAsync) {
            async(!Bukkit.isPrimaryThread());
        }
    }

    public T call() {
        try {
            Bukkit.getPluginManager().callEvent(this);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return (T) this;
    }

    public T async(boolean value) {
        Reflex.Companion.from(Event.class, this).write("async", value);
        return (T) this;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    protected static HandlerList getHandlerList() {
        return handlers;
    }
}
