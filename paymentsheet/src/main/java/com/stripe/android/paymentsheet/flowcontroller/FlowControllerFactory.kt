package com.stripe.android.paymentsheet.flowcontroller

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback

internal class FlowControllerFactory(
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val lifecycleOwner: LifecycleOwner,
    private val appContext: Context,
    private val activityResultRegistryOwner: ActivityResultRegistryOwner,
    private val statusBarColor: () -> Int?,
    private val paymentOptionCallback: PaymentOptionCallback,
    private val paymentResultCallback: PaymentSheetResultCallback
) {
    constructor(
        activity: ComponentActivity,
        paymentOptionCallback: PaymentOptionCallback,
        paymentResultCallback: PaymentSheetResultCallback
    ) : this(
        viewModelStoreOwner = activity,
        lifecycleOwner = activity,
        appContext = activity.applicationContext,
        activityResultRegistryOwner = activity,
        statusBarColor = { activity.window.statusBarColor },
        paymentOptionCallback = paymentOptionCallback,
        paymentResultCallback = paymentResultCallback,
    )

    constructor(
        fragment: Fragment,
        paymentOptionCallback: PaymentOptionCallback,
        paymentResultCallback: PaymentSheetResultCallback
    ) : this(
        viewModelStoreOwner = fragment,
        lifecycleOwner = fragment,
        appContext = fragment.requireContext().applicationContext,
        activityResultRegistryOwner = (fragment.host as? ActivityResultRegistryOwner) ?: fragment.requireActivity(),
        statusBarColor = { fragment.activity?.window?.statusBarColor },
        paymentOptionCallback = paymentOptionCallback,
        paymentResultCallback = paymentResultCallback,
    )

    fun create(): PaymentSheet.FlowController = DefaultFlowController.getInstance(
        appContext = appContext,
        viewModelStoreOwner = viewModelStoreOwner,
        lifecycleOwner = lifecycleOwner,
        activityResultRegistryOwner = activityResultRegistryOwner,
        statusBarColor = statusBarColor,
        paymentOptionCallback = paymentOptionCallback,
        paymentResultCallback = paymentResultCallback
    )
}
