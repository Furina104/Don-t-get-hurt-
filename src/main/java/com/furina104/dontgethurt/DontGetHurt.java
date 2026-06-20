package com.furina104.dontgethurt;

import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Random;

public class DontGetHurt implements ModInitializer {
    public static final String MOD_ID = "dont_get_hurt";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Random RANDOM = new Random();
    private static final double SPAWN_RADIUS = 5.0;
    private static TrackedData<Boolean> CREEPER_CHARGED_DATA;

    static {
        try {
            Field field = CreeperEntity.class.getDeclaredField("CHARGED");
            field.setAccessible(true);
            CREEPER_CHARGED_DATA = (TrackedData<Boolean>) field.get(null);
        } catch (Exception e) {
            LOGGER.error("Failed to get CreeperEntity.CHARGED field", e);
        }
    }

    @Override
    public void onInitialize() {
        LOGGER.info("不要受伤 Mod 已加载！");
    }

    /**
     * 从 6 个选项中等概率随机选择并生成生物
     */
    public static void spawnMobs(ServerPlayerEntity player) {
        int option = RANDOM.nextInt(6);
        ServerWorld world = (ServerWorld) player.world;
        switch (option) {
            case 0 -> spawnWither(world, player);
            case 1 -> spawnEnderDragon(world, player);
            case 2 -> spawnWarden(world, player);
            case 3 -> spawnZombiesAndSkeletons(world, player);
            case 4 -> spawnChargedCreepers(world, player);
            case 5 -> spawnHostileIronGolems(world, player);
        }
    }

    /**
     * 在玩家附近半径 5 格内随机位置生成
     */
    private static Vec3d getRandomSpawnPos(ServerPlayerEntity player) {
        double angle = RANDOM.nextDouble() * 2 * Math.PI;
        double radius = RANDOM.nextDouble() * SPAWN_RADIUS;
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
        WardenEntity warden = EntityType.WARDEN.create(world, SpawnReason.EVENT);
        if (warden != null) {
            warden.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);
            // 让寻声守卫直接感知到玩家
            warden.setAttacker(player);
            warden.setTarget(player);
            world.spawnEntityAndPassengers(warden);
        }
    }

    private static void spawnZombiesAndSkeletons(ServerWorld world, ServerPlayerEntity player) {
        // 生成 10 只僵尸
        for (int i = 0; i < 10; i++) {
            Vec3d pos = getRandomSpawnPos(player);
            ZombieEntity zombie = EntityType.ZOMBIE.create(world, SpawnReason.EVENT);
            if (zombie != null) {
                zombie.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);
                zombie.setTarget(player);
                world.spawnEntityAndPassengers(zombie);
            }
        }
        // 生成 10 只骷髅
        for (int i = 0; i < 10; i++) {
            Vec3d pos = getRandomSpawnPos(player);
            SkeletonEntity skeleton = EntityType.SKELETON.create(world, SpawnReason.EVENT);
            if (skeleton != null) {
                skeleton.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);
                skeleton.setTarget(player);
                world.spawnEntityAndPassengers(skeleton);
            }
        }
    }

    private static void spawnChargedCreepers(ServerWorld world, ServerPlayerEntity player) {
        for (int i = 0; i < 10; i++) {
            Vec3d pos = getRandomSpawnPos(player);
            CreeperEntity creeper = EntityType.CREEPER.create(world, SpawnReason.EVENT);
            if (creeper != null) {
                creeper.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);
                // 设置为闪电苦力怕（充能状态）
                if (CREEPER_CHARGED_DATA != null) {
                    creeper.getDataTracker().set(CREEPER_CHARGED_DATA, true);
                }
                creeper.setTarget(player);
                world.spawnEntityAndPassengers(creeper);
            }
        }
    }

    private static void spawnHostileIronGolems(ServerWorld world, ServerPlayerEntity player) {
        for (int i = 0; i < 10; i++) {
            Vec3d pos = getRandomSpawnPos(player);
            IronGolemEntity ironGolem = EntityType.IRON_GOLEM.create(world, SpawnReason.EVENT);
            if (ironGolem != null) {
                ironGolem.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);
                // 让铁傀儡对玩家产生敌意并攻击玩家
                ironGolem.setPlayerCreated(false);
                ironGolem.setTarget(player);
                ironGolem.tryAttack(world, player);
                world.spawnEntityAndPassengers(ironGolem);
            }
        }
    }
}
