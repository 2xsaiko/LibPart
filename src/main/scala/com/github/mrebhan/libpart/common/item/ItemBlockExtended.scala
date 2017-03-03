package com.github.mrebhan.libpart.common.item

import com.github.mrebhan.libpart.common.block.BlockPart
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{ItemBlock, ItemStack}
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
  * Created by marco on 03.03.17.
  */
class ItemBlockExtended(block: BlockPart) extends ItemBlock(block) {

  override def placeBlockAt(stack: ItemStack, player: EntityPlayer, world: World, pos: BlockPos, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, newState: IBlockState): Boolean = {
    if (!world.setBlockState(pos, newState, 11)) return false
    val state = world.getBlockState(pos)
    if (state.getBlock == this.block) {
      ItemBlock.setTileEntityNBT(world, player, pos, stack)
      this.block.onBlockPlacedBy(world, pos, state, player, stack, side)
    }
    true
  }

}
