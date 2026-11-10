package visitor;

import ast.*;

/**
 * Impressor da árvore: um nó por linha, no formato Nome(atributos), com 2 espaços
 * de indentação por nível. Os filhos aparecem na ordem dos campos do nó; filhos
 * null (ELSE, valor inicial, valor do RETURN) não aparecem. A posição não é impressa.
 * O formato está documentado na Seção 6 do ast-design.md.
 */
public class ImpressoraArvore implements Visitor<Void> {

    private final StringBuilder saida = new StringBuilder();
    private int nivel = 0;

    /** Atalho: imprime a árvore a partir de qualquer nó. */
    public static String imprimir(Node raiz) {
        ImpressoraArvore p = new ImpressoraArvore();
        raiz.accept(p);
        return p.saida.toString();
    }

    private void linha(String texto) {
        for (int i = 0; i < nivel; i++) saida.append("  ");
        saida.append(texto).append('\n');
    }

    /** Visita um filho um nível abaixo; filho null não é impresso. */
    private void filho(Node n) {
        if (n == null) return;
        nivel++;
        n.accept(this);
        nivel--;
    }

    private void filhos(Iterable<? extends Node> nos) {
        for (Node n : nos) filho(n);
    }

    // ---------- declarações ----------

    @Override public Void visitPrograma(Programa n) {
        linha("Programa");
        filhos(n.funcoes);
        return null;
    }

    @Override public Void visitFuncDecl(FuncDecl n) {
        String retorno = n.tipoRetorno == null ? "sem retorno" : n.tipoRetorno.name();
        linha("FuncDecl(" + n.nome + ", " + retorno + ")");
        filhos(n.params);
        filho(n.corpo);
        return null;
    }

    @Override public Void visitParamDecl(ParamDecl n) {
        linha("Param(" + n.nome + ", " + n.tipo.name() + ")");
        return null;
    }

    @Override public Void visitVarDecl(VarDecl n) {
        linha("Var(" + n.nome + ")");
        filho(n.inicial);
        return null;
    }

    // ---------- comandos ----------

    @Override public Void visitBloco(Bloco n) {
        linha("Bloco");
        filhos(n.comandos);
        return null;
    }

    @Override public Void visitLocal(LocalCmd n) {
        linha("Local(" + n.tipo.name() + ")");
        filhos(n.vars);
        return null;
    }

    @Override public Void visitAtrib(AtribCmd n) {
        linha("Atrib");
        filho(n.alvo);
        filho(n.valor);
        return null;
    }

    @Override public Void visitChamadaCmd(ChamadaCmd n) {
        linha("ChamadaCmd");
        filho(n.chamada);
        return null;
    }

    @Override public Void visitIf(IfCmd n) {
        linha("If");
        filho(n.condicao);
        filho(n.entao);
        filho(n.senao);
        return null;
    }

    @Override public Void visitWhile(WhileCmd n) {
        linha("While");
        filho(n.condicao);
        filho(n.corpo);
        return null;
    }

    @Override public Void visitReturn(ReturnCmd n) {
        linha("Return");
        filho(n.valor);
        return null;
    }

    // ---------- expressões ----------

    @Override public Void visitInt(IntExpr n) {
        linha("Int(" + n.texto + ")");
        return null;
    }

    @Override public Void visitDouble(DoubleExpr n) {
        linha("Double(" + n.texto + ")");
        return null;
    }

    @Override public Void visitBool(BoolExpr n) {
        linha("Bool(" + (n.valor ? ".T." : ".F.") + ")");
        return null;
    }

    @Override public Void visitChar(CharExpr n) {
        linha("Char(" + Literais.caractere(n.valor) + ")");
        return null;
    }

    @Override public Void visitString(StringExpr n) {
        linha("String(" + Literais.string(n.valor) + ")");
        return null;
    }

    @Override public Void visitId(IdExpr n) {
        linha("Id(" + n.nome + ")");
        return null;
    }

    @Override public Void visitChamada(ChamadaExpr n) {
        linha("Chamada(" + n.nome + ")");
        filhos(n.args);
        return null;
    }

    @Override public Void visitBin(BinExpr n) {
        linha("Bin(" + n.op.simbolo + ")");
        filho(n.esq);
        filho(n.dir);
        return null;
    }

    @Override public Void visitUn(UnExpr n) {
        linha("Un(" + n.op.simbolo + ")");
        filho(n.operando);
        return null;
    }
}
