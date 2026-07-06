package net.splatcraft.neoforge;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.TranslatableEnum;

public final class SplatcraftConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.EnumValue<SquidKeyMode> SQUID_KEY_MODE;
    public static final ModConfigSpec.EnumValue<InkIndicatorMode> INK_INDICATOR;
    public static final ModConfigSpec.BooleanValue VANILLA_INK_DURABILITY_COLOR;
    public static final ModConfigSpec.BooleanValue HOLD_BARRIER_TO_RENDER;
    public static final ModConfigSpec.IntValue BARRIER_RENDER_DISTANCE;
    public static final ModConfigSpec.BooleanValue COLOR_LOCK;
    public static final ModConfigSpec.EnumValue<PreventBobView> PREVENT_BOB_VIEW;
    public static final ModConfigSpec.BooleanValue LOW_INK_WARNING;
    public static final ModConfigSpec.ConfigValue<String> INK_COLORED_SKIN_LAYER_PATH;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.translation("splatcraft.configuration.section.splatcraft-client.splatcraft")
                .push("splatcraft");
        SQUID_KEY_MODE = builder
                .comment("How the squid form key behaves.")
                .translation("splatcraft.configuration.squid_key_mode")
                .defineEnum("squidKeyMode", SquidKeyMode.TOGGLE);
        INK_INDICATOR = builder
                .comment("Where the client renders the ink level indicator.")
                .translation("splatcraft.configuration.ink_indicator")
                .defineEnum("inkIndicator", InkIndicatorMode.BOTH);
        VANILLA_INK_DURABILITY_COLOR = builder
                .comment("Use vanilla durability colors for ink bars.")
                .translation("splatcraft.configuration.vanilla_ink_durability_color")
                .define("vanillaInkDurabilityColor", false);
        HOLD_BARRIER_TO_RENDER = builder
                .comment("Only render stage barriers in creative while holding a barrier item.")
                .translation("splatcraft.configuration.hold_barrier_to_render")
                .define("holdBarrierToRender", false);
        BARRIER_RENDER_DISTANCE = builder
                .comment("Stage barrier render distance in blocks.")
                .translation("splatcraft.configuration.barrier_render_distance")
                .defineInRange("barrierRenderDistance", 40, 4, 80);
        COLOR_LOCK = builder
                .comment("Render local player ink as friendly yellow and all other ink as hostile purple.")
                .translation("splatcraft.configuration.color_lock")
                .define("colorLock", false);
        PREVENT_BOB_VIEW = builder
                .comment("Suppress view bobbing/FOV changes while swimming in squid form.")
                .translation("splatcraft.configuration.prevent_bob_view")
                .defineEnum("preventBobView", PreventBobView.OFF);
        LOW_INK_WARNING = builder
                .comment("Show the low-ink warning near the crosshair.")
                .translation("splatcraft.configuration.low_ink_warning")
                .define("lowInkWarning", true);
        INK_COLORED_SKIN_LAYER_PATH = builder
                .comment("Path to the optional player ink color skin overlay.")
                .translation("splatcraft.configuration.ink_colored_skin_layer_path")
                .define("inkColoredSkinLayerPath", "config/splatcraft/player_ink_color.png");
        builder.pop();

        SPEC = builder.build();
    }

    private SplatcraftConfig() {
    }

    private static Component enumName(String key) {
        return Component.translatable("splatcraft.configuration.enum." + key);
    }

    public enum SquidKeyMode implements TranslatableEnum {
        HOLD,
        TOGGLE;

        @Override
        public Component getTranslatedName() {
            return enumName("squid_key_mode." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    public enum InkIndicatorMode implements TranslatableEnum {
        CROSSHAIR,
        DURABILITY,
        BOTH,
        NONE;

        @Override
        public Component getTranslatedName() {
            return enumName("ink_indicator." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    public enum PreventBobView implements TranslatableEnum {
        SUBMERGED,
        ALWAYS,
        OFF;

        @Override
        public Component getTranslatedName() {
            return enumName("prevent_bob_view." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }
}
