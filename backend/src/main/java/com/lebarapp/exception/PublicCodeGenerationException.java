package com.lebarapp.exception;

/**
 * Thrown when a unique public code could not be generated within the configured
 * retry budget (500). Technical collision details are not exposed to the client.
 */
public class PublicCodeGenerationException extends BusinessException {

    public PublicCodeGenerationException() {
        super(ApiErrorCode.PUBLIC_CODE_GENERATION_FAILED);
    }
}
