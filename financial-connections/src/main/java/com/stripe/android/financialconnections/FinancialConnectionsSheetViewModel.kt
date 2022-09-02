package com.stripe.android.financialconnections

import android.content.Intent
import android.net.Uri
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnectionsSheetState.AuthFlowStatus
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.FinishWithResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEventReporter
import com.stripe.android.financialconnections.di.APPLICATION_ID
import com.stripe.android.financialconnections.di.DaggerFinancialConnectionsSheetComponent
import com.stripe.android.financialconnections.domain.FetchFinancialConnectionsSession
import com.stripe.android.financialconnections.domain.FetchFinancialConnectionsSessionForToken
import com.stripe.android.financialconnections.domain.GenerateFinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.ForData
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.ForLink
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.ForToken
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Canceled
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Completed
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Named

@Suppress("LongParameterList", "TooManyFunctions")
internal class FinancialConnectionsSheetViewModel @Inject constructor(
    @Named(APPLICATION_ID) private val applicationId: String,
    private val generateFinancialConnectionsSessionManifest: GenerateFinancialConnectionsSessionManifest,
    private val fetchFinancialConnectionsSession: FetchFinancialConnectionsSession,
    private val fetchFinancialConnectionsSessionForToken: FetchFinancialConnectionsSessionForToken,
    private val logger: Logger,
    private val eventReporter: FinancialConnectionsEventReporter,
    initialState: FinancialConnectionsSheetState
) : MavericksViewModel<FinancialConnectionsSheetState>(initialState) {

    private val mutex = Mutex()

    init {
        eventReporter.onPresented(initialState.initialArgs.configuration)
        // avoid re-fetching manifest if already exists (this will happen on process recreations)
        if (initialState.manifest == null) {
            fetchManifest()
        }
        viewModelScope.launch {
            stateFlow.collect { logger.debug("STATE: ${it.authFlowStatus}, recreated: ${it.activityRecreated}") }
        }
    }

    /**
     * Fetches the [FinancialConnectionsSessionManifest] from the Stripe API to get the hosted auth flow URL
     * as well as the success and cancel callback URLs to verify.
     */
    private fun fetchManifest() {
        withState { state ->
            viewModelScope.launch {
                kotlin.runCatching {
                    state.initialArgs.validate()
                    generateFinancialConnectionsSessionManifest(
                        clientSecret = state.sessionSecret,
                        applicationId = applicationId
                    )
                }.onFailure {
                    onFatal(state, it)
                }.onSuccess {
                    openAuthFlow(it)
                }
            }
        }
    }

    /**
     * Builds the ChromeCustomTab intent to launch the hosted auth flow and launches it.
     *
     * @param manifest the manifest containing the hosted auth flow URL to launch
     *
     */
    private fun openAuthFlow(manifest: FinancialConnectionsSessionManifest) {
        // stores manifest in state for future references.
        setState {
            copy(
                manifest = manifest,
                authFlowStatus = AuthFlowStatus.WEB,
                viewEffect = OpenAuthFlowWithUrl(manifest.hostedAuthUrl)
            )
        }
    }

    /**
     * Activity recreation changes the lifecycle order:
     *
     * If config change happens while in web flow:
     * - onResume -> onNewIntent -> activityResult -> onResume(again)
     * If no config change happens:
     * - onActivityResult -> onNewIntent -> onResume
     *
     * (note [handleOnNewIntent] will just get called if user completed the web flow and clicked
     * the deeplink that redirects back to the app)
     *
     * We need to rely on a post-onNewIntent lifecycle callback to figure if the user completed
     * or cancelled the web flow. [FinancialConnectionsSheetState.activityRecreated] will be used to
     * figure which lifecycle callback happens after onNewIntent.
     *
     * @see onResume (we rely on this on regular flows)
     * @see onActivityResult (we rely on this on config changes)
     */
    fun onActivityRecreated() {
        setState {
            copy(
                activityRecreated = true
            )
        }
    }

    /**
     *  If activity resumes and we did not receive a callback from the custom tabs,
     *  then the user hit the back button or closed the custom tabs UI, so return result as
     *  canceled.
     */
    internal fun onResume() {
        viewModelScope.launch {
            mutex.withLock {
                setState {
                    logger.debug("STATE: entering onResume, status = $authFlowStatus")
                    if (activityRecreated.not()) {
                        when (authFlowStatus) {
                            AuthFlowStatus.WEB -> copy(viewEffect = FinishWithResult(Canceled))
                            AuthFlowStatus.APP2APP -> copy(authFlowStatus = AuthFlowStatus.WEB)
                            AuthFlowStatus.NONE -> this
                        }
                    } else this
                }
            }
        }
    }

    /**
     * If activity receives result and we did not receive a callback from the custom tabs,
     * if activity got recreated and the auth flow is still active then the user hit
     * the back button or closed the custom tabs UI, so return result as canceled.
     */
    internal fun onActivityResult() {
        viewModelScope.launch {
            mutex.withLock {
                setState {
                    logger.debug("STATE: entering onActivityResult, status = $authFlowStatus")
                    if (activityRecreated) {
                        when (authFlowStatus) {
                            AuthFlowStatus.WEB -> copy(viewEffect = FinishWithResult(Canceled))
                            AuthFlowStatus.APP2APP -> copy(authFlowStatus = AuthFlowStatus.WEB)
                            AuthFlowStatus.NONE -> this
                        }
                    } else this
                }
            }
        }
    }

    /**
     * For regular connections flows requesting a session:
     *
     * On successfully completing the hosted auth flow and receiving the success callback intent,
     * fetch the updated [FinancialConnectionsSession] model from the API
     * and return it back as a [Completed] result.
     */
    private fun fetchFinancialConnectionsSession(state: FinancialConnectionsSheetState) {
        viewModelScope.launch {
            kotlin.runCatching {
                fetchFinancialConnectionsSession(state.sessionSecret)
            }.onSuccess {
                val result = Completed(financialConnectionsSession = it)
                eventReporter.onResult(state.initialArgs.configuration, result)
                setState { copy(viewEffect = FinishWithResult(result)) }
            }.onFailure {
                onFatal(state, it)
            }
        }
    }

    /**
     * For connections flows requesting an account [com.stripe.android.model.Token]:
     *
     * On successfully completing the hosted auth flow and receiving the success callback intent,
     * fetch the updated [FinancialConnectionsSession] and the generated [com.stripe.android.model.Token]
     * and return it back as a [Completed] result.
     */
    private fun fetchFinancialConnectionsSessionForToken(state: FinancialConnectionsSheetState) {
        viewModelScope.launch {
            kotlin.runCatching {
                fetchFinancialConnectionsSessionForToken(clientSecret = state.sessionSecret)
            }.onSuccess { (las, token) ->
                val result = Completed(financialConnectionsSession = las, token = token)
                eventReporter.onResult(state.initialArgs.configuration, result)
                setState { copy(viewEffect = FinishWithResult(result)) }
            }.onFailure {
                onFatal(state, it)
            }
        }
    }

    /**
     * If an error occurs during the auth flow, return that error via the
     * [FinancialConnectionsSheetResultCallback] and [FinancialConnectionsSheetResult.Failed].
     *
     * @param throwable the error encountered during the [FinancialConnectionsSheet] auth flow
     */
    private fun onFatal(state: FinancialConnectionsSheetState, throwable: Throwable) {
        val result = FinancialConnectionsSheetActivityResult.Failed(throwable)
        eventReporter.onResult(state.initialArgs.configuration, result)
        setState { copy(viewEffect = FinishWithResult(result)) }
    }

    /**
     * If a user cancels the hosted auth flow either by closing the custom tab with the back button
     * or clicking a cancel link within the hosted auth flow and the activity received the canceled
     * URL callback, notify the [FinancialConnectionsSheetResultCallback] with [Canceled]
     */
    private fun onUserCancel(state: FinancialConnectionsSheetState) {
        val result = Canceled
        eventReporter.onResult(state.initialArgs.configuration, result)
        setState { copy(viewEffect = FinishWithResult(result)) }
    }

    /**
     * The hosted auth flow will redirect to a URL scheme stripe-auth://link-accounts which will be
     * handled by the [FinancialConnectionsSheetActivity] per the intent filter in the Android manifest and
     * with the launch mode for the activity being `singleTask` it will trigger a new intent for the
     * activity which this method will receive
     *
     * @param intent the new intent with the redirect URL in the intent data
     */
    internal fun handleOnNewIntent(intent: Intent?) {
        viewModelScope.launch {
            mutex.withLock {
                val receivedUrl: Uri? = intent?.data?.toString()?.toUriOrNull()
                withState { state ->
                    when {
                        // stripe-auth://native-redirect
                        // TODO@carlosmuvi include applicationId!
                        receivedUrl?.host == "native-redirect" ->
                            onStartApp2App(receivedUrl)
                        // stripe-auth://link-accounts/login
                        // TODO@carlosmuvi example return_url subject to change.
                        // TODO@carlosmuvi include applicationId!
                        receivedUrl?.host == "link-accounts" && receivedUrl.path == "/login" ->
                            onReturnUrlReceived(receivedUrl)
                        // stripe-auth://link-accounts/{applicationId/success
                        receivedUrl?.buildUpon()?.clearQuery()
                            .toString() == state.manifest?.successUrl ->
                            onWebFlowSucceed(state, receivedUrl)
                        // stripe-auth://link-accounts/{applicationId/cancel
                        receivedUrl?.buildUpon()?.clearQuery()
                            .toString() == state.manifest?.cancelUrl -> {
                            onWebFlowCancelled(state)
                        }
                        else -> {
                            setState { copy(authFlowStatus = AuthFlowStatus.NONE) }
                            onFatal(
                                state,
                                Exception("Error processing FinancialConnectionsSheet intent")
                            )
                        }
                    }
                }
            }
        }
    }

    private fun onStartApp2App(receivedUrl: Uri) {
        setState {
            copy(
                authFlowStatus = AuthFlowStatus.APP2APP,
                viewEffect = OpenAuthFlowWithUrl(
                    receivedUrl.toString()
                        .replaceFirst("stripe-auth://native-redirect/", "")
                )
            )
        }
    }

    private fun onWebFlowCancelled(state: FinancialConnectionsSheetState) {
        setState { copy(authFlowStatus = AuthFlowStatus.NONE) }
        onUserCancel(state)
    }

    private fun onWebFlowSucceed(
        state: FinancialConnectionsSheetState,
        receivedUrl: Uri?
    ) {
        setState { copy(authFlowStatus = AuthFlowStatus.NONE) }
        when (state.initialArgs) {
            is ForData -> fetchFinancialConnectionsSession(state)
            is ForToken -> fetchFinancialConnectionsSessionForToken(state)
            is ForLink -> onSuccessFromLinkFlow(receivedUrl)
        }
    }

    private fun onReturnUrlReceived(receivedUrl: Uri) {
        setState {
            val authFlowResumeUrl =
                "${manifest!!.hostedAuthUrl}&startPolling=true&${receivedUrl.fragment}"
            copy(
                authFlowStatus = AuthFlowStatus.APP2APP,
                viewEffect = OpenAuthFlowWithUrl(authFlowResumeUrl)
            )
        }
    }

    /**
     * Link flows do not need to fetch the FC session, since the linked account id is
     * appended to the web success url.
     */
    private fun onSuccessFromLinkFlow(url: Uri?) {
        kotlin.runCatching {
            requireNotNull(url?.getQueryParameter(QUERY_PARAM_LINKED_ACCOUNT))
        }.onSuccess {
            setState { copy(viewEffect = FinishWithResult(Completed(linkedAccountId = it))) }
        }.onFailure { error ->
            logger.error("Could not retrieve linked account from success url", error)
            withState { state -> onFatal(state, error) }
        }
    }

    fun onViewEffectLaunched() {
        setState { copy(viewEffect = null) }
    }

    private fun String.toUriOrNull(): Uri? {
        Uri.parse(this).buildUpon().clearQuery()
        return kotlin.runCatching {
            return Uri.parse(this)
        }.onFailure {
            logger.error("Could not parse web flow url", it)
        }.getOrNull()
    }

    companion object :
        MavericksViewModelFactory<FinancialConnectionsSheetViewModel, FinancialConnectionsSheetState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: FinancialConnectionsSheetState
        ): FinancialConnectionsSheetViewModel {
            return DaggerFinancialConnectionsSheetComponent
                .builder()
                .application(viewModelContext.app())
                .initialState(state)
                .internalArgs(state.initialArgs)
                .build().viewModel
        }

        internal const val MAX_ACCOUNTS = 100
        internal const val QUERY_PARAM_LINKED_ACCOUNT = "linked_account"
    }
}
