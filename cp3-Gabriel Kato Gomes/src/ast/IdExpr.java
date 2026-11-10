package ast;

/** Uso de um identificador (variável ou parâmetro). */
public final class IdExpr extends Expr {
    public final String nome;

    public IdExpr(String nome, int linha, int coluna) {
        super(linha, coluna);
        this.nome = nome;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitId(this);
    }
}
