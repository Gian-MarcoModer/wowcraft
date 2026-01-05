package com.gianmarco.wowcraft.ability;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.ability.mage.ArcaneExplosion;
import com.gianmarco.wowcraft.ability.mage.ArcaneMissiles;
import com.gianmarco.wowcraft.ability.mage.Blink;
import com.gianmarco.wowcraft.ability.mage.Fireball;
import com.gianmarco.wowcraft.ability.mage.FrostNova;
import com.gianmarco.wowcraft.ability.mage.IceLance;
import com.gianmarco.wowcraft.ability.warrior.BattleShout;
import com.gianmarco.wowcraft.ability.warrior.Charge;
import com.gianmarco.wowcraft.ability.warrior.Execute;
import com.gianmarco.wowcraft.ability.warrior.HeroicStrike;
import com.gianmarco.wowcraft.ability.warrior.ThunderClap;
import com.gianmarco.wowcraft.ability.warrior.Whirlwind;
import com.gianmarco.wowcraft.ability.rogue.Backstab;
import com.gianmarco.wowcraft.ability.rogue.Eviscerate;
import com.gianmarco.wowcraft.ability.rogue.KidneyShot;
import com.gianmarco.wowcraft.ability.rogue.Shadowstep;
import com.gianmarco.wowcraft.ability.rogue.SinisterStrike;
import com.gianmarco.wowcraft.ability.rogue.Sprint;
import com.gianmarco.wowcraft.ability.rogue.Stealth;
import com.gianmarco.wowcraft.playerclass.PlayerClass;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of all abilities, organized by class.
 * Each class has up to 8 ability slots.
 */
public class AbilityRegistry {

    public static final int ACTION_BAR_SIZE = 8;

    // Warrior abilities
    public static final HeroicStrike HEROIC_STRIKE = new HeroicStrike();
    public static final Charge CHARGE = new Charge();
    public static final BattleShout BATTLE_SHOUT = new BattleShout();
    public static final Execute EXECUTE = new Execute();
    public static final Whirlwind WHIRLWIND = new Whirlwind();
    public static final ThunderClap THUNDER_CLAP = new ThunderClap();

    // Mage abilities
    public static final Fireball FIREBALL = new Fireball();
    public static final FrostNova FROST_NOVA = new FrostNova();
    public static final Blink BLINK = new Blink();
    public static final ArcaneMissiles ARCANE_MISSILES = new ArcaneMissiles();
    public static final IceLance ICE_LANCE = new IceLance();
    public static final ArcaneExplosion ARCANE_EXPLOSION = new ArcaneExplosion();

    // Rogue abilities
    public static final SinisterStrike SINISTER_STRIKE = new SinisterStrike();
    public static final Stealth STEALTH = new Stealth();
    public static final Eviscerate EVISCERATE = new Eviscerate();
    public static final Backstab BACKSTAB = new Backstab();
    public static final Shadowstep SHADOWSTEP = new Shadowstep();
    public static final KidneyShot KIDNEY_SHOT = new KidneyShot();
    public static final Sprint SPRINT = new Sprint();

    // All abilities by ID
    private static final Map<String, Ability> ALL_ABILITIES = new HashMap<>();

    // Default action bar layouts per class (8 slots)
    private static final Map<PlayerClass, Ability[]> DEFAULT_ACTION_BARS = new HashMap<>();

    static {
        // Register all abilities by ID
        registerAbility(HEROIC_STRIKE);
        registerAbility(CHARGE);
        registerAbility(BATTLE_SHOUT);
        registerAbility(EXECUTE);
        registerAbility(WHIRLWIND);
        registerAbility(THUNDER_CLAP);
        registerAbility(FIREBALL);
        registerAbility(FROST_NOVA);
        registerAbility(BLINK);
        registerAbility(ARCANE_MISSILES);
        registerAbility(ICE_LANCE);
        registerAbility(ARCANE_EXPLOSION);
        registerAbility(SINISTER_STRIKE);
        registerAbility(STEALTH);
        registerAbility(EVISCERATE);
        registerAbility(BACKSTAB);
        registerAbility(SHADOWSTEP);
        registerAbility(KIDNEY_SHOT);
        registerAbility(SPRINT);

        // Default action bars (8 slots, null = empty slot)
        // Keys: 1, 2, 3, 4, 5, R, F, G
        DEFAULT_ACTION_BARS.put(PlayerClass.WARRIOR, new Ability[] {
                HEROIC_STRIKE, // Key 1
                CHARGE, // Key 2
                BATTLE_SHOUT, // Key 3
                EXECUTE, // Key 4
                WHIRLWIND, // Key 5
                THUNDER_CLAP, // Key R
                null, // Key F (empty)
                null // Key G (empty)
        });

        DEFAULT_ACTION_BARS.put(PlayerClass.MAGE, new Ability[] {
                FIREBALL, // Key 1
                FROST_NOVA, // Key 2
                BLINK, // Key 3
                ICE_LANCE, // Key 4
                ARCANE_MISSILES, // Key 5
                ARCANE_EXPLOSION, // Key R
                null, // Key F (empty)
                null // Key G (empty)
        });

        DEFAULT_ACTION_BARS.put(PlayerClass.ROGUE, new Ability[] {
                SINISTER_STRIKE, // Key 1
                STEALTH, // Key 2
                EVISCERATE, // Key 3
                BACKSTAB, // Key 4
                SHADOWSTEP, // Key 5
                KIDNEY_SHOT, // Key R
                SPRINT, // Key F
                null // Key G (empty)
        });
    }

    private static void registerAbility(Ability ability) {
        ALL_ABILITIES.put(ability.getId(), ability);
    }

    /**
     * Get an ability by its ID
     */
    public static Ability getAbilityById(String id) {
        return ALL_ABILITIES.get(id);
    }

    /**
     * Get the default action bar for a class (8 slots)
     */
    public static Ability[] getDefaultActionBar(PlayerClass playerClass) {
        Ability[] defaults = DEFAULT_ACTION_BARS.get(playerClass);
        if (defaults == null) {
            return new Ability[ACTION_BAR_SIZE];
        }
        // Return a copy to avoid modification
        Ability[] copy = new Ability[ACTION_BAR_SIZE];
        System.arraycopy(defaults, 0, copy, 0, Math.min(defaults.length, ACTION_BAR_SIZE));
        return copy;
    }

    /**
     * Get the abilities for a specific class (for compatibility)
     * 
     * @deprecated Use getDefaultActionBar instead
     */
    @Deprecated
    public static Ability[] getAbilitiesForClass(PlayerClass playerClass) {
        return getDefaultActionBar(playerClass);
    }

    /**
     * Get a specific ability by class and slot (0-7)
     */
    public static Ability getAbility(PlayerClass playerClass, int slot) {
        Ability[] abilities = getDefaultActionBar(playerClass);
        if (slot < 0 || slot >= ACTION_BAR_SIZE) {
            return null;
        }
        return abilities[slot];
    }

    /**
     * Initialize the registry
     */
    public static void register() {
        WowCraft.LOGGER.info("Registered {} total abilities", ALL_ABILITIES.size());
    }
}
