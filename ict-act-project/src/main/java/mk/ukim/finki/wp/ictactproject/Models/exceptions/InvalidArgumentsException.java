package mk.ukim.finki.wp.ictactproject.Models.exceptions;

public class InvalidArgumentsException extends RuntimeException {

    public InvalidArgumentsException() {
        super("Invalid arguments! Please try again.");
    }
}
