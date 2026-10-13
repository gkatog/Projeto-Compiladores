import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;


public class Parser {



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

    
    private static final Set<TokenType> ANCORAS = uniao(FIM_BLOCO,
        conj(TokenType.SEMI, TokenType.LOCAL, TokenType.IF, TokenType.WHILE, TokenType.RETURN));

    private static final class ErroSintatico extends RuntimeException {
        private static final long serialVersionUID = 1L;
        ErroSintatico() { super(null, null, false, false); }
    }

    
    public enum Traco { NENHUM, RESUMIDO, COMPLETO }

    private final Scanner scanner;
    private Token lookahead;
    private final List<SyntaxError> erros = new ArrayList<>();

    private boolean emPanico = false;

    private Traco traco = Traco.NENHUM;
    private int profundidade = 0;   // nível de chamadas, só para indentar o traço

    public Parser(Scanner scanner) {
        this.scanner = scanner;
        this.lookahead = scanner.nextToken();   // primeiro token
    }

    public void setTraco(Traco traco) { this.traco = traco; }

    /** Analisa o programa inteiro. Retorna true se não houve erro léxico nem sintático. */
    public boolean parse() {
        parsePrograma();
        return erros.isEmpty() && scanner.getErrors().isEmpty();
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
    // Programa e funções
    // ==================================================================

    // Programa → Funcao ListaFuncoes EOF
    private void parsePrograma() {
        entrar("Programa");
        try {
            parseFuncao();
            parseListaFuncoes();
            match(TokenType.EOF);
        } catch (ErroSintatico e) {
            sincronizar("Programa", FOLLOW_PROGRAMA);
        }
        sair();
    }

    // ListaFuncoes → Funcao ListaFuncoes | ε
    private void parseListaFuncoes() {
        entrar("ListaFuncoes");
        if (ve(TokenType.FUNCTION)) {
            parseFuncao();
            parseListaFuncoes();
        } else if (ve(FOLLOW_LISTAFUNCOES)) {
            vazio("ListaFuncoes");
            // ε
        } else {
            // ex.: ENDIF/ENDDO/ELSE sobrando no fim de uma função.
            // Sincroniza só em FIRST ∪ FOLLOW: como o lookahead não está em nenhum dos dois,
            // pelo menos um token é descartado, então a chamada recursiva não entra em loop.
            reportar("esperado FUNCTION ou fim do arquivo, encontrado " + texto(lookahead));
            descartarAte("FIRST ∪ FOLLOW de ListaFuncoes", conj(TokenType.FUNCTION, TokenType.EOF));
            parseListaFuncoes();
        }
        sair();
    }

    // Funcao → FUNCTION TipoRet ID LPAREN Params RPAREN ListaCmd
    private void parseFuncao() {
        entrar("Funcao");
        try {
            match(TokenType.FUNCTION);
            parseTipoRet();
            match(TokenType.ID);
            match(TokenType.LPAREN);
            parseParams();
            match(TokenType.RPAREN);
        } catch (ErroSintatico e) {
            // Erro no cabeçalho: além do FOLLOW(Funcao), sincroniza no início de um comando,
            // para ainda analisar o corpo da função.
            sincronizar("Funcao", uniao(FOLLOW_FUNCAO, INICIO_CMD));
        }
        parseListaCmd();
        sair();
    }

    // TipoRet → Tipo | ε
    private void parseTipoRet() {
        entrar("TipoRet");
        try {
            if (ve(TIPOS)) {
                parseTipo();
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
    }

    // Params → Param ParamsResto | ε
    private void parseParams() {
        entrar("Params");
        try {
            if (ve(TIPOS)) {
                parseParam();
                parseParamsResto();
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
    private void parseParamsResto() {
        entrar("ParamsResto");
        try {
            if (ve(TokenType.COMMA)) {
                match(TokenType.COMMA);
                parseParam();
                parseParamsResto();
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
    private void parseParam() {
        entrar("Param");
        try {
            parseTipo();
            match(TokenType.ID);
        } catch (ErroSintatico e) {
            sincronizar("Param", FOLLOW_PARAM);
        }
        sair();
    }

    // Tipo → INT | DOUBLE | BOOL | CHAR | STRING_TYPE
    private void parseTipo() {
        entrar("Tipo");
        try {
            if (ve(TIPOS)) {
                consumir();
            } else {
                throw erro("esperado um tipo (INT, DOUBLE, BOOL, CHAR ou STRING), encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("Tipo", FOLLOW_TIPO);
        }
        sair();
    }

    // ==================================================================
    // Comandos
    // ==================================================================

    // ListaCmd → Cmd ListaCmd | ε
    private void parseListaCmd() {
        entrar("ListaCmd");
        if (ve(INICIO_CMD)) {
            parseCmd();
            parseListaCmd();
        } else if (ve(FOLLOW_LISTACMD)) {
            vazio("ListaCmd");
            // ε: fim do bloco
        } else {
            // Token que não começa comando nem fecha bloco (ex.: um número solto).
            // Sincroniza em FIRST ∪ FOLLOW (sem âncoras extras): o lookahead não está
            // nesse conjunto, então pelo menos um token é descartado antes da recursão.
            reportar("esperado início de comando, encontrado " + texto(lookahead));
            descartarAte("FIRST ∪ FOLLOW de ListaCmd", uniao(INICIO_CMD, FOLLOW_LISTACMD));
            parseListaCmd();
        }
        sair();
    }

    // Cmd → CmdBase PontoVirgula
    private void parseCmd() {
        entrar("Cmd");
        parseCmdBase();
        parsePontoVirgula();
        sair();
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
    private void parseCmdBase() {
        entrar("CmdBase");
        try {
            switch (lookahead.type) {
                case LOCAL:  parseDecl();      break;
                case ID:     parseCmdId();     break;
                case IF:     parseCmdIf();     break;
                case WHILE:  parseCmdWhile();  break;
                case RETURN: parseCmdReturn(); break;
                default:
                    throw erro("esperado comando, encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("CmdBase", FOLLOW_CMDBASE);
        }
        sair();
    }

    // Decl → LOCAL Tipo Var ListaVar
    private void parseDecl() {
        entrar("Decl");
        try {
            match(TokenType.LOCAL);
            parseTipo();
            parseVar();
            parseListaVar();
        } catch (ErroSintatico e) {
            sincronizar("Decl", FOLLOW_CMDBASE);
        }
        sair();
    }

    // ListaVar → COMMA Var ListaVar | ε
    private void parseListaVar() {
        entrar("ListaVar");
        try {
            if (ve(TokenType.COMMA)) {
                match(TokenType.COMMA);
                parseVar();
                parseListaVar();
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
    private void parseVar() {
        entrar("Var");
        try {
            match(TokenType.ID);
            parseInit();
        } catch (ErroSintatico e) {
            sincronizar("Var", FOLLOW_VAR);
        }
        sair();
    }

    // Init → ASSIGN Expr | ε
    private void parseInit() {
        entrar("Init");
        try {
            if (ve(TokenType.ASSIGN)) {
                match(TokenType.ASSIGN);
                parseExpr();
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
    }

    // CmdId → ID SufixoId
    private void parseCmdId() {
        entrar("CmdId");
        try {
            match(TokenType.ID);
            parseSufixoId();
        } catch (ErroSintatico e) {
            sincronizar("CmdId", FOLLOW_CMDBASE);
        }
        sair();
    }

    // SufixoId → ASSIGN Expr | LPAREN Args RPAREN
    private void parseSufixoId() {
        entrar("SufixoId");
        try {
            if (ve(TokenType.ASSIGN)) {
                match(TokenType.ASSIGN);
                parseExpr();
            } else if (ve(TokenType.LPAREN)) {
                match(TokenType.LPAREN);
                parseArgs();
                match(TokenType.RPAREN);
            } else {
                throw erro("esperado ':=' ou '(' depois do identificador, encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("SufixoId", FOLLOW_CMDBASE);
        }
        sair();
    }

    // CmdIf → IF Expr ListaCmd Senao ENDIF
    private void parseCmdIf() {
        entrar("CmdIf");
        try {
            match(TokenType.IF);
            parseExpr();
            parseListaCmd();
            parseSenao();
            match(TokenType.ENDIF);
        } catch (ErroSintatico e) {
            sincronizar("CmdIf", FOLLOW_CMDBASE);
        }
        sair();
    }

    // Senao → ELSE ListaCmd | ε
    private void parseSenao() {
        entrar("Senao");
        try {
            if (ve(TokenType.ELSE)) {
                match(TokenType.ELSE);
                parseListaCmd();
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
    }

    // CmdWhile → WHILE Expr ListaCmd ENDDO
    private void parseCmdWhile() {
        entrar("CmdWhile");
        try {
            match(TokenType.WHILE);
            parseExpr();
            parseListaCmd();
            match(TokenType.ENDDO);
        } catch (ErroSintatico e) {
            sincronizar("CmdWhile", FOLLOW_CMDBASE);
        }
        sair();
    }

    // CmdReturn → RETURN LPAREN RetValor RPAREN
    private void parseCmdReturn() {
        entrar("CmdReturn");
        try {
            match(TokenType.RETURN);
            match(TokenType.LPAREN);
            parseRetValor();
            match(TokenType.RPAREN);
        } catch (ErroSintatico e) {
            sincronizar("CmdReturn", FOLLOW_CMDBASE);
        }
        sair();
    }

    // RetValor → Expr | ε
    private void parseRetValor() {
        entrar("RetValor");
        try {
            if (ve(FIRST_EXPR)) {
                parseExpr();
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
    }

    // ==================================================================
    // Expressões
    // ==================================================================

    // Expr → ExprAnd ExprOrResto
    private void parseExpr() {
        entrar("Expr");
        parseExprAnd();
        parseExprOrResto();
        sair();
    }

    // ExprOrResto → OR ExprAnd ExprOrResto | ε
    private void parseExprOrResto() {
        entrar("ExprOrResto");
        try {
            if (ve(TokenType.OR)) {
                match(TokenType.OR);
                parseExprAnd();
                parseExprOrResto();
            } else if (ve(FOLLOW_EXPR)) {
                vazio("ExprOrResto");
                // ε
            } else {
                throw erro("token inesperado na expressão: " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("ExprOrResto", FOLLOW_EXPR);
        }
        sair();
    }

    // ExprAnd → ExprNot ExprAndResto
    private void parseExprAnd() {
        entrar("ExprAnd");
        parseExprNot();
        parseExprAndResto();
        sair();
    }

    // ExprAndResto → AND ExprNot ExprAndResto | ε
    private void parseExprAndResto() {
        entrar("ExprAndResto");
        try {
            if (ve(TokenType.AND)) {
                match(TokenType.AND);
                parseExprNot();
                parseExprAndResto();
            } else if (ve(FOLLOW_EXPRAND)) {
                vazio("ExprAndResto");
                // ε
            } else {
                throw erro("token inesperado na expressão: " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("ExprAndResto", FOLLOW_EXPRAND);
        }
        sair();
    }

    // ExprNot → NOT ExprNot | ExprRel
    private void parseExprNot() {
        entrar("ExprNot");
        try {
            if (ve(TokenType.NOT)) {
                match(TokenType.NOT);
                parseExprNot();
            } else if (ve(INICIO_OPERANDO)) {
                parseExprRel();
            } else {
                throw erro("esperado expressão, encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("ExprNot", FOLLOW_EXPRNOT);
        }
        sair();
    }

    // ExprRel → ExprAdd ExprRelResto
    private void parseExprRel() {
        entrar("ExprRel");
        parseExprAdd();
        parseExprRelResto();
        sair();
    }

    // ExprRelResto → OpRel ExprAdd | ε
    private void parseExprRelResto() {
        entrar("ExprRelResto");
        try {
            if (ve(OP_REL)) {
                parseOpRel();
                parseExprAdd();
            } else if (ve(FOLLOW_EXPRNOT)) {
                vazio("ExprRelResto");
                // ε
            } else {
                throw erro("token inesperado na expressão: " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("ExprRelResto", FOLLOW_EXPRNOT);
        }
        sair();
    }

    // OpRel → EQ | NEQ | LT | LE | GT | GE
    private void parseOpRel() {
        entrar("OpRel");
        try {
            if (ve(OP_REL)) {
                consumir();
            } else {
                throw erro("esperado operador relacional, encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("OpRel", FOLLOW_OPERADOR);
        }
        sair();
    }

    // ExprAdd → Termo ExprAddResto
    private void parseExprAdd() {
        entrar("ExprAdd");
        parseTermo();
        parseExprAddResto();
        sair();
    }

    // ExprAddResto → OpAdd Termo ExprAddResto | ε
    private void parseExprAddResto() {
        entrar("ExprAddResto");
        try {
            if (ve(TokenType.PLUS) || ve(TokenType.MINUS)) {
                parseOpAdd();
                parseTermo();
                parseExprAddResto();
            } else if (ve(FOLLOW_EXPRADD)) {
                vazio("ExprAddResto");
                // ε
            } else {
                throw erro("token inesperado na expressão: " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("ExprAddResto", FOLLOW_EXPRADD);
        }
        sair();
    }

    // OpAdd → PLUS | MINUS
    private void parseOpAdd() {
        entrar("OpAdd");
        try {
            if (ve(TokenType.PLUS) || ve(TokenType.MINUS)) {
                consumir();
            } else {
                throw erro("esperado '+' ou '-', encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("OpAdd", FOLLOW_OPERADOR);
        }
        sair();
    }

    // Termo → Unario TermoResto
    private void parseTermo() {
        entrar("Termo");
        parseUnario();
        parseTermoResto();
        sair();
    }

    // TermoResto → OpMul Unario TermoResto | ε
    private void parseTermoResto() {
        entrar("TermoResto");
        try {
            if (ve(TokenType.MULT) || ve(TokenType.DIV)) {
                parseOpMul();
                parseUnario();
                parseTermoResto();
            } else if (ve(FOLLOW_TERMO)) {
                vazio("TermoResto");
                // ε
            } else {
                throw erro("token inesperado na expressão: " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("TermoResto", FOLLOW_TERMO);
        }
        sair();
    }

    // OpMul → MULT | DIV
    private void parseOpMul() {
        entrar("OpMul");
        try {
            if (ve(TokenType.MULT) || ve(TokenType.DIV)) {
                consumir();
            } else {
                throw erro("esperado '*' ou '/', encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("OpMul", FOLLOW_OPERADOR);
        }
        sair();
    }

    // Unario → MINUS Unario | Primario
    private void parseUnario() {
        entrar("Unario");
        try {
            if (ve(TokenType.MINUS)) {
                match(TokenType.MINUS);
                parseUnario();
            } else if (ve(FIRST_PRIMARIO)) {
                parsePrimario();
            } else {
                throw erro("esperado operando, encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("Unario", FOLLOW_UNARIO);
        }
        sair();
    }

    // Primario → NUM | LIT_STRING | LOGICAL | LPAREN Expr RPAREN | ID SufixoChamada
    private void parsePrimario() {
        entrar("Primario");
        try {
            switch (lookahead.type) {
                case NUM:
                case LIT_STRING:
                case LOGICAL:
                    consumir();
                    break;
                case LPAREN:
                    match(TokenType.LPAREN);
                    parseExpr();
                    match(TokenType.RPAREN);
                    break;
                case ID:
                    match(TokenType.ID);
                    parseSufixoChamada();
                    break;
                default:
                    throw erro("esperado operando, encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("Primario", FOLLOW_UNARIO);
        }
        sair();
    }

    // SufixoChamada → LPAREN Args RPAREN | ε
    private void parseSufixoChamada() {
        entrar("SufixoChamada");
        try {
            if (ve(TokenType.LPAREN)) {
                match(TokenType.LPAREN);
                parseArgs();
                match(TokenType.RPAREN);
            } else if (ve(FOLLOW_UNARIO)) {
                vazio("SufixoChamada");
                // ε: é só uma variável, não uma chamada
            } else {
                throw erro("token inesperado depois do identificador: " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("SufixoChamada", FOLLOW_UNARIO);
        }
        sair();
    }

    // Args → Expr ArgsResto | ε
    private void parseArgs() {
        entrar("Args");
        try {
            if (ve(FIRST_EXPR)) {
                parseExpr();
                parseArgsResto();
            } else if (ve(FOLLOW_ARGS)) {
                vazio("Args");
                // ε: chamada sem argumentos
            } else {
                throw erro("esperado argumento ou ')', encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("Args", FOLLOW_ARGS);
        }
        sair();
    }

    // ArgsResto → COMMA Expr ArgsResto | ε
    private void parseArgsResto() {
        entrar("ArgsResto");
        try {
            if (ve(TokenType.COMMA)) {
                match(TokenType.COMMA);
                parseExpr();
                parseArgsResto();
            } else if (ve(FOLLOW_ARGS)) {
                vazio("ArgsResto");
                // ε
            } else {
                throw erro("esperado ',' ou ')' nos argumentos, encontrado " + texto(lookahead));
            }
        } catch (ErroSintatico e) {
            sincronizar("ArgsResto", FOLLOW_ARGS);
        }
        sair();
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