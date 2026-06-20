package com.furina.dontgethurt.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class PlayerDamageHandler implements ServerLivingEntityEvents.AllowDamage {
    private static final Random RANDOM = new Random();
    private static final int SPAWN_RADIUS = 5;

    @Override
    public boolean allowDamage(@NotNull LivingEntity entity, net.minecraft.world.damage.DamageSource source, float amount) {
        // 只处理玩家受到的伤害，且只在服务端
        if (entity instanceof PlayerEntity player && !player.getWorld().isClient) {
            spawnRandomMobs(player);
        }
        return true; // 允许伤害正常发生
    }

    private void spawnRandomMobs(PlayerEntity player) {
        World world = player.getWorld();
        int option = RANDOM.nextInt(6); // 0-5，6个选项等概率

        switch (option) {
            case 0 -> spawnWither(player, world);
            case 1 -> spawnEnderDragon(player, world);
            case 2 -> spawnWarden(player, world);
            case 3 -> spawnZombiesAndSkeletons(player, world);
            case 4 -> spawnChargedCreepers(player, world);
            case 5 -> spawnHostileIronGolems(player, world);
        }
    }

    private Vec3d getRandomSpawnPosition(PlayerEntity player) {
        double angle = RANDOM.nextDouble() * 2 * Math.PI;
        double distance = RANDOM.nextDouble() * SPAWN_RADIUS;
        double x = player.getX() + Math.cos(angle) * distance;
        double z = player.getZ() + Math.sin(angle) * distance;
        double y = player.getY();
        return new Vec3d(x, y, z);
    }

    private void spawnWither(PlayerEntity player, World world) {
        WitherEntity wither = EntityType.WITHER.create(world);
        if (wither != null) {
            Vec3d pos = getRandomSpawnPosition(player);
            wither.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);
            world.spawnEntity(wither);
        }
    }

    private void spawnEnderDragon(PlayerEntity player, World world) {
        EnderDragonEntity dragon = EntityType.ENDER_DRAGON.create(world);
        if (dragon != null) {
            Vec3d pos = getRandomSpawnPosition(player);
            dragon.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);
            world.spawnEntity(dragon);
        }
    }

    private void spawnWarden(PlayerEntity player, World world) {
        WardenEntity warden = EntityType.WARDEN.create(world);
        if (warden != null) {
            Vec3d pos = getRandomSpawnPosition(player);
            warden.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);
            world.spawnEntity(warden);
        }
    }

    private void spawnZombiesAndSkeletons(PlayerEntity player, World world) {
        // 10只僵尸
        for (int i = 0; i < 10; i++) {
            ZombieEntity zombie = EntityType.ZOMBIE.create(world);
            if (zombie != null) {
                Vec3d pos = getRandomSpawnPosition(player);
                zombie.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);
                world.spawnEntity(zombie);
            }
        }
        // 10只骷髅
        for (int i = 0; i < 10; i++) {
            SkeletonEntity skeleton = EntityType.SKELETON.create(world);
            if (skeleton != null) {
                Vec3d pos = getRandomSpawnPosition(player);
                skeleton.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);
                world.spawnEntity(skeleton);
            }
        }
    }

    private void spawnChargedCreepers(PlayerEntity player, World world) {
        for (int i = 0; i < 10; i++) {
            CreeperEntity creeper = EntityType.CREEPER.create(world);
            if (creeper != null) {
                Vec3d pos = getRandomSpawnPosition(player);
                creeper.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);
                creeper.setCharged(true); // 设置为闪电苦力怕
                world.spawnEntity(creeper);
            }
        }
    }

    private void spawnHostileIronGolems(PlayerEntity player, World world) {
        for (int i = 0; i < 10; i++) {
            IronGolemEntity golem = EntityType.IRON_GOLEM.create(world);
            if (golem != null) {
                Vec3d pos = getRandomSpawnPosition(player);
                golem.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);

                // 让铁傀儡对玩家产生敌意
                golem.setTarget(player);
                // 添加攻击目标 AI
                golem.targetSelector.add(1, new ActiveTargetGoal<>(golem, PlayerEntity.class, true));
                // 添加近战攻击 AI
                golem.goalSelector.add(2, new MeleeAttackGoal(golem, 1.0D, true));

                world.spawnEntity(golem);
            }
        }
    }
}
