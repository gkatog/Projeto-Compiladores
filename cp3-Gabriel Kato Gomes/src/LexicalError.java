public class LexicalError {
    public final String message;
    public final int line;
    public final int col;

    public LexicalError(String message, int line, int col) {
        this.message = message;
        this.line = line;
        this.col = col;
    }

    @Override
    public String toString() {
        return String.format("Erro léxico (linha %d, coluna %d): %s", line, col, message);
    }
}
