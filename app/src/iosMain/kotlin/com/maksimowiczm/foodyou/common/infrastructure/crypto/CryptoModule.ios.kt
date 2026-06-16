package com.maksimowiczm.foodyou.common.infrastructure.crypto

import com.maksimowiczm.foodyou.common.crypto.IdentityCrypto
import com.maksimowiczm.foodyou.common.crypto.MasterCrypto
import com.maksimowiczm.foodyou.common.crypto.SignatureVerifier
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module

internal actual fun Module.masterCryptoDefinition(): KoinDefinition<out MasterCrypto> =
    error("MasterCrypto is not available on iOS")

internal actual fun Module.identityCryptoDefinition(): KoinDefinition<out IdentityCrypto> =
    error("IdentityCrypto is not available on iOS")

internal actual fun Module.signatureVerifierDefinition(): KoinDefinition<out SignatureVerifier> =
    error("SignatureVerifier is not available on iOS")
