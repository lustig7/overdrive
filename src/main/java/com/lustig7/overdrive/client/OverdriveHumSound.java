package com.lustig7.overdrive.client;

import com.lustig7.overdrive.block.OverdriveBlockEntity;
import com.lustig7.overdrive.logic.OverdriveMath;
import com.lustig7.overdrive.registry.ModRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

import java.util.Map;
import java.util.WeakHashMap;

/** Looping hum of a running Overdrive. Pitch rises with the multiplier: 0.6 + 0.15 * log2(m). */
public class OverdriveHumSound extends AbstractTickableSoundInstance {
    private static final Map<OverdriveBlockEntity, OverdriveHumSound> PLAYING = new WeakHashMap<>();

    private final OverdriveBlockEntity be;

    private OverdriveHumSound(OverdriveBlockEntity be) {
        super(ModRegistry.SOUND_HUM.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.be = be;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.35F;
        this.x = be.getBlockPos().getX() + 0.5;
        this.y = be.getBlockPos().getY() + 0.5;
        this.z = be.getBlockPos().getZ() + 0.5;
        updatePitch();
    }

    /** Called every client tick while the Overdrive runs above 1x. */
    public static void ensurePlaying(OverdriveBlockEntity be) {
        OverdriveHumSound sound = PLAYING.get(be);
        if (sound == null || sound.isStopped()) {
            sound = new OverdriveHumSound(be);
            PLAYING.put(be, sound);
            Minecraft.getInstance().getSoundManager().play(sound);
        }
    }

    private void updatePitch() {
        pitch = (float) (0.6 + 0.15 * OverdriveMath.log2(be.getClientEffective()));
    }

    @Override
    public void tick() {
        if (be.isRemoved() || be.getClientEffective() <= 1 || be.getLevel() == null
                || be.getLevel().getBlockEntity(be.getBlockPos()) != be) {
            PLAYING.remove(be);
            stop();
            return;
        }
        updatePitch();
    }
}
