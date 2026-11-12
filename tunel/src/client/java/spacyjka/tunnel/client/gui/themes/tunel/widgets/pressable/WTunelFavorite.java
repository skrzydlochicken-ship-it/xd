/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client.gui.themes.tunel.widgets.pressable;

import spacyjka.tunnel.client.gui.themes.tunel.TunelWidget;
import spacyjka.tunnel.client.gui.widgets.pressable.WFavorite;
import spacyjka.tunnel.client.utils.render.color.Color;

public class WTunelFavorite extends WFavorite implements TunelWidget {
    public WTunelFavorite(boolean checked) {
        super(checked);
    }

    @Override
    protected Color getColor() {
        return theme().favoriteColor.get();
    }
}
