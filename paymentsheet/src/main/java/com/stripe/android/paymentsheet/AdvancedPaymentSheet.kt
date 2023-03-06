package com.stripe.android.paymentsheet

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AdvancedPaymentSheet internal constructor(
    private val paymentSheetLauncher: PaymentSheetLauncher,
) {

    /**
     * Present the payment sheet with an [IntentConfiguration].
     *
     * @param intentConfiguration The [IntentConfiguration] to use.
     * @param configuration An optional [PaymentSheet] configuration.
     */
    @JvmOverloads
    fun presentWithIntentConfiguration(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        configuration: PaymentSheet.Configuration? = null,
    ) {
        paymentSheetLauncher.present(
            mode = PaymentSheet.InitializationMode.DeferredIntent(intentConfiguration),
            configuration = configuration,
        )
    }
}
