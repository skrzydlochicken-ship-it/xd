/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client.systems.modules;

import net.minecraft.world.item.Items;

public class Categories {
    public static final Category Esp = new Category("ESP", Items.GLASS.getDefaultInstance());

    public static boolean REGISTERING;

    public static void init() {
        REGISTERING = true;

        Modules.registerCategory(Esp);

        REGISTERING = false;
    }
}
