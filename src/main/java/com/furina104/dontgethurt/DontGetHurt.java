package com.furina104.dontgethurt;

import com.furina104.dontgethurt.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DontGetHurt implements ModInitializer {
    public static final String MOD_ID = "dont_get_hurt";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Random RANDOM = new Random();
    private static TrackedData<Boolean> CREEPER_CHARGED_DATA;

    private static ModConfig config;
    private static final ConcurrentHashMap<UUID, Long> cooldownMap = new ConcurrentHashMap<>();

    static {
        try {
            Field chargedField = CreeperEntity.class.getDeclaredField("CHARGED");
            chargedField.setAccessible(true);
            CREEPER_CHARGED_DATA = (TrackedData<Boolean>) chargedField.get(null);
        } catch (Exception e) {
            LOGGER.error("Failed to get CreeperEntity.CHARGED field", e);
        }
    }

    @Override
    public void onInitialize() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        LOGGER.info("不要受伤 Mod 已加载！");
    }

    public static ModConfig getConfig() {
        return config;
    }

    /**
     * 从启用的选项中随机选择并生成生物
     */
    public static void spawnMobs(ServerWorld world, ServerPlayerEntity player) {
        if (!config.enabled) {
            return;
        }

        // 检查冷却时间
        if (config.cooldownTicks > 0) {
            long currentTime = world.getTime();
            Long lastSpawn = cooldownMap.get(player.getUuid());
            if (lastSpawn != null && currentTime - lastSpawn < config.cooldownTicks) {
                return;
            }
            cooldownMap.put(player.getUuid(), currentTime);
        }

        // 收集启用的生物类型
        List<Runnable> enabledMobs = new ArrayList<>();
        if (config.enableWither) {
            enabledMobs.add(() -> spawnWither(world, player));
        }
        if (config.enableEnderDragon) {
            enabledMobs.add(() -> spawnEnderDragon(world, player));
        }
        if (config.enableWarden) {
            enabledMobs.add(() -> spawnWarden(world, player));
        }
        if (config.enableZombiesAndSkeletons) {
            enabledMobs.add(() -> spawnZombiesAndSkeletons(world, player));
        }
        if (config.enableChargedCreepers) {
            enabledMobs.add(() -> spawnChargedCreepers(world, player));
        }
        if (config.enableHostileIronGolems) {
            enabledMobs.add(() -> spawnHostileIronGolems(world, player));
        }

        if (enabledMobs.isEmpty()) {
            return;
        }

        // 随机选择一种生物生成
        int option = RANDOM.nextInt(enabledMobs.size());
        enabledMobs.get(option).run();
    }

    /**
     * 在玩家附近半径内随机位置生成
     */
    private static Vec3d getRandomSpawnPos(ServerPlayerEntity player) {
        double angle = RANDOM.nextDouble() * 2 * Math.PI;
        double radius = RANDOM.nextDouble() * config.spawnRadius;
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
        WardenEntity warden = EntityType.WARDEN.create(world, SpawnReason.TRIGGERED);
        if (warden != null) {
            warden.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);
            warden.setPersistent();
            // 让监守者直接对玩家产生最大愤怒值
            warden.increaseAngerAt(player, 150);
            warden.setAttacker(player);
            warden.setTarget(player);
            world.spawnEntityAndPassengers(warden);
        }
    }

    private static void spawnZombiesAndSkeletons(ServerWorld world, ServerPlayerEntity player) {
        // 生成僵尸
        for (int i = 0; i < config.zombieCount; i++) {
            Vec3d pos = getRandomSpawnPos(player);
            ZombieEntity zombie = EntityType.ZOMBIE.create(world, SpawnReason.EVENT);
            if (zombie != null) {
                zombie.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);
                zombie.setTarget(player);
                world.spawnEntityAndPassengers(zombie);
            }
        }
        // 生成骷髅
        for (int i = 0; i < config.skeletonCount; i++) {
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
        for (int i = 0; i < config.chargedCreeperCount; i++) {
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
        for (int i = 0; i < config.ironGolemCount; i++) {
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
