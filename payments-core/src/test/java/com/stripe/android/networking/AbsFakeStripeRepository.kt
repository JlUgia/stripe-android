package com.stripe.android.networking

import com.stripe.android.cards.Bin
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.StripeFileParams
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.model.BankStatuses
import com.stripe.android.model.BinFixtures
import com.stripe.android.model.CardMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.CreateFinancialConnectionsSessionParams
import com.stripe.android.model.Customer
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.FinancialConnectionsSession
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.model.RadarSession
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.Source
import com.stripe.android.model.SourceParams
import com.stripe.android.model.Stripe3ds2AuthParams
import com.stripe.android.model.Stripe3ds2AuthResultFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.Token
import com.stripe.android.model.TokenParams
import java.util.Locale

internal abstract class AbsFakeStripeRepository : StripeRepository() {

    override suspend fun retrieveStripeIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): Result<StripeIntent> {
        TODO()
    }

    override suspend fun confirmPaymentIntent(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): Result<PaymentIntent> {
        TODO()
    }

    override suspend fun retrievePaymentIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): Result<PaymentIntent> {
        TODO()
    }

    override suspend fun refreshPaymentIntent(
        clientSecret: String,
        options: ApiRequest.Options
    ): Result<PaymentIntent> {
        TODO()
    }

    override suspend fun cancelPaymentIntentSource(
        paymentIntentId: String,
        sourceId: String,
        options: ApiRequest.Options
    ): Result<PaymentIntent> {
        TODO()
    }

    override suspend fun confirmSetupIntent(
        confirmSetupIntentParams: ConfirmSetupIntentParams,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): Result<SetupIntent> {
        TODO()
    }

    override suspend fun retrieveSetupIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): Result<SetupIntent> {
        TODO()
    }

    override suspend fun cancelSetupIntentSource(
        setupIntentId: String,
        sourceId: String,
        options: ApiRequest.Options
    ): Result<SetupIntent> {
        TODO()
    }

    override suspend fun createSource(
        sourceParams: SourceParams,
        options: ApiRequest.Options
    ): Result<Source> {
        TODO()
    }

    override suspend fun retrieveSource(
        sourceId: String,
        clientSecret: String,
        options: ApiRequest.Options
    ): Result<Source> {
        TODO()
    }

    override suspend fun createPaymentMethod(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        options: ApiRequest.Options
    ): Result<PaymentMethod> {
        TODO()
    }

    override suspend fun createToken(
        tokenParams: TokenParams,
        options: ApiRequest.Options
    ): Result<Token> {
        TODO()
    }

    @Throws(APIException::class)
    override suspend fun addCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        sourceType: String,
        requestOptions: ApiRequest.Options
    ): Result<Source> {
        TODO()
    }

    @Throws(APIException::class)
    override suspend fun deleteCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        requestOptions: ApiRequest.Options
    ): Result<Source> {
        TODO()
    }

    @Throws(APIException::class)
    override suspend fun attachPaymentMethod(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): Result<PaymentMethod> {
        TODO()
    }

    @Throws(APIException::class)
    override suspend fun detachPaymentMethod(
        publishableKey: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): Result<PaymentMethod> {
        TODO()
    }

    @Throws(APIException::class)
    override suspend fun getPaymentMethods(
        listPaymentMethodsParams: ListPaymentMethodsParams,
        publishableKey: String,
        productUsageTokens: Set<String>,
        requestOptions: ApiRequest.Options
    ): Result<List<PaymentMethod>> {
        TODO()
    }

    @Throws(APIException::class)
    override suspend fun setDefaultCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        sourceType: String,
        requestOptions: ApiRequest.Options
    ): Result<Customer> {
        TODO()
    }

    override suspend fun setCustomerShippingInfo(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        shippingInformation: ShippingInformation,
        requestOptions: ApiRequest.Options
    ): Result<Customer> {
        TODO()
    }

    override suspend fun retrieveCustomer(
        customerId: String,
        productUsageTokens: Set<String>,
        requestOptions: ApiRequest.Options
    ): Result<Customer> {
        TODO()
    }

    override suspend fun retrieveIssuingCardPin(
        cardId: String,
        verificationId: String,
        userOneTimeCode: String,
        requestOptions: ApiRequest.Options
    ): Result<String> {
        TODO()
    }

    override suspend fun updateIssuingCardPin(
        cardId: String,
        newPin: String,
        verificationId: String,
        userOneTimeCode: String,
        requestOptions: ApiRequest.Options
    ) {
    }

    override suspend fun getFpxBankStatus(
        options: ApiRequest.Options
    ) = BankStatuses()

    override suspend fun getCardMetadata(bin: Bin, options: ApiRequest.Options) =
        CardMetadata(
            BinFixtures.VISA,
            emptyList()
        )

    override suspend fun start3ds2Auth(
        authParams: Stripe3ds2AuthParams,
        requestOptions: ApiRequest.Options
    ) = Stripe3ds2AuthResultFixtures.ARES_CHALLENGE_FLOW

    override suspend fun complete3ds2Auth(
        sourceId: String,
        requestOptions: ApiRequest.Options
    ) = Stripe3ds2AuthResultFixtures.CHALLENGE_COMPLETION

    override suspend fun createFile(
        fileParams: StripeFileParams,
        requestOptions: ApiRequest.Options
    ): Result<StripeFile>{
        TODO()
    }

    override suspend fun retrieveObject(
        url: String,
        requestOptions: ApiRequest.Options
    ) = StripeResponse(1, "response")

    override suspend fun createRadarSession(
        requestOptions: ApiRequest.Options
    ) = Result.success(RadarSession("rse_abc123"))

    override suspend fun consumerSignUp(
        email: String,
        phoneNumber: String,
        country: String,
        name: String?,
        locale: Locale?,
        authSessionCookie: String?,
        consentAction: ConsumerSignUpConsentAction,
        requestOptions: ApiRequest.Options
    ): Result<ConsumerSession> {
        TODO()
    }

    override suspend fun logoutConsumer(
        consumerSessionClientSecret: String,
        authSessionCookie: String?,
        requestOptions: ApiRequest.Options
    ): Result<ConsumerSession> {
        TODO()
    }

    override suspend fun createLinkFinancialConnectionsSession(
        consumerSessionClientSecret: String,
        requestOptions: ApiRequest.Options
    ): Result<FinancialConnectionsSession> {
        TODO()
    }

    override suspend fun createPaymentDetails(
        consumerSessionClientSecret: String,
        financialConnectionsAccountId: String,
        requestOptions: ApiRequest.Options
    ): Result<ConsumerPaymentDetails> {
        TODO()
    }

    override suspend fun createPaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsCreateParams: ConsumerPaymentDetailsCreateParams,
        requestOptions: ApiRequest.Options
    ): Result<ConsumerPaymentDetails> {
        TODO()
    }

    override suspend fun listPaymentDetails(
        consumerSessionClientSecret: String,
        paymentMethodTypes: Set<String>,
        requestOptions: ApiRequest.Options
    ): Result<ConsumerPaymentDetails> {
        TODO()
    }

    override suspend fun deletePaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsId: String,
        requestOptions: ApiRequest.Options
    ) {
    }

    override suspend fun updatePaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsUpdateParams: ConsumerPaymentDetailsUpdateParams,
        requestOptions: ApiRequest.Options
    ): Result<ConsumerPaymentDetails> {
        TODO()
    }

    override suspend fun attachFinancialConnectionsSessionToPaymentIntent(
        clientSecret: String,
        paymentIntentId: String,
        financialConnectionsSessionId: String,
        requestOptions: ApiRequest.Options,
        expandFields: List<String>
    ): Result<PaymentIntent> {
        TODO()
    }

    override suspend fun attachFinancialConnectionsSessionToSetupIntent(
        clientSecret: String,
        setupIntentId: String,
        financialConnectionsSessionId: String,
        requestOptions: ApiRequest.Options,
        expandFields: List<String>
    ): Result<SetupIntent> {
        TODO()
    }

    override suspend fun createPaymentIntentFinancialConnectionsSession(
        paymentIntentId: String,
        params: CreateFinancialConnectionsSessionParams,
        requestOptions: ApiRequest.Options
    ): Result<FinancialConnectionsSession> {
        TODO()
    }

    override suspend fun createSetupIntentFinancialConnectionsSession(
        setupIntentId: String,
        params: CreateFinancialConnectionsSessionParams,
        requestOptions: ApiRequest.Options
    ): Result<FinancialConnectionsSession> {
        TODO()
    }

    override suspend fun verifyPaymentIntentWithMicrodeposits(
        clientSecret: String,
        firstAmount: Int,
        secondAmount: Int,
        requestOptions: ApiRequest.Options
    ): Result<PaymentIntent> {
        TODO()
    }

    override suspend fun verifyPaymentIntentWithMicrodeposits(
        clientSecret: String,
        descriptorCode: String,
        requestOptions: ApiRequest.Options
    ): Result<PaymentIntent> {
        TODO()
    }

    override suspend fun verifySetupIntentWithMicrodeposits(
        clientSecret: String,
        firstAmount: Int,
        secondAmount: Int,
        requestOptions: ApiRequest.Options
    ): Result<SetupIntent> {
        TODO()
    }

    override suspend fun verifySetupIntentWithMicrodeposits(
        clientSecret: String,
        descriptorCode: String,
        requestOptions: ApiRequest.Options
    ): Result<SetupIntent> {
        TODO()
    }

    override suspend fun retrievePaymentMethodMessage(
        paymentMethods: List<String>,
        amount: Int,
        currency: String,
        country: String,
        locale: String,
        logoColor: String,
        requestOptions: ApiRequest.Options
    ): Result<PaymentMethodMessage> {
        TODO()
    }

    override suspend fun retrieveElementsSession(
        params: ElementsSessionParams,
        options: ApiRequest.Options
    ): Result<ElementsSession> {
        TODO()
    }

    override suspend fun retrieveCardMetadata(
        cardNumber: String,
        requestOptions: ApiRequest.Options
    ): Result<CardMetadata> {
        TODO()
    }
}
