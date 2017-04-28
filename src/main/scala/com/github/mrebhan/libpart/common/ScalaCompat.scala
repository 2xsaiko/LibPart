package com.github.mrebhan.libpart.common

import java.util.concurrent.Callable
import java.util.function.{Consumer, Function => JFunction, Predicate => JPredicate}

import mcmultipart.api.item.ItemBlockMultipart.{IBlockPlacementLogic, IExtendedBlockPlacementInfo, IPartPlacementLogic}
import mcmultipart.api.multipart.IMultipart
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.{EnumFacing, EnumHand}
import net.minecraft.world.World

/**
  * Created by marco on 27.04.17.
  * This is used as a helper class until Scala 2.12 is available in Forge.
  */
object ScalaCompat {

  implicit def Lambda2Function[A, B](op: A => B): JFunction[A, B] = new JFunction[A, B] {
    override def apply(t: A): B = op(t)
  }

  implicit def Lambda2Predicate[A](op: A => Boolean): JPredicate[A] = new JPredicate[A] {
    override def test(t: A): Boolean = op(t)
  }

  implicit def Lambda2IExtendedBlockPlacementInfo(op: (World, BlockPos, EnumFacing, Float, Float, Float, Int, EntityLivingBase, EnumHand, IBlockState) => IBlockState): IExtendedBlockPlacementInfo = new IExtendedBlockPlacementInfo {
    override def getStateForPlacement(world: World, pos: BlockPos, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, meta: Int, placer: EntityLivingBase, hand: EnumHand, state: IBlockState): IBlockState =
      op(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand, state)
  }

  implicit def Lambda2IBlockPlacementLogic(op: (ItemStack, EntityPlayer, World, BlockPos, EnumFacing, Float, Float, Float, IBlockState) => Boolean): IBlockPlacementLogic = new IBlockPlacementLogic {
    override def place(stack: ItemStack, player: EntityPlayer, world: World, pos: BlockPos, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, newState: IBlockState): Boolean =
      op(stack, player, world, pos, facing, hitX, hitY, hitZ, newState)
  }

  implicit def Lambda2IPartPlacementLogic(op: (ItemStack, EntityPlayer, EnumHand, World, BlockPos, EnumFacing, Float, Float, Float, IMultipart, IBlockState) => Boolean): IPartPlacementLogic = new IPartPlacementLogic {
    override def placePart(stack: ItemStack, player: EntityPlayer, hand: EnumHand, world: World, pos: BlockPos, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, multipartBlock: IMultipart, state: IBlockState): Boolean =
      op(stack, player, hand, world, pos, facing, hitX, hitY, hitZ, multipartBlock, state)
  }

  implicit def Lambda2Callable[V](op: () => V): Callable[V] = new Callable[V] {
    override def call(): V = op()
  }

  implicit def Lambda2Consumer[V](op: V => Unit): Consumer[V] = new Consumer[V] {
    override def accept(t: V): Unit = op(t)
  }

}
