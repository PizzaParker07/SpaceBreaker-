package com.pizzaparker07.spacebreaker.content.rocketry.design;

import com.pizzaparker07.spacebreaker.content.rocketry.part.PartType;
import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketPart;

import java.util.ArrayList;
import java.util.List;

/**
 * Where each part of a rocket sits, in model pixels above the bottom of the lowest attached stage.
 * Shared by the renderer (drawing), the entity (size, seat) and spent stages.
 * Rules: axial parts stack; an engine right above an interstage sits inside it; radial parts are drawn
 * four times around their stage: fins at the top of the engine, landing legs from the bottom,
 * grid fins and RCS just under the top of the stage.
 */
public final class RocketGeometry {
    /** One drawn part. yaw in degrees around the rocket axis (radial parts), y in px. */
    public record Placement(int part, double y, float yaw) {
    }

    private final List<Placement> placements;
    private final double height;
    private final double payloadBottom;
    private final double capsuleBottom;

    private RocketGeometry(List<Placement> placements, double height, double payloadBottom, double capsuleBottom) {
        this.placements = placements;
        this.height = height;
        this.payloadBottom = payloadBottom;
        this.capsuleBottom = capsuleBottom;
    }

    /** Geometry of parts [from, to] (part indices) of the layout, with the bottom of part {@code from} at y = 0. */
    public static RocketGeometry of(RocketLayout layout, int from, int to) {
        List<RocketPart> parts = layout.parts();
        List<Placement> out = new ArrayList<>();
        double[] bottom = new double[parts.size()];
        double[] top = new double[parts.size()];
        double cursor = 0;
        RocketPart prevAxial = null;
        int prevAxialIndex = -1;
        double payloadBottom = -1;
        double capsuleBottom = -1;
        for (int i = from; i <= to && i < parts.size(); i++) {
            RocketPart p = parts.get(i);
            if (p.type().radial()) {
                continue;
            }
            double y = cursor;
            if (prevAxial != null && prevAxial.type() == PartType.INTERSTAGE && p.type() == PartType.ENGINE) {
                y = Math.max(bottom[prevAxialIndex], top[prevAxialIndex] - p.height());
            }
            bottom[i] = y;
            top[i] = y + p.height();
            cursor = Math.max(cursor, top[i]);
            out.add(new Placement(i, y, 0));
            if (p.type().payload() && payloadBottom < 0 && i >= layout.payload().firstPart()) {
                payloadBottom = y;
            }
            if (p.type() == PartType.CAPSULE && capsuleBottom < 0) {
                capsuleBottom = y;
            }
            prevAxial = p;
            prevAxialIndex = i;
        }
        // radial parts, anchored to their stage
        for (int i = from; i <= to && i < parts.size(); i++) {
            RocketPart p = parts.get(i);
            if (!p.type().radial()) {
                continue;
            }
            int[] range = stageRange(layout, i);
            int lo = Math.max(range[0], from);
            int hi = Math.min(range[1], to);
            double stageBottom = Double.MAX_VALUE, engineTop = -1, stageTop = 0;
            for (int j = lo; j <= hi; j++) {
                RocketPart q = parts.get(j);
                if (q.type().radial()) {
                    continue;
                }
                stageBottom = Math.min(stageBottom, bottom[j]);
                if (q.type() == PartType.ENGINE && engineTop < 0) {
                    engineTop = top[j];
                }
                if (!q.type().separator()) {
                    stageTop = Math.max(stageTop, top[j]);
                }
            }
            if (stageBottom == Double.MAX_VALUE) {
                stageBottom = 0;
            }
            double anchor;
            float base;
            switch (p.type()) {
                case FINS -> {
                    anchor = engineTop >= 0 ? engineTop : stageBottom;
                    base = 0;
                }
                case LANDING_LEGS -> {
                    anchor = stageBottom + 16;
                    base = 45;
                }
                case GRID_FINS -> {
                    anchor = stageTop;
                    base = 0;
                }
                default -> {
                    anchor = stageTop;
                    base = 45;
                }
            }
            for (int k = 0; k < 4; k++) {
                out.add(new Placement(i, anchor, base + 90 * k));
            }
        }
        return new RocketGeometry(List.copyOf(out), cursor, Math.max(0, payloadBottom), Math.max(0, capsuleBottom));
    }

    /** Geometry of the rocket with stage {@code active} as its lowest stage. */
    public static RocketGeometry ofActive(RocketLayout layout, int active) {
        return of(layout, layout.firstPartOf(active), layout.parts().size() - 1);
    }

    /** Geometry of a single stage (for spent stages). */
    public static RocketGeometry ofStage(RocketLayout layout, int stage) {
        RocketLayout.Stage s = layout.stages().get(stage);
        return of(layout, s.firstPart(), s.lastPart());
    }

    private static int[] stageRange(RocketLayout layout, int part) {
        for (RocketLayout.Stage s : layout.stages()) {
            if (part >= s.firstPart() && part <= s.lastPart()) {
                return new int[]{s.firstPart(), s.lastPart()};
            }
        }
        return new int[]{layout.payload().firstPart(), layout.parts().size() - 1};
    }

    public List<Placement> placements() {
        return placements;
    }

    /** Stack height in px. */
    public double height() {
        return height;
    }

    public double payloadBottom() {
        return payloadBottom;
    }

    /** Bottom of the crew capsule in px (where the seat is). */
    public double capsuleBottom() {
        return capsuleBottom;
    }
}
