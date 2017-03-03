package com.github.mrebhan.libpart.common

import com.github.mrebhan.libpart.LibPart
import com.github.mrebhan.libpart.common.block.BlockPart
import com.github.mrebhan.libpart.common.item.ItemBlockExtended
import com.github.mrebhan.libpart.common.part.IPart
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.item.Item
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.registry.GameRegistry

import scala.collection.mutable

/**
  * Created by marco on 24.02.17.
  */
object Registry {

  private val map = new mutable.HashMap[ResourceLocation, Class[_ <: IPart]]()
  private val revMap = new mutable.HashMap[Class[_ <: IPart], ResourceLocation]()
  private val blocksMap = new mutable.HashMap[ResourceLocation, BlockPart]()
  private val itemsMap = new mutable.HashMap[ResourceLocation, List[Item]]()

  //         ↓  I don't know if this does what I think it does
  private[libpart] lazy val multipartQueue = new mutable.Queue[AnyRef]()

  def registerPart(clazz: Class[_ <: IPart], path: String, asMultipart: Boolean = true, toitem: (BlockPart, ResourceLocation) => List[Item] = std_toitem): Item = {
    if (clazz == null) throw new IllegalArgumentException("Part class must not be null!")
    if (path == null) throw new IllegalArgumentException("Path must not be null!")
    val rl = getResourceLocation(path)
    if (map.contains(rl)) {
      throw new IllegalArgumentException(s"$rl is already registered to ${map(rl)}!")
    }
    if (revMap.contains(clazz)) {
      throw new IllegalArgumentException(s"$clazz is already registered to ${revMap(clazz)}!")
    }
    map.put(rl, clazz)
    revMap.put(clazz, rl)
    val block = new BlockPart(rl)
    blocksMap.put(rl, block)
    GameRegistry.register(block)
    val items = toitem(block, rl)
    if (asMultipart) {
      if (LibPart.multipartCompat) {
        multipartQueue += block
        multipartQueue ++= items
      } else {
        LibPart.LOGGER.error(s"MCMultipart is not loaded! The part $path ($clazz) could not be registered as a multipart. Some functionality of the block will not be present.")
      }
    }
    for (item <- items) GameRegistry.register(item)

    itemsMap.put(rl, items)

    items.head
  }

  private def std_toitem(b: BlockPart, rl: ResourceLocation) = new ItemBlockExtended(b).setRegistryName(rl).setUnlocalizedName(rl.toString) :: Nil

  def getPartClass(rl: ResourceLocation): Class[_ <: IPart] = map.get(rl).orNull

  def getPartType(clazz: Class[_ <: IPart]): ResourceLocation = revMap.get(clazz).orNull

  def getDefaultState(part: IPart): BlockStateContainer = getDefaultState(part.getType)

  def getDefaultState(rl: ResourceLocation): BlockStateContainer = getBlock(rl).getBlockState

  def getBlock(part: IPart): BlockPart = getBlock(part.getType)

  def getBlock(rl: ResourceLocation): BlockPart = blocksMap.get(rl).orNull

  private def getResourceLocation(identifier: String): ResourceLocation = {
    if (identifier.contains(":")) new ResourceLocation(identifier)
    else new ResourceLocation(Loader.instance.activeModContainer.getModId, identifier)
  }

}
