public class SyntaxError {
    public final String message;
    public final int line;
    public final int col;

    public SyntaxError(String message, int line, int col) {
        this.message = message;
        this.line = line;
        this.col = col;
    }

    @Override
    public String toString() {
        return String.format("Erro sintático (linha %d, coluna %d): %s", line, col, message);
    }
}
