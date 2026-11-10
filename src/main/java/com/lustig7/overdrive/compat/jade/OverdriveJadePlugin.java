package com.lustig7.overdrive.compat.jade;

import com.lustig7.overdrive.block.OverdriveBlock;
import com.lustig7.overdrive.block.OverdriveBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/** Discovered by Jade through the annotation; never loaded without Jade. */
@WailaPlugin
public class OverdriveJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(OverdriveJadeProvider.INSTANCE, OverdriveBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(OverdriveJadeProvider.INSTANCE, OverdriveBlock.class);
    }
}
