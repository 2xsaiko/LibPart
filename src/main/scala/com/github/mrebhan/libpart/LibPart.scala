package com.github.mrebhan.libpart

import net.minecraftforge.fml.common.event.{FMLInitializationEvent, FMLPostInitializationEvent, FMLPreInitializationEvent}
import net.minecraftforge.fml.common.{Loader, Mod, SidedProxy}
import org.apache.logging.log4j.{LogManager, Logger}

/**
  * Created by marco on 24.02.17.
  */
@Mod(modid = LibPart.MODID, name = LibPart.NAME, version = LibPart.VERSION, modLanguage = "scala")
object LibPart {
  final val MODID = "libpart"
  final val VERSION = "1.0.0"
  final val NAME = "LibPart"

  val LOGGER: Logger = LogManager.getLogger(MODID)

  lazy val multipartCompat: Boolean = Loader.isModLoaded("mcmultipart")

  @SidedProxy(clientSide = "com.github.mrebhan.libpart.client.Proxy", serverSide = "com.github.mrebhan.libpart.common.Proxy")
  var proxy: com.github.mrebhan.libpart.common.Proxy = _

  @Mod.EventHandler def onPreInit(e: FMLPreInitializationEvent): Unit = proxy.preInit(e)

  @Mod.EventHandler def onInit(e: FMLInitializationEvent): Unit = proxy.init(e)

  @Mod.EventHandler def onPostInit(e: FMLPostInitializationEvent): Unit = proxy.postInit(e)

}