package net.splatcraft.neoforge.particle;

final class InkParticleColors {
    private InkParticleColors() {
    }

    static float red(int color) {
        return (float) ((color >> 16) & 0xFF) / 255.0F;
    }

    static float green(int color) {
        return (float) ((color >> 8) & 0xFF) / 255.0F;
    }

    static float blue(int color) {
        return (float) (color & 0xFF) / 255.0F;
    }
}
