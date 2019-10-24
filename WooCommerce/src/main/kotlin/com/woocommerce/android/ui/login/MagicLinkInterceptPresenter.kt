package com.woocommerce.android.ui.login

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import javax.inject.Inject

class MagicLinkInterceptPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore
) : MagicLinkInterceptContract.Presenter {
    private var mainView: MagicLinkInterceptContract.View? = null

    private var isHandlingMagicLink: Boolean = false

    override fun takeView(view: MagicLinkInterceptContract.View) {
        mainView = view
        dispatcher.register(this)
    }

    override fun dropView() {
        mainView = null
        dispatcher.unregister(this)
    }

    override fun storeMagicLinkToken(token: String) {
        // Save Token to the AccountStore. This will trigger an OnAuthenticationChanged.
        isHandlingMagicLink = true
        mainView?.showProgressDialog()
        dispatcher.dispatch(AccountActionBuilder.newUpdateAccessTokenAction(UpdateTokenPayload(token)))
    }

    override fun userIsLoggedIn() = accountStore.hasAccessToken()

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAuthenticationChanged(event: OnAuthenticationChanged) {
        if (event.isError) {
            // TODO Handle AuthenticationErrorType.INVALID_TOKEN
            isHandlingMagicLink = false
            return
        }

        if (userIsLoggedIn()) {
            // This means a login via magic link was performed, and the access token was just updated
            // In this case, we need to fetch account details and the site list, and finally notify the view
            // In all other login cases, this logic is handled by the login library
            mainView?.notifyMagicLinkTokenUpdated()
            dispatcher.dispatch(AccountActionBuilder.newFetchAccountAction())
        } else {
            mainView?.hideProgressDialog()
            mainView?.showLoginScreen()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (event.isError) {
            mainView?.hideProgressDialog()

            // TODO: Notify the user of the problem
            isHandlingMagicLink = false
            return
        }

        if (isHandlingMagicLink) {
            if (event.causeOfChange == AccountAction.FETCH_ACCOUNT) {
                // The user's account info has been fetched and stored - next, fetch the user's settings
                dispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction())
            } else if (event.causeOfChange == AccountAction.FETCH_SETTINGS) {
                // The user's account settings have also been fetched and stored - now we can fetch the user's sites
                dispatcher.dispatch(SiteActionBuilder.newFetchSitesAction())
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteChanged(event: OnSiteChanged) {
        mainView?.hideProgressDialog()

        if (event.isError) {
            // TODO: Notify the user of the problem
            isHandlingMagicLink = false
            return
        }

        if (isHandlingMagicLink) {
            // Magic link login is now complete - notify the view to set the selected site and proceed with loading UI
            mainView?.showSitePickerScreen()
            isHandlingMagicLink = false
        }
    }
}
