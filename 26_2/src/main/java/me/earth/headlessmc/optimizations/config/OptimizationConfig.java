package me.earth.headlessmc.optimizations.config;

import java.util.Locale;

/**
 * Startup-only configuration for the mixin plugin.
 *
 * <p>These properties are deliberately read before Minecraft classes are
 * transformed. Changing them while the game is running has no effect.</p>
 */
public final class OptimizationConfig {
    public static final String PREFIX = "hmc.optimizations.";

    private OptimizationConfig() {
    }

    public static boolean enabled() {
        return property("enabled", true);
    }

    public static boolean render() {
        return enabled() && property("render", true);
    }

    /**
     * World render-state extraction invokes renderer-mod callbacks and is kept
     * as a separate opt-in compatibility boundary.
     */
    public static boolean worldRenderState() {
        boolean configured = property("world_render_state", false);
        // Extracting stale state and then asking the graphical renderer to
        // consume it is not a supported combination.
        return enabled() && render() && configured;
    }

    public static boolean particles() {
        return enabled() && property("particles", true);
    }

    public static boolean sound() {
        return enabled() && property("sound", true);
    }

    /** Animated texture uploads are visual-only but may carry mod callbacks. */
    public static boolean animatedTextures() {
        return enabled() && property("animated_textures", false);
    }

    /**
     * Chunk meshes are the most compatibility-sensitive optimization and are
     * therefore opt-in. The supplied benchmark harness enables this property
     * explicitly for its optimized profile.
     */
    public static boolean chunkMesh() {
        return enabled() && property("chunk_mesh", false);
    }

    public static String summary() {
        return "enabled=" + enabled()
            + ", render=" + render()
            + ", world_render_state=" + worldRenderState()
            + ", particles=" + particles()
            + ", sound=" + sound()
            + ", animated_textures=" + animatedTextures()
            + ", chunk_mesh=" + chunkMesh();
    }

    private static boolean property(String name, boolean defaultValue) {
        String value = System.getProperty(PREFIX + name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true", "1", "yes", "on" -> true;
            case "false", "0", "no", "off" -> false;
            default -> throw new IllegalArgumentException(
                "Invalid boolean value for -D" + PREFIX + name + ": " + value);
        };
    }
}
