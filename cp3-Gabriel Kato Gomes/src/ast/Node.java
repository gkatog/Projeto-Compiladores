package ast;

/**
 * Raiz da hierarquia da AST. Todo nó guarda a posição (linha e coluna) do primeiro
 * token que o originou, para a análise semântica reportar erros nesse ponto.
 * O único acréscimo do padrão Visitor é o método accept (Aula 7, slide 10).
 */
public abstract class Node {
    public final int linha;
    public final int coluna;

    protected Node(int linha, int coluna) {
        this.linha = linha;
        this.coluna = coluna;
    }

    public abstract <T> T accept(Visitor<T> v);
}
