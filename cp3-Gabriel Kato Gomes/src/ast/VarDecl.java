package ast;

/** Uma variável declarada num LOCAL: Var → ID Init. inicial == null quando não há ':='. */
public final class VarDecl extends Node {
    public final String nome;
    public final Expr inicial;

    public VarDecl(String nome, Expr inicial, int linha, int coluna) {
        super(linha, coluna);
        this.nome = nome;
        this.inicial = inicial;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitVarDecl(this);
    }
}
