package ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Declaração de função ou procedimento. tipoRetorno == null indica procedimento (FUNCTION sem tipo). */
public final class FuncDecl extends Node {
    public final Tipo tipoRetorno;
    public final String nome;
    public final List<ParamDecl> params;
    public final Bloco corpo;

    public FuncDecl(Tipo tipoRetorno, String nome, List<ParamDecl> params, Bloco corpo, int linha, int coluna) {
        super(linha, coluna);
        this.tipoRetorno = tipoRetorno;
        this.nome = nome;
        this.params = Collections.unmodifiableList(new ArrayList<>(params));
        this.corpo = corpo;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitFuncDecl(this);
    }
}
