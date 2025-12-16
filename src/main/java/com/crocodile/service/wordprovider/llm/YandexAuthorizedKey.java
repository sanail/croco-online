package com.crocodile.service.wordprovider.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Yandex Cloud Authorized Key JSON file
 * 
 * Represents the structure of the authorized key file downloaded from Yandex Cloud Console.
 * Used for JWT generation to obtain IAM tokens for API authentication.
 * 
 * Example JSON structure:
 * {
 *   "id": "ajexxxxxxxxx",
 *   "service_account_id": "ajeyyyyyyy",
 *   "created_at": "2024-01-01T00:00:00Z",
 *   "key_algorithm": "RSA_2048",
 *   "public_key": "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----",
 *   "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----"
 * }
 */
@Data
@NoArgsConstructor
public class YandexAuthorizedKey {
    
    /**
     * Key ID - used in JWT header as 'kid' claim
     */
    @JsonProperty("id")
    private String id;
    
    /**
     * Service Account ID - used in JWT as 'iss' (issuer) claim
     */
    @JsonProperty("service_account_id")
    private String serviceAccountId;
    
    /**
     * Timestamp when the key was created
     */
    @JsonProperty("created_at")
    private String createdAt;
    
    /**
     * Algorithm used for the key (typically RSA_2048)
     */
    @JsonProperty("key_algorithm")
    private String keyAlgorithm;
    
    /**
     * Public key in PEM format (not used for JWT signing, but included in the file)
     */
    @JsonProperty("public_key")
    private String publicKey;
    
    /**
     * Private key in PEM format - used to sign JWT
     * Format: "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----"
     */
    @JsonProperty("private_key")
    private String privateKey;
}

