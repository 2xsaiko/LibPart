package com.github.mrebhan.libpart.common.mcmpcompat

import com.github.mrebhan.libpart.LibPart
import com.github.mrebhan.libpart.common.Registry
import com.github.mrebhan.libpart.common.block.{BlockPart, TilePart}
import mcmultipart.MCMultiPart
import mcmultipart.api.addon.{IMCMPAddon, MCMPAddon}
import mcmultipart.api.container.IPartInfo
import mcmultipart.api.item.ItemBlockMultipart
import mcmultipart.api.multipart.{IMultipart, IMultipartRegistry, IMultipartTile, MultipartHelper}
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
  //  override def registerParts(registry: IMultipartRegistry): Unit = {
  //    for (any <- Registry.multipartQueue) {
  //      any match {
  //        case x: (BlockPart, ResourceLocation, ItemBuilder => List[ItemBlock], ItemBlock => Unit) =>
  //          val multipart = new BlockMultipart(x._1)
  //          registry.registerPartWrapper(x._1, multipart)
  //          val mib = new MultipartItemBuilder(x._1, x._2, multipart)
  //          val items = x._3(mib)
  //          for (item <- items) GameRegistry.register(item)
  //          Registry.itemsMap.put(x._2, items)
  //          x._4(items.head)
  //        case _ => LibPart.LOGGER.warn(s"Invalid object '$any' in multipart registry queue, ignoring.")
  //      }
  //    }
  //  }

  override def registerParts(registry: IMultipartRegistry): Unit = {
    for (any <- Registry.multipartQueue) {
      any match {
        case block: BlockPart =>
          registry.registerPartWrapper(block, new BlockMultipart(block))
        case item: ItemBlock =>
          val block = item.getBlock.asInstanceOf[BlockPart]
          val wrappedblock = registry.registerStackWrapper(item, _ => true, block)
          wrappedblock.setPlacementInfo((world, pos, facing, hitX, hitY, hitZ, meta, placer, hand, state) =>
            block.defaultPart.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand, state))
          wrappedblock.setBlockPlacementLogic((stack, player, world, pos, facing, hitX, hitY, hitZ, newState) =>
            player.canPlayerEdit(pos, facing, stack) &&
              world.getBlockState(pos).getBlock.isReplaceable(world, pos) &&
              block.canPlaceBlockAt(world, pos) &&
              block.canPlaceBlockOnSide(world, pos, facing) &&
              item.placeBlockAt(stack, player, world, pos, facing, hitX, hitY, hitZ, newState))
          wrappedblock.setPartPlacementLogic(MPUtils.placePartAt)
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

  override def getSlotFromWorld(world: IBlockAccess, pos: BlockPos, state: IBlockState): IPartSlot = block.defaultPart.getSlotFromWorld(world, pos, state)

  override def getSlotForPlacement(world: World, pos: BlockPos, state: IBlockState, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, placer: EntityLivingBase): IPartSlot =
    block.defaultPart.getSlotForPlacement(world, pos, state, facing, hitX, hitY, hitZ, placer)

  override def getBoundingBox(part: IPartInfo): AxisAlignedBB = part.getTile.getTileEntity.asInstanceOf[TilePart].getPart.getSelectionBox

  def onPartPlacedBy(part: IPartInfo, placer: EntityLivingBase, stack: ItemStack, facing: EnumFacing): Unit = {
    block.onBlockPlacedBy(part.getWorld, part.getPos, part.getState, placer, stack, facing)
  }
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

  def placePartAt(stack: ItemStack, player: EntityPlayer, hand: EnumHand, world: World, pos: BlockPos, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, multipartBlock: IMultipart, state: IBlockState): Boolean = {
    val slot = multipartBlock.getSlotForPlacement(world, pos, state, facing, hitX, hitY, hitZ, player)
    if (multipartBlock.getBlock.canPlaceBlockOnSide(world, pos, facing))
      if (MultipartHelper.addPart(world, pos, slot, state, false)) {
        if (!world.isRemote) {
          val info = MultipartHelper.getContainer(world, pos).flatMap(c => c.get(slot)).orElse(null)
          if (info != null) {
            ItemBlockMultipart.setMultipartTileNBT(player, stack, info)
            multipartBlock.onPartPlacedBy(info, player, stack)
          }
        }
        return true
      }
    false
  }
}
