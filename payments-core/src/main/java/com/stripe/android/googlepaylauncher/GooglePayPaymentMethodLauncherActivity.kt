package com.stripe.android.googlepaylauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.contract.ApiTaskResult
import com.google.android.gms.wallet.contract.TaskResultContracts.GetPaymentDataResult
import com.stripe.android.model.PaymentMethod
import com.stripe.android.utils.fadeOut
import kotlinx.coroutines.launch

/**
 * [GooglePayPaymentMethodLauncherActivity] is used to return the result of a Google Pay operation.
 * The activity's result will be a [GooglePayPaymentMethodLauncher.Result].
 *
 * [GooglePayPaymentMethodLauncherActivity] will:
 * 1. Verify that Google Pay is available
 * 2. Create a [PaymentDataRequest](https://developers.google.com/pay/api/web/reference/request-objects#PaymentDataRequest)
 * 3. Create a [PaymentMethod] using the Google Pay token
 *
 * Use [GooglePayPaymentMethodLauncherContract] to start [GooglePayPaymentMethodLauncherActivity].
 *
 * See [Troubleshooting](https://developers.google.com/pay/api/android/support/troubleshooting)
 * for a guide to troubleshooting Google Pay issues.
 */
internal class GooglePayPaymentMethodLauncherActivity : AppCompatActivity() {
    private val viewModel: GooglePayPaymentMethodLauncherViewModel by viewModels {
        GooglePayPaymentMethodLauncherViewModel.Factory(args)
    }

    private lateinit var args: GooglePayPaymentMethodLauncherContractV2.Args

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFadeAnimations()

        val nullableArgs = GooglePayPaymentMethodLauncherContractV2.Args.fromIntent(intent)
        if (nullableArgs == null) {
            finishWithResult(
                GooglePayPaymentMethodLauncher.Result.Failed(
                    RuntimeException(
                        "GooglePayPaymentMethodLauncherActivity was started without arguments."
                    ),
                    GooglePayPaymentMethodLauncher.DEVELOPER_ERROR
                )
            )
            return
        }
        args = nullableArgs

        lifecycleScope.launch {
            viewModel.googlePayResult.collect { googlePayResult ->
                googlePayResult?.let(::finishWithResult)
            }
        }

        val googlePayLauncher = registerForActivityResult(GetPaymentDataResult()) {
            onGooglePayResult(it)
        }

        if (!viewModel.hasLaunched) {
            lifecycleScope.launch {
                runCatching {
                    viewModel.createLoadPaymentDataTask()
                }.fold(
                    onSuccess = {
                        googlePayLauncher.launch(it)
                        viewModel.hasLaunched = true
                    },
                    onFailure = {
                        updateResult(
                            GooglePayPaymentMethodLauncher.Result.Failed(
                                it,
                                GooglePayPaymentMethodLauncher.INTERNAL_ERROR
                            )
                        )
                    }
                )
            }
        }
    }

    override fun finish() {
        super.finish()
        setFadeAnimations()
    }

    private fun onGooglePayResult(taskResult: ApiTaskResult<PaymentData>) {
        when (taskResult.status.statusCode) {
            CommonStatusCodes.SUCCESS -> {
                onGooglePayResult(taskResult.result!!)
            }
            CommonStatusCodes.CANCELED -> {
                updateResult(
                    GooglePayPaymentMethodLauncher.Result.Canceled
                )
            }
            AutoResolveHelper.RESULT_ERROR -> {
                val status = taskResult.status
                updateResult(
                    GooglePayPaymentMethodLauncher.Result.Failed(
                        RuntimeException(
                            "Google Pay failed with error ${status.statusCode}: ${status.statusMessage}"
                        ),
                        googlePayStatusCodeToErrorCode(status.statusCode)
                    )
                )
            }
            else -> {
                updateResult(
                    GooglePayPaymentMethodLauncher.Result.Failed(
                        RuntimeException("Google Pay returned an expected result code."),
                        GooglePayPaymentMethodLauncher.INTERNAL_ERROR
                    )
                )
            }
        }
    }

    private fun onGooglePayResult(paymentData: PaymentData) {
        lifecycleScope.launch {
            finishWithResult(
                viewModel.createPaymentMethod(paymentData)
            )
        }
    }

    private fun updateResult(result: GooglePayPaymentMethodLauncher.Result) {
        viewModel.updateResult(result)
    }

    private fun finishWithResult(result: GooglePayPaymentMethodLauncher.Result) {
        setResult(
            RESULT_OK,
            Intent()
                .putExtras(
                    bundleOf(GooglePayPaymentMethodLauncherContract.EXTRA_RESULT to result)
                )
        )
        finish()
    }

    private fun setFadeAnimations() {
        fadeOut()
    }

    private fun googlePayStatusCodeToErrorCode(googlePayStatusCode: Int):
        @GooglePayPaymentMethodLauncher.ErrorCode Int {
        return when (googlePayStatusCode) {
            CommonStatusCodes.NETWORK_ERROR -> GooglePayPaymentMethodLauncher.NETWORK_ERROR
            CommonStatusCodes.DEVELOPER_ERROR -> GooglePayPaymentMethodLauncher.DEVELOPER_ERROR
            else -> GooglePayPaymentMethodLauncher.INTERNAL_ERROR
        }
    }
}
