package visitor;

import ast.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Reimpressor de código-fonte: gera o programa na forma canônica da Mini-ADVPL.
 * Palavras reservadas em maiúsculas, um comando por linha, 4 espaços por nível,
 * uma linha em branco entre funções e toda expressão binária ou unária entre
 * parênteses, para o texto sempre poder ser lido de novo pelo parser.
 * O formato está documentado na Seção 7 do ast-design.md.
 */
public class Reimpressora implements Visitor<String> {

    private int nivel = 0;

    /** Atalho: reimprime o programa (ou qualquer nó) como texto. */
    public static String reimprimir(Node raiz) {
        return raiz.accept(new Reimpressora());
    }

    private String ind() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < nivel; i++) s.append("    ");
        return s.toString();
    }

    /** Bloco um nível mais para dentro. */
    private String bloco(Bloco b) {
        nivel++;
        String s = b.accept(this);
        nivel--;
        return s;
    }

    private String lista(List<? extends Node> nos) {
        List<String> partes = new ArrayList<>();
        for (Node n : nos) partes.add(n.accept(this));
        return String.join(", ", partes);
    }

    // ---------- declarações ----------

    @Override public String visitPrograma(Programa n) {
        List<String> funcoes = new ArrayList<>();
        for (FuncDecl f : n.funcoes) funcoes.add(f.accept(this));
        return String.join("\n", funcoes);
    }

    @Override public String visitFuncDecl(FuncDecl n) {
        String tipo = n.tipoRetorno == null ? "" : n.tipoRetorno.name() + " ";
        return "FUNCTION " + tipo + n.nome + "(" + lista(n.params) + ")\n" + bloco(n.corpo);
    }

    @Override public String visitParamDecl(ParamDecl n) {
        return n.tipo.name() + " " + n.nome;
    }

    @Override public String visitVarDecl(VarDecl n) {
        return n.inicial == null ? n.nome : n.nome + " := " + n.inicial.accept(this);
    }

    // ---------- comandos (cada um termina com '\n') ----------

    @Override public String visitBloco(Bloco n) {
        StringBuilder s = new StringBuilder();
        for (Cmd c : n.comandos) s.append(c.accept(this));
        return s.toString();
    }

    @Override public String visitLocal(LocalCmd n) {
        return ind() + "LOCAL " + n.tipo.name() + " " + lista(n.vars) + "\n";
    }

    @Override public String visitAtrib(AtribCmd n) {
        return ind() + n.alvo.accept(this) + " := " + n.valor.accept(this) + "\n";
    }

    @Override public String visitChamadaCmd(ChamadaCmd n) {
        return ind() + n.chamada.accept(this) + "\n";
    }

    @Override public String visitIf(IfCmd n) {
        String s = ind() + "IF " + n.condicao.accept(this) + "\n" + bloco(n.entao);
        if (n.senao != null) s += ind() + "ELSE\n" + bloco(n.senao);
        return s + ind() + "ENDIF\n";
    }

    @Override public String visitWhile(WhileCmd n) {
        return ind() + "WHILE " + n.condicao.accept(this) + "\n" + bloco(n.corpo) + ind() + "ENDDO\n";
    }

    @Override public String visitReturn(ReturnCmd n) {
        return ind() + "RETURN(" + (n.valor == null ? "" : n.valor.accept(this)) + ")\n";
    }

    // ---------- expressões ----------

    @Override public String visitInt(IntExpr n)       { return n.texto; }
    @Override public String visitDouble(DoubleExpr n) { return n.texto; }
    @Override public String visitBool(BoolExpr n)     { return n.valor ? ".T." : ".F."; }
    @Override public String visitChar(CharExpr n)     { return Literais.caractere(n.valor); }
    @Override public String visitString(StringExpr n) { return Literais.string(n.valor); }
    @Override public String visitId(IdExpr n)         { return n.nome; }

    @Override public String visitChamada(ChamadaExpr n) {
        return n.nome + "(" + lista(n.args) + ")";
    }

    @Override public String visitBin(BinExpr n) {
        return "(" + n.esq.accept(this) + " " + n.op.simbolo + " " + n.dir.accept(this) + ")";
    }

    @Override public String visitUn(UnExpr n) {
        String espaco = n.op == OpUn.NOT ? " " : "";
        return "(" + n.op.simbolo + espaco + n.operando.accept(this) + ")";
    }
}
