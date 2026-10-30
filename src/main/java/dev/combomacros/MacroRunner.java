package dev.combomacros;

import java.util.ArrayDeque;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Выполняет последовательность шагов с задержкой между ними.
 * Шаги, попадающие в одно окно тика (50 мс), выполняются вместе.
 */
public final class MacroRunner {
	public enum Type {
		SLOT, RMB, LMB
	}

	public record Step(Type type, int arg) {
		public static Step slot(int n) {
			return new Step(Type.SLOT, n);
		}

		public static Step rmb() {
			return new Step(Type.RMB, 0);
		}

		public static Step lmb() {
			return new Step(Type.LMB, 0);
		}
	}

	private static final ArrayDeque<Step> QUEUE = new ArrayDeque<>();
	private static long nextDueNs = 0L;

	private MacroRunner() {
	}

	public static void start(List<Step> steps) {
		if (!QUEUE.isEmpty()) {
			return; // макро уже идёт
		}
		QUEUE.addAll(steps);
		nextDueNs = System.nanoTime();
	}

	public static void clear() {
		QUEUE.clear();
	}

	public static void tick(Minecraft mc) {
		if (QUEUE.isEmpty()) {
			return;
		}
		if (mc.player == null || mc.gameMode == null) {
			QUEUE.clear();
			return;
		}

		long windowEnd = System.nanoTime() + 49_000_000L;
		long delayNs = Math.max(0, MacroConfig.INSTANCE.delayMs) * 1_000_000L;

		while (!QUEUE.isEmpty() && nextDueNs <= windowEnd) {
			run(mc, QUEUE.poll());
			nextDueNs += delayNs;
		}
	}

	private static void run(Minecraft mc, Step step) {
		switch (step.type()) {
			case SLOT -> mc.player.getInventory().setSelectedSlot(step.arg() - 1);
			case RMB -> rightClick(mc);
			case LMB -> leftClick(mc);
		}
	}

	private static void rightClick(Minecraft mc) {
		if (mc.hitResult instanceof BlockHitResult blockHit && blockHit.getType() != HitResult.Type.MISS) {
			var result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHit);
			if (result.consumesAction()) {
				mc.player.swing(InteractionHand.MAIN_HAND);
				return;
			}
		}
		mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
		mc.player.swing(InteractionHand.MAIN_HAND);
	}

	private static void leftClick(Minecraft mc) {
		if (mc.hitResult instanceof EntityHitResult entityHit) {
			mc.gameMode.attack(mc.player, entityHit.getEntity());
			mc.player.resetAttackStrengthTicker();
		}
		mc.player.swing(InteractionHand.MAIN_HAND);
	}
    }
