package no.sikt.nva.handle.exceptions;

public class HandleAlreadyExistException extends RuntimeException {

    public HandleAlreadyExistException(String message) {
        super(message);
    }
}
