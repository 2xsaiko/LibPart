package com.github.mrebhan.libpart.common.block

import com.github.mrebhan.libpart.LibPart
import com.github.mrebhan.libpart.common.Registry
import com.github.mrebhan.libpart.common.mcmpcompat.MPUtils
import com.github.mrebhan.libpart.common.part.IPart
import net.minecraft.block.state.{BlockStateContainer, IBlockState}
import net.minecraft.block.{Block, ITileEntityProvider}
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util._
import net.minecraft.util.math.{AxisAlignedBB, BlockPos, RayTraceResult}
import net.minecraft.world.{IBlockAccess, World}

/**
  * Created by marco on 27.02.17.
  */
class BlockPart(rl: ResourceLocation) extends Block(Registry.getPartClass(rl).newInstance().getMaterial) with ITileEntityProvider {

  /**
    * The default part. Do not modify in any way!
    */

  lazy val defaultPart: IPart = createPart()

  setRegistryName(rl)
  setUnlocalizedName(rl.toString)
  setSoundType(defaultPart.getSoundType)

  override def canRenderInLayer(state: IBlockState, layer: BlockRenderLayer): Boolean = defaultPart.canRenderInLayer(layer)

  override def canPlaceBlockOnSide(worldIn: World, pos: BlockPos, side: EnumFacing): Boolean = defaultPart.canPlaceOnSide(worldIn, pos, side)

  override def canPlaceBlockAt(worldIn: World, pos: BlockPos): Boolean = defaultPart.canPlaceAt(worldIn, pos)

  override def onBlockPlacedBy(worldIn: World, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, stack: ItemStack): Unit = getPartAt(worldIn, pos).onPlacedBy(placer, stack)

  override def createBlockState(): BlockStateContainer = defaultPart.createBlockState(this)

  override def getActualState(state: IBlockState, worldIn: IBlockAccess, pos: BlockPos): IBlockState = getPartAt(worldIn, pos).getActualState(state)

  override def getExtendedState(state: IBlockState, world: IBlockAccess, pos: BlockPos): IBlockState = getPartAt(world, pos).getExtendedState(state)

  override def createNewTileEntity(worldIn: World, meta: Int): TileEntity = if (defaultPart.isInstanceOf[ITickable]) new TilePart.Ticking(rl) else new TilePart(rl)

  override def getMetaFromState(state: IBlockState): Int = 0

  override def isOpaqueCube(state: IBlockState): Boolean = defaultPart.isFullBlock

  override def isFullCube(state: IBlockState): Boolean = defaultPart.isFullBlock

  override def getSelectedBoundingBox(state: IBlockState, worldIn: World, pos: BlockPos): AxisAlignedBB = getPartAt(worldIn, pos).getSelectionBox.offset(pos)

  override def getCollisionBoundingBox(blockState: IBlockState, worldIn: IBlockAccess, pos: BlockPos): AxisAlignedBB = getPartAt(worldIn, pos, warn = false).getCollisionBox

  override def getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos): AxisAlignedBB = getPartAt(source, pos, warn = false).getBoundingBox

  override def getPickBlock(state: IBlockState, target: RayTraceResult, world: World, pos: BlockPos, player: EntityPlayer): ItemStack = getPartAt(world, pos).getPickBlock

  def createPart(): IPart = Registry.getPartClass(rl).newInstance()

  def getTileAt(world: IBlockAccess, pos: BlockPos, warn: Boolean = true): TilePart =
    world.getTileEntity(pos) match {
      case te: TilePart => te
      case _ =>
        if (world.isInstanceOf[WorldClient] && LibPart.multipartCompat) {
          MPUtils.getTileFromHit(world, pos)
        } else {
          if (warn) LibPart.LOGGER.warn(s"There's no tile at $pos when there should be!")
          null
        }
    }

  def getPartAt(world: IBlockAccess, pos: BlockPos, warn: Boolean = true, giveMeDefault: Boolean = true): IPart =
    getTileAt(world, pos, warn) match {
      case te: TilePart => te.getPart
      case _ =>
        if (warn) LibPart.LOGGER.warn(s"There's no part at $pos when there should be!")
        if (giveMeDefault) defaultPart else null
    }

  @SuppressWarnings(Array("deprecation"))
  override def getPlayerRelativeBlockHardness(state: IBlockState, player: EntityPlayer, worldIn: World, pos: BlockPos): Float = {
    val part1 = getPartAt(worldIn, pos)
    if (part1 != null) {
      BlockPart.breakingPart = part1
      try
        return part1.getStrength(player)
      finally BlockPart.breakingPart = null
    }
    super.getPlayerRelativeBlockHardness(state, player, worldIn, pos)
  }

  override def isToolEffective(`type`: String, state: IBlockState): Boolean = {
    if (BlockPart.breakingPart != null) BlockPart.breakingPart.isToolEffective(`type`)
    else super.isToolEffective(`type`, state)
  }

  override def getHarvestLevel(state: IBlockState): Int = {
    if (BlockPart.breakingPart != null) BlockPart.breakingPart.getHarvestLevel
    else 0
  }

  override def getHarvestTool(state: IBlockState): String = {
    if (BlockPart.breakingPart != null) BlockPart.breakingPart.getHarvestTool
    else null
  }

  override def onBlockActivated(worldIn: World, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    getPartAt(worldIn, pos, giveMeDefault = false).onActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ)
  }

}

object BlockPart {
  var breakingPart: IPart = _
}
