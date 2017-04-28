package com.github.mrebhan.libpart.common.mcmpcompat

import com.github.mrebhan.libpart.LibPart
import com.github.mrebhan.libpart.common.Registry
import com.github.mrebhan.libpart.common.ScalaCompat._
import com.github.mrebhan.libpart.common.block.{BlockPart, TilePart}
import com.github.mrebhan.libpart.common.part.ICustomIntersect
import mcmultipart.MCMultiPart
import mcmultipart.api.addon.{IMCMPAddon, MCMPAddon}
import mcmultipart.api.container.{IMultipartContainer, IPartInfo}
import mcmultipart.api.item.ItemBlockMultipart
import mcmultipart.api.multipart._
import mcmultipart.api.slot.IPartSlot
import mcmultipart.block.BlockMultipartContainer
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{ItemBlock, ItemStack}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.{AxisAlignedBB, BlockPos}
import net.minecraft.util.{EnumFacing, EnumHand}
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
          val block = item.getBlock.asInstanceOf[BlockPart]
          val wrappedblock = registry.registerStackWrapper(item, (_: ItemStack) => true, block)
          wrappedblock.setPlacementInfo((world: World, pos: BlockPos, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, meta: Int, placer: EntityLivingBase, hand: EnumHand, state: IBlockState) =>
            block.defaultPart.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand, state))
          wrappedblock.setBlockPlacementLogic((stack: ItemStack, player: EntityPlayer, world: World, pos: BlockPos, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, newState: IBlockState) =>
            player.canPlayerEdit(pos, facing, stack) &&
              world.getBlockState(pos).getBlock.isReplaceable(world, pos) &&
              block.canPlaceBlockAt(world, pos) &&
              block.canPlaceBlockOnSide(world, pos, facing) &&
              item.placeBlockAt(stack, player, world, pos, facing, hitX, hitY, hitZ, newState))
          wrappedblock.setPartPlacementLogic(MPUtils.placePartAt _)
        case _ => LibPart.LOGGER.warn(s"Invalid object '$any' (${any.getClass}) in multipart registry queue, ignoring.")
      }
    }
  }
}

class TileMultipart(tile: TilePart) extends IMultipartTile {
  override def getTileEntity: TileEntity = tile
}

class BlockMultipart(block: BlockPart) extends IMultipart {
  override def getBlock: Block = block

  override def getSlotFromWorld(world: IBlockAccess, pos: BlockPos, state: IBlockState): IPartSlot = block.defaultPart.getSlotFromWorld(world, pos, state.getActualState(world, pos))

  override def getSlotForPlacement(world: World, pos: BlockPos, state: IBlockState, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, placer: EntityLivingBase): IPartSlot =
    block.defaultPart.getSlotForPlacement(world, pos, state, facing, hitX, hitY, hitZ, placer)

  override def getBoundingBox(part: IPartInfo): AxisAlignedBB = part.getTile.getTileEntity.asInstanceOf[TilePart].getPart.getSelectionBox

  def onPartPlacedBy(part: IPartInfo, placer: EntityLivingBase, stack: ItemStack, facing: EnumFacing): Unit = {
    block.onBlockPlacedBy(part.getPartWorld, part.getPartPos, part.getState, placer, stack, facing)
  }

  override def neighborChanged(part: IPartInfo, neighborBlock: Block, neighborPos: BlockPos): Unit = {
    val part1 = part.getTile.getTileEntity.asInstanceOf[TilePart].getPart
    if (!part1.canStay) {
      getBlock.dropBlockAsItem(part.getActualWorld, part.getPartPos, part.getState, 0)
      part.getContainer.removePart(part.getSlot)
    } else {
      part1.neighborChanged(EnumFacing.VALUES.filter(f => neighborPos.offset(f) == part.getPartPos).head)
    }
  }

  override def testIntersection(self: IPartInfo, otherPart: IPartInfo): Boolean = {
    val p0 = self.getTile.getTileEntity match {
      case te: TilePart => te.getPart match {
        case part: ICustomIntersect => part
        case _ => null
      }
      case _ => null
    }

    val p1 = otherPart.getTile.getTileEntity match {
      case te: TilePart => te.getPart match {
        case part: ICustomIntersect => part
        case _ => null
      }
      case _ => null
    }

    var b0 = getOcclusionBoxes(self)
    var b1 = otherPart.getPart.getOcclusionBoxes(otherPart)

    if (p0 != null && p1 != null) {
      b0 = p0.getIntersectionBB(p1)
      b1 = p1.getIntersectionBB(p0)
    }

    MultipartOcclusionHelper.testBoxIntersection(b0, b1)
  }

  override def onPartChanged(part: IPartInfo, otherPart: IPartInfo): Unit = part.getTile.getTileEntity.asInstanceOf[TilePart].getPart.partChanged()

  override def onRemoved(part: IPartInfo): Unit = part.getTile.getTileEntity.asInstanceOf[TilePart].getPart.onRemoved()

  override def onPartAdded(part: IPartInfo, otherPart: IPartInfo): Unit = part.getTile.getTileEntity.asInstanceOf[TilePart].getPart.onAdded()
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
        val id = MCMultiPart.slotRegistry.getObjectById(slotID)
        if (id != null) {
          val info = tile.get.getParts.get(id)
          if (info != null)
            te = info.getTile.getTileEntity
        }
      }
    }
    te match {
      case part: TilePart => part
      case _ => null
    }
  }

  def placePartAt(stack: ItemStack, player: EntityPlayer, hand: EnumHand, world: World, pos: BlockPos, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, multipartBlock: IMultipart, state: IBlockState): Boolean = {
    val slot = multipartBlock.getSlotForPlacement(world, pos, state, facing, hitX, hitY, hitZ, player)
    if (multipartBlock.getBlock.canPlaceBlockOnSide(world, pos, facing))
      if (MultipartHelper.addPart(world, pos, slot, state, false)) {
        if (!world.isRemote) {
          val info = MultipartHelper.getContainer(world, pos).flatMap((c: IMultipartContainer) => c.get(slot)).orElse(null)
          if (info != null) {
            ItemBlockMultipart.setMultipartTileNBT(player, stack, info)
            multipartBlock match {
              case b: BlockMultipart => b.onPartPlacedBy(info, player, stack, facing)
              case b => b.onPartPlacedBy(info, player, stack)
            }
          }
        }
        return true
      }
    false
  }
}
