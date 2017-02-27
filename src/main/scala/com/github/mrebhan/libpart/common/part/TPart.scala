package com.github.mrebhan.libpart.common.part

import com.github.mrebhan.libpart.common.Registry
import com.github.mrebhan.libpart.common.block.TilePart
import net.minecraft.util.ResourceLocation

/**
  * Created by marco on 24.02.17.
  */
trait TPart extends IPart {

  private var container: TilePart = _

  override def setContainer(pt: TilePart): TPart = {
    container = pt
    this
  }

  override def getContainer: TilePart = container

  override def getType: ResourceLocation = Registry.getPartType(getClass)

}

/**
  * Java compatibility class
  */

abstract class PartImpl extends TPart