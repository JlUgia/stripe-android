package com.stripe.android.paymentsheet.example.samples.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.flowcontroller.rememberPaymentSheetFlowController
import com.stripe.android.paymentsheet.model.PaymentOption
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class LaunchPaymentSheetCustomActivity : BasePaymentSheetActivity() {

    private val isLoading = MutableLiveData(true)
    private val paymentCompleted = MutableLiveData(false)
    private val selectedPaymentMethod = MutableLiveData<PaymentOption?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedPaymentMethodLabel = selectedPaymentMethod.map {
            it?.label ?: resources.getString(R.string.select)
        }
        val selectedPaymentMethodIcon = selectedPaymentMethod.map {
            it?.icon()
        }

        setContent {
            MaterialTheme {
                val flowController = rememberPaymentSheetFlowController(
                    paymentOptionCallback = ::onPaymentOption,
                    paymentResultCallback = ::onPaymentSheetResult,
                )

                LaunchedEffect(Unit) {
                    val (customerConfig, clientSecret) = prepareCheckout()

                    val error = flowController.configureWithPaymentIntent(
                        paymentIntentClientSecret = clientSecret,
                        configuration = makeConfiguration(customerConfig),
                    )

                    if (error != null) {
                        viewModel.status.postValue(
                            "Failed to configure PaymentSheetFlowController: ${error.message}"
                        )
                    } else {
                        onPaymentOption(flowController.getPaymentOption())
                    }
                }

                val isLoadingState by isLoading.observeAsState(true)
                val paymentCompletedState by paymentCompleted.observeAsState(false)
                val status by viewModel.status.observeAsState("")
                val paymentMethodLabel by selectedPaymentMethodLabel.observeAsState(stringResource(R.string.loading))
                val paymentMethodIcon by selectedPaymentMethodIcon.observeAsState()

                if (status.isNotBlank()) {
                    snackbar.setText(status).show()
                    viewModel.statusDisplayed()
                }

                Receipt(isLoadingState) {
                    PaymentMethodSelector(
                        isEnabled = !isLoadingState && !paymentCompletedState,
                        paymentMethodLabel = paymentMethodLabel,
                        paymentMethodIcon = paymentMethodIcon,
                        onClick = {
                            flowController.presentPaymentOptions()
                        }
                    )
                    BuyButton(
                        buyButtonEnabled = !isLoadingState && !paymentCompletedState &&
                            selectedPaymentMethod.value != null,
                        onClick = {
                            isLoading.value = true
                            flowController.confirm()
                        }
                    )
                }
            }
        }
    }

    private fun makeConfiguration(
        customerConfig: PaymentSheet.CustomerConfiguration? = null
    ): PaymentSheet.Configuration {
        return PaymentSheet.Configuration(
            merchantDisplayName = merchantName,
            customer = customerConfig,
            googlePay = googlePayConfig,
            // Set `allowsDelayedPaymentMethods` to true if your business can handle payment
            // methods that complete payment after a delay, like SEPA Debit and Sofort.
            allowsDelayedPaymentMethods = true
        )
    }

    private fun onPaymentOption(paymentOption: PaymentOption?) {
        isLoading.value = false
        selectedPaymentMethod.value = paymentOption
    }

    override fun onPaymentSheetResult(
        paymentResult: PaymentSheetResult
    ) {
        super.onPaymentSheetResult(paymentResult)

        isLoading.value = false
        if (paymentResult !is PaymentSheetResult.Canceled) {
            paymentCompleted.value = true
        }
    }
}

private suspend fun PaymentSheet.FlowController.configureWithPaymentIntent(
    paymentIntentClientSecret: String,
    configuration: PaymentSheet.Configuration? = null,
): Throwable? {
    return suspendCancellableCoroutine { continuation ->
        configureWithPaymentIntent(
            paymentIntentClientSecret = paymentIntentClientSecret,
            configuration = configuration,
            callback = { _, error ->
                continuation.resume(error)
            }
        )
    }
}
