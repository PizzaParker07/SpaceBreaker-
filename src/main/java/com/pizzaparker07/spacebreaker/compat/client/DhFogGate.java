package com.pizzaparker07.spacebreaker.compat.client;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeFogRenderEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;

/**
 * Holds Distant Horizons' fog pass back by cancelling its before-fog event, like {@link DhRenderGate} for its LOD
 * pass: no DH setting is touched, so nothing can be saved to the player's config. Only loaded by {@link DhCompat}
 * once DH's classes are known to be there.
 */
public final class DhFogGate extends DhApiBeforeFogRenderEvent {
    static volatile boolean closed;
    private static boolean installed;

    /** Registers the gate with DH once; it stays registered and only acts while {@link #closed}. */
    static void install() {
        if (!installed) {
            DhApi.events.bind(DhApiBeforeFogRenderEvent.class, new DhFogGate());
            installed = true;
        }
    }

    @Override
    public void beforeRender(DhApiCancelableEventParam<DhApiBeforeFogRenderEvent.EventParam> event) {
        if (closed) {
            event.cancelEvent();
        }
    }
}
