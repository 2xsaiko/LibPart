package com.github.mrebhan.libpart.common

import com.github.mrebhan.libpart.LibPart
import com.github.mrebhan.libpart.common.block.BlockPart
import com.github.mrebhan.libpart.common.item.ItemBlockExtended
import com.github.mrebhan.libpart.common.part.IPart
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.item.{Item, ItemBlock}
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
  private[libpart] val itemsMap = new mutable.HashMap[ResourceLocation, List[Item]]()
  private[libpart] lazy val multipartQueue = new mutable.Queue[AnyRef]()

  def registerPart(clazz: Class[_ <: IPart], path: String, asMultipart: Boolean = true,
                   toitem: ItemBuilder => List[ItemBlock] = std_toitem): List[ItemBlock] = {
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
    val items = toitem(new DefaultItemBuilder(block, rl))
    for (item <- items) GameRegistry.register(item)
    itemsMap.put(rl, items)
    if (asMultipart) {
      if (LibPart.multipartCompat) multipartQueue ++= block :: items
      else LibPart.LOGGER.error(s"MCMultipart is not loaded! The part $path ($clazz) could not be registered as a multipart. Some functionality of the block will not be present.")
    }
    items
  }

  private def std_toitem(ib: ItemBuilder): List[ItemBlock] = {
    val item = ib.createItem()
    item.setRegistryName(ib.resource)
    item.setUnlocalizedName(ib.resource.toString)
    item :: Nil
  }

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

  class DefaultItemBuilder(blockIn: BlockPart, rl: ResourceLocation) extends ItemBuilder {
    override def createItem(): ItemBlock = new ItemBlockExtended(block)

    override def block: BlockPart = blockIn

    override def resource: ResourceLocation = rl
  }

}

trait ItemBuilder {
  def block: BlockPart

  def resource: ResourceLocation

  def createItem(): ItemBlock
}