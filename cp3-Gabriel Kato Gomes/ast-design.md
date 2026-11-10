# Projeto da AST — Mini-ADVPL
**Projeto de Construção de Compiladores — Checkpoint 3**

A AST parte da gramática do CP2, que não mudou. Segui a Aula 7: um tipo de nó por **categoria sintática** que importa para as próximas fases (uma classe abstrata por categoria e uma subclasse por forma), e não um nó por não-terminal.

## 1. Hierarquia de nós

Classes abstratas:

| Classe | Atributos | Observação |
| :--- | :--- | :--- |
| `Node` | `int linha`, `int coluna` | Raiz de todos os nós; declara `abstract <T> T accept(Visitor<T> v)` |
| `Cmd extends Node` | — | Categoria dos comandos |
| `Expr extends Node` | — | Categoria das expressões (na análise semântica ganha o campo do tipo) |

Nós concretos:

| Nó | Atributos (tipo) | Produções do CP2 que geram |
| :--- | :--- | :--- |
| `Programa` | `List<FuncDecl> funcoes` | `Programa → Funcao ListaFuncoes EOF`, `ListaFuncoes → Funcao ListaFuncoes \| ε` |
| `FuncDecl` | `Tipo tipoRetorno` (null = procedimento), `String nome`, `List<ParamDecl> params`, `Bloco corpo` | `Funcao → FUNCTION TipoRet ID ( Params ) ListaCmd`, `TipoRet → Tipo \| ε` |
| `ParamDecl` | `Tipo tipo`, `String nome` | `Param → Tipo ID` (a lista vem de `Params`/`ParamsResto`) |
| `Bloco` (é um `Cmd`) | `List<Cmd> comandos` | `ListaCmd → Cmd ListaCmd \| ε` (corpo de função, de IF, de ELSE e de WHILE) |
| `LocalCmd` | `Tipo tipo`, `List<VarDecl> vars` | `Decl → LOCAL Tipo Var ListaVar`, `ListaVar → , Var ListaVar \| ε` |
| `VarDecl` | `String nome`, `Expr inicial` (null = sem valor inicial) | `Var → ID Init`, `Init → := Expr \| ε` |
| `AtribCmd` | `IdExpr alvo`, `Expr valor` | `CmdId → ID SufixoId` com `SufixoId → := Expr` |
| `ChamadaCmd` | `ChamadaExpr chamada` | `CmdId → ID SufixoId` com `SufixoId → ( Args )` |
| `IfCmd` | `Expr condicao`, `Bloco entao`, `Bloco senao` (null = sem ELSE) | `CmdIf → IF Expr ListaCmd Senao ENDIF`, `Senao → ELSE ListaCmd \| ε` |
| `WhileCmd` | `Expr condicao`, `Bloco corpo` | `CmdWhile → WHILE Expr ListaCmd ENDDO` |
| `ReturnCmd` | `Expr valor` (null = RETURN sem valor) | `CmdReturn → RETURN ( RetValor )`, `RetValor → Expr \| ε` |
| `IntExpr` | `String texto` | `Primario → NUM` (sem `.`) |
| `DoubleExpr` | `String texto` | `Primario → NUM` (com `.`) |
| `BoolExpr` | `boolean valor` | `Primario → LOGICAL` |
| `CharExpr` | `char valor` | `Primario → LIT_STRING` com exatamente 1 caractere |
| `StringExpr` | `String valor` | `Primario → LIT_STRING` com 0 ou 2+ caracteres |
| `IdExpr` | `String nome` | `Primario → ID SufixoChamada` com `SufixoChamada → ε` |
| `ChamadaExpr` | `String nome`, `List<Expr> args` | `Primario → ID SufixoChamada` com `SufixoChamada → ( Args )`, `Args`, `ArgsResto` |
| `BinExpr` | `Expr esq`, `OpBin op`, `Expr dir` | `ExprOrResto`, `ExprAndResto`, `ExprRelResto`, `ExprAddResto`, `TermoResto` |
| `UnExpr` | `OpUn op`, `Expr operando` | `ExprNot → NOT ExprNot`, `Unario → MINUS Unario` |

Tipos auxiliares (enums, não são nós):
* `Tipo` = `INT`, `DOUBLE`, `BOOL`, `CHAR`, `STRING`. É o mesmo enum que a análise semântica vai usar para o tipo das expressões.
* `OpBin` = `OR`, `AND`, `EQ`, `NEQ`, `LT`, `LE`, `GT`, `GE`, `ADD`, `SUB`, `MUL`, `DIV`.
* `OpUn` = `NOT`, `NEG`.

## 2. Mapeamento da gramática para a árvore

Cada um dos 42 não-terminais do CP2 cai em um destes casos.

**Viram nó:**

| Não-terminal | Nó |
| :--- | :--- |
| Programa | `Programa` |
| Funcao | `FuncDecl` |
| Param | `ParamDecl` |
| ListaCmd | `Bloco` (quem chama `ListaCmd` embrulha a lista num `Bloco`) |
| Decl | `LocalCmd` |
| Var | `VarDecl` |
| CmdIf / CmdWhile / CmdReturn | `IfCmd` / `WhileCmd` / `ReturnCmd` |
| Primario | `IntExpr`, `DoubleExpr`, `BoolExpr`, `CharExpr`, `StringExpr`, `IdExpr` ou `ChamadaExpr` (no caso `( Expr )`, devolve o nó de dentro) |
| ExprNot | `UnExpr(NOT)` quando tem `.NOT.`; senão repassa o nó de `ExprRel` |
| Unario | `UnExpr(NEG)` quando tem `-`; senão repassa o nó de `Primario` |

**Viram atributo de outro nó:**

| Não-terminal | Vira |
| :--- | :--- |
| Tipo | `Tipo` (enum) em `ParamDecl`, `LocalCmd` e `FuncDecl` |
| ListaFuncoes | lista `funcoes` de `Programa` |
| Params, ParamsResto | lista `params` de `FuncDecl` |
| ListaVar | lista `vars` de `LocalCmd` |
| Senao | `senao` de `IfCmd` (null quando é ε) |
| RetValor | `valor` de `ReturnCmd` (null quando é ε) |
| Args, ArgsResto | lista `args` de `ChamadaExpr` |
| OpRel, OpAdd, OpMul | `op` de `BinExpr` |

**Não viram nada**, e são os três casos da Aula 7:

* **(a) Criados para a precedência:** `Expr`, `ExprAnd`, `ExprNot` (sem `.NOT.`), `ExprRel`, `ExprAdd`, `Termo`, `Unario` (sem `-`). Cada nível só existe para o parser saber quem liga mais forte. Na AST, a precedência já está na forma da árvore, então esses métodos só repassam o nó que receberam de baixo.
* **(b) Criados para eliminar a recursão à esquerda:** `ExprOrResto`, `ExprAndResto`, `ExprRelResto`, `ExprAddResto`, `TermoResto`. São os E' e T' da aula. Recebem o operando esquerdo já construído e devolvem o `BinExpr` maior, ou o próprio operando no ε. Isso garante que `a - b - c` vire `(a - b) - c`.
* **(c) Criados pela fatoração:** `SufixoId` (correção 1 da Seção 2.2 do `gramatica-first-follow.md`), `SufixoChamada` e `Init` (correção 2) e `TipoRet` (correção 4). Eles só existem para adiar a decisão até depois do prefixo comum. `SufixoId` e `SufixoChamada` recebem o token `ID` já lido (o prefixo) e devolvem o nó certo (`AtribCmd`/`ChamadaCmd` ou `IdExpr`/`ChamadaExpr`). `Init` e `TipoRet` viram um atributo que pode ser null. `CmdId` também não vira nó: ele só guarda o prefixo `ID` para o `SufixoId`.
* **Outros sem nó:** `CmdBase` só escolhe a alternativa e devolve o nó dela. `Cmd` devolve o nó do `CmdBase`. `PontoVirgula` é só o `;` opcional.
* **Pontuação:** parênteses, vírgulas e `;` não viram nó. Os parênteses já ficam representados na forma da árvore.

## 3. Decisões de representação

* **Função e procedimento são o mesmo nó** (`FuncDecl`). O procedimento tem `tipoRetorno == null`, igual à gramática, em que procedimento é `FUNCTION` sem tipo. Assim a tabela de símbolos trata os dois do mesmo jeito.
* **`Bloco` é um comando.** A gramática não tem bloco explícito, mas o corpo de função, de IF, de ELSE e de WHILE é sempre uma `ListaCmd`, e esse é o lugar onde a Aula 11 abre um escopo novo.
* **`LOCAL INT a := 1, b` vira um `LocalCmd` com dois `VarDecl`.** Cada variável declarada é um nó próprio, porque cada uma vai virar um símbolo.
* **O alvo da atribuição é um `IdExpr`.** Assim todo uso de nome é um `IdExpr`, e é nele que o ponteiro para o símbolo vai ficar.
* **A chamada é um único nó, `ChamadaExpr`.** Como comando, ela fica dentro de um `ChamadaCmd`, para não duplicar nome e argumentos.
* **Números guardam o texto do lexema** (`"10"`, `"2.5"`). Assim um número grande demais não quebra o parser, e conferir a faixa de valores fica para a semântica.
* **Char ou string:** o scanner do CP1 não distingue `'a'` de `"a"`. O parser cria `CharExpr` quando o literal tem exatamente um caractere e `StringExpr` nos outros casos. A semântica vai aceitar um `CharExpr` onde se espera `STRING`.
* **Programa com erro:** o `parse()` devolve um `Resultado` com a lista de erros. Se houver qualquer erro léxico ou sintático, a árvore vem **null**, e as fases seguintes nunca recebem uma árvore incompleta.

## 4. Posição

Todo nó guarda `linha` e `coluna` do **primeiro token** que o originou. Por exemplo, um `BinExpr` de `a + b` fica com a posição do `a`, um `IfCmd` com a do `IF` e um `Bloco` com a do primeiro token da lista. O `Token` do CP1 já tem linha e coluna, então não precisei mudar o scanner.

## 5. Campos para as próximas fases

Nenhum foi implementado ainda, mas a forma da árvore não precisa mudar para recebê-los:
* **Tabela de símbolos (Aula 11):** um campo `Symbol simbolo` em `IdExpr` e `ChamadaExpr` (usos) e em `VarDecl`, `ParamDecl` e `FuncDecl` (declarações). Todo uso e toda declaração de nome já são nós próprios.
* **Tipos (análise semântica):** um campo `Tipo tipo` na classe `Expr`, que todas as expressões herdam.

Acrescentar um campo numa classe não muda o `accept`, nem o `Visitor`, nem os visitors já existentes.

## 6. Formato do impressor de árvore (`ImpressoraArvore`)

* **Um nó por linha**, no formato `Nome(atributos)`. Os filhos aparecem nas linhas seguintes, com **2 espaços a mais** de indentação por nível. A raiz não tem indentação.
* **Filhos na ordem dos campos da tabela da Seção 1.** Um filho null (ELSE, valor inicial, valor do RETURN) simplesmente não aparece, o que já distingue, por exemplo, `IF` com e sem `ELSE`.
* **A posição não é impressa.** Assim a árvore de um programa e a do mesmo programa reimpresso são iguais (teste de ida e volta).

| Nó | Linha impressa | Nó | Linha impressa |
| :--- | :--- | :--- | :--- |
| `Programa` | `Programa` | `IntExpr` | `Int(10)` |
| `FuncDecl` | `FuncDecl(Media, DOUBLE)` ou `FuncDecl(Avisa, sem retorno)` | `DoubleExpr` | `Double(2.5)` |
| `ParamDecl` | `Param(nA, INT)` | `BoolExpr` | `Bool(.T.)` ou `Bool(.F.)` |
| `Bloco` | `Bloco` | `CharExpr` | `Char('A')` |
| `LocalCmd` | `Local(INT)` | `StringExpr` | `String("TOTVS")` |
| `VarDecl` | `Var(nX)` | `IdExpr` | `Id(nX)` |
| `AtribCmd` | `Atrib` | `ChamadaExpr` | `Chamada(Media)` |
| `ChamadaCmd` | `ChamadaCmd` | `BinExpr` | `Bin(+)`; os operadores são `+ - * / == <> < <= > >= .AND. .OR.` |
| `IfCmd` / `WhileCmd` | `If` / `While` | `UnExpr` | `Un(-)` ou `Un(.NOT.)` |
| `ReturnCmd` | `Return` | | |

Exemplo, `a - b - c * d`:
```
Bin(-)
  Bin(-)
    Id(a)
    Id(b)
  Bin(*)
    Id(c)
    Id(d)
```

## 7. Formato do reimpressor (`Reimpressora`)

Ele gera o programa na forma canônica: palavras reservadas em maiúsculas, um comando por linha, 4 espaços por nível de bloco, uma linha em branco entre funções, e `RETURN(...)` sempre com parênteses. **Toda expressão binária e toda unária ficam entre parênteses**, como `((a - b) - c)` e `(-x)`. Assim o texto gerado sempre é aceito pelo parser e reproduz a mesma árvore, inclusive em casos como `-(.NOT. x)`, que sem parênteses não seriam válidos na gramática. Strings usam aspas duplas, a menos que o conteúdo tenha uma `"`.

O segundo visitor (o reimpressor) **não exigiu mudar nenhuma classe de nó**. Ele só implementa `Visitor<String>`, e os nós continuam com seu único `accept`.

## 8. Árvore desenhada à mão

Programa:
```
FUNCTION INT Soma(INT n)
    LOCAL INT s := 0
    WHILE n > 0
        IF n <> 3
            s := s + n
        ENDIF
        n := n - 1
    ENDDO
    RETURN(s)
```

AST (29 nós):
```
Programa
└─ FuncDecl (Soma, INT)
   ├─ ParamDecl (n, INT)
   └─ Bloco
      ├─ LocalCmd (INT)
      │  └─ VarDecl (s)
      │     └─ IntExpr 0
      ├─ WhileCmd
      │  ├─ BinExpr (>)
      │  │  ├─ IdExpr n
      │  │  └─ IntExpr 0
      │  └─ Bloco
      │     ├─ IfCmd
      │     │  ├─ BinExpr (<>)
      │     │  │  ├─ IdExpr n
      │     │  │  └─ IntExpr 3
      │     │  └─ Bloco
      │     │     └─ AtribCmd
      │     │        ├─ IdExpr s
      │     │        └─ BinExpr (+)
      │     │           ├─ IdExpr s
      │     │           └─ IdExpr n
      │     └─ AtribCmd
      │        ├─ IdExpr n
      │        └─ BinExpr (-)
      │           ├─ IdExpr n
      │           └─ IntExpr 1
      └─ ReturnCmd
         └─ IdExpr s
```

Pedaços da árvore de derivação que a AST descartou:
1. **A cadeia de precedência.** O `n` de `n > 0` passa, na derivação, por `Expr → ExprAnd → ExprNot → ExprRel → ExprAdd → Termo → Unario → Primario → ID SufixoChamada`, com `SufixoChamada → ε`, `TermoResto → ε` e `ExprAddResto → ε` pelo caminho. Na AST tudo isso é um único `IdExpr n`.
2. **Os "Resto" da recursão à esquerda.** Em `s + n`, a derivação tem `ExprAddResto → OpAdd Termo ExprAddResto` e depois `ExprAddResto → ε`, mais um `TermoResto → ε` para cada operando. Na AST sobra um `BinExpr (+)` com dois filhos.
3. **A fatoração do comando com ID.** `s := s + n` é `CmdId → ID SufixoId` com `SufixoId → := Expr`. Na AST isso vira direto um `AtribCmd`, sem `CmdId` nem `SufixoId`.
4. **A lista de comandos e a pontuação.** O corpo do `WHILE` é `ListaCmd → Cmd ListaCmd → Cmd ListaCmd → ε`, com `Cmd → CmdBase PontoVirgula` e `PontoVirgula → ε` em cada comando. Na AST fica um `Bloco` com uma lista de dois comandos. Os parênteses de `RETURN(s)` e o `RetValor` também somem: sobra um `ReturnCmd` com o `IdExpr s`.
