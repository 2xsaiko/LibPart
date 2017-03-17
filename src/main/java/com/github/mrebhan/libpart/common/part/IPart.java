package com.github.mrebhan.libpart.common.part;

import com.github.mrebhan.libpart.common.Registry;
import com.github.mrebhan.libpart.common.block.TilePart;
import mcmultipart.api.slot.IPartSlot;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;

/**
 * Created by marco on 25.02.17.
 */
public interface IPart {

    AxisAlignedBB FULL_AABB = new AxisAlignedBB(0, 0, 0, 1, 1, 1);

    default void sendUpdatePacket() {
        getContainer().updateClient(true);
    }

    default void writeToNBT(NBTTagCompound nbt) {}

    default void readFromNBT(NBTTagCompound nbt) {}

    default void writeUpdatePacket(PacketBuffer buf) {}

    default void readUpdatePacket(PacketBuffer buf) {}

    TilePart getContainer();

    IPart setContainer(TilePart pt);

    default BlockPos getPos() {
        return getContainer().getPos();
    }

    default World getWorld() {
        return getContainer().getWorld();
    }

    ResourceLocation getType();

    default boolean onActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        return false;
    }

    default boolean canPlaceAt(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock().isReplaceable(world, pos);
    }

    default boolean canPlaceOnSide(World world, BlockPos pos, EnumFacing side) {
        return canPlaceAt(world, pos);
    }

    default boolean canStay() {
        return true;
    }

    default boolean canRenderInLayer(BlockRenderLayer layer) {
        return layer == BlockRenderLayer.SOLID;
    }

    default void onPlacedBy(EntityLivingBase e, ItemStack stack, EnumFacing side) {}

    default BlockStateContainer createBlockState(Block block) {
        return new BlockStateContainer(block);
    }

    default IBlockState getBaseState() {
        return Registry.getDefaultState(this).getBaseState();
    }

    default IBlockState getActualState(IBlockState state) {
        return state;
    }

    default IBlockState getExtendedState(IBlockState state) {
        return state;
    }

    default void onLoaded() {}

    default void onUnloaded() {}

    default void markDirty() {
        getContainer().markDirty();
    }

    default AxisAlignedBB getSelectionBox() {
        return getBoundingBox();
    }

    default AxisAlignedBB getCollisionBox() {
        return getBoundingBox();
    }

    default AxisAlignedBB getBoundingBox() {
        return FULL_AABB;
    }

    default boolean isFullBlock() {
        return getBoundingBox() == FULL_AABB;
    }

    default float getHardness() {
        return 1;
    }

    default float getStrength(EntityPlayer player) {
        float hardness = getHardness();
        if (hardness < 0.0F)
            return 0.0F;
        else if (hardness == 0.0F)
            return 1.0F;

        Material mat = getMaterial();
        ItemStack stack = player.getHeldItemMainhand();
        boolean effective = mat.isToolNotRequired();
        IBlockState state = getActualState(getBaseState());
        if (!effective && !stack.isEmpty())
            for (String tool : stack.getItem().getToolClasses(stack))
                if (effective = isToolEffective(tool, stack.getItem().getHarvestLevel(stack, tool, player, state)))
                    break;

        float breakSpeed = player.getDigSpeed(state, getPos());

        if (!effective)
            return breakSpeed / hardness / 100F;
        else
            return breakSpeed / hardness / 30F;
    }

    default String getHarvestTool() {
        return null;
    }

    default int getHarvestLevel() {
        return 0;
    }

    default boolean isToolEffective(String type, int level) {
        String t = getHarvestTool();
        return (t == null && type == null) || type.equals(t);
    }

    default boolean isToolEffective(String type) {
        return isToolEffective(type, -1);
    }

    @Nonnull
    Material getMaterial();

    @Nonnull
    SoundType getSoundType();

    ItemStack getPickBlock();

    default IPartSlot getSlotForPlacement(World world, BlockPos pos, IBlockState state, EnumFacing facing, float hitX, float hitY, float hitZ, EntityLivingBase placer) {
        return getSlot();
    }

    default IPartSlot getSlotFromWorld(IBlockAccess world, BlockPos pos, IBlockState state) {
        return getSlot();
    }

    @Nonnull
    IPartSlot getSlot();

    default <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        return null;
    }

    default <T> boolean hasCapability(Capability<T> capability, EnumFacing facing) {
        return false;
    }

    default IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                             EntityLivingBase placer, EnumHand hand, IBlockState state) {
        return state;
    }

    default void handlePlacedState(IBlockState state) {}

    default void neighborChanged(EnumFacing neighbor) {}

    default void partChanged() {}

    default void onRemoved() {}

    default void onAdded() {}
}
