package com.sza.tourist.core.share

import android.net.Uri

/**
 * The unit of share for [SystemShareInvoker]. Sealed so the invoker can exhaustively switch on the
 * variant without leaking variant-specific knobs into the call sites.
 *
 * - [Text] shares plain text. Optionally attaches a [streamUri] for a "share the file that contains
 *   this text" flow.
 * - [Image] shares a single image URI under the supplied [mime] type.
 */
sealed class SharePayload {

    data class Text(
        val content: String,
        val streamUri: Uri? = null,
        val grantReadPermission: Boolean = false,
    ) : SharePayload()

    data class Image(
        val uri: Uri,
        val mime: String,
    ) : SharePayload()
}
