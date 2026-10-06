package com.gtladd.gtladditions.api.machine

import com.hepdd.gtmthings.api.capability.IBindable
import java.util.UUID

/** Cloud binding can be cleared without changing GTMThings' read-only binding contract. */
interface ICloudTeamBindable : IBindable {
    fun setUUID(uuid: UUID?)
}
