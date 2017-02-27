package com.github.mrebhan.libpart.common.block

import com.github.mrebhan.libpart.LibPart
import com.github.mrebhan.libpart.common.Registry
import com.github.mrebhan.libpart.common.part.IPart
import mcmultipart.MCMultiPart
import mcmultipart.api.container.IPartInfo
import mcmultipart.api.multipart.IMultipart
import mcmultipart.api.slot.IPartSlot
import mcmultipart.block.BlockMultipartContainer
import net.minecraft.block.state.{BlockStateContainer, IBlockState}
import net.minecraft.block.{Block, ITileEntityProvider}
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.{AxisAlignedBB, BlockPos}
import net.minecraft.util.{EnumFacing, EnumHand, ITickable, ResourceLocation}
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

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

  override def createBlockState(): BlockStateContainer = defaultPart.createBlockState(this)

  override def getActualState(state: IBlockState, worldIn: IBlockAccess, pos: BlockPos): IBlockState = getPartAt(worldIn, pos).getActualState(state)

  override def getExtendedState(state: IBlockState, world: IBlockAccess, pos: BlockPos): IBlockState = getPartAt(world, pos).getExtendedState(state)

  override def createNewTileEntity(worldIn: World, meta: Int): TileEntity = if (defaultPart.isInstanceOf[ITickable]) new TilePart.Ticking(rl) else new TilePart(rl)

  override def getMetaFromState(state: IBlockState): Int = 0

  override def isOpaqueCube(state: IBlockState): Boolean = false

  override def getSelectedBoundingBox(state: IBlockState, worldIn: World, pos: BlockPos): AxisAlignedBB = getPartAt(worldIn, pos).getSelectionBox.offset(pos)

  override def getCollisionBoundingBox(blockState: IBlockState, worldIn: IBlockAccess, pos: BlockPos): AxisAlignedBB = getPartAt(worldIn, pos, false).getBoundingBox

  override def getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos): AxisAlignedBB = getPartAt(source, pos).getSelectionBox

  def createPart(): IPart = Registry.getPartClass(rl).newInstance()

  def getTileAt(world: IBlockAccess, pos: BlockPos, warn: Boolean = true): TilePart =
    world.getTileEntity(pos) match {
      case te: TilePart => te
      case _ =>
        if (world.isInstanceOf[WorldClient]) {
          getTileFromHit(world, pos)
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

  @SideOnly(Side.CLIENT)
  private def getTileFromHit(world: IBlockAccess, pos: BlockPos): TilePart = {
    val hit = Minecraft.getMinecraft.objectMouseOver
    var te: TileEntity = null
    if (world.getBlockState(pos).getBlock == MCMultiPart.multipart) {
      val tile = BlockMultipartContainer.getTile(world, pos)
      if (tile.isPresent) {
        val slotID = hit.subHit
        val info = tile.get.getParts.get(MCMultiPart.slotRegistry.getObjectById(slotID))
        if (info != null)
          te = info.getTile.getTileEntity
      }
    }
    te match {
      case part: TilePart => part
      case _ => null
    }
  }

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
    getPartAt(worldIn, pos, true, false).onActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ)
  }

  object Multipart extends IMultipart {
    override def getBlock: Block = BlockPart.this

    override def getSlotFromWorld(world: IBlockAccess, pos: BlockPos, state: IBlockState): IPartSlot = createPart().getSlot

    override def getSlotForPlacement(world: World, pos: BlockPos, state: IBlockState, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, placer: EntityLivingBase): IPartSlot = createPart().getSlot

    override def getBoundingBox(part: IPartInfo): AxisAlignedBB = part.getTile.getTileEntity.asInstanceOf[TilePart].getPart.getSelectionBox
  }

}

object BlockPart {
  var breakingPart: IPart = _
}
