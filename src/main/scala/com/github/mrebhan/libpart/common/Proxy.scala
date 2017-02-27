package com.github.mrebhan.libpart.common

import com.github.mrebhan.libpart.common.block.TilePart
import net.minecraftforge.fml.common.event.{FMLInitializationEvent, FMLPostInitializationEvent, FMLPreInitializationEvent}
import net.minecraftforge.fml.common.registry.GameRegistry

/**
  * Created by marco on 24.02.17.
  */
class Proxy {

  def preInit(e: FMLPreInitializationEvent): Unit = {
    GameRegistry.registerTileEntity(classOf[TilePart], "libpart:part")
    GameRegistry.registerTileEntity(classOf[TilePart.Ticking], "libpart:part_ticking")
  }

  def init(e: FMLInitializationEvent): Unit = ()

  def postInit(e: FMLPostInitializationEvent): Unit = ()

}
