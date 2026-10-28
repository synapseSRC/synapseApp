package com.synapse.social.studioasinc.domain.model

/**
 * A third-party library shipped inside the app, together with the licence it is distributed under.
 *
 * [licenseId] is a stable machine token (for example `apache-2.0`) rather than display text;
 * turning it into a localised label is a presentation concern.
 */
data class OpenSourceLicense(
    val name: String,
    val author: String,
    val licenseId: String,
    val licenseNote: String?,
    val repositoryUrl: String
)
