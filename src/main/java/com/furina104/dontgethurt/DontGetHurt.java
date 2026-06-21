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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.data.TrackedData;
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
    private static Method nbtWriteMethod;
    private static Method nbtReadMethod;

    private static ModConfig config;
    private static final ConcurrentHashMap<UUID, Long> cooldownMap = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> isSpawning = ThreadLocal.withInitial(() -> false);

    static {
        // 查找 NBT 读写方法
        try {
            Class<?> entityClass = net.minecraft.entity.Entity.class;
            Method[] methods = entityClass.getDeclaredMethods();
            
            // 查找写方法（返回 NbtCompound 的方法）
            for (Method method : methods) {
                if (method.getReturnType() == NbtCompound.class) {
                    String name = method.getName().toLowerCase();
                    // 优先选择名字包含 save 或 write 的方法
                    if (name.contains("save") || name.contains("write") || name.contains("nbt")) {
                        nbtWriteMethod = method;
                        nbtWriteMethod.setAccessible(true);
                        break;
                    }
                }
            }
            
            // 查找读方法（接受 NbtCompound 参数的方法）
            for (Method method : methods) {
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == NbtCompound.class) {
                    String name = method.getName().toLowerCase();
                    if (name.contains("load") || name.contains("read") || name.contains("from")) {
                        nbtReadMethod = method;
                        nbtReadMethod.setAccessible(true);
                        break;
                    }
                }
            }
            
            LOGGER.info("Found NBT methods - write: {} (params={}), read: {} (params={})", 
                nbtWriteMethod != null ? nbtWriteMethod.getName() : "null",
                nbtWriteMethod != null ? nbtWriteMethod.getParameterCount() : -1,
                nbtReadMethod != null ? nbtReadMethod.getName() : "null",
                nbtReadMethod != null ? nbtReadMethod.getParameterCount() : -1);
        } catch (Exception e) {
            LOGGER.error("Failed to find NBT methods", e);
        }

        // 查找 CHARGED 字段
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
        // 每次都从 ConfigHolder 获取最新配置，确保修改后实时生效
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    }

    /**
     * 从启用的选项中随机选择并生成生物
     */
    public static void spawnMobs(ServerWorld world, ServerPlayerEntity player) {
        // 每次都获取最新配置，确保修改后实时生效
        ModConfig currentConfig = getConfig();
        
        if (!currentConfig.enabled) {
            return;
        }

        // 递归保护：防止生成生物过程中造成的伤害再次触发生成
        if (isSpawning.get()) {
            return;
        }

        // 检查冷却时间
        if (currentConfig.cooldownTicks > 0) {
            long currentTime = world.getTime();
            Long lastSpawn = cooldownMap.get(player.getUuid());
            if (lastSpawn != null && currentTime - lastSpawn < currentConfig.cooldownTicks) {
                return;
            }
            cooldownMap.put(player.getUuid(), currentTime);
        }

        // 收集启用的生物类型
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

        // 设置生成中标志（在确认要生成后再设置）
        isSpawning.set(true);
        try {
            // 随机选择一种生物生成
            int option = RANDOM.nextInt(enabledMobs.size());
            enabledMobs.get(option).run();
        } finally {
            // 清除生成中标志
            isSpawning.remove();
        }
    }

    /**
     * 在玩家附近半径内随机位置生成
     */
    private static Vec3d getRandomSpawnPos(ServerPlayerEntity player) {
        ModConfig currentConfig = getConfig();
        double angle = RANDOM.nextDouble() * 2 * Math.PI;
        // 最小半径 2 格，避免生成在玩家身上
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
        // 找到地面位置，避免生成在半空中或卡在方块里
        int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(pos.x), (int) Math.floor(pos.z));
        BlockPos blockPos = new BlockPos((int) Math.floor(pos.x), topY, (int) Math.floor(pos.z));

        WardenEntity warden = EntityType.WARDEN.spawn(world, blockPos, SpawnReason.EVENT);
        if (warden != null) {
            warden.setPersistent();

            // 生成后设置，避免被初始化逻辑覆盖
            warden.increaseAngerAt(player, 150, false);
            warden.setAttacker(player);
            warden.setTarget(player);
            warden.setAiDisabled(false);
        }
    }

    private static void spawnZombiesAndSkeletons(ServerWorld world, ServerPlayerEntity player) {
        ModConfig currentConfig = getConfig();
        // 生成僵尸
        for (int i = 0; i < currentConfig.zombieCount; i++) {
            Vec3d pos = getRandomSpawnPos(player);
            // 找到地面位置，避免生成在半空中或卡在方块里
            int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(pos.x), (int) Math.floor(pos.z));
            ZombieEntity zombie = EntityType.ZOMBIE.create(world, SpawnReason.EVENT);
            if (zombie != null) {
                zombie.refreshPositionAndAngles(pos.x, topY, pos.z, 0, 0);
                zombie.setTarget(player);
                world.spawnEntityAndPassengers(zombie);
            }
        }
        // 生成骷髅
        for (int i = 0; i < currentConfig.skeletonCount; i++) {
            Vec3d pos = getRandomSpawnPos(player);
            // 找到地面位置，避免生成在半空中或卡在方块里
            int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(pos.x), (int) Math.floor(pos.z));
            BlockPos blockPos = new BlockPos((int) Math.floor(pos.x), topY, (int) Math.floor(pos.z));
            SkeletonEntity skeleton = EntityType.SKELETON.spawn(world, blockPos, SpawnReason.EVENT);
            if (skeleton != null) {
                // 确保骷髅有弓（spawn方法会自动装备，但保险起见再确认一下）
                skeleton.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                skeleton.setTarget(player);
            }
        }
    }

    private static void spawnChargedCreepers(ServerWorld world, ServerPlayerEntity player) {
        ModConfig currentConfig = getConfig();
        for (int i = 0; i < currentConfig.chargedCreeperCount; i++) {
            Vec3d pos = getRandomSpawnPos(player);
            // 找到地面位置，避免生成在半空中或卡在方块里
            int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(pos.x), (int) Math.floor(pos.z));
            CreeperEntity creeper = EntityType.CREEPER.create(world, SpawnReason.EVENT);
            if (creeper != null) {
                creeper.refreshPositionAndAngles(pos.x, topY, pos.z, 0, 0);
                // 设置为闪电苦力怕（充能状态）
                // 优先使用 TrackedData 方式，如果失败则使用 NBT 方式
                boolean charged = false;
                if (CREEPER_CHARGED_DATA != null) {
                    try {
                        creeper.getDataTracker().set(CREEPER_CHARGED_DATA, true);
                        charged = true;
                    } catch (Exception e) {
                        LOGGER.warn("Failed to set charged via TrackedData, falling back to NBT", e);
                    }
                }
                if (!charged && nbtWriteMethod != null && nbtReadMethod != null) {
                    // 使用反射调用 NBT 方法设置充能状态
                    try {
                        NbtCompound nbt;
                        if (nbtWriteMethod.getParameterCount() == 0) {
                            // 无参数的写方法，直接调用获取 NBT
                            nbt = (NbtCompound) nbtWriteMethod.invoke(creeper);
                        } else {
                            // 有参数的写方法，传入新的 NbtCompound
                            nbt = (NbtCompound) nbtWriteMethod.invoke(creeper, new NbtCompound());
                        }
                        nbt.putBoolean("powered", true);
                        nbtReadMethod.invoke(creeper, nbt);
                        charged = true;
                        LOGGER.info("Successfully set creeper charged via NBT");
                    } catch (Exception e) {
                        LOGGER.warn("Failed to set charged via NBT reflection", e);
                    }
                }
                creeper.setTarget(player);
                world.spawnEntityAndPassengers(creeper);
            }
        }
    }

    private static void spawnHostileIronGolems(ServerWorld world, ServerPlayerEntity player) {
        ModConfig currentConfig = getConfig();
        for (int i = 0; i < currentConfig.ironGolemCount; i++) {
            Vec3d pos = getRandomSpawnPos(player);
            // 找到地面位置，避免生成在半空中或卡在方块里
            int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(pos.x), (int) Math.floor(pos.z));
            IronGolemEntity ironGolem = EntityType.IRON_GOLEM.create(world, SpawnReason.EVENT);
            if (ironGolem != null) {
                ironGolem.refreshPositionAndAngles(pos.x, topY, pos.z, 0, 0);
                // 让铁傀儡对玩家产生敌意
                ironGolem.setPlayerCreated(false);
                ironGolem.setTarget(player);
                world.spawnEntityAndPassengers(ironGolem);
            }
        }
    }
}
