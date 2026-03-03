package com.synapse.social.studioasinc.shared.data.source.remote

import com.synapse.social.studioasinc.shared.data.PlatformUtils

object S3Signer {

    private const val S3_SERVICE = "s3"

    fun signS3(
        method: String,
        canonicalPath: String,
        region: String,
        host: String,
        accessKeyId: String,
        secretAccessKey: String,
        amzDate: String,
        contentType: String = "application/octet-stream"
    ): Map<String, String> {
        val dateStamp = amzDate.substring(0, 8)
        val unsignedPayload = "UNSIGNED-PAYLOAD"

        val canonicalHeaders = "content-type:$contentType\n" +
                "host:$host\n" +
                "x-amz-content-sha256:$unsignedPayload\n" +
                "x-amz-date:$amzDate\n"
        val signedHeaders = "content-type;host;x-amz-content-sha256;x-amz-date"
        val canonicalQuery = ""
        val canonicalRequest = "$method\n$canonicalPath\n$canonicalQuery\n$canonicalHeaders\n$signedHeaders\n$unsignedPayload"
        val credentialScope = "$dateStamp/$region/$S3_SERVICE/aws4_request"
        val stringToSign = "AWS4-HMAC-SHA256\n$amzDate\n$credentialScope\n${PlatformUtils.sha256(canonicalRequest)}"

        val signingKey = getSignatureKey(secretAccessKey, dateStamp, region, S3_SERVICE)
        val signature = PlatformUtils.hmacSha256(signingKey, stringToSign).toHexString()
        val authorization = "AWS4-HMAC-SHA256 Credential=$accessKeyId/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"

        return mapOf(
            "Authorization" to authorization,
            "x-amz-date" to amzDate,
            "x-amz-content-sha256" to unsignedPayload,
            "Content-Type" to contentType,
            "Host" to host
        )
    }

    private fun getSignatureKey(key: String, dateStamp: String, regionName: String, serviceName: String): ByteArray {
        val kSecret = ("AWS4" + key).encodeToByteArray()
        val kDate = PlatformUtils.hmacSha256(kSecret, dateStamp)
        val kRegion = PlatformUtils.hmacSha256(kDate, regionName)
        val kService = PlatformUtils.hmacSha256(kRegion, serviceName)
        return PlatformUtils.hmacSha256(kService, "aws4_request")
    }
}
