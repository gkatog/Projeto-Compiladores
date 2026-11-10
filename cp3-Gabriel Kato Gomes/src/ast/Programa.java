package ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Programa → Funcao ListaFuncoes EOF. Raiz da árvore. */
public final class Programa extends Node {
    public final List<FuncDecl> funcoes;

    public Programa(List<FuncDecl> funcoes, int linha, int coluna) {
        super(linha, coluna);
        this.funcoes = Collections.unmodifiableList(new ArrayList<>(funcoes));
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitPrograma(this);
    }
}
