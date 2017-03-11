package com.github.mrebhan.libpart.common.block

import com.github.mrebhan.libpart.LibPart
import com.github.mrebhan.libpart.common.Registry
import com.github.mrebhan.libpart.common.mcmpcompat.TileMultipart
import com.github.mrebhan.libpart.common.part.IPart
import io.netty.buffer.Unpooled
import mcmultipart.api.ref.MCMPCapabilities
import net.minecraft.block.state.IBlockState
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.network.{NetworkManager, PacketBuffer}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{EnumFacing, ITickable, ResourceLocation}
import net.minecraftforge.common.capabilities.Capability
import org.apache.logging.log4j.Level

/**
  * Created by marco on 27.02.17.
  */
class TilePart(var rl: ResourceLocation) extends TileEntity {

  def this() = this(null)

  private var part: IPart = if (rl == null) null else createPart()

  private lazy val mp = new TileMultipart(this)

  private var rerender = false

  def getPart: IPart = part

  def writePartInfo(nbt: NBTTagCompound): Unit = {
    val partNBT = new NBTTagCompound
    part.writeToNBT(partNBT)
    if (!partNBT.hasNoTags)
      nbt.setTag("PartData", partNBT)
  }

  def readPartInfo(nbt: NBTTagCompound): Unit = {
    val partNBT = nbt.getCompoundTag("PartData")
    part.readFromNBT(partNBT)
  }

  override def writeToNBT(nbt: NBTTagCompound): NBTTagCompound = {
    super.writeToNBT(nbt)
    nbt.setString("PartType", rl.toString)
    writePartInfo(nbt)
    nbt
  }

  override def readFromNBT(nbt: NBTTagCompound): Unit = {
    super.readFromNBT(nbt)
    rl = new ResourceLocation(nbt.getString("PartType"))
    part = createPart()
    readPartInfo(nbt)
  }

  override def handleUpdateTag(nbt: NBTTagCompound): Unit = {
    part.readUpdatePacket(new PacketBuffer(Unpooled.wrappedBuffer(nbt.getByteArray("PartData"))))
    rl = new ResourceLocation(nbt.getString("PartType"))
    if (nbt.getBoolean("ShouldRefresh")) {
      getWorld.markBlockRangeForRenderUpdate(getPos, getPos)
    }
  }

  override def onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity): Unit = {
    handleUpdateTag(pkt.getNbtCompound)
  }

  override def getUpdateTag: NBTTagCompound = {
    val tag = super.getUpdateTag
    val buf = new PacketBuffer(Unpooled.buffer())
    part.writeUpdatePacket(buf)
    val arr = new Array[Byte](buf.writerIndex())
    System.arraycopy(buf.array(), buf.arrayOffset(), arr, 0, arr.length)
    tag.setByteArray("PartData", arr)
    tag.setString("PartType", rl.toString)
    tag.setBoolean("ShouldRefresh", rerender)
    rerender = false
    tag
  }

  override def getUpdatePacket: SPacketUpdateTileEntity = {
    new SPacketUpdateTileEntity(getPos, 0, getUpdateTag)
  }

  def updateClient(reRenderClient: Boolean): Unit = {
    if (getWorld != null && !getWorld.isRemote) {
      val actualState = part.getActualState(part.getBaseState)
      getWorld.notifyBlockUpdate(getPos, actualState, actualState, 3)
      rerender = reRenderClient
      LibPart.LOGGER.log(Level.INFO, "Sending update packet!")
    }
  }

  override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean = {
    capability match {
      case MCMPCapabilities.MULTIPART_TILE => true
      case _ => part.hasCapability(capability, facing) || super.hasCapability(capability, facing)
    }
  }

  override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = {
    capability match {
      case MCMPCapabilities.MULTIPART_TILE => mp.asInstanceOf[T]
      case _ => Option.apply(part.getCapability(capability, facing)).getOrElse(super.getCapability(capability, facing))
    }
  }

  def updateClient(): Unit = updateClient(true)

  def createPart(): IPart = Registry.getBlock(rl).createPart().setContainer(this)

  def getPartID: ResourceLocation = rl

  def setPlacedState(state: IBlockState): Unit = {
    part.handlePlacedState(state)
  }

}

object TilePart {

  class Ticking(rl: ResourceLocation) extends TilePart(rl) with ITickable {
    def this() = this(null)

    override def update(): Unit = getPart.asInstanceOf[ITickable].update()
  }

}
