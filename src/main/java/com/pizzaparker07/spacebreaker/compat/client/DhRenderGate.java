package com.pizzaparker07.spacebreaker.compat.client;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeRenderEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;

/**
 * Holds Distant Horizons' LOD pass back for a frame by cancelling its before-render event, DH's own way of skipping
 * a frame (it does the same for dimensions the player told it to ignore). Unlike an override of its "rendering
 * enabled" setting this touches no config entry: DH 3.3 links that setting to its quick toggle, which saves the
 * override to the player's config file, so LODs would stay off after the game is closed.
 * <p>
 * Only loaded by {@link DhCompat} once DH's classes are known to be there.
 */
public final class DhRenderGate extends DhApiBeforeRenderEvent {
    static volatile boolean closed;
    private static boolean installed;

    /** Registers the gate with DH once; it stays registered and only acts while {@link #closed}. */
    static void install() {
        if (!installed) {
            DhApi.events.bind(DhApiBeforeRenderEvent.class, new DhRenderGate());
            installed = true;
        }
    }

    @Override
    public void beforeRender(DhApiCancelableEventParam<DhApiRenderParam> event) {
        if (closed) {
            event.cancelEvent();
        }
    }
}
