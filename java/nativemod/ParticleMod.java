package org.levimc.launcher.core.mods.inbuilt.nativemod;

/**
 * Bridge JNI para o Particle Control mod.
 *
 * Categorias de partículas (bitmask):
 *   TERRAIN   = 1  – quebrar blocos, passos
 *   COMBAT    = 2  – crits, hits
 *   AMBIENT   = 4  – corações, redstone, slime
 *   FIRE      = 8  – chama, fumaça, lava
 *   WATER     = 16 – bolha, splash, drip
 *   MAGIC     = 32 – portal, encantamento, poção
 *   EXPLOSION = 64 – explosões
 */
public class ParticleMod {

    // ── Categorias (espelhadas do C++) ──────────────────────────
    public static final int CAT_TERRAIN   = 1 << 0;
    public static final int CAT_COMBAT    = 1 << 1;
    public static final int CAT_AMBIENT   = 1 << 2;
    public static final int CAT_FIRE      = 1 << 3;
    public static final int CAT_WATER     = 1 << 4;
    public static final int CAT_MAGIC     = 1 << 5;
    public static final int CAT_EXPLOSION = 1 << 6;

    // ── Init ────────────────────────────────────────────────────
    public static boolean init() {
        if (!InbuiltModsNative.loadLibrary()) return false;
        return nativeInit();
    }

    /**
     * Permite definir o offset manual do addParticle
     * para versões do MCBE sem símbolo exportado.
     * Use após encontrar o offset no IDA/Ghidra.
     *
     * Exemplo:
     *   ParticleMod.setManualOffset(0x1A2B3C4D);
     */
    public static boolean setManualOffset(long offset) {
        return nativeSetOffset(offset);
    }

    // ── Controle em tempo real ─────────────────────────────────
    public static void setAllDisabled(boolean disabled) {
        nativeSetAllDisabled(disabled);
    }

    public static void setCategoryDisabled(int category, boolean disabled) {
        nativeSetCategoryDisabled(category, disabled);
    }

    public static int getDisabledCategories() {
        return nativeGetDisabledCategories();
    }

    public static boolean isCategoryDisabled(int category) {
        return (getDisabledCategories() & category) != 0;
    }

    // ── Status ──────────────────────────────────────────────────
    public static native boolean nativeInit();
    public static native boolean nativeSetOffset(long offset);
    public static native void    nativeSetAllDisabled(boolean disabled);
    public static native void    nativeSetCategoryDisabled(int category, boolean disabled);
    public static native int     nativeGetDisabledCategories();
    public static native boolean nativeIsHookActive();
    public static native boolean nativeIsInitialized();
}
