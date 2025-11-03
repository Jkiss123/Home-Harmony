package com.example.furniturecloudy.utils.payment

/**
 * MoMo Payment Configuration
 *
 * IMPORTANT: Get your credentials from https://business.momo.vn
 * After registering your business, you will receive:
 * - Merchant Code
 * - Merchant Name
 * - Access Key
 * - Secret Key
 *
 * For testing, use the development environment
 * For production, switch to production environment and use real credentials
 */
object MoMoConfig {
    /**
     * Development/Test credentials
     * Replace these with your actual credentials from https://business.momo.vn
     */
    const val MERCHANT_NAME = "Home Harmony Store"
    const val MERCHANT_CODE = "MOMOC2IC20220510" // Replace with your merchant code
    const val MERCHANT_NAME_LABEL = "Home Harmony"
    const val DESCRIPTION = "Payment for furniture order"

    /**
     * Production credentials
     * TODO: Replace with production credentials before release
     */
    const val PROD_MERCHANT_NAME = "Home Harmony Store"
    const val PROD_MERCHANT_CODE = "YOUR_PRODUCTION_MERCHANT_CODE"
    const val PROD_MERCHANT_NAME_LABEL = "Home Harmony"

    /**
     * Environment flag
     * Set to false for production
     */
    const val IS_DEVELOPMENT = true

    /**
     * Get MoMo Payment Helper instance with current configuration
     */
    fun createPaymentHelper(): MoMoPaymentHelper {
        return if (IS_DEVELOPMENT) {
            MoMoPaymentHelper(
                merchantName = MERCHANT_NAME,
                merchantCode = MERCHANT_CODE,
                merchantNameLabel = MERCHANT_NAME_LABEL,
                description = DESCRIPTION
            )
        } else {
            MoMoPaymentHelper(
                merchantName = PROD_MERCHANT_NAME,
                merchantCode = PROD_MERCHANT_CODE,
                merchantNameLabel = PROD_MERCHANT_NAME_LABEL,
                description = DESCRIPTION
            )
        }
    }
}
