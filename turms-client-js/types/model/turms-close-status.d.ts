declare enum TurmsCloseStatus {
    REDIRECT = 4300,
    ILLEGAL_REQUEST = 4400,
    HEARTBEAT_TIMEOUT = 4401,
    DISCONNECTED_BY_CLIENT = 4402,
    DISCONNECTED_BY_OTHER_DEVICE = 4403,
    SERVER_ERROR = 4500,
    SERVER_CLOSED = 4501,
    SERVER_UNAVAILABLE = 4502,
    LOGIN_CONFLICT = 4600,
    DISCONNECTED_BY_ADMIN = 4700,
    USER_IS_DELETED_OR_INACTIVATED = 4701,
    UNKNOWN_ERROR = 4900,
    WEBSOCKET_ERROR = 4901
}
export default TurmsCloseStatus;
