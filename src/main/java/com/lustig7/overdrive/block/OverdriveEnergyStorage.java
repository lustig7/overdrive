package com.lustig7.overdrive.block;

import com.lustig7.overdrive.Config;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Receive-only FE buffer. Capacity and input rate are read live from the server config. */
public class OverdriveEnergyStorage implements IEnergyStorage {
    private final Runnable onChanged;
    private int energy;

    public OverdriveEnergyStorage(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (maxReceive <= 0) return 0;
        int accepted = Math.min(getMaxEnergyStored() - energy, Math.min(Config.get(Config.MAX_ENERGY_INPUT), maxReceive));
        if (accepted <= 0) return 0;
        if (!simulate) {
            energy += accepted;
            onChanged.run();
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return Math.min(energy, getMaxEnergyStored());
    }

    @Override
    public int getMaxEnergyStored() {
        return Config.get(Config.ENERGY_BUFFER);
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    /** Internal drain for running costs. */
    public void consume(long amount) {
        if (amount <= 0) return;
        energy = (int) Math.max(0L, energy - amount);
    }

    public void setEnergy(int value) {
        energy = Math.clamp(value, 0, getMaxEnergyStored());
    }
}
