package com.woocommerce.android.ui.products

import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

@OpenClassOnDebug
class ProductImagesRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    fun getProduct(remoteProductId: Long): Product? =
            productStore.getProductByRemoteId(selectedSite.get(), remoteProductId)?.toAppModel()
}
