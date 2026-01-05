package com.gianmarco.wowcraft.economy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Represents a player's money pouch containing copper, silver, and gold coins.
 * 
 * Conversion rates:
 * - 100 copper = 1 silver
 * - 100 silver = 1 gold
 */
public record MoneyPouch(int copper, int silver, int gold) {

    public static final MoneyPouch EMPTY = new MoneyPouch(0, 0, 0);

    public static final int COPPER_PER_SILVER = 100;
    public static final int SILVER_PER_GOLD = 100;
    public static final int COPPER_PER_GOLD = COPPER_PER_SILVER * SILVER_PER_GOLD;

    // Codec for serialization/deserialization
    public static final Codec<MoneyPouch> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("copper").forGetter(MoneyPouch::copper),
            Codec.INT.fieldOf("silver").forGetter(MoneyPouch::silver),
            Codec.INT.fieldOf("gold").forGetter(MoneyPouch::gold)).apply(instance, MoneyPouch::new));

    /**
     * Get total value in copper
     */
    public long getTotalCopper() {
        return (long) copper + ((long) silver * COPPER_PER_SILVER) + ((long) gold * COPPER_PER_GOLD);
    }

    /**
     * Add copper coins, automatically converting to higher denominations
     */
    public MoneyPouch addCopper(int amount) {
        long totalCopper = getTotalCopper() + amount;
        return fromTotalCopper(totalCopper);
    }

    /**
     * Add silver coins, automatically converting to higher denominations
     */
    public MoneyPouch addSilver(int amount) {
        return addCopper(amount * COPPER_PER_SILVER);
    }

    /**
     * Add gold coins
     */
    public MoneyPouch addGold(int amount) {
        return addCopper(amount * COPPER_PER_GOLD);
    }

    /**
     * Add another money pouch to this one
     */
    public MoneyPouch add(MoneyPouch other) {
        long totalCopper = getTotalCopper() + other.getTotalCopper();
        return fromTotalCopper(totalCopper);
    }

    /**
     * Remove copper coins. Returns null if not enough funds.
     */
    public MoneyPouch removeCopper(int amount) {
        long totalCopper = getTotalCopper() - amount;
        if (totalCopper < 0) {
            return null; // Not enough funds
        }
        return fromTotalCopper(totalCopper);
    }

    /**
     * Remove silver coins. Returns null if not enough funds.
     */
    public MoneyPouch removeSilver(int amount) {
        return removeCopper(amount * COPPER_PER_SILVER);
    }

    /**
     * Remove gold coins. Returns null if not enough funds.
     */
    public MoneyPouch removeGold(int amount) {
        return removeCopper(amount * COPPER_PER_GOLD);
    }

    /**
     * Check if we have at least this much copper
     */
    public boolean hasCopper(int amount) {
        return getTotalCopper() >= amount;
    }

    /**
     * Check if we have at least this much in total value (expressed in copper)
     */
    public boolean hasFunds(int copperAmount) {
        return getTotalCopper() >= copperAmount;
    }

    /**
     * Create a MoneyPouch from a total copper amount, normalizing to
     * gold/silver/copper
     */
    public static MoneyPouch fromTotalCopper(long totalCopper) {
        if (totalCopper <= 0) {
            return EMPTY;
        }

        int gold = (int) (totalCopper / COPPER_PER_GOLD);
        totalCopper %= COPPER_PER_GOLD;

        int silver = (int) (totalCopper / COPPER_PER_SILVER);
        totalCopper %= COPPER_PER_SILVER;

        int copper = (int) totalCopper;

        return new MoneyPouch(copper, silver, gold);
    }

    /**
     * Format as a readable string (e.g., "1g 50s 25c")
     */
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        boolean hasContent = false;

        if (gold > 0) {
            sb.append(gold).append("g");
            hasContent = true;
        }
        if (silver > 0 || hasContent) {
            if (hasContent)
                sb.append(" ");
            sb.append(silver).append("s");
            hasContent = true;
        }
        if (hasContent)
            sb.append(" ");
        sb.append(copper).append("c");

        return sb.toString();
    }

    /**
     * Format as colored string for display in chat
     */
    public String toColoredString() {
        StringBuilder sb = new StringBuilder();

        if (gold > 0) {
            sb.append("§6").append(gold).append("g§r ");
        }
        if (silver > 0 || gold > 0) {
            sb.append("§7").append(silver).append("s§r ");
        }
        sb.append("§#c87533").append(copper).append("c§r");

        return sb.toString();
    }

    @Override
    public String toString() {
        return toDisplayString();
    }
}
