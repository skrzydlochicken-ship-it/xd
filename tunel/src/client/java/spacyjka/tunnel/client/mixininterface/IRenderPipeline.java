/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client.mixininterface;

public interface IRenderPipeline {
    void tunel$setLineSmooth(boolean lineSmooth);

    boolean tunel$getLineSmooth();
}
