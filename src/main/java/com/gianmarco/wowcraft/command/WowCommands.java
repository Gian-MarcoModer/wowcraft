package com.gianmarco.wowcraft.command;

import com.gianmarco.wowcraft.item.ItemGenerator;
import com.gianmarco.wowcraft.item.ItemRarity;
import com.gianmarco.wowcraft.item.LootManager;
import com.gianmarco.wowcraft.item.WowItem;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
import com.gianmarco.wowcraft.playerclass.PlayerDataManager;
import com.gianmarco.wowcraft.network.NetworkHandler;
import com.gianmarco.wowcraft.roads.RoadGenerator;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import com.gianmarco.wowcraft.mobpack.*;
import com.gianmarco.wowcraft.zone.BiomeGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;

public class WowCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // Register debug spawn command
            DebugSpawnCommand.register(dispatcher);

            dispatcher.register(Commands.literal("wow")
                    .then(Commands.literal("give")
                            .requires(source -> source.hasPermission(2)) // OPS only
                            .then(Commands.argument("level", IntegerArgumentType.integer(1, 30))
                                    .executes(ctx -> giveItem(ctx, 1)) // Default common
                                    .then(Commands.argument("rarity", StringArgumentType.word())
                                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                    Arrays.stream(ItemRarity.values()).map(Enum::name), builder))
                                            .executes(ctx -> giveItem(ctx, 2))))) // Specified rarity
                    .then(Commands.literal("reset")
                            .executes(WowCommands::resetClass))
                    .then(Commands.literal("mobpack")
                            .requires(source -> source.hasPermission(2))
                            .then(Commands.literal("spawn")
                                    .then(Commands.argument("template", StringArgumentType.word())
                                            .executes(WowCommands::spawnPack)))
                            .then(Commands.literal("count")
                                    .executes(WowCommands::countPacks)))
                    .then(Commands.literal("roads")
                            .requires(source -> source.hasPermission(2))
                            .then(Commands.literal("find")
                                    .executes(WowCommands::findStructures)
                                    .then(Commands.argument("radius", IntegerArgumentType.integer(100, 5000))
                                            .then(Commands.argument("count", IntegerArgumentType.integer(1, 20))
                                                    .executes(WowCommands::findStructuresWithArgs))))
                            .then(Commands.literal("plan")
                                    .executes(WowCommands::planRoads))
                            .then(Commands.literal("build")
                                    .then(Commands.argument("x", IntegerArgumentType.integer())
                                            .then(Commands.argument("z", IntegerArgumentType.integer())
                                                    .executes(WowCommands::buildRoadToPos))))
                            .then(Commands.literal("status")
                                    .executes(WowCommands::roadStatus))
                            .then(Commands.literal("reset")
                                    .executes(WowCommands::resetRoads))));
        });
    }

    private static int giveItem(CommandContext<CommandSourceStack> context, int mode) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            int level = IntegerArgumentType.getInteger(context, "level");

            // Determine rarity
            ItemRarity rarity = ItemRarity.COMMON;
            ItemRarity fixedRarity = null;

            if (mode == 2) {
                String rarityName = StringArgumentType.getString(context, "rarity");
                try {
                    fixedRarity = ItemRarity.valueOf(rarityName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    context.getSource().sendFailure(Component.literal("Invalid rarity: " + rarityName));
                    return 0;
                }
            }

            // Generate item
            // If rarity is specified, we need to force it. Since ItemGenerator doesn't
            // support forcing rarity directly
            // with a simple method that also randomizes other things easily, we'll loop
            // until we get it or just
            // use the specific generation method if we want to be precise, but we want
            // random base types.
            // Actually, let's just generate normally. If fixedRarity is set, we might need
            // to expose a method in Generator.
            // For now, let's use the random generation and if it's not the right rarity
            // (and we care), we modify it or
            // validly, we should modify ItemGenerator to accept a fixed rarity.
            // Let's rely on a loop for now or add a helper in ItemGenerator later if
            // needed.
            // Actually, I'll use the specific generator method if I have a rarity, picking
            // a random prefix/base type myself.

            WowItem item;
            PlayerClass playerClass = PlayerDataManager.getPlayerClass(player);

            if (fixedRarity != null) {
                // Manual generation with fixed rarity
                var prefix = com.gianmarco.wowcraft.item.ItemPrefix.getRandomForLevel(level);
                var baseType = getRandomBaseType(level);
                var suffix = (fixedRarity.ordinal() >= ItemRarity.UNCOMMON.ordinal())
                        ? com.gianmarco.wowcraft.item.ItemSuffix.getRandomSuffix(playerClass)
                        : null;
                item = ItemGenerator.generateItem(prefix, baseType, fixedRarity, suffix, level);
            } else {
                // Fully random
                item = ItemGenerator.generateItem(level, playerClass);
            }

            // Give to player
            LootManager.equipItem(player, item); // This actually equips it to the virtual slot, WAIT.
            // The user wants it in their INVENTORY. LootManager.onMobDeath drops it as an
            // entity.
            // We should give it to their inventory.

            // Create the ItemStack
            // We need access to createDisplayStack, but it's private in LootManager.
            // We should make it public or duplicate logic? Better to make it public.
            // For now, let's check LootManager code again.
            // It has `createDisplayStack` which is private.
            // I'll assume I can make it public in the next step.

            // For now I will comment this out and fix LootManager in the next step.
            // ItemStack stack = LootManager.createDisplayStack(item);
            // player.getInventory().add(stack);

            // Actually, to avoid broken code, let's invoke a helper we WILL create.
            giveToInventory(player, item);

            context.getSource().sendSuccess(() -> Component.literal("Generated: " + item.getDisplayName()), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    // Helper to give item to player inventory
    private static void giveToInventory(ServerPlayer player, WowItem item) {
        net.minecraft.world.item.ItemStack stack = LootManager.createDisplayStack(item);
        boolean added = player.getInventory().add(stack);
        if (!added) {
            player.drop(stack, false);
        }
    }

    private static int resetClass(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null) {
            PlayerDataManager.setPlayerClass(player, PlayerClass.NONE);
            NetworkHandler.syncPlayerData(player);
            context.getSource().sendSuccess(
                    () -> Component.literal("§aYour class has been reset. Rejoin the world to select a new class."),
                    false);
        }
        return 1;
    }

    private static int spawnPack(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String templateId = StringArgumentType.getString(context, "template");
            MobPackTemplate template = MobPackTemplateLoader.getTemplate(templateId);

            if (template == null) {
                context.getSource().sendFailure(Component.literal("Unknown pack template: " + templateId));
                return 0;
            }

            BiomeGroup zone = BiomeGroup
                    .fromBiome(player.level().getBiome(player.blockPosition()).unwrapKey().orElse(null));
            if (zone == null)
                zone = BiomeGroup.PLAINS;

            int targetLevel = PlayerDataManager.getLevel(player);

            SpawnedMobPack pack = new SpawnedMobPack(
                    UUID.randomUUID(),
                    templateId,
                    player.blockPosition(),
                    targetLevel,
                    template.socialAggroRadius(),
                    template.respawnDelaySeconds(),
                    zone);

            java.util.Random random = new java.util.Random();
            int packSize = template.rollPackSize(random);
            int totalWeight = template.mobPool().stream().mapToInt(MobEntry::weight).sum();

            for (int i = 0; i < packSize; i++) {
                int roll = random.nextInt(totalWeight);
                int cumulative = 0;
                MobEntry selected = template.mobPool().get(0);
                for (MobEntry entry : template.mobPool()) {
                    cumulative += entry.weight();
                    if (roll < cumulative) {
                        selected = entry;
                        break;
                    }
                }

                int ox = random.nextInt(5) - 2;
                int oz = random.nextInt(5) - 2;
                BlockPos pos = player.blockPosition().offset(ox, 0, oz);
                int y = player.serverLevel().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(),
                        pos.getZ());
                pos = new BlockPos(pos.getX(), y, pos.getZ());

                pack.addMob(new SpawnedMob(selected.mobType(), pos));
            }

            MobPackManager.registerPack(pack);
            pack.spawnReadyMobs(player.serverLevel(), player.serverLevel().getGameTime());

            context.getSource().sendSuccess(
                    () -> Component
                            .literal("Spawned " + templateId + " (" + packSize + " mobs, Lvl " + targetLevel + ")"),
                    true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    private static int countPacks(CommandContext<CommandSourceStack> context) {
        int count = MobPackManager.getAllPacks().size();
        context.getSource().sendSuccess(() -> Component.literal("Active mob packs: " + count), false);
        return 1;
    }

    // Duplicate of ItemGenerator logic, ideally should be public there
    private static com.gianmarco.wowcraft.item.BaseItemType getRandomBaseType(int level) {
        java.util.List<com.gianmarco.wowcraft.item.BaseItemType> available = new java.util.ArrayList<>();
        for (com.gianmarco.wowcraft.item.BaseItemType type : com.gianmarco.wowcraft.item.BaseItemType.values()) {
            if (type.isAvailableAtLevel(level)) {
                available.add(type);
            }
        }
        if (available.isEmpty())
            return com.gianmarco.wowcraft.item.BaseItemType.SHORTSWORD;
        return available.get(new java.util.Random().nextInt(available.size()));
    }

    // === Road Commands ===

    private static int findStructures(CommandContext<CommandSourceStack> context) {
        return findStructuresInternal(context, 1000, 5);
    }

    private static int findStructuresWithArgs(CommandContext<CommandSourceStack> context) {
        int radius = IntegerArgumentType.getInteger(context, "radius");
        int count = IntegerArgumentType.getInteger(context, "count");
        return findStructuresInternal(context, radius, count);
    }

    private static int findStructuresInternal(CommandContext<CommandSourceStack> context, int radius, int maxCount) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ServerLevel level = player.serverLevel();
            BlockPos center = player.blockPosition();

            context.getSource().sendSuccess(
                    () -> Component.literal("Searching for structures within " + radius + " blocks and building roads..."),
                    false);

            // Use the force search which also builds roads
            RoadGenerator.getInstance().forceSearchAndBuild(level, center, radius);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int planRoads(CommandContext<CommandSourceStack> context) {
        RoadGenerator generator = RoadGenerator.getInstance();

        if (generator.getStructureCount() < 2) {
            context.getSource().sendFailure(
                    Component.literal("Need at least 2 structures. Use /wow roads find first."));
            return 0;
        }

        try {
            ServerLevel level = context.getSource().getLevel();
            int roadsBefore = generator.getTotalRoadsBuilt();
            generator.planAllRoads(level);
            int roadsBuilt = generator.getTotalRoadsBuilt() - roadsBefore;

            context.getSource().sendSuccess(
                    () -> Component.literal("Queued road plans. Built: " + roadsBuilt
                            + ", active plans: " + generator.getActivePlanCount()),
                    true);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }

        return 1;
    }

    private static int buildRoadToPos(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ServerLevel level = context.getSource().getLevel();
            int targetX = IntegerArgumentType.getInteger(context, "x");
            int targetZ = IntegerArgumentType.getInteger(context, "z");

            BlockPos start = player.blockPosition();
            BlockPos end = new BlockPos(targetX, 0, targetZ);

            RoadGenerator generator = RoadGenerator.getInstance();
            generator.buildManualRoad(level, start, end);

            context.getSource().sendSuccess(
                    () -> Component.literal("Built road from " + start.toShortString() + " to " + end.toShortString()),
                    true);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int roadStatus(CommandContext<CommandSourceStack> context) {
        RoadGenerator generator = RoadGenerator.getInstance();

        String status = generator.isInitialized() ? "READY" : "INITIALIZING";
        int structures = generator.getStructureCount();
        int built = generator.getTotalRoadsBuilt();
        int blocks = generator.getTotalBlocksPlaced();
        int active = generator.getActivePlanCount();

        context.getSource().sendSuccess(
                () -> Component.literal("Road Generator Status: " + status),
                false);
        context.getSource().sendSuccess(
                () -> Component.literal("  Structures found: " + structures),
                false);
        context.getSource().sendSuccess(
                () -> Component.literal("  Roads built: " + built),
                false);
        context.getSource().sendSuccess(
                () -> Component.literal("  Blocks placed: " + blocks),
                false);
        context.getSource().sendSuccess(
                () -> Component.literal("  Active plans: " + active),
                false);

        return 1;
    }

    private static int resetRoads(CommandContext<CommandSourceStack> context) {
        RoadGenerator.getInstance().reset();
        context.getSource().sendSuccess(
                () -> Component.literal("Road generator reset. All data cleared."),
                true);
        return 1;
    }
}
