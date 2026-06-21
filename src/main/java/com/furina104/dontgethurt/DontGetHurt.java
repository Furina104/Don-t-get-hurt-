package com.furina104.dontgethurt;

import com.furina104.dontgethurt.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DontGetHurt implements ModInitializer {
    public static final String MOD_ID = "dont_get_hurt";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Random RANDOM = new Random();

    private static ModConfig config;
    private static final ConcurrentHashMap<UUID, Long> cooldownMap = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> isSpawning = ThreadLocal.withInitial(() -> false);

    @Override
    public void onInitialize() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        LOGGER.info("不要受伤 Mod 已加载！");
    }

    public static ModConfig getConfig() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    }

    /**
     * 从启用的选项中随机选择并生成生物
     */
    public static void spawnMobs(ServerWorld world, ServerPlayerEntity player) {
        ModConfig currentConfig = getConfig();

        if (!currentConfig.enabled) {
            return;
        }

        if (isSpawning.get()) {
            return;
        }

        if (currentConfig.cooldownTicks > 0) {
            long currentTime = world.getTime();
            Long lastSpawn = cooldownMap.get(player.getUuid());
            if (lastSpawn != null && currentTime - lastSpawn < currentConfig.cooldownTicks) {
                return;
            }
            cooldownMap.put(player.getUuid(), currentTime);
        }

        List<Runnable> enabledMobs = new ArrayList<>();
        if (currentConfig.enableWither) {
            enabledMobs.add(() -> spawnWither(world, player));
        }
        if (currentConfig.enableEnderDragon) {
            enabledMobs.add(() -> spawnEnderDragon(world, player));
        }
        if (currentConfig.enableWarden) {
            enabledMobs.add(() -> spawnWarden(world, player));
        }
        if (currentConfig.enableZombiesAndSkeletons) {
            enabledMobs.add(() -> spawnZombiesAndSkeletons(world, player));
        }
        if (currentConfig.enableChargedCreepers) {
            enabledMobs.add(() -> spawnChargedCreepers(world, player));
        }
        if (currentConfig.enableHostileIronGolems) {
            enabledMobs.add(() -> spawnHostileIronGolems(world, player));
        }

        if (enabledMobs.isEmpty()) {
            return;
        }

        isSpawning.set(true);
        try {
            int option = RANDOM.nextInt(enabledMobs.size());
            enabledMobs.get(option).run();
        } finally {
            isSpawning.remove();
        }
    }

    private static Vec3d getRandomSpawnPos(ServerPlayerEntity player) {
        ModConfig currentConfig = getConfig();
        double angle = RANDOM.nextDouble() * 2 * Math.PI;
        double minRadius = 2.0;
        double radius = minRadius + RANDOM.nextDouble() * Math.max(0, currentConfig.spawnRadius - minRadius);
        double x = player.getX() + Math.cos(angle) * radius;
        double z = player.getZ() + Math.sin(angle) * radius;
        double y = player.getY();
        return new Vec3d(x, y, z);
    }

    private static void spawnWither(ServerWorld world, ServerPlayerEntity player) {
        Vec3d pos = getRandomSpawnPos(player);
        WitherEntity wither = EntityType.WITHER.create(world, SpawnReason.EVENT);
        if (wither != null) {
            wither.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);
            world.spawnEntityAndPassengers(wither);
        }
    }

    private static void spawnEnderDragon(ServerWorld world, ServerPlayerEntity player) {
        Vec3d pos = getRandomSpawnPos(player);
        EnderDragonEntity dragon = EntityType.ENDER_DRAGON.create(world, SpawnReason.EVENT);
        if (dragon != null) {
            dragon.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);
            world.spawnEntityAndPassengers(dragon);
        }
    }

    private static void spawnWarden(ServerWorld world, ServerPlayerEntity player) {
        Vec3d pos = getRandomSpawnPos(player);
        int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(pos.x), (int) Math.floor(pos.z));
        BlockPos blockPos = new BlockPos((int) Math.floor(pos.x), topY, (int) Math.floor(pos.z));

        WardenEntity warden = EntityType.WARDEN.spawn(world, blockPos, SpawnReason.EVENT);
        if (warden != null) {
            warden.setPersistent();
            warden.increaseAngerAt(player, 150, false);
            warden.setAttacker(player);
            warden.setTarget(player);
            warden.setAiDisabled(false);
        }
    }

    private static void spawnZombiesAndSkeletons(ServerWorld world, ServerPlayerEntity player) {
        ModConfig currentConfig = getConfig();
        for (int i = 0; i < currentConfig.zombieCount; i++) {
            Vec3d pos = getRandomSpawnPos(player);
            int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(pos.x), (int) Math.floor(pos.z));
            ZombieEntity zombie = EntityType.ZOMBIE.create(world, SpawnReason.EVENT);
            if (zombie != null) {
                zombie.refreshPositionAndAngles(pos.x, topY, pos.z, 0, 0);
                zombie.setTarget(player);
                world.spawnEntityAndPassengers(zombie);
            }
        }
        for (int i = 0; i < currentConfig.skeletonCount; i++) {
            Vec3d pos = getRandomSpawnPos(player);
            int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(pos.x), (int) Math.floor(pos.z));
            BlockPos blockPos = new BlockPos((int) Math.floor(pos.x), topY, (int) Math.floor(pos.z));
            SkeletonEntity skeleton = EntityType.SKELETON.spawn(world, blockPos, SpawnReason.EVENT);
            if (skeleton != null) {
                skeleton.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                skeleton.setTarget(player);
            }
        }
    }

    private static void spawnChargedCreepers(ServerWorld world, ServerPlayerEntity player) {
        ModConfig currentConfig = getConfig();
        for (int i = 0; i < currentConfig.chargedCreeperCount; i++) {
            Vec3d pos = getRandomSpawnPos(player);
            int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(pos.x), (int) Math.floor(pos.z));
            CreeperEntity creeper = EntityType.CREEPER.create(world, SpawnReason.EVENT);
            if (creeper != null) {
                creeper.refreshPositionAndAngles(pos.x, topY, pos.z, 0, 0);

                // 设置为闪电苦力怕（充能状态）
                // 使用 Access Widener 直接访问 CHARGED 字段
                creeper.getDataTracker().set(CreeperEntity.CHARGED, true);

                creeper.setTarget(player);
                world.spawnEntityAndPassengers(creeper);
            }
        }
    }

    private static void spawnHostileIronGolems(ServerWorld world, ServerPlayerEntity player) {
        ModConfig currentConfig = getConfig();
        for (int i = 0; i < currentConfig.ironGolemCount; i++) {
            Vec3d pos = getRandomSpawnPos(player);
            int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(pos.x), (int) Math.floor(pos.z));
            IronGolemEntity ironGolem = EntityType.IRON_GOLEM.create(world, SpawnReason.EVENT);
            if (ironGolem != null) {
                ironGolem.refreshPositionAndAngles(pos.x, topY, pos.z, 0, 0);
                ironGolem.setPlayerCreated(false);
                ironGolem.setTarget(player);
                world.spawnEntityAndPassengers(ironGolem);
            }
        }
    }
}
