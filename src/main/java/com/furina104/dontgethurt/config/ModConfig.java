package com.furina104.dontgethurt.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "dont_get_hurt")
public class ModConfig implements ConfigData {

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean enabled = true;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 50)
    public int spawnRadius = 5;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 6000)
    public int cooldownTicks = 0;

    @ConfigEntry.Category("mobs")
    @ConfigEntry.Gui.Tooltip
    public boolean enableWither = true;

    @ConfigEntry.Category("mobs")
    @ConfigEntry.Gui.Tooltip
    public boolean enableEnderDragon = true;

    @ConfigEntry.Category("mobs")
    @ConfigEntry.Gui.Tooltip
    public boolean enableWarden = true;

    @ConfigEntry.Category("mobs")
    @ConfigEntry.Gui.Tooltip
    public boolean enableZombiesAndSkeletons = true;

    @ConfigEntry.Category("mobs")
    @ConfigEntry.Gui.Tooltip
    public boolean enableChargedCreepers = true;

    @ConfigEntry.Category("mobs")
    @ConfigEntry.Gui.Tooltip
    public boolean enableHostileIronGolems = true;

    @ConfigEntry.Category("amounts")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int zombieCount = 10;

    @ConfigEntry.Category("amounts")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int skeletonCount = 10;

    @ConfigEntry.Category("amounts")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int chargedCreeperCount = 10;

    @ConfigEntry.Category("amounts")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int ironGolemCount = 10;
}
