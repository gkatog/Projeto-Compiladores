import ast.Programa;

import java.util.Collections;
import java.util.List;

/**
 * Resultado de Parser.parse(): a árvore e os erros encontrados.
 * A árvore só existe quando o programa foi aceito (sem erro léxico nem sintático);
 * caso contrário arvore() devolve null.
 */
public final class Resultado {
    private final Programa arvore;
    private final List<SyntaxError> erros;
    private final List<LexicalError> errosLexicos;

    public Resultado(Programa arvore, List<SyntaxError> erros, List<LexicalError> errosLexicos) {
        this.arvore = arvore;
        this.erros = Collections.unmodifiableList(erros);
        this.errosLexicos = Collections.unmodifiableList(errosLexicos);
    }

    /** true se não houve nenhum erro léxico nem sintático. */
    public boolean aceito() { return erros.isEmpty() && errosLexicos.isEmpty(); }

    /** A AST do programa, ou null se houve erro. */
    public Programa arvore() { return arvore; }

    public List<SyntaxError> erros() { return erros; }

    public List<LexicalError> errosLexicos() { return errosLexicos; }
}
