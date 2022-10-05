package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.model.RadarSession
import org.json.JSONObject

internal class PaymentMethodMessageJsonParser : ModelJsonParser<PaymentMethodMessage> {
    override fun parse(json: JSONObject): PaymentMethodMessage {
        return PaymentMethodMessage(
            displayLHtml = StripeJsonUtils.optString(json, FIELD_L_HTML) ?: "",
            learnMoreModalUrl = StripeJsonUtils.optString(json, FIELD_LEARN_MORE_MODAL_URL) ?: ""
        )
    }

    private companion object {
        private const val FIELD_L_HTML = "display_l_html"
        private const val FIELD_LEARN_MORE_MODAL_URL = "learn_more_modal_url"
    }
}
