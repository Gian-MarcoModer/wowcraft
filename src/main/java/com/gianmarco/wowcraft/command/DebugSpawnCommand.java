package com.gianmarco.wowcraft.command;

import com.gianmarco.wowcraft.spawn.POIDebugVisualizer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * Command to toggle POI debug visualization.
 * Usage: /debugspawn [on|off]
 */
public class DebugSpawnCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("debugspawn")
            .requires(source -> source.hasPermission(2)) // Require OP
            .executes(DebugSpawnCommand::toggle)
            .then(Commands.literal("on")
                .executes(DebugSpawnCommand::enable))
            .then(Commands.literal("off")
                .executes(DebugSpawnCommand::disable))
        );
    }

    /**
     * Toggle debug mode.
     */
    private static int toggle(CommandContext<CommandSourceStack> context) {
        boolean current = POIDebugVisualizer.isDebugEnabled();
        POIDebugVisualizer.setDebugEnabled(!current);

        String status = !current ? "enabled" : "disabled";
        context.getSource().sendSuccess(
            () -> Component.literal("§aSpawn debug visualization " + status + "!"),
            true
        );

        return 1;
    }

    /**
     * Enable debug mode.
     */
    private static int enable(CommandContext<CommandSourceStack> context) {
        POIDebugVisualizer.setDebugEnabled(true);

        context.getSource().sendSuccess(
            () -> Component.literal("§aSpawn debug visualization enabled!\n" +
                "§7POI markers will appear in newly loaded regions.\n" +
                "§7Legend:\n" +
                "§c⚔ CAMP §7- Hostile camp (3-5 mobs)\n" +
                "§9☠ LAIR §7- Boss with guards (+levels)\n" +
                "§a🦌 WILDLIFE §7- Neutral animals\n" +
                "§e⛏ RESOURCE §7- Territorial guards\n" +
                "§5👁 PATROL §7- Moving patrol (TODO)"),
            false
        );

        return 1;
    }

    /**
     * Disable debug mode.
     */
    private static int disable(CommandContext<CommandSourceStack> context) {
        POIDebugVisualizer.setDebugEnabled(false);

        ServerLevel level = context.getSource().getLevel();
        POIDebugVisualizer.clearMarkers(level);

        context.getSource().sendSuccess(
            () -> Component.literal("§cSpawn debug visualization disabled!"),
            true
        );

        return 1;
    }
}
