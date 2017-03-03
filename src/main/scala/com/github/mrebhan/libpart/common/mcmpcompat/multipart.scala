package com.github.mrebhan.libpart.common.mcmpcompat

import com.github.mrebhan.libpart.LibPart
import com.github.mrebhan.libpart.common.Registry
import com.github.mrebhan.libpart.common.block.{BlockPart, TilePart}
import mcmultipart.MCMultiPart
import mcmultipart.api.addon.{IMCMPAddon, MCMPAddon}
import mcmultipart.api.container.IPartInfo
import mcmultipart.api.multipart.{IMultipart, IMultipartRegistry, IMultipartTile}
import mcmultipart.api.slot.IPartSlot
import mcmultipart.block.BlockMultipartContainer
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemBlock
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.{AxisAlignedBB, BlockPos}
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

/**
  * Created by marco on 28.02.17.
  */
@MCMPAddon
class MCMPCompat extends IMCMPAddon {
  override def registerParts(registry: IMultipartRegistry): Unit = {
    for (any <- Registry.multipartQueue) {
      any match {
        case block: BlockPart =>
          registry.registerPartWrapper(block, new BlockMultipart(block))
        case item: ItemBlock =>
          registry.registerStackWrapper(item, _ => true, item.block)
        case _ => LibPart.LOGGER.warn(s"Invalid object '$any' in multipart registry queue, ignoring.")
      }
    }
  }
}

class TileMultipart(tile: TilePart) extends IMultipartTile {
  override def getTileEntity: TileEntity = tile
}

class BlockMultipart(block: BlockPart) extends IMultipart {
  override def getBlock: Block = block

  override def getSlotFromWorld(world: IBlockAccess, pos: BlockPos, state: IBlockState): IPartSlot = block.defaultPart.getSlotFromWorld(world, pos, state) // TODO make it get the slot from the existing part

  override def getSlotForPlacement(world: World, pos: BlockPos, state: IBlockState, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, placer: EntityLivingBase): IPartSlot =
    block.defaultPart.getSlotForPlacement(world, pos, state, facing, hitX, hitY, hitZ, placer)

  override def getBoundingBox(part: IPartInfo): AxisAlignedBB = part.getTile.getTileEntity.asInstanceOf[TilePart].getPart.getSelectionBox
}

object MPUtils {
  @SideOnly(Side.CLIENT)
  def getTileFromHit(world: IBlockAccess, pos: BlockPos): TilePart = {
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
}