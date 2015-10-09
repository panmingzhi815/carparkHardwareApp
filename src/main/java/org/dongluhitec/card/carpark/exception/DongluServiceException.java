package org.dongluhitec.card.carpark.exception;

/**
 * Created by panmingzhi815 on 2015/10/8 0008.
 */
public class DongluServiceException extends RuntimeException {

    public DongluServiceException(String message) {
        super(message);
    }

    public DongluServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
