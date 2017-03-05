package com.github.mrebhan.libpart.common.mcmpcompat

import com.github.mrebhan.libpart.LibPart
import com.github.mrebhan.libpart.common.Registry.DefaultItemBuilder
import com.github.mrebhan.libpart.common.block.{BlockPart, TilePart}
import com.github.mrebhan.libpart.common.item.TExtendedPlacement
import com.github.mrebhan.libpart.common.{ItemBuilder, Registry}
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
import net.minecraft.util.{EnumActionResult, EnumFacing, EnumHand, ResourceLocation}
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.fml.common.registry.GameRegistry
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

/**
  * Created by marco on 28.02.17.
  */
@MCMPAddon
class MCMPCompat extends IMCMPAddon {
  override def registerParts(registry: IMultipartRegistry): Unit = {
    for (any <- Registry.multipartQueue) {
      any match {
        case x: (BlockPart, ResourceLocation, ItemBuilder => List[ItemBlock], ItemBlock => Unit) =>
          val multipart = new BlockMultipart(x._1)
          registry.registerPartWrapper(x._1, multipart)
          val mib = new MultipartItemBuilder(x._1, x._2, multipart)
          val items = x._3(mib)
          for (item <- items) GameRegistry.register(item)
          Registry.itemsMap.put(x._2, items)
          x._4(items.head)
        case _ => LibPart.LOGGER.warn(s"Invalid object '$any' in multipart registry queue, ignoring.")
      }
    }
  }
}

class MultipartItemBuilder(blockIn: BlockPart, rl: ResourceLocation, multipartBlock: BlockMultipart) extends DefaultItemBuilder(blockIn, rl) {
  override def createItem(): ItemBlock = new ItemMultipartExtended(blockIn, multipartBlock)
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

class ItemMultipartExtended(block: BlockPart, multipartBlock: BlockMultipart) extends ItemBlockMultipart(block, multipartBlock) with TExtendedPlacement {

  override def placeBlockAtTested(stack: ItemStack, player: EntityPlayer, world: World, pos: BlockPos, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, newState: IBlockState): Boolean = {
    player.canPlayerEdit(pos, facing, stack) &&
      world.getBlockState(pos).getBlock.isReplaceable(world, pos) &&
      this.block.canPlaceBlockAt(world, pos) &&
      this.block.canPlaceBlockOnSide(world, pos, facing) &&
      placeBlockAt(stack, player, world, pos, facing, hitX, hitY, hitZ, newState)
  }

  override def placeBlockAt(stack: ItemStack, player: EntityPlayer, world: World, pos: BlockPos, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, newState: IBlockState): Boolean =
    placeBlockAtExt(stack, player, world, pos, side, hitX, hitY, hitZ, newState)

  override def onItemUse(player: EntityPlayer, world: World, pos: BlockPos, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): EnumActionResult = {
    ItemBlockMultipart.place(player, world, pos, hand, facing, hitX, hitY, hitZ, this, block.getStateForPlacement, this.multipartBlock, this.placeBlockAtTested, placePartAt)
  }

  def placePartAt(stack: ItemStack, player: EntityPlayer, hand: EnumHand, world: World, pos: BlockPos, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, multipartBlock: IMultipart, state: IBlockState): Boolean = {
    val slot = multipartBlock.getSlotForPlacement(world, pos, state, facing, hitX, hitY, hitZ, player)
    if (MultipartHelper.addPart(world, pos, slot, state, false)) {
      if (!world.isRemote) {
        val info = MultipartHelper.getContainer(world, pos).flatMap(c => c.get(slot)).orElse(null)
        if (info != null) {
          ItemBlockMultipart.setMultipartTileNBT(player, stack, info)
          this.multipartBlock.onPartPlacedBy(info, player, stack, facing)
        }
      }
      true
    }
    else false
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
}