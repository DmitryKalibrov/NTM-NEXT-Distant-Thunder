package com.hbm.thunder.client;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * A far-away sound: no vanilla distance attenuation (we computed the volume ourselves), placed a
 * few blocks from the listener in the source's direction so stereo panning still points at it.
 */
final class DistantSound extends AbstractSoundInstance {

    /** OpenAL low-pass high-frequency gain, 0..1. */
    final float gainHF;

    DistantSound(Identifier sound, SoundSource source, Vec3 listener, Vec3 towards,
                 float volume, float pitch, float gainHF) {
        super(sound, source, RandomSource.create());
        Vec3 at = listener.add(towards.subtract(listener).normalize().scale(8));
        this.x = at.x;
        this.y = at.y;
        this.z = at.z;
        this.volume = volume;
        this.pitch = pitch;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.relative = false;
        this.gainHF = gainHF;
    }
}
