package moe.takanashihoshino.nyaniduserserver.utils.ErrUtils;

public enum ErrorCode {
    IllegalClient(403,"Illegal Client Request"),
    IllegalRequest(403,"Illegal Request"),
    UNKNOWN_ERROR(500,"Unknown Error"),
    NotFoundAccount(404,"Not Found Account"),
    MethodNotAllowed(405,"Method Not Allowed"),
    Unauthorized(401,"Unauthorized"),
    Dimples1337(999,"Your IP access frequency is too high and has been restricted from access."),
    NotFoundResource(404,"Not Found Resource"),
    ForbiddenOperationException(403,"ForbiddenOperationException"),
    IllegalArgumentException(400,"IllegalArgumentException");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
