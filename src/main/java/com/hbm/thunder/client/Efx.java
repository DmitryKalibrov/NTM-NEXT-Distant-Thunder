package com.hbm.thunder.client;

import com.hbm.thunder.DistantThunder;
import com.mojang.blaze3d.audio.Channel;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.stream.Stream;

import static org.lwjgl.openal.AL10.alSourcei;
import static org.lwjgl.openal.AL11.alSource3i;
import static org.lwjgl.openal.EXTEfx.*;

/**
 * OpenAL EFX for the addon. Everything here runs on the sound thread (ChannelHandle.execute,
 * Channel.play, executeOnChannels); the client thread only writes the volatile deafness values.
 *
 * <ul>
 *   <li>Direct low-pass per sound: distance for our far sounds, walls between you and the source
 *       (occlusion) for world sounds, plus deafness for everything.</li>
 *   <li>One room reverb slot that every world sound sends into. {@link Room} measures the space
 *       around the player and retunes it a few times a second: nothing outdoors, a short slap in a
 *       house, a long rolling tail in a big cave.</li>
 * </ul>
 *
 * With Sound Physics Remastered installed, occlusion and reverb are left to it (see SoundRules).
 */
public final class Efx {

    /** hf/gain < 0: no direct filter of ours. reverb: send into the room slot. */
    public record Setup(float hf, float gain, boolean reverb) {}

    static final Setup NONE = new Setup(-1F, -1F, false);

    private static final Int2ObjectOpenHashMap<Setup> PENDING = new Int2ObjectOpenHashMap<>();
    /** Sources currently playing with a filter of ours -> that setup. */
    private static final Int2ObjectOpenHashMap<Setup> FILTERED = new Int2ObjectOpenHashMap<>();
    /** Sources we put a direct filter on (ours, or deafened). */
    private static final IntOpenHashSet TOUCHED = new IntOpenHashSet();
    private static final IntOpenHashSet REVERBED = new IntOpenHashSet();

    static volatile float deafHF = 1F, deafGain = 1F;

    private static int filter, effect, slot;
    private static boolean disabled;

    private Efx() {}

    /** Channel set up for a new sound; the setup takes effect when it starts playing. */
    public static void prepare(int source, Setup setup) {
        PENDING.put(source, setup);
    }

    public static void onPlay(int source) {
        if (disabled) return;
        Setup s = PENDING.remove(source);
        if (s == null) s = NONE;
        try {
            if (s.hf >= 0) FILTERED.put(source, s);
            else FILTERED.remove(source);
            direct(source);
            if (s.reverb) {
                alSource3i(source, AL_AUXILIARY_SEND_FILTER, slot(), 0, AL_FILTER_NULL);
                REVERBED.add(source);
            } else if (REVERBED.remove(source)) {
                alSource3i(source, AL_AUXILIARY_SEND_FILTER, AL_EFFECTSLOT_NULL, 0, AL_FILTER_NULL);
            }
        } catch (Throwable t) {
            fail(t);
        }
    }

    /** Re-applies the direct filter to everything playing (deafness fading in or out). */
    public static void refresh(Stream<Channel> channels) {
        if (disabled) return;
        channels.forEach(c -> direct(((SourceHolder) c).ntmdt$source()));
    }

    /** Retunes the shared room reverb. decay in s, send 0..1, delay in s, hf 0..1 (wall material). */
    static void room(float decay, float send, float delay, float hf) {
        if (disabled) return;
        try {
            slot();
            alEffectf(effect, AL_REVERB_DECAY_TIME, clamp(decay, 0.1F, 20F));
            alEffectf(effect, AL_REVERB_REFLECTIONS_DELAY, clamp(delay, 0F, 0.3F));
            alEffectf(effect, AL_REVERB_LATE_REVERB_DELAY, clamp(delay * 0.5F, 0F, 0.1F));
            alEffectf(effect, AL_REVERB_GAINHF, clamp(hf, 0F, 1F));
            alAuxiliaryEffectSloti(slot, AL_EFFECTSLOT_EFFECT, effect); // re-attach: picks up new params
            alAuxiliaryEffectSlotf(slot, AL_EFFECTSLOT_GAIN, clamp(send, 0F, 1F));
        } catch (Throwable t) {
            fail(t);
        }
    }

    private static void direct(int source) {
        Setup s = FILTERED.get(source);
        float hf = Math.min(s != null ? s.hf : 1F, deafHF);
        float gain = (s != null ? s.gain : 1F) * deafGain;
        if (s == null && hf >= 0.999F && gain >= 0.999F) {
            if (TOUCHED.remove(source)) alSourcei(source, AL_DIRECT_FILTER, AL_FILTER_NULL);
            return;
        }
        if (filter == 0) {
            filter = alGenFilters();
            alFilteri(filter, AL_FILTER_TYPE, AL_FILTER_LOWPASS);
        }
        alFilterf(filter, AL_LOWPASS_GAIN, clamp(gain, 0F, 1F));
        alFilterf(filter, AL_LOWPASS_GAINHF, clamp(hf, 0F, 1F));
        alSourcei(source, AL_DIRECT_FILTER, filter); // OpenAL copies the filter params
        TOUCHED.add(source);
    }

    private static int slot() {
        if (slot == 0) {
            effect = alGenEffects();
            alEffecti(effect, AL_EFFECT_TYPE, AL_EFFECT_REVERB);
            alEffectf(effect, AL_REVERB_DENSITY, 1F);
            alEffectf(effect, AL_REVERB_DIFFUSION, 1F);
            alEffectf(effect, AL_REVERB_GAIN, 0.7F);
            alEffectf(effect, AL_REVERB_REFLECTIONS_GAIN, 1.2F);
            alEffectf(effect, AL_REVERB_LATE_REVERB_GAIN, 2.0F);
            slot = alGenAuxiliaryEffectSlots();
            alAuxiliaryEffectSloti(slot, AL_EFFECTSLOT_EFFECT, effect);
            alAuxiliaryEffectSlotf(slot, AL_EFFECTSLOT_GAIN, 0F);
        }
        return slot;
    }

    private static void fail(Throwable t) { // no ALC_EXT_EFX: sounds still play, just plain
        disabled = true;
        DistantThunder.LOG.warn("OpenAL EFX unavailable: no muffling, reverb or deafness", t);
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
