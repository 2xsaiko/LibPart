package com.github.mrebhan.libpart.common.part;

import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;

/**
 * Created by marco on 10.03.17.
 */
public interface ICustomIntersect {

    List<AxisAlignedBB> getIntersectionBB(IPart other);

}
