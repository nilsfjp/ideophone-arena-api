package io.github.nilsfjp.ideophonearena.exception;

// A free-form romaji entry that failed the input gate or did not segment into morae.
// Surfaces as 400 with validationErrors.input, matching the Bean Validation shape, so
// the client can render the parse-error helper inline against the input field.
public class UnparseableInputException extends RuntimeException {

    public UnparseableInputException(String message) {
        super(message);
    }
}
