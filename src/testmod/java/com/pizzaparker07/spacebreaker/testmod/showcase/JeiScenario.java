package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.client.CameraType;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe viewer showcase (-Pscenario=jei -PwithJei=true, or -Pscenario=emi -PwithEmi=true): opens JEI on several Spacebreaker categories and on an info page.
 * Each shot's client step goes through {@link JeiScreens} so this class has no JEI types and loads without JEI.
 */
final class JeiScenario implements ShowcaseScenario {
    /** false: the same shots through EMI (-Pscenario=emi -PwithEmi=true). */
    private final boolean jei;

    JeiScenario(boolean jei) {
        this.jei = jei;
    }

    @Override
    public void build(ServerLevel level) {
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        CameraType fp = CameraType.FIRST_PERSON;
        String[] categories = {"crushing", "arc_smelting", "fabricating", "refining", "electrolysis", "sabatier", "regolith_processing",
                "chemical_reaction", "sintering", "suit_modules"};
        int n = 1;
        for (String c : categories) {
            String name = String.format("%02d_%s", n++, c);
            s.add(new ShowcaseDriver.Shot(name, 6000, 0.5, -55, 0.5, 0F, 20F, false, fp, 60, null,
                    () -> {
                        if (jei) {
                            JeiScreens.showCategory(Spacebreaker.id(c));
                        } else {
                            EmiScreens.showCategory(Spacebreaker.id(c));
                        }
                    }));
        }
        s.add(new ShowcaseDriver.Shot(String.format("%02d_info_rocket_assembler", n), 6000, 0.5, -55, 0.5, 0F, 20F, false, fp, 60, null,
                () -> {
                    if (jei) {
                        JeiScreens.showInfo();
                    } else {
                        EmiScreens.showInfo(Spacebreaker.id("/info/rocket_assembler"));
                    }
                }));
        // items added later by other areas (crafted with vanilla recipes or fabricated): their recipes must show up
        for (String item : new String[]{"rover", "rtg", "space_probe", "comm_satellite", "observation_satellite", "crew_capsule_large",
                "cargo_module", "landing_beacon", "suit_station", "regolith_excavator", "aerostat_kit", "descent_probe", "solar_array"}) {
            String name = String.format("%02d_recipe_%s", ++n, item);
            s.add(new ShowcaseDriver.Shot(name, 6000, 0.5, -55, 0.5, 0F, 20F, false, fp, 60, null,
                    () -> {
                        if (jei) {
                            JeiScreens.showRecipesFor(Spacebreaker.id(item));
                        } else {
                            EmiScreens.showRecipesFor(Spacebreaker.id(item));
                        }
                    }));
        }
        return s;
    }
}
