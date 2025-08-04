package com.ma_wuyan.autolandingwater.mixin;

import com.ma_wuyan.autolandingwater.AutoLandingWater;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    @Unique private BlockPos lastWaterPos = null;
    @Unique private int cooldown = 0; // 添加冷却机制防止高频触发

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        // 只在服务器端处理
        if (getWorld().isClient) return;

        PlayerEntity player = (PlayerEntity) (Object) this;

        // 冷却计时器
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        // 检查是否正在下落且高度足够造成伤害
        if (player.fallDistance > 3.0F &&
                player.getVelocity().y < 0 &&
                !player.isOnGround() &&
                !player.isSwimming() &&
                !player.isFallFlying() &&
                !player.isTouchingWater()) {

            // 检查主手或副手是否有水桶
            ItemStack mainHand = player.getStackInHand(Hand.MAIN_HAND);
            ItemStack offHand = player.getStackInHand(Hand.OFF_HAND);
            boolean hasWaterBucket = mainHand.isOf(Items.WATER_BUCKET) || offHand.isOf(Items.WATER_BUCKET);

            // 关键修复：计算到地面的距离
            BlockPos feetPos = BlockPos.ofFloored(player.getPos());
            int distanceToGround = calculateDistanceToGround(feetPos);

            // 只在接近地面时放置水（距离地面1-3格）
            if (hasWaterBucket && distanceToGround >= 1 && distanceToGround <= 1.5) {
                // 在脚下位置放置水
                BlockPos waterPos = feetPos.down();

                if (getWorld().getBlockState(waterPos).isAir()) {
                    getWorld().setBlockState(waterPos, net.minecraft.block.Blocks.WATER.getDefaultState());
                    lastWaterPos = waterPos;
                    cooldown = 2; // 设置冷却防止连续放置
                    AutoLandingWater.LOGGER.debug("Placed water at {} (distance to ground: {})", waterPos, distanceToGround);

                    // 移除水桶并添加空桶
                    if (mainHand.isOf(Items.WATER_BUCKET)) {
                        player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.BUCKET));
                    } else {
                        player.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.BUCKET));
                    }
                }
            }
        }

        // 落地后回收水
        if (player.isOnGround() && lastWaterPos != null) {
            if (getWorld().getBlockState(lastWaterPos).isOf(net.minecraft.block.Blocks.WATER)) {
                getWorld().setBlockState(lastWaterPos, net.minecraft.block.Blocks.AIR.getDefaultState());

                // 检查是否有空桶可以装水
                ItemStack mainHand = player.getStackInHand(Hand.MAIN_HAND);
                ItemStack offHand = player.getStackInHand(Hand.OFF_HAND);

                if (mainHand.isOf(Items.BUCKET)) {
                    player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
                } else if (offHand.isOf(Items.BUCKET)) {
                    player.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.WATER_BUCKET));
                }

                AutoLandingWater.LOGGER.debug("Removed water at {}", lastWaterPos);
            }
            lastWaterPos = null;
        }
    }

    @Unique
    private int calculateDistanceToGround(BlockPos startPos) {
        World world = getWorld();
        BlockPos currentPos = startPos.down();
        int distance = 0;

        // 向下搜索直到找到非空气方块
        while (distance < 20) { // 最大搜索20格
            if (!world.getBlockState(currentPos).isAir()) {
                return distance;
            }
            distance++;
            currentPos = currentPos.down();
        }
        return distance; // 超过20格仍为空气
    }
}