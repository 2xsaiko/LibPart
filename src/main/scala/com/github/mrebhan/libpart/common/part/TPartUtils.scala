package com.github.mrebhan.libpart.common.part

import com.github.mrebhan.libpart.LibPart
import com.github.mrebhan.libpart.common.block.TilePart
import com.github.mrebhan.libpart.common.mcmpcompat.MPUtils
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess

/**
  * Created by marco on 11.03.17.
  */
trait TPartUtils {

  def getTileAt(world: IBlockAccess, pos: BlockPos, warn: Boolean = true): TilePart =
    world.getTileEntity(pos) match {
      case te: TilePart => te
      case _ =>
        if (world.isInstanceOf[WorldClient] && LibPart.multipartCompat) {
          MPUtils.getTileFromHit(world, pos)
        } else {
          if (warn) LibPart.LOGGER.warn(s"There's no tile at $pos when there should be!")
          null
        }
    }

  def getPartAt(world: IBlockAccess, pos: BlockPos, warn: Boolean = true, giveMeDefault: Boolean = true): IPart =
    getTileAt(world, pos, warn) match {
      case te: TilePart => te.getPart
      case _ =>
        if (warn) LibPart.LOGGER.warn(s"There's no part at $pos when there should be!")
        if (giveMeDefault) defaultPart else null
    }

  def defaultPart: IPart = ???

}
