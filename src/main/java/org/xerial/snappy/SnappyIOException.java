package org.xerial.snappy;

import java.io.IOException;

/**
 * Enhanced IOException with SnappyErrorCode
 */
public class SnappyIOException
        extends IOException
{
    private final SnappyErrorCode errorCode;

    public SnappyIOException(SnappyErrorCode errorCode, String message)
    {
        super(message);
        this.errorCode = errorCode;
    }

    @Override
    public String getMessage()
    {
        return String.format("[%s] %s", errorCode.name(), super.getMessage());
    }

    public SnappyErrorCode getErrorCode() { return errorCode; }
}
