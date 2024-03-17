package org.jetbrains.plugins.bsp.extension.points

import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.intellij.openapi.extensions.ExtensionPointName

public interface GoSdkExtension {
    public fun addGoSdk(
        goSdk: GoSdk,
        goSdkService: GoSdkService,
    )
}

private val ep = ExtensionPointName.create<GoSdkExtension>(
    "org.jetbrains.bsp.GoSdkExtension",
)

public fun goSdkExtension(): GoSdkExtension? = ep.extensionList.firstOrNull()

public fun goSdkExtensionExists(): Boolean = ep.extensionList.isNotEmpty()

public class GoSdkGetter : GoSdkExtension {
    override fun addGoSdk(
        goSdk: GoSdk,
        goSdkService: GoSdkService,
    ) {
        goSdkService.setSdk(goSdk)
    }
}
