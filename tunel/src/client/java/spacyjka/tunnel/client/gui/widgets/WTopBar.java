/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client.gui.widgets;

import spacyjka.tunnel.client.gui.renderer.GuiRenderer;
import spacyjka.tunnel.client.gui.tabs.Tab;
import spacyjka.tunnel.client.gui.tabs.TabScreen;
import spacyjka.tunnel.client.gui.tabs.Tabs;
import spacyjka.tunnel.client.gui.widgets.containers.WHorizontalList;
import spacyjka.tunnel.client.gui.widgets.pressable.WPressable;
import spacyjka.tunnel.client.utils.render.color.Color;
import net.minecraft.client.gui.screens.Screen;

import static spacyjka.tunnel.client.TunelClient.mc;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPos;

public abstract class WTopBar extends WHorizontalList {
    protected abstract Color getButtonColor(boolean pressed, boolean hovered);

    protected abstract Color getNameColor();

    public WTopBar() {
        spacing = 0;
    }

    @Override
    public void init() {
        for (Tab tab : Tabs.get()) {
            add(new WTopBarButton(tab));
        }
    }

    protected class WTopBarButton extends WPressable {
        private final Tab tab;

        public WTopBarButton(Tab tab) {
            this.tab = tab;
        }

        @Override
        protected void onCalculateSize() {
            double pad = pad();

            width = pad + theme.textWidth(tab.name) + pad;
            height = pad + theme.textHeight() + pad;
        }

        @Override
        protected void onPressed(int button) {
            Screen screen = mc.screen;

            if (!(screen instanceof TabScreen) || ((TabScreen) screen).tab != tab) {
                double mouseX = mc.mouseHandler.xpos();
                double mouseY = mc.mouseHandler.ypos();

                tab.openScreen(theme);
                glfwSetCursorPos(mc.getWindow().handle(), mouseX, mouseY);
            }
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            double pad = pad();
            Color color = getButtonColor(pressed || (mc.screen instanceof TabScreen && ((TabScreen) mc.screen).tab == tab), mouseOver);

            renderer.quad(x, y, width, height, color);
            renderer.text(tab.name, x + pad, y + pad, getNameColor(), false);
        }
    }
}
