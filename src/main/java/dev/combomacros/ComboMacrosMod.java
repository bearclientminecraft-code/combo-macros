package dev.combomacros;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import dev.combomacros.MacroRunner.Step;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ComboMacrosMod implements ClientModInitializer {
	// Макро 1 (Z): слот 4 -> ПКМ -> слот 9
	private static final List<Step> MACRO_1 = List.of(Step.slot(4), Step.rmb(), Step.slot(9));
	// Макро 2 (X): слот 5 -> ПКМ -> ЛКМ -> ПКМ
	private static final List<Step> MACRO_2 = List.of(Step.slot(5), Step.rmb(), Step.lmb(), Step.rmb());
	// Макро 3 (R): слот 6 -> ПКМ -> слот 7 -> ПКМ -> слот 9 -> ПКМ
	private static final List<Step> MACRO_3 = List.of(
			Step.slot(6), Step.rmb(), Step.slot(7), Step.rmb(), Step.slot(9), Step.rmb());

	private static KeyMapping key1;
	private static KeyMapping key2;
	private static KeyMapping key3;
	private static KeyMapping keyToggle;

	@Override
	public void onInitializeClient() {
		MacroConfig.load();

		key1 = register("key.combomacros.macro1", GLFW.GLFW_KEY_Z);
		key2 = register("key.combomacros.macro2", GLFW.GLFW_KEY_X);
		key3 = register("key.combomacros.macro3", GLFW.GLFW_KEY_R);
		keyToggle = register("key.combomacros.toggle", GLFW.GLFW_KEY_F10);

		ClientTickEvents.START_CLIENT_TICK.register(ComboMacrosMod::onTick);
		registerCommands();
	}

	private static KeyMapping register(String id, int keyCode) {
		return KeyBindingHelper.registerKeyBinding(
				new KeyMapping(id, InputConstants.Type.KEYSYM, keyCode, KeyMapping.Category.MISC));
	}

	private static void onTick(Minecraft mc) {
		while (keyToggle.consumeClick()) {
			toggle(mc);
		}

		boolean canRun = MacroConfig.INSTANCE.enabled && mc.player != null && mc.screen == null;
		if (canRun) {
			while (key1.consumeClick()) {
				MacroRunner.start(MACRO_1);
			}
			while (key2.consumeClick()) {
				MacroRunner.start(MACRO_2);
			}
			while (key3.consumeClick()) {
				MacroRunner.start(MACRO_3);
			}
		} else {
			// сбрасываем накопившиеся нажатия
			while (key1.consumeClick()) {
			}
			while (key2.consumeClick()) {
			}
			while (key3.consumeClick()) {
			}
		}

		MacroRunner.tick(mc);
	}

	private static void toggle(Minecraft mc) {
		MacroConfig c = MacroConfig.INSTANCE;
		c.enabled = !c.enabled;
		MacroConfig.save();
		if (!c.enabled) {
			MacroRunner.clear();
		}
		if (mc.player != null) {
			mc.player.displayClientMessage(
					Component.literal(c.enabled ? "\u00a7aMacros ON" : "\u00a7cMacros OFF"), true);
		}
	}

	private static void registerCommands() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("macro")
						.then(ClientCommandManager.literal("toggle").executes(ctx -> {
							toggle(Minecraft.getInstance());
							return 1;
						}))
						.then(ClientCommandManager.literal("delay")
								.then(ClientCommandManager.argument("ms", IntegerArgumentType.integer(0, 2000))
										.executes(ctx -> {
											int ms = IntegerArgumentType.getInteger(ctx, "ms");
											MacroConfig.INSTANCE.delayMs = ms;
											MacroConfig.save();
											ctx.getSource().sendFeedback(
													Component.literal("\u00a7aDelay = " + ms + " ms"));
											return 1;
										})))
						.executes(ctx -> {
							MacroConfig c = MacroConfig.INSTANCE;
							ctx.getSource().sendFeedback(Component.literal(
									"\u00a7eMacros " + (c.enabled ? "ON" : "OFF") + ", delay " + c.delayMs
											+ " ms. /macro toggle | /macro delay <ms>"));
							return 1;
						})));
	}
}
