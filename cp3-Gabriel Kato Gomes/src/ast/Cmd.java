package ast;

/** Categoria dos comandos: Bloco, LocalCmd, AtribCmd, ChamadaCmd, IfCmd, WhileCmd, ReturnCmd. */
public abstract class Cmd extends Node {
    protected Cmd(int linha, int coluna) {
        super(linha, coluna);
    }
}
