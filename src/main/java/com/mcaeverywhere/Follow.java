package com.mcaeverywhere;

import java.util.UUID;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * "Seguir": feito por tick (sem injetar goals), assim funciona tanto em mobs com GoalSelector
 * quanto em mobs com Brain (aldeão, piglin, allay, rã, etc.) e em mobs de outros mods.
 */
final class Follow {
    private Follow() {}

    static void tick(Mob mob) {
        if (FriendData.mode(mob) != FriendData.FOLLOW) return;
        UUID ownerId = FriendData.owner(mob);
        if (ownerId == null) return;

        Player owner = mob.level().getPlayerByUUID(ownerId);
        if (owner == null || owner.isSpectator() || mob.isPassenger() || mob.isSleeping()) return;

        double dist2 = mob.distanceToSqr(owner);
        double tp = Config.TELEPORT_DISTANCE.get();
        if (dist2 > tp * tp) {
            teleportNear(mob, owner);
            return;
        }

        Brain<?> brain = mob.getBrain();
        boolean brainDriven = brain.checkMemory(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED);

        if (dist2 > 16.0D) {
            if (brainDriven) {
                brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(owner, false), 1.1F, 3));
                brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(owner, true));
            } else if (!mob.getNavigation().moveTo(owner, 1.15D)) {
                mob.getMoveControl().setWantedPosition(owner.getX(), owner.getY(), owner.getZ(), 1.0D);
            }
        } else if (dist2 < 9.0D) {
            if (brainDriven) {
                brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            } else {
                mob.getNavigation().stop();
            }
        }
        if (!brainDriven) {
            mob.getLookControl().setLookAt(owner, 30.0F, 30.0F);
        }
    }

    private static void teleportNear(Mob mob, Player owner) {
        RandomSource random = mob.getRandom();
        for (int i = 0; i < 16; i++) {
            double x = owner.getX() + random.nextInt(7) - 3;
            double z = owner.getZ() + random.nextInt(7) - 3;
            double y = owner.getY();
            if (Math.abs(x - owner.getX()) < 1.5D && Math.abs(z - owner.getZ()) < 1.5D) continue;

            AABB box = mob.getBoundingBox().move(x - mob.getX(), y - mob.getY(), z - mob.getZ());
            if (mob.level().noCollision(mob, box)) {
                mob.moveTo(x, y, z, mob.getYRot(), mob.getXRot());
                mob.getNavigation().stop();
                return;
            }
        }
    }
}
