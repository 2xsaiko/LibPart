package com.github.mrebhan.libpart.common

import com.github.mrebhan.libpart.common.block.BlockPart
import com.github.mrebhan.libpart.common.part.IPart
import mcmultipart.api.item.ItemBlockMultipart
import mcmultipart.multipart.MultipartRegistry
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.item.ItemBlock
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

  def registerPart(clazz: Class[_ <: IPart], path: String, asMultipart: Boolean = true): ItemBlock = {
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
    val item =
      if (asMultipart) {
        MultipartRegistry.INSTANCE.registerPartWrapper(block, block.Multipart)
        new ItemBlockMultipart(block, block.Multipart)
      } else {
        new ItemBlock(block)
      }
    item.setRegistryName(rl)
    item.setUnlocalizedName(rl.toString)
    GameRegistry.register(item)

    item
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

}
