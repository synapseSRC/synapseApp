package com.synapse.social.studioasinc.data.local

import android.os.Build
import com.synapse.social.studioasinc.domain.repository.DeviceInfoProvider
import javax.inject.Inject

class AndroidDeviceInfoProvider @Inject constructor() : DeviceInfoProvider {
    override fun getDeviceModel(): String = Build.MODEL
}
