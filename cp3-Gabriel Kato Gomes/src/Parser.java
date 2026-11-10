import ast.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Parser recursivo descendente da Mini-ADVPL — Checkpoint 3 (devolve a AST).
 *
 * Implementa exatamente a gramática de gramatica-first-follow.md:
 *  - um método parseX() por não-terminal X;
 *  - terminal vira match(), não-terminal vira chamada;
 *  - a alternativa é escolhida olhando o lookahead (token atual) contra o FIRST;
 *  - ε só é aceito quando o lookahead está no FOLLOW (versão robusta);
 *  - em caso de erro, modo pânico: reporta e descarta tokens até o
 *    conjunto de sincronização do não-terminal (FOLLOW + âncoras);
 *  - cada parseX() que corresponde a um nó devolve esse nó (Aula 7); os "Resto"
 *    recebem o operando esquerdo e devolvem a árvore maior.
 *
 * Usa o Scanner/Token do CP1 sem nenhuma modificação.
 */
public class Parser {

    // ==================================================================
    // Conjuntos FIRST / FOLLOW (mesmos nomes da Seção 5 do documento)
    // ==================================================================

    private static final Set<TokenType> TIPOS = conj(
        TokenType.INT, TokenType.DOUBLE, TokenType.BOOL, TokenType.CHAR, TokenType.STRING_TYPE);

    private static final Set<TokenType> INICIO_CMD = conj(
        TokenType.LOCAL, TokenType.ID, TokenType.IF, TokenType.WHILE, TokenType.RETURN);

    private static final Set<TokenType> FIM_BLOCO = conj(
        TokenType.ELSE, TokenType.ENDIF, TokenType.ENDDO, TokenType.FUNCTION, TokenType.EOF);

    private static final Set<TokenType> INICIO_OPERANDO = conj(
        TokenType.NUM, TokenType.LIT_STRING, TokenType.LOGICAL, TokenType.LPAREN, TokenType.ID, TokenType.MINUS);

    private static final Set<TokenType> OP_REL = conj(
        TokenType.EQ, TokenType.NEQ, TokenType.LT, TokenType.LE, TokenType.GT, TokenType.GE);

    private static final Set<TokenType> FIRST_EXPR = uniao(INICIO_OPERANDO, conj(TokenType.NOT));
    private static final Set<TokenType> FIRST_PRIMARIO = conj(
        TokenType.NUM, TokenType.LIT_STRING, TokenType.LOGICAL, TokenType.LPAREN, TokenType.ID);

    // FOLLOW de cada não-terminal (os que têm o mesmo FOLLOW compartilham a constante)
    private static final Set<TokenType> FOLLOW_PROGRAMA      = conj(TokenType.EOF);
    private static final Set<TokenType> FOLLOW_LISTAFUNCOES  = conj(TokenType.EOF);
    private static final Set<TokenType> FOLLOW_FUNCAO        = conj(TokenType.FUNCTION, TokenType.EOF);
    private static final Set<TokenType> FOLLOW_TIPO          = conj(TokenType.ID);                       // Tipo, TipoRet
    private static final Set<TokenType> FOLLOW_PARAMS        = conj(TokenType.RPAREN);                   // Params, ParamsResto
    private static final Set<TokenType> FOLLOW_PARAM         = conj(TokenType.COMMA, TokenType.RPAREN);
    private static final Set<TokenType> FOLLOW_LISTACMD      = FIM_BLOCO;
    private static final Set<TokenType> FOLLOW_CMD           = uniao(INICIO_CMD, FIM_BLOCO);             // Cmd, PontoVirgula
    private static final Set<TokenType> FOLLOW_CMDBASE       = uniao(FOLLOW_CMD, conj(TokenType.SEMI));  // CmdBase, Decl, ListaVar, CmdId,
                                                                                                         // SufixoId, CmdIf, CmdWhile, CmdReturn
    private static final Set<TokenType> FOLLOW_VAR           = uniao(FOLLOW_CMDBASE, conj(TokenType.COMMA)); // Var, Init
    private static final Set<TokenType> FOLLOW_SENAO         = conj(TokenType.ENDIF);
    private static final Set<TokenType> FOLLOW_RETVALOR      = conj(TokenType.RPAREN);
    private static final Set<TokenType> FOLLOW_EXPR          = uniao(FOLLOW_CMDBASE, conj(TokenType.COMMA, TokenType.RPAREN)); // = F_EXPR
    private static final Set<TokenType> FOLLOW_EXPRAND       = uniao(FOLLOW_EXPR, conj(TokenType.OR));    // ExprAnd, ExprAndResto
    private static final Set<TokenType> FOLLOW_EXPRNOT       = uniao(FOLLOW_EXPRAND, conj(TokenType.AND)); // ExprNot, ExprRel, ExprRelResto
    private static final Set<TokenType> FOLLOW_EXPRADD       = uniao(FOLLOW_EXPRNOT, OP_REL);            // ExprAdd, ExprAddResto
    private static final Set<TokenType> FOLLOW_TERMO         = uniao(FOLLOW_EXPRADD, conj(TokenType.PLUS, TokenType.MINUS)); // Termo, TermoResto
    private static final Set<TokenType> FOLLOW_UNARIO        = uniao(FOLLOW_TERMO, conj(TokenType.MULT, TokenType.DIV));     // Unario, Primario, SufixoChamada
    private static final Set<TokenType> FOLLOW_OPERADOR      = INICIO_OPERANDO;                          // OpRel, OpAdd, OpMul
    private static final Set<TokenType> FOLLOW_ARGS          = conj(TokenType.RPAREN);                   // Args, ArgsResto

    /**
     * Tokens-âncora: palavras que só aparecem no começo ou no fim de comandos/blocos,
     * mais o ';'. Entram em todo conjunto de sincronização para que, por exemplo,
     * um ')' esquecido não faça o parser descartar o resto do programa procurando por ele.
     * O ID não entra porque também aparece no meio de expressões.
     */
    private static final Set<TokenType> ANCORAS = uniao(FIM_BLOCO,
        conj(TokenType.SEMI, TokenType.LOCAL, TokenType.IF, TokenType.WHILE, TokenType.RETURN));

    // ==================================================================
    // Estado
    // ==================================================================

    /** Lançada por match()/erro() para abandonar o resto do método atual. */
    private static final class ErroSintatico extends RuntimeException {
        private static final long serialVersionUID = 1L;
        ErroSintatico() { super(null, null, false, false); }
    }

    /**
     * Nível do traço impresso durante a análise:
     *  NENHUM   - não imprime nada;
     *  RESUMIDO - só os erros e os passos da sincronização;
     *  COMPLETO - cada chamada parseX() com o lookahead e a decisão tomada (como no traço da Aula 6).
     */
    public enum Traco { NENHUM, RESUMIDO, COMPLETO }

    private final Scanner scanner;
    private Token lookahead;
    private final List<SyntaxError> erros = new ArrayList<>();

    /**
     * Fica true depois de um erro e volta a false quando um token é consumido com sucesso.
     * Enquanto está true, novos erros no mesmo ponto não são reportados (evita erros em cascata).
     */
    private boolean emPanico = false;

    private Traco traco = Traco.NENHUM;
    private int profundidade = 0;   // nível de chamadas, só para indentar o traço

    public Parser(Scanner scanner) {
        this.scanner = scanner;
        this.lookahead = scanner.nextToken();   // primeiro token
    }

    public void setTraco(Traco traco) { this.traco = traco; }

    /**
     * Analisa o programa inteiro e devolve a AST junto com os erros.
     * Se houver qualquer erro léxico ou sintático, a árvore do resultado é null:
     * uma árvore incompleta nunca é entregue às fases seguintes.
     */
    public Resultado parse() {
        Programa raiz = parsePrograma();
        boolean ok = erros.isEmpty() && scanner.getErrors().isEmpty();
        return new Resultado(ok ? raiz : null, erros, scanner.getErrors());
    }

    public List<SyntaxError> getErrors() { return Collections.unmodifiableList(erros); }

    public List<LexicalError> getLexicalErrors() { return scanner.getErrors(); }

    // ==================================================================
    // Utilitários: match, erro e sincronização
    // ==================================================================

    private boolean ve(TokenType t) { return lookahead.type == t; }

    private boolean ve(Set<TokenType> s) { return s.contains(lookahead.type); }

    /** Consome o lookahead (só é chamado quando o token é o esperado). */
    private void consumir() {
        log("consome " + texto(lookahead));
        lookahead = scanner.nextToken();
        emPanico = false;
    }

    private void match(TokenType esperado) {
        if (lookahead.type == esperado) {
            consumir();
        } else {
            throw erro("esperado " + nome(esperado) + ", encontrado " + texto(lookahead));
        }
    }

    /** Registra o erro (se não estiver em pânico) e devolve a exceção para o chamador lançar. */
    private ErroSintatico erro(String mensagem) {
        reportar(mensagem);
        return new ErroSintatico();
    }

    private void reportar(String mensagem) {
        if (emPanico) {
            logPanico("(erro em cascata suprimido: " + mensagem + ")");
        } else {
            SyntaxError e = new SyntaxError(mensagem, lookahead.line, lookahead.col);
            erros.add(e);
            logPanico("ERRO -- " + e);
        }
        emPanico = true;
    }

    /**
     * Modo pânico: descarta tokens até achar um do FOLLOW do não-terminal ou uma âncora.
     * Sempre para no EOF, então não tem como ficar em loop.
     */
    private void sincronizar(String naoTerminal, Set<TokenType> follow) {
        descartarAte("FOLLOW(" + naoTerminal + ") + âncoras", uniao(follow, ANCORAS));
    }

    private void descartarAte(String nomeConjunto, Set<TokenType> conjunto) {
        logPanico("sincronizar(" + nomeConjunto + ")");
        boolean descartou = false;
        while (!conjunto.contains(lookahead.type) && lookahead.type != TokenType.EOF) {
            logPanico("  descarta " + texto(lookahead) + " (linha " + lookahead.line + ")");
            lookahead = scanner.nextToken();
            descartou = true;
        }
        if (descartou) {
            logPanico("  retoma em " + texto(lookahead));
        } else {
            logPanico("  lookahead " + texto(lookahead) + " já está no conjunto, não descarta nada -- retorna");
        }
    }

    // ---------- traço ----------

    private void entrar(String naoTerminal) {
        log("parse" + naoTerminal + "(): lookahead=" + texto(lookahead));
        profundidade++;
    }

    private void sair() {
        profundidade--;
    }

    /** Registra no traço que a alternativa ε foi escolhida porque o lookahead está no FOLLOW. */
    private void vazio(String naoTerminal) {
        log("ε: " + texto(lookahead) + " está em FOLLOW(" + naoTerminal + ")");
    }

    /** Linha do traço COMPLETO. */
    private void log(String msg) {
        if (traco == Traco.COMPLETO) System.out.println("    " + "  ".repeat(profundidade) + msg);
    }

    /** Linha que aparece nos traços RESUMIDO e COMPLETO (erros e sincronização). */
    private void logPanico(String msg) {
        if (traco == Traco.COMPLETO) System.out.println("    " + "  ".repeat(profundidade) + msg);
        else if (traco == Traco.RESUMIDO) System.out.println("    " + msg);
    }

    // ==================================================================
    // Auxiliares da AST
    // ==================================================================

    /** Acrescenta à lista só os nós que foram construídos (num caminho de erro, o nó pode ser null). */
    private static <N> void add(List<N> lista, N no) {
        if (no != null) lista.add(no);
    }

    /** Posição do primeiro token de um nó; se ele não existir (caminho de erro), usa o token dado. */
    private static int linhaDe(Node n, Token t) { return n != null ? n.linha : t.line; }

    private static int colunaDe(Node n, Token t) { return n != null ? n.coluna : t.col; }

    // ==================================================================
    // Programa e funções
    // ==================================================================

    // Programa → Funcao ListaFuncoes EOF
    private Programa parsePrograma() {
        entrar("Programa");
        Token inicio = lookahead;
        List<FuncDecl> funcoes = new ArrayList<>();
        try {
            add(funcoes, parseFuncao());
            parseListaFuncoes(funcoes);
            match(TokenType.EOF);
        } catch (ErroSintatico e) {
            sincronizar("Programa", FOLLOW_PROGRAMA);
        }
        sair();
        return new Programa(funcoes, inicio.line, inicio.col);
    }

    // ListaFuncoes → Funcao ListaFuncoes | ε
    // Não vira nó: acrescenta cada função à lista do Programa.
    private void parseListaFuncoes(List<FuncDecl> funcoes) {
        entrar("ListaFuncoes");
        if (ve(TokenType.FUNCTION)) {
            add(funcoes, parseFuncao());
            parseListaFuncoes(funcoes);
        } else if (ve(FOLLOW_LISTAFUNCOES)) {
            vazio("ListaFuncoes");
            // ε
        } else {
            // ex.: ENDIF/ENDDO/ELSE sobrando no fim de uma função.
            // Sincroniza só em FIRST ∪ FOLLOW: como o lookahead não está em nenhum dos dois,
            // pelo menos um token é descartado, então a chamada recursiva não entra em loop.
            reportar("esperado FUNCTION ou fim do arquivo, encontrado " + texto(lookahead));
            descartarAte("FIRST ∪ FOLLOW de ListaFuncoes", conj(TokenType.FUNCTION, TokenType.EOF));
            parseListaFuncoes(funcoes);
        }
        sair();
    }

    // Funcao → FUNCTION TipoRet ID LPAREN Params RPAREN ListaCmd
    private FuncDecl parseFuncao() {
        entrar("Funcao");
        Token inicio = lookahead;
        Tipo tipoRetorno = null;
        String nome = null;
        List<ParamDecl> params = new ArrayList<>();
        try {
            match(TokenType.FUNCTION);
            tipoRetorno = parseTipoRet();
            Token id = lookahead;
            match(TokenType.ID);
            nome = id.lexeme;
            match(TokenType.LPAREN);
            parseParams(params);
            match(TokenType.RPAREN);
        } catch (ErroSintatico e) {
            // Erro no cabeçalho: além do FOLLOW(Funcao), sincroniza no início de um comando,
            // para ainda analisar o corpo da função.
            sincronizar("Funcao", uniao(FOLLOW_FUNCAO, INICIO_CMD));
        }
        Bloco corpo = parseBloco();
        sair();
        return new FuncDecl(tipoRetorno, nome, params, corpo, inicio.line, inicio.col);
    }

    // TipoRet → Tipo | ε
    // Fatoração (caso c): vira o atributo tipoRetorno de FuncDecl; null = procedimento.
    private Tipo parseTipoRet() {
        entrar("TipoRet");
        Tipo tipo = null;
        try {
            if (ve(TIPOS)) {
                tipo = parseTipo();
            } else if (ve(FOLLOW_TIPO)) {
                vazio("TipoRet");
                // ε: procedimento (sem tipo de retorno)
            } else {
                throw erro("esperado tipo de retorno ou nome da função, encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("TipoRet", FOLLOW_TIPO);
        }
        sair();
        return tipo;
    }

    // Params → Param ParamsResto | ε
    // Não vira nó: preenche a lista de parâmetros de FuncDecl.
    private void parseParams(List<ParamDecl> params) {
        entrar("Params");
        try {
            if (ve(TIPOS)) {
                add(params, parseParam());
                parseParamsResto(params);
            } else if (ve(FOLLOW_PARAMS)) {
                vazio("Params");
                // ε: função sem parâmetros
            } else {
                throw erro("esperado tipo do parâmetro ou ')', encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("Params", FOLLOW_PARAMS);
        }
        sair();
    }

    // ParamsResto → COMMA Param ParamsResto | ε
    private void parseParamsResto(List<ParamDecl> params) {
        entrar("ParamsResto");
        try {
            if (ve(TokenType.COMMA)) {
                match(TokenType.COMMA);
                add(params, parseParam());
                parseParamsResto(params);
            } else if (ve(FOLLOW_PARAMS)) {
                vazio("ParamsResto");
                // ε
            } else {
                throw erro("esperado ',' ou ')' na lista de parâmetros, encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("ParamsResto", FOLLOW_PARAMS);
        }
        sair();
    }

    // Param → Tipo ID
    private ParamDecl parseParam() {
        entrar("Param");
        Token inicio = lookahead;
        ParamDecl param = null;
        try {
            Tipo tipo = parseTipo();
            Token id = lookahead;
            match(TokenType.ID);
            param = new ParamDecl(tipo, id.lexeme, inicio.line, inicio.col);
        } catch (ErroSintatico e) {
            sincronizar("Param", FOLLOW_PARAM);
        }
        sair();
        return param;
    }

    // Tipo → INT | DOUBLE | BOOL | CHAR | STRING_TYPE
    // Vira o enum Tipo (atributo), não um nó.
    private Tipo parseTipo() {
        entrar("Tipo");
        Tipo tipo = null;
        try {
            if (ve(TIPOS)) {
                tipo = tipoDe(lookahead.type);
                consumir();
            } else {
                throw erro("esperado um tipo (INT, DOUBLE, BOOL, CHAR ou STRING), encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("Tipo", FOLLOW_TIPO);
        }
        sair();
        return tipo;
    }

    // ==================================================================
    // Comandos
    // ==================================================================

    /**
     * Não é um não-terminal novo: só embrulha a ListaCmd num nó Bloco.
     * É chamado onde a gramática tem ListaCmd (corpo de função, IF, ELSE e WHILE).
     */
    private Bloco parseBloco() {
        Token inicio = lookahead;
        List<Cmd> comandos = new ArrayList<>();
        parseListaCmd(comandos);
        return new Bloco(comandos, inicio.line, inicio.col);
    }

    // ListaCmd → Cmd ListaCmd | ε
    // Vira o nó Bloco (em parseBloco); aqui só acrescenta cada comando à lista.
    private void parseListaCmd(List<Cmd> comandos) {
        entrar("ListaCmd");
        if (ve(INICIO_CMD)) {
            add(comandos, parseCmd());
            parseListaCmd(comandos);
        } else if (ve(FOLLOW_LISTACMD)) {
            vazio("ListaCmd");
            // ε: fim do bloco
        } else {
            // Token que não começa comando nem fecha bloco (ex.: um número solto).
            // Sincroniza em FIRST ∪ FOLLOW (sem âncoras extras): o lookahead não está
            // nesse conjunto, então pelo menos um token é descartado antes da recursão.
            reportar("esperado início de comando, encontrado " + texto(lookahead));
            descartarAte("FIRST ∪ FOLLOW de ListaCmd", uniao(INICIO_CMD, FOLLOW_LISTACMD));
            parseListaCmd(comandos);
        }
        sair();
    }

    // Cmd → CmdBase PontoVirgula
    // Não vira nó: devolve o nó do CmdBase (o ';' é descartado).
    private Cmd parseCmd() {
        entrar("Cmd");
        Cmd cmd = parseCmdBase();
        parsePontoVirgula();
        sair();
        return cmd;
    }

    // PontoVirgula → SEMI | ε
    private void parsePontoVirgula() {
        entrar("PontoVirgula");
        try {
            if (ve(TokenType.SEMI)) {
                match(TokenType.SEMI);
            } else if (ve(FOLLOW_CMD)) {
                vazio("PontoVirgula");
                // ε: o ';' é opcional
            } else {
                throw erro("esperado ';' ou início de novo comando, encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("PontoVirgula", FOLLOW_CMD);
        }
        sair();
    }

    // CmdBase → Decl | CmdId | CmdIf | CmdWhile | CmdReturn
    // Não vira nó: só escolhe a alternativa e devolve o nó dela.
    private Cmd parseCmdBase() {
        entrar("CmdBase");
        Cmd cmd = null;
        try {
            switch (lookahead.type) {
                case LOCAL:  cmd = parseDecl();      break;
                case ID:     cmd = parseCmdId();     break;
                case IF:     cmd = parseCmdIf();     break;
                case WHILE:  cmd = parseCmdWhile();  break;
                case RETURN: cmd = parseCmdReturn(); break;
                default:
                    throw erro("esperado comando, encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("CmdBase", FOLLOW_CMDBASE);
        }
        sair();
        return cmd;
    }

    // Decl → LOCAL Tipo Var ListaVar
    private LocalCmd parseDecl() {
        entrar("Decl");
        Token inicio = lookahead;
        LocalCmd decl = null;
        List<VarDecl> vars = new ArrayList<>();
        try {
            match(TokenType.LOCAL);
            Tipo tipo = parseTipo();
            add(vars, parseVar());
            parseListaVar(vars);
            decl = new LocalCmd(tipo, vars, inicio.line, inicio.col);
        } catch (ErroSintatico e) {
            sincronizar("Decl", FOLLOW_CMDBASE);
        }
        sair();
        return decl;
    }

    // ListaVar → COMMA Var ListaVar | ε
    // Não vira nó: preenche a lista de variáveis do LocalCmd.
    private void parseListaVar(List<VarDecl> vars) {
        entrar("ListaVar");
        try {
            if (ve(TokenType.COMMA)) {
                match(TokenType.COMMA);
                add(vars, parseVar());
                parseListaVar(vars);
            } else if (ve(FOLLOW_CMDBASE)) {
                vazio("ListaVar");
                // ε
            } else {
                throw erro("esperado ',' ou fim da declaração, encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("ListaVar", FOLLOW_CMDBASE);
        }
        sair();
    }

    // Var → ID Init
    private VarDecl parseVar() {
        entrar("Var");
        VarDecl var = null;
        try {
            Token id = lookahead;
            match(TokenType.ID);
            Expr inicial = parseInit();
            var = new VarDecl(id.lexeme, inicial, id.line, id.col);
        } catch (ErroSintatico e) {
            sincronizar("Var", FOLLOW_VAR);
        }
        sair();
        return var;
    }

    // Init → ASSIGN Expr | ε
    // Fatoração (caso c): vira o atributo inicial de VarDecl; null = sem valor inicial.
    private Expr parseInit() {
        entrar("Init");
        Expr inicial = null;
        try {
            if (ve(TokenType.ASSIGN)) {
                match(TokenType.ASSIGN);
                inicial = parseExpr();
            } else if (ve(FOLLOW_VAR)) {
                vazio("Init");
                // ε: variável sem valor inicial
            } else {
                throw erro("esperado ':=' ou fim da declaração, encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("Init", FOLLOW_VAR);
        }
        sair();
        return inicial;
    }

    // CmdId → ID SufixoId
    // Não vira nó: guarda o prefixo ID e deixa SufixoId decidir entre atribuição e chamada.
    private Cmd parseCmdId() {
        entrar("CmdId");
        Cmd cmd = null;
        try {
            Token id = lookahead;
            match(TokenType.ID);
            cmd = parseSufixoId(id);
        } catch (ErroSintatico e) {
            sincronizar("CmdId", FOLLOW_CMDBASE);
        }
        sair();
        return cmd;
    }

    // SufixoId → ASSIGN Expr | LPAREN Args RPAREN
    // Fatoração (caso c): recebe o ID já lido e devolve AtribCmd ou ChamadaCmd.
    private Cmd parseSufixoId(Token id) {
        entrar("SufixoId");
        Cmd cmd = null;
        try {
            if (ve(TokenType.ASSIGN)) {
                match(TokenType.ASSIGN);
                Expr valor = parseExpr();
                cmd = new AtribCmd(new IdExpr(id.lexeme, id.line, id.col), valor, id.line, id.col);
            } else if (ve(TokenType.LPAREN)) {
                match(TokenType.LPAREN);
                List<Expr> args = new ArrayList<>();
                parseArgs(args);
                match(TokenType.RPAREN);
                ChamadaExpr chamada = new ChamadaExpr(id.lexeme, args, id.line, id.col);
                cmd = new ChamadaCmd(chamada, id.line, id.col);
            } else {
                throw erro("esperado ':=' ou '(' depois do identificador, encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("SufixoId", FOLLOW_CMDBASE);
        }
        sair();
        return cmd;
    }

    // CmdIf → IF Expr ListaCmd Senao ENDIF
    private IfCmd parseCmdIf() {
        entrar("CmdIf");
        Token inicio = lookahead;
        IfCmd cmd = null;
        try {
            match(TokenType.IF);
            Expr condicao = parseExpr();
            Bloco entao = parseBloco();
            Bloco senao = parseSenao();
            match(TokenType.ENDIF);
            cmd = new IfCmd(condicao, entao, senao, inicio.line, inicio.col);
        } catch (ErroSintatico e) {
            sincronizar("CmdIf", FOLLOW_CMDBASE);
        }
        sair();
        return cmd;
    }

    // Senao → ELSE ListaCmd | ε
    // Vira o atributo senao de IfCmd; null = IF sem ELSE.
    private Bloco parseSenao() {
        entrar("Senao");
        Bloco senao = null;
        try {
            if (ve(TokenType.ELSE)) {
                match(TokenType.ELSE);
                senao = parseBloco();
            } else if (ve(FOLLOW_SENAO)) {
                vazio("Senao");
                // ε: IF sem ELSE
            } else {
                throw erro("esperado ELSE ou ENDIF, encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("Senao", FOLLOW_SENAO);
        }
        sair();
        return senao;
    }

    // CmdWhile → WHILE Expr ListaCmd ENDDO
    private WhileCmd parseCmdWhile() {
        entrar("CmdWhile");
        Token inicio = lookahead;
        WhileCmd cmd = null;
        try {
            match(TokenType.WHILE);
            Expr condicao = parseExpr();
            Bloco corpo = parseBloco();
            match(TokenType.ENDDO);
            cmd = new WhileCmd(condicao, corpo, inicio.line, inicio.col);
        } catch (ErroSintatico e) {
            sincronizar("CmdWhile", FOLLOW_CMDBASE);
        }
        sair();
        return cmd;
    }

    // CmdReturn → RETURN LPAREN RetValor RPAREN
    private ReturnCmd parseCmdReturn() {
        entrar("CmdReturn");
        Token inicio = lookahead;
        ReturnCmd cmd = null;
        try {
            match(TokenType.RETURN);
            match(TokenType.LPAREN);
            Expr valor = parseRetValor();
            match(TokenType.RPAREN);
            cmd = new ReturnCmd(valor, inicio.line, inicio.col);
        } catch (ErroSintatico e) {
            sincronizar("CmdReturn", FOLLOW_CMDBASE);
        }
        sair();
        return cmd;
    }

    // RetValor → Expr | ε
    // Vira o atributo valor de ReturnCmd; null = RETURN() sem valor.
    private Expr parseRetValor() {
        entrar("RetValor");
        Expr valor = null;
        try {
            if (ve(FIRST_EXPR)) {
                valor = parseExpr();
            } else if (ve(FOLLOW_RETVALOR)) {
                vazio("RetValor");
                // ε: RETURN() sem valor
            } else {
                throw erro("esperado expressão ou ')' no RETURN, encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("RetValor", FOLLOW_RETVALOR);
        }
        sair();
        return valor;
    }

    // ==================================================================
    // Expressões
    //
    // (a) Expr, ExprAnd, ExprRel, ExprAdd e Termo são níveis de precedência:
    //     não criam nó, só repassam o que veio de baixo.
    // (b) Os "Resto" vêm da eliminação da recursão à esquerda: recebem o operando
    //     esquerdo já construído (esq) e devolvem a árvore maior (Aula 7, slide 18).
    //     Montar new BinExpr(esq, op, dir) e passar adiante dá associatividade à esquerda.
    // ==================================================================

    // Expr → ExprAnd ExprOrResto
    private Expr parseExpr() {
        entrar("Expr");
        Expr esq = parseExprAnd();
        Expr e = parseExprOrResto(esq);
        sair();
        return e;
    }

    // ExprOrResto → OR ExprAnd ExprOrResto | ε
    private Expr parseExprOrResto(Expr esq) {
        entrar("ExprOrResto");
        Expr e = esq;
        try {
            if (ve(TokenType.OR)) {
                Token op = lookahead;
                match(TokenType.OR);
                Expr dir = parseExprAnd();
                e = parseExprOrResto(new BinExpr(esq, OpBin.OR, dir, linhaDe(esq, op), colunaDe(esq, op)));
            } else if (ve(FOLLOW_EXPR)) {
                vazio("ExprOrResto");
                // ε: devolve a árvore já construída
            } else {
                throw erro("token inesperado na expressão: " + texto(lookahead));
            }
        } catch (ErroSintatico ex) {
            sincronizar("ExprOrResto", FOLLOW_EXPR);
        }
        sair();
        return e;
    }

    // ExprAnd → ExprNot ExprAndResto
    private Expr parseExprAnd() {
        entrar("ExprAnd");
        Expr esq = parseExprNot();
        Expr e = parseExprAndResto(esq);
        sair();
        return e;
    }

    // ExprAndResto → AND ExprNot ExprAndResto | ε
    private Expr parseExprAndResto(Expr esq) {
        entrar("ExprAndResto");
        Expr e = esq;
        try {
            if (ve(TokenType.AND)) {
                Token op = lookahead;
                match(TokenType.AND);
                Expr dir = parseExprNot();
                e = parseExprAndResto(new BinExpr(esq, OpBin.AND, dir, linhaDe(esq, op), colunaDe(esq, op)));
            } else if (ve(FOLLOW_EXPRAND)) {
                vazio("ExprAndResto");
                // ε
            } else {
                throw erro("token inesperado na expressão: " + texto(lookahead));
            }
        } catch (ErroSintatico ex) {
            sincronizar("ExprAndResto", FOLLOW_EXPRAND);
        }
        sair();
        return e;
    }

    // ExprNot → NOT ExprNot | ExprRel
    private Expr parseExprNot() {
        entrar("ExprNot");
        Expr e = null;
        try {
            if (ve(TokenType.NOT)) {
                Token op = lookahead;
                match(TokenType.NOT);
                Expr operando = parseExprNot();
                e = new UnExpr(OpUn.NOT, operando, op.line, op.col);
            } else if (ve(INICIO_OPERANDO)) {
                e = parseExprRel();
            } else {
                throw erro("esperado expressão, encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico ex) {
            sincronizar("ExprNot", FOLLOW_EXPRNOT);
        }
        sair();
        return e;
    }

    // ExprRel → ExprAdd ExprRelResto
    private Expr parseExprRel() {
        entrar("ExprRel");
        Expr esq = parseExprAdd();
        Expr e = parseExprRelResto(esq);
        sair();
        return e;
    }

    // ExprRelResto → OpRel ExprAdd | ε
    // Não chama a si mesmo: por isso a < b < c não é aceito.
    private Expr parseExprRelResto(Expr esq) {
        entrar("ExprRelResto");
        Expr e = esq;
        try {
            if (ve(OP_REL)) {
                Token tokOp = lookahead;
                OpBin op = parseOpRel();
                Expr dir = parseExprAdd();
                e = new BinExpr(esq, op, dir, linhaDe(esq, tokOp), colunaDe(esq, tokOp));
            } else if (ve(FOLLOW_EXPRNOT)) {
                vazio("ExprRelResto");
                // ε
            } else {
                throw erro("token inesperado na expressão: " + texto(lookahead));
            }
        } catch (ErroSintatico ex) {
            sincronizar("ExprRelResto", FOLLOW_EXPRNOT);
        }
        sair();
        return e;
    }

    // OpRel → EQ | NEQ | LT | LE | GT | GE
    // Vira o atributo op de BinExpr.
    private OpBin parseOpRel() {
        entrar("OpRel");
        OpBin op = null;
        try {
            if (ve(OP_REL)) {
                op = opDe(lookahead.type);
                consumir();
            } else {
                throw erro("esperado operador relacional, encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico ex) {
            sincronizar("OpRel", FOLLOW_OPERADOR);
        }
        sair();
        return op;
    }

    // ExprAdd → Termo ExprAddResto
    private Expr parseExprAdd() {
        entrar("ExprAdd");
        Expr esq = parseTermo();
        Expr e = parseExprAddResto(esq);
        sair();
        return e;
    }

    // ExprAddResto → OpAdd Termo ExprAddResto | ε
    private Expr parseExprAddResto(Expr esq) {
        entrar("ExprAddResto");
        Expr e = esq;
        try {
            if (ve(TokenType.PLUS) || ve(TokenType.MINUS)) {
                Token tokOp = lookahead;
                OpBin op = parseOpAdd();
                Expr dir = parseTermo();
                e = parseExprAddResto(new BinExpr(esq, op, dir, linhaDe(esq, tokOp), colunaDe(esq, tokOp)));
            } else if (ve(FOLLOW_EXPRADD)) {
                vazio("ExprAddResto");
                // ε
            } else {
                throw erro("token inesperado na expressão: " + texto(lookahead));
            }
        } catch (ErroSintatico ex) {
            sincronizar("ExprAddResto", FOLLOW_EXPRADD);
        }
        sair();
        return e;
    }

    // OpAdd → PLUS | MINUS
    private OpBin parseOpAdd() {
        entrar("OpAdd");
        OpBin op = null;
        try {
            if (ve(TokenType.PLUS) || ve(TokenType.MINUS)) {
                op = opDe(lookahead.type);
                consumir();
            } else {
                throw erro("esperado '+' ou '-', encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico ex) {
            sincronizar("OpAdd", FOLLOW_OPERADOR);
        }
        sair();
        return op;
    }

    // Termo → Unario TermoResto
    private Expr parseTermo() {
        entrar("Termo");
        Expr esq = parseUnario();
        Expr e = parseTermoResto(esq);
        sair();
        return e;
    }

    // TermoResto → OpMul Unario TermoResto | ε
    private Expr parseTermoResto(Expr esq) {
        entrar("TermoResto");
        Expr e = esq;
        try {
            if (ve(TokenType.MULT) || ve(TokenType.DIV)) {
                Token tokOp = lookahead;
                OpBin op = parseOpMul();
                Expr dir = parseUnario();
                e = parseTermoResto(new BinExpr(esq, op, dir, linhaDe(esq, tokOp), colunaDe(esq, tokOp)));
            } else if (ve(FOLLOW_TERMO)) {
                vazio("TermoResto");
                // ε
            } else {
                throw erro("token inesperado na expressão: " + texto(lookahead));
            }
        } catch (ErroSintatico ex) {
            sincronizar("TermoResto", FOLLOW_TERMO);
        }
        sair();
        return e;
    }

    // OpMul → MULT | DIV
    private OpBin parseOpMul() {
        entrar("OpMul");
        OpBin op = null;
        try {
            if (ve(TokenType.MULT) || ve(TokenType.DIV)) {
                op = opDe(lookahead.type);
                consumir();
            } else {
                throw erro("esperado '*' ou '/', encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico ex) {
            sincronizar("OpMul", FOLLOW_OPERADOR);
        }
        sair();
        return op;
    }

    // Unario → MINUS Unario | Primario
    private Expr parseUnario() {
        entrar("Unario");
        Expr e = null;
        try {
            if (ve(TokenType.MINUS)) {
                Token op = lookahead;
                match(TokenType.MINUS);
                Expr operando = parseUnario();
                e = new UnExpr(OpUn.NEG, operando, op.line, op.col);
            } else if (ve(FIRST_PRIMARIO)) {
                e = parsePrimario();
            } else {
                throw erro("esperado operando, encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico ex) {
            sincronizar("Unario", FOLLOW_UNARIO);
        }
        sair();
        return e;
    }

    // Primario → NUM | LIT_STRING | LOGICAL | LPAREN Expr RPAREN | ID SufixoChamada
    private Expr parsePrimario() {
        entrar("Primario");
        Token t = lookahead;
        Expr e = null;
        try {
            switch (lookahead.type) {
                case NUM:
                    consumir();
                    e = t.lexeme.indexOf('.') >= 0
                        ? new DoubleExpr(t.lexeme, t.line, t.col)
                        : new IntExpr(t.lexeme, t.line, t.col);
                    break;
                case LIT_STRING: {
                    consumir();
                    String conteudo = t.lexeme.substring(1, t.lexeme.length() - 1);   // sem as aspas
                    e = conteudo.length() == 1
                        ? new CharExpr(conteudo.charAt(0), t.line, t.col)
                        : new StringExpr(conteudo, t.line, t.col);
                    break;
                }
                case LOGICAL:
                    consumir();
                    e = new BoolExpr(t.lexeme.equalsIgnoreCase(".T."), t.line, t.col);
                    break;
                case LPAREN:
                    // os parênteses não viram nó: devolve a expressão de dentro
                    match(TokenType.LPAREN);
                    e = parseExpr();
                    match(TokenType.RPAREN);
                    break;
                case ID:
                    match(TokenType.ID);
                    e = parseSufixoChamada(t);
                    break;
                default:
                    throw erro("esperado operando, encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico ex) {
            sincronizar("Primario", FOLLOW_UNARIO);
        }
        sair();
        return e;
    }

    // SufixoChamada → LPAREN Args RPAREN | ε
    // Fatoração (caso c): recebe o ID já lido e devolve ChamadaExpr ou IdExpr.
    private Expr parseSufixoChamada(Token id) {
        entrar("SufixoChamada");
        Expr e = new IdExpr(id.lexeme, id.line, id.col);
        try {
            if (ve(TokenType.LPAREN)) {
                match(TokenType.LPAREN);
                List<Expr> args = new ArrayList<>();
                parseArgs(args);
                match(TokenType.RPAREN);
                e = new ChamadaExpr(id.lexeme, args, id.line, id.col);
            } else if (ve(FOLLOW_UNARIO)) {
                vazio("SufixoChamada");
                // ε: é só uma variável, não uma chamada
            } else {
                throw erro("token inesperado depois do identificador: " + texto(lookahead));
            }
        } catch (ErroSintatico ex) {
            sincronizar("SufixoChamada", FOLLOW_UNARIO);
        }
        sair();
        return e;
    }

    // Args → Expr ArgsResto | ε
    // Não vira nó: preenche a lista de argumentos da chamada.
    private void parseArgs(List<Expr> args) {
        entrar("Args");
        try {
            if (ve(FIRST_EXPR)) {
                add(args, parseExpr());
                parseArgsResto(args);
            } else if (ve(FOLLOW_ARGS)) {
                vazio("Args");
                // ε: chamada sem argumentos
            } else {
                throw erro("esperado argumento ou ')', encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico ex) {
            sincronizar("Args", FOLLOW_ARGS);
        }
        sair();
    }

    // ArgsResto → COMMA Expr ArgsResto | ε
    private void parseArgsResto(List<Expr> args) {
        entrar("ArgsResto");
        try {
            if (ve(TokenType.COMMA)) {
                match(TokenType.COMMA);
                add(args, parseExpr());
                parseArgsResto(args);
            } else if (ve(FOLLOW_ARGS)) {
                vazio("ArgsResto");
                // ε
            } else {
                throw erro("esperado ',' ou ')' nos argumentos, encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico ex) {
            sincronizar("ArgsResto", FOLLOW_ARGS);
        }
        sair();
    }

    // ==================================================================
    // Conversão de token para atributo da AST
    // ==================================================================

    private static Tipo tipoDe(TokenType t) {
        switch (t) {
            case INT:    return Tipo.INT;
            case DOUBLE: return Tipo.DOUBLE;
            case BOOL:   return Tipo.BOOL;
            case CHAR:   return Tipo.CHAR;
            default:     return Tipo.STRING;   // STRING_TYPE
        }
    }

    private static OpBin opDe(TokenType t) {
        switch (t) {
            case EQ:    return OpBin.EQ;
            case NEQ:   return OpBin.NEQ;
            case LT:    return OpBin.LT;
            case LE:    return OpBin.LE;
            case GT:    return OpBin.GT;
            case GE:    return OpBin.GE;
            case PLUS:  return OpBin.ADD;
            case MINUS: return OpBin.SUB;
            case MULT:  return OpBin.MUL;
            default:    return OpBin.DIV;
        }
    }

    // ==================================================================
    // Auxiliares
    // ==================================================================

    private static Set<TokenType> conj(TokenType... tipos) {
        EnumSet<TokenType> s = EnumSet.noneOf(TokenType.class);
        Collections.addAll(s, tipos);
        return Collections.unmodifiableSet(s);
    }

    @SafeVarargs
    private static Set<TokenType> uniao(Set<TokenType>... conjuntos) {
        EnumSet<TokenType> s = EnumSet.noneOf(TokenType.class);
        for (Set<TokenType> c : conjuntos) s.addAll(c);
        return Collections.unmodifiableSet(s);
    }

    /** Como o token aparece na mensagem de erro. */
    private static String texto(Token t) {
        return t.type == TokenType.EOF ? "fim do arquivo" : "'" + t.lexeme + "'";
    }

    /** Nome legível de um tipo de token esperado. */
    private static String nome(TokenType t) {
        switch (t) {
            case ID:          return "identificador";
            case ASSIGN:      return "':='";
            case LPAREN:      return "'('";
            case RPAREN:      return "')'";
            case COMMA:       return "','";
            case SEMI:        return "';'";
            case STRING_TYPE: return "STRING";
            case EOF:         return "fim do arquivo";
            default:          return t.name();
        }
    }
}
