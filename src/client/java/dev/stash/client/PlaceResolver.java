package dev.stash.client;

import dev.stash.domain.ContainerKind;
import dev.stash.domain.ContainerSnapshot;
import dev.stash.domain.LocationKeys;
import dev.stash.domain.StoredSlot;
import dev.stash.domain.Texts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

final class PlaceResolver {
    private PlaceResolver() {
    }

    static Place capture(Minecraft client, MenuType<?> menuType, String title) {
        ContainerSnapshot probed = resolve(client, menuType, title, List.of(), System.currentTimeMillis());
        if (probed == null) {
            return null;
        }
        return new Place(probed.locationKey(), probed.dimensionId(), probed.x(), probed.y(), probed.z(), probed.kind(), probed.label());
    }

    static ContainerSnapshot resolve(Minecraft client, MenuType<?> menuType, String title, List<StoredSlot> slots, long seenAt) {
        ClientLevel level = client.level;
        Player player = client.player;
        if (level == null || player == null) {
            return null;
        }
        String dimensionId = level.dimension().identifier().toString();
        HitResult hit = client.hitResult;
        if (hit instanceof EntityHitResult entityHit && inReach(player, entityHit.getEntity().blockPosition())) {
            ContainerSnapshot minecart = minecart(entityHit.getEntity(), dimensionId, title, slots, seenAt);
            if (minecart != null) {
                return minecart;
            }
        }
        if (!(hit instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = blockHit.getBlockPos();
        if (!inReach(player, pos)) {
            return null;
        }
        BlockState state = level.getBlockState(pos);
        ContainerKind kind = kind(state, menuType);
        if (kind == ContainerKind.CHEST) {
            pos = canonicalChest(level, pos, state);
        }
        String key = kind == ContainerKind.ENDER
                ? LocationKeys.ENDER
                : LocationKeys.block(dimensionId, pos.getX(), pos.getY(), pos.getZ());
        return new ContainerSnapshot(key, dimensionId, pos.getX(), pos.getY(), pos.getZ(), kind, title, slots, seenAt);
    }

    private static ContainerSnapshot minecart(Entity entity, String dimensionId, String title, List<StoredSlot> slots, long seenAt) {
        EntityType<?> type = entity.getType();
        ContainerKind kind;
        if (type == EntityType.CHEST_MINECART) {
            kind = ContainerKind.MINECART;
        } else if (type == EntityType.HOPPER_MINECART) {
            kind = ContainerKind.HOPPER;
        } else {
            return null;
        }
        BlockPos pos = entity.blockPosition();
        return new ContainerSnapshot(
                LocationKeys.entity(entity.getStringUUID()),
                dimensionId,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                kind,
                title,
                slots,
                seenAt
        );
    }

    private static ContainerKind kind(BlockState state, MenuType<?> menuType) {
        if (state.is(Blocks.ENDER_CHEST)) {
            return ContainerKind.ENDER;
        }
        if (state.is(Blocks.BARREL)) {
            return ContainerKind.BARREL;
        }
        if (menuType == MenuType.SHULKER_BOX || isShulker(state)) {
            return ContainerKind.SHULKER;
        }
        if (state.is(Blocks.HOPPER) || menuType == MenuType.HOPPER) {
            return ContainerKind.HOPPER;
        }
        if (state.is(Blocks.DISPENSER)) {
            return ContainerKind.DISPENSER;
        }
        if (state.is(Blocks.DROPPER)) {
            return ContainerKind.DROPPER;
        }
        if (menuType == MenuType.FURNACE || menuType == MenuType.BLAST_FURNACE || menuType == MenuType.SMOKER) {
            return ContainerKind.FURNACE;
        }
        if (menuType == MenuType.BREWING_STAND) {
            return ContainerKind.BREWING;
        }
        if (menuType == MenuType.CRAFTER_3x3) {
            return ContainerKind.CRAFTER;
        }
        if (state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)) {
            return ContainerKind.CHEST;
        }
        return ContainerKind.OTHER;
    }

    private static boolean isShulker(BlockState state) {
        String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return path.contains("shulker_box");
    }

    private static BlockPos canonicalChest(ClientLevel level, BlockPos pos, BlockState state) {
        if (!state.hasProperty(ChestBlock.TYPE) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return pos;
        }
        BlockPos other = ChestBlock.getConnectedBlockPos(pos, state);
        if (!level.getBlockState(other).is(state.getBlock())) {
            return pos;
        }
        return compare(pos, other) <= 0 ? pos : other;
    }

    private static int compare(BlockPos left, BlockPos right) {
        int byX = Integer.compare(left.getX(), right.getX());
        if (byX != 0) {
            return byX;
        }
        int byZ = Integer.compare(left.getZ(), right.getZ());
        if (byZ != 0) {
            return byZ;
        }
        return Integer.compare(left.getY(), right.getY());
    }

    private static boolean inReach(Player player, BlockPos pos) {
        double range = player.blockInteractionRange() + Reach.EXTRA_BLOCKS;
        return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= range * range;
    }

    record Place(String locationKey, String dimensionId, int x, int y, int z, ContainerKind kind, String label) {
        ContainerSnapshot snapshot(List<StoredSlot> slots, long seenAt) {
            return new ContainerSnapshot(locationKey, dimensionId, x, y, z, kind, label, slots, seenAt);
        }
    }

    static String titleOf(net.minecraft.network.chat.Component title) {
        return Texts.oneLine(title.getString(), Texts.MAX_LABEL_LENGTH);
    }
}
