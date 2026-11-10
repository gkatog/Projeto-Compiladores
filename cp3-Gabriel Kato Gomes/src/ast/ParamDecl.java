package ast;

/** Parâmetro de uma função: Param → Tipo ID. */
public final class ParamDecl extends Node {
    public final Tipo tipo;
    public final String nome;

    public ParamDecl(Tipo tipo, String nome, int linha, int coluna) {
        super(linha, coluna);
        this.tipo = tipo;
        this.nome = nome;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitParamDecl(this);
    }
}
