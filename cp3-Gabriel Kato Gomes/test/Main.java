import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Suíte de testes do scanner (Seção 3.4 do enunciado).
 * Não usa JUnit: cada verificação é um "assert" simples feito pelo método verificar().
 * O programa termina com código 1 se algum teste falhar.
 */
public class Main {

    private static int aprovados = 0;
    private static int reprovados = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   SUÍTE DE TESTES - CHECKPOINT 1 (ANALISADOR LÉXICO)");
        System.out.println("==================================================");

        // 1. Casos válidos de cada categoria
        testarIdentificadores();
        testarPalavrasReservadas();
        testarStrings();
        testarOperadores();
        testarLiteraisNumericos();
        testarLiteraisLogicos();
        testarMaximalMunch();
        testarLinhaEColuna();

        // 2. Casos de erro (com recuperação)
        testarErroStringNaoFechadaAteEOF();
        testarErroStringNaoFechadaAteFimDeLinha();
        testarErroCaractereForaDoAlfabeto();
        testarErroComentarioDeBlocoAteEOF();
        testarErrosDeOperadores();

        // 3. Código realista
        testarLinhaRealista();
        testarScriptCompleto();

        System.out.println("\n==================================================");
        System.out.printf("   RESULTADO: %d verificações aprovadas, %d reprovadas%n", aprovados, reprovados);
        System.out.println("==================================================");
        if (reprovados > 0) System.exit(1);
    }

    // ==================================================================
    // Casos válidos
    // ==================================================================

    private static void testarIdentificadores() {
        titulo("Identificadores");
        Scanner s = new Scanner("total x1 contaItens _status n_valor_2");
        List<Token> t = tokens(s);
        verificarTipos(t, TokenType.ID, TokenType.ID, TokenType.ID, TokenType.ID, TokenType.ID);
        verificarLexemas(t, "total", "x1", "contaItens", "_status", "n_valor_2");
        verificar(s.getErrors().isEmpty(), "nenhum erro léxico");
    }

    private static void testarPalavrasReservadas() {
        titulo("Palavras reservadas (case-insensitive)");
        Scanner s = new Scanner("int DOUBLE Bool char string LOCAL if Else ENDIF while EndDo function Return");
        List<Token> t = tokens(s);
        verificarTipos(t,
            TokenType.INT, TokenType.DOUBLE, TokenType.BOOL, TokenType.CHAR, TokenType.STRING_TYPE,
            TokenType.LOCAL, TokenType.IF, TokenType.ELSE, TokenType.ENDIF,
            TokenType.WHILE, TokenType.ENDDO, TokenType.FUNCTION, TokenType.RETURN);

        // Palavra reservada como prefixo de identificador continua sendo identificador (maximal munch)
        List<Token> t2 = tokens(new Scanner("iffy whileX return1"));
        verificarTipos(t2, TokenType.ID, TokenType.ID, TokenType.ID);
    }

    private static void testarStrings() {
        titulo("Strings (aspas duplas e simples)");
        Scanner s = new Scanner("\"ok\" 'Protheus' \"linha 1\" \"\" \"tem 'aspa' simples\"");
        List<Token> t = tokens(s);
        verificarTipos(t, TokenType.LIT_STRING, TokenType.LIT_STRING, TokenType.LIT_STRING,
                          TokenType.LIT_STRING, TokenType.LIT_STRING);
        verificarLexemas(t, "\"ok\"", "'Protheus'", "\"linha 1\"", "\"\"", "\"tem 'aspa' simples\"");
        verificar(s.getErrors().isEmpty(), "nenhum erro léxico");
    }

    private static void testarOperadores() {
        titulo("Operadores e delimitadores");
        Scanner s = new Scanner("+ - * / := = == <> < <= > >= .AND. .OR. .NOT. ( ) , ;");
        List<Token> t = tokens(s);
        verificarTipos(t,
            TokenType.PLUS, TokenType.MINUS, TokenType.MULT, TokenType.DIV,
            TokenType.ASSIGN, TokenType.EQ, TokenType.EQ, TokenType.NEQ,
            TokenType.LT, TokenType.LE, TokenType.GT, TokenType.GE,
            TokenType.AND, TokenType.OR, TokenType.NOT,
            TokenType.LPAREN, TokenType.RPAREN, TokenType.COMMA, TokenType.SEMI);
        verificar(s.getErrors().isEmpty(), "nenhum erro léxico");

        // operadores lógicos também são case-insensitive
        verificarTipos(tokens(new Scanner(".and. .Or. .not.")), TokenType.AND, TokenType.OR, TokenType.NOT);
    }

    private static void testarLiteraisNumericos() {
        titulo("Literais numéricos");
        Scanner s = new Scanner("10 3.14 0 150.75 007");
        List<Token> t = tokens(s);
        verificarTipos(t, TokenType.NUM, TokenType.NUM, TokenType.NUM, TokenType.NUM, TokenType.NUM);
        verificarLexemas(t, "10", "3.14", "0", "150.75", "007");
        verificar(s.getErrors().isEmpty(), "nenhum erro léxico");
    }

    private static void testarLiteraisLogicos() {
        titulo("Literais lógicos");
        List<Token> t = tokens(new Scanner(".T. .F. .t. .f."));
        verificarTipos(t, TokenType.LOGICAL, TokenType.LOGICAL, TokenType.LOGICAL, TokenType.LOGICAL);
    }

    private static void testarMaximalMunch() {
        titulo("Maximal munch (sem espaços entre os tokens)");
        Scanner s = new Scanner("a:=b==c<=d<>e>=f=g");
        verificarTipos(tokens(s),
            TokenType.ID, TokenType.ASSIGN, TokenType.ID, TokenType.EQ, TokenType.ID,
            TokenType.LE, TokenType.ID, TokenType.NEQ, TokenType.ID, TokenType.GE,
            TokenType.ID, TokenType.EQ, TokenType.ID);

        // número seguido de literal lógico: "1.5.T." -> 1.5 e .T.
        List<Token> t = tokens(new Scanner("1.5.T."));
        verificarTipos(t, TokenType.NUM, TokenType.LOGICAL);
        verificarLexemas(t, "1.5", ".T.");

        // operador lógico colado em identificadores
        verificarTipos(tokens(new Scanner("lA.AND.lB")), TokenType.ID, TokenType.AND, TokenType.ID);
    }

    private static void testarLinhaEColuna() {
        titulo("Linha e coluna dos tokens");
        List<Token> t = tokens(new Scanner("x := 1\n  y := \"ab\""));
        verificar(t.get(0).line == 1 && t.get(0).col == 1, "'x' em 1:1");
        verificar(t.get(1).line == 1 && t.get(1).col == 3, "':=' em 1:3");
        verificar(t.get(3).line == 2 && t.get(3).col == 3, "'y' em 2:3");
        verificar(t.get(5).line == 2 && t.get(5).col == 8, "string em 2:8");
    }

    // ==================================================================
    // Casos de erro
    // ==================================================================

    private static void testarErroStringNaoFechadaAteEOF() {
        titulo("ERRO: string não fechada até o EOF");
        Scanner s = new Scanner("x := \"sem fim");
        List<Token> t = tokens(s);
        verificarTipos(t, TokenType.ID, TokenType.ASSIGN);
        verificarErro(s, 0, "string não fechada até o fim do arquivo", 1, 6);
    }

    private static void testarErroStringNaoFechadaAteFimDeLinha() {
        titulo("ERRO: string não fechada até o fim da linha (com recuperação)");
        Scanner s = new Scanner("cVar := \"Erro de quebra\nnNovaLinha := 1");
        List<Token> t = tokens(s);
        // a linha seguinte é tokenizada normalmente
        verificarTipos(t, TokenType.ID, TokenType.ASSIGN, TokenType.ID, TokenType.ASSIGN, TokenType.NUM);
        verificar(t.get(2).line == 2, "recuperação: 'nNovaLinha' reconhecido na linha 2");
        verificarErro(s, 0, "string não fechada até o fim da linha", 1, 9);
    }

    private static void testarErroCaractereForaDoAlfabeto() {
        titulo("ERRO: caractere fora do alfabeto (com recuperação)");
        Scanner s = new Scanner("x := 10 @ y $ 20 ç");
        List<Token> t = tokens(s);
        verificarTipos(t, TokenType.ID, TokenType.ASSIGN, TokenType.NUM, TokenType.ID, TokenType.NUM);
        verificar(s.getErrors().size() == 3, "3 erros reportados ('@', '$' e 'ç')");
        verificarErro(s, 0, "caractere fora do alfabeto '@'", 1, 9);
        verificarErro(s, 1, "caractere fora do alfabeto '$'", 1, 13);
        verificarErro(s, 2, "caractere fora do alfabeto 'ç'", 1, 18);
    }

    private static void testarErroComentarioDeBlocoAteEOF() {
        titulo("ERRO: comentário de bloco não fechado até o EOF");
        Scanner s = new Scanner("x := 1 /* comentário esquecido\n y := 2");
        List<Token> t = tokens(s);
        verificarTipos(t, TokenType.ID, TokenType.ASSIGN, TokenType.NUM);
        verificarErro(s, 0, "comentário de bloco não fechado até o fim do arquivo", 1, 8);
    }

    private static void testarErrosDeOperadores() {
        titulo("ERRO: operadores inválidos (':' sozinho, '.' inválido)");
        Scanner s = new Scanner("a : b");
        verificarTipos(tokens(s), TokenType.ID, TokenType.ID);
        verificarErro(s, 0, "':' deve ser seguido de '=' (atribuição ':=')", 1, 3);

        // ".AND" sem o ponto final: reporta o '.' e recupera "AND" como identificador
        Scanner s2 = new Scanner("x .AND y");
        verificarTipos(tokens(s2), TokenType.ID, TokenType.ID, TokenType.ID);
        verificar(s2.getErrors().size() == 1, "1 erro reportado para '.AND' sem ponto final");

        // palavra desconhecida entre pontos
        Scanner s3 = new Scanner("x .XOR. y");
        verificarTipos(tokens(s3), TokenType.ID, TokenType.ID);
        verificar(s3.getErrors().size() == 1, "1 erro reportado para '.XOR.'");
    }

    // ==================================================================
    // Código realista
    // ==================================================================

    private static void testarLinhaRealista() {
        titulo("Linha realista com comentários misturados");
        String linha = "LOCAL nTotal := nPreco * 2 + 10.5 /* bloco */ .AND. lOk // fim da linha";
        Scanner s = new Scanner(linha);
        List<Token> t = tokens(s);
        verificarTipos(t,
            TokenType.LOCAL, TokenType.ID, TokenType.ASSIGN, TokenType.ID, TokenType.MULT,
            TokenType.NUM, TokenType.PLUS, TokenType.NUM, TokenType.AND, TokenType.ID);
        verificar(s.getErrors().isEmpty(), "nenhum erro léxico");
    }

    private static void testarScriptCompleto() {
        titulo("Script Mini-ADVPL completo");
        String script =
            "// Rotina de Calculo e Validacao de Estoque\n" +
            "FUNCTION ControlaEstoque(nQtd)\n" +
            "    LOCAL nEstoqueAtual := 150.75\n" +
            "    LOCAL nLimiteMinimo := 50\n" +
            "    LOCAL lAtivo := .T.\n" +
            "\n" +
            "    /* Verifica se precisa\n" +
            "       repor o estoque */\n" +
            "    WHILE nEstoqueAtual <= nLimiteMinimo .AND. lAtivo == .T.\n" +
            "        nEstoqueAtual := nEstoqueAtual + 10.0\n" +
            "        IF nEstoqueAtual <> 150\n" +
            "            nEstoqueAtual := nEstoqueAtual + 1\n" +
            "        ELSE\n" +
            "            lAtivo := .F.\n" +
            "        ENDIF\n" +
            "    ENDDO\n" +
            "\n" +
            "    RETURN lAtivo\n" +
            "/* Fim da rotina */";

        Scanner s = new Scanner(script);
        List<Token> t = tokens(s);
        for (Token tk : t) System.out.println("    " + tk);

        verificar(t.size() == 47, "47 tokens gerados (obtido: " + t.size() + ")");
        verificar(s.getErrors().isEmpty(), "nenhum erro léxico");
        verificar(t.get(0).type == TokenType.FUNCTION && t.get(0).line == 2, "primeiro token é FUNCTION na linha 2");
        Token ultimo = t.get(t.size() - 1);
        verificar(ultimo.type == TokenType.ID && ultimo.lexeme.equals("lAtivo") && ultimo.line == 18,
                  "último token é 'lAtivo' na linha 18");
        verificar(t.stream().anyMatch(x -> x.type == TokenType.WHILE && x.line == 9),
                  "WHILE na linha 9 (comentário de bloco multilinha contou as linhas)");
    }

    // ==================================================================
    // Utilitários
    // ==================================================================

    /** Tokeniza tudo até EOF (o EOF não entra na lista). */
    private static List<Token> tokens(Scanner s) {
        List<Token> lista = new ArrayList<>();
        Token t;
        while ((t = s.nextToken()).type != TokenType.EOF) lista.add(t);
        return lista;
    }

    private static void titulo(String nome) {
        System.out.println("\n--- " + nome + " ---");
    }

    private static void verificar(boolean condicao, String descricao) {
        if (condicao) {
            aprovados++;
            System.out.println("  [OK]    " + descricao);
        } else {
            reprovados++;
            System.out.println("  [FALHA] " + descricao);
        }
    }

    private static void verificarTipos(List<Token> t, TokenType... esperados) {
        List<TokenType> obtidos = new ArrayList<>();
        for (Token tk : t) obtidos.add(tk.type);
        verificar(obtidos.equals(Arrays.asList(esperados)),
                  "tipos " + Arrays.toString(esperados) + (obtidos.equals(Arrays.asList(esperados)) ? "" : " | obtido: " + obtidos));
    }

    private static void verificarLexemas(List<Token> t, String... esperados) {
        List<String> obtidos = new ArrayList<>();
        for (Token tk : t) obtidos.add(tk.lexeme);
        verificar(obtidos.equals(Arrays.asList(esperados)),
                  "lexemas " + Arrays.toString(esperados) + (obtidos.equals(Arrays.asList(esperados)) ? "" : " | obtido: " + obtidos));
    }

    private static void verificarErro(Scanner s, int indice, String mensagem, int linha, int coluna) {
        List<LexicalError> erros = s.getErrors();
        boolean ok = indice < erros.size()
                && erros.get(indice).message.equals(mensagem)
                && erros.get(indice).line == linha
                && erros.get(indice).col == coluna;
        verificar(ok, "erro \"" + mensagem + "\" em " + linha + ":" + coluna
                  + (ok ? "" : " | obtido: " + (indice < erros.size() ? erros.get(indice) : "nenhum")));
    }
}
