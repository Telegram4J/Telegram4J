package telegram4j.mtproto;

import telegram4j.tl.mtproto.RpcError;

public class RpcException extends RuntimeException {
    private static final long serialVersionUID = -4159674899075462143L;

    private final RpcError error;

    public RpcException(String message, RpcError error) {
        super(message);
        this.error = error;
    }

    // TODO: message templates with X should be detected via a regexp
    static RpcException create(RpcError error) {
        String message = null;

        switch (error.errorCode()) {
            case 303: // SEE_OTHER
                switch (error.errorMessage()) {
                    case "FILE_MIGRATE_X": message = "The file to be accessed is currently stored in a different data center."; break;
                    case "PHONE_MIGRATE_X": message = "The phone number a user is trying to use for authorization is associated with a different data center."; break;
                    case "NETWORK_MIGRATE_X": message = "The source IP address is associated with a different data center (for registration)."; break;
                    case "USER_MIGRATE_X": message = "The user whose identity is being used to execute queries is associated with a different data center (for registration)."; break;
                }
                break;

            case 400: // BAD_REQUEST
                switch (error.errorMessage()) {
                    case "FIRSTNAME_INVALID": message = "The first name is invalid."; break;
                    case "LASTNAME_INVALID": message = "The last name is invalid."; break;
                    case "PHONE_NUMBER_INVALID": message = "The phone number is invalid."; break;
                    case "PHONE_CODE_HASH_EMPTY": message = "phone_code_hash is missing"; break;
                    case "PHONE_CODE_EMPTY": message = "phone_code is missing."; break;
                    case "PHONE_CODE_EXPIRED": message = "The confirmation code has expired."; break;
                    case "API_ID_INVALID": message = "The api_id/api_hash combination is invalid."; break;
                    case "PHONE_NUMBER_OCCUPIED": message = "The phone number is already in use."; break;
                    case "PHONE_NUMBER_UNOCCUPIED": message = "The phone number is not yet being used."; break;
                    case "USERS_TOO_FEW": message = "Not enough users."; break;
                    case "USERS_TOO_MUCH": message = "The maximum number of users has been exceeded."; break;
                    case "TYPE_CONSTRUCTOR_INVALID": message = "The type constructor is invalid."; break;
                    case "FILE_PART_INVALID": message = "The file part number is invalid."; break;
                    case "FILE_PARTS_INVALID": message = "The number of file parts is invalid."; break;
                    case "FILE_PART_Х_MISSING": message = "Part of the file is missing from storage."; break;
                    case "MD5_CHECKSUM_INVALID": message = "The MD5 checksums do not match."; break;
                    case "PHOTO_INVALID_DIMENSIONS": message = "The photo dimensions are invalid."; break;
                    case "FIELD_NAME_INVALID": message = "The field with the name FIELD_NAME is invalid."; break;
                    case "FIELD_NAME_EMPTY": message = "The field with the name FIELD_NAME is missing."; break;
                    case "MSG_WAIT_FAILED": message = "A request that must be completed before processing the current request returned an error."; break;
                    case "MSG_WAIT_TIMEOUT": message = "A request that must be completed before processing the current request didn't finish processing yet."; break;
                }
                break;

            case 401: // UNAUTHORIZED
                switch (error.errorMessage()) {
                    case "AUTH_KEY_UNREGISTERED": message = "The key is not registered in the system."; break;
                    case "AUTH_KEY_INVALID": message = "The key is invalid."; break;
                    case "USER_DEACTIVATED": message = "The user has been deleted/deactivated."; break;
                    case "SESSION_REVOKED": message = "The authorization has been invalidated, because of the user terminating all sessions."; break;
                    case "SESSION_EXPIRED": message = "The authorization has expired."; break;
                    case "AUTH_KEY_PERM_EMPTY": message = "The method is unavailable for temporary authorization key, not bound to permanent."; break;
                }
                break;

            case 403: // FORBIDDEN
                break;

            case 404: // NOT_FOUND
                message = "Attempt to invoke a non-existent object/method.";
                break;

            case 406: // NOT_ACCEPTABLE
                break;

            case 420: // FLOOD
                switch (error.errorMessage()) {
                    case "FLOOD_WAIT_X": message = "Wait a certain number of seconds is required."; break;
                }
                break;

            case 500: // INTERNAL
                break;
        }

        String format = "code: " + error.errorCode() + ", original message: "
                + error.errorMessage() + (message != null ? ", message: " + message : "");

        return new RpcException(format, error);
    }

    public RpcError getError() {
        return error;
    }
}
