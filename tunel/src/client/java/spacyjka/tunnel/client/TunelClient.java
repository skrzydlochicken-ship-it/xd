/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacyjka.tunnel.client.eventbus.EventBus;
import spacyjka.tunnel.client.eventbus.EventHandler;
import spacyjka.tunnel.client.eventbus.EventPriority;
import spacyjka.tunnel.client.eventbus.IEventBus;
import spacyjka.tunnel.client.events.game.OpenScreenEvent;
import spacyjka.tunnel.client.events.tunel.KeyEvent;
import spacyjka.tunnel.client.events.tunel.MouseClickEvent;
import spacyjka.tunnel.client.gui.GuiThemes;
import spacyjka.tunnel.client.gui.WidgetScreen;
import spacyjka.tunnel.client.gui.renderer.GuiRenderer;
import spacyjka.tunnel.client.gui.tabs.Tabs;
import spacyjka.tunnel.client.renderer.Fonts;
import spacyjka.tunnel.client.renderer.FullScreenRenderer;
import spacyjka.tunnel.client.renderer.Renderer2D;
import spacyjka.tunnel.client.systems.Systems;
import spacyjka.tunnel.client.systems.modules.Categories;
import spacyjka.tunnel.client.utils.Utils;
import spacyjka.tunnel.client.utils.misc.input.KeyAction;
import spacyjka.tunnel.client.utils.misc.input.KeyBinds;
import spacyjka.tunnel.client.utils.network.TunelExecutor;
import spacyjka.tunnel.client.utils.render.RenderUtils;
import spacyjka.tunnel.client.utils.render.color.RainbowColors;

import java.io.File;
import java.lang.invoke.MethodHandles;

public class TunelClient implements ClientModInitializer {
    public static final String MOD_ID = "tunel";
    public static final String NAME = "Tunel";

    public static TunelClient INSTANCE;

    public static Minecraft mc;
    public static final IEventBus EVENT_BUS = new EventBus();
    public static final File FOLDER = FabricLoader.getInstance().getGameDir().resolve(MOD_ID).toFile();
    public static final Logger LOG = LoggerFactory.getLogger(NAME);

    @Override
    public void onInitializeClient() {
        // Fabric calls this before the Minecraft instance exists; the real init runs from MinecraftMixin once it does.
        if (INSTANCE == null) {
            INSTANCE = this;
            KeyBinds.register();
            return;
        }

        mc = Minecraft.getInstance();

        LOG.info("Initializing {}", NAME);

        if (!FOLDER.exists()) {
            FOLDER.getParentFile().mkdirs();
            FOLDER.mkdir();
        }

        EVENT_BUS.registerLambdaFactory("spacyjka.tunnel.client", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));

        // Pre init
        Utils.init();
        TunelExecutor.init();
        Fonts.refresh();
        Renderer2D.init();
        FullScreenRenderer.init();
        GuiThemes.init();
        Tabs.init();

        Categories.init();
        Systems.init();

        EVENT_BUS.subscribe(this);

        Systems.load();

        // Post init
        GuiRenderer.init();
        GuiThemes.postInit();
        RainbowColors.init();
        RenderUtils.init();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Systems.save();
            GuiThemes.save();
        }));
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (event.action == KeyAction.Press && KeyBinds.OPEN_GUI.matches(event.input)) {
            toggleGui();
        }
    }

    @EventHandler
    private void onMouseClick(MouseClickEvent event) {
        if (event.action == KeyAction.Press && KeyBinds.OPEN_GUI.matchesMouse(event.click)) {
            toggleGui();
        }
    }

    private void toggleGui() {
        if (Utils.canCloseGui()) mc.screen.onClose();
        else if (Utils.canOpenGui()) Tabs.get().getFirst().openScreen(GuiThemes.get());
    }

    // Hide HUD

    private boolean wasWidgetScreen, wasHudHiddenRoot;

    @EventHandler(priority = EventPriority.LOWEST)
    private void onOpenScreen(OpenScreenEvent event) {
        if (event.screen instanceof WidgetScreen) {
            if (!wasWidgetScreen) wasHudHiddenRoot = mc.options.hideGui;
            if (GuiThemes.get().hideHUD() || wasHudHiddenRoot) mc.options.hideGui = true;
        } else {
            if (wasWidgetScreen) mc.options.hideGui = wasHudHiddenRoot;
            wasHudHiddenRoot = mc.options.hideGui;
        }

        wasWidgetScreen = event.screen instanceof WidgetScreen;
    }

    public static Identifier identifier(String path) {
        return Identifier.fromNamespaceAndPath(TunelClient.MOD_ID, path);
    }
}
