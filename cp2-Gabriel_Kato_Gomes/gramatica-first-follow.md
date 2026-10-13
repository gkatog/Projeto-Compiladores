# Gramática, FIRST/FOLLOW e LL(1) — Mini-ADVPL
**Projeto de Construção de Compiladores — Checkpoint 2**

Os terminais são os tokens do `TokenType` do CP1 (scanner sem modificação). Cada não-terminal virou um método `parseX()` no `Parser.java`.

## 1. Decisões de sintaxe

* O programa é uma lista de funções; uma função termina na próxima `FUNCTION` ou no fim do arquivo.
* Tipos escritos no código: `LOCAL INT nX := 1`, `FUNCTION DOUBLE Media(INT nA, INT nB)`.
* **Procedimento** é uma `FUNCTION` sem tipo de retorno .
* **Retorno:** `RETURN(expr)` com valor e `RETURN()` sem valor.
* O `\n` não é token, então o `;` é um separador opcional de comandos.
* Precedência (menor → maior): `.OR.`, `.AND.`, `.NOT.`, relacionais, `+ -`, `* /`, `-` unário. Comparações não podem ser encadeadas (`a < b < c` é erro).

## 2. Correções (antes → depois)

### 2.1 Remoção da recursão à esquerda (expressões)

Na primeira versão, as expressões tinham recursão à esquerda direta. Apliquei a regra da aula: `A → A α | β` vira `A → β A'` e `A' → α A' | ε`.

**Antes (com recursão à esquerda):**
```
Expr    → Expr OR ExprAnd | ExprAnd
ExprAnd → ExprAnd AND ExprNot | ExprNot
ExprRel → ExprRel OpRel ExprAdd | ExprAdd
ExprAdd → ExprAdd OpAdd Termo | Termo
Termo   → Termo OpMul Unario | Unario
```

**Depois (sem recursão à esquerda):**
```
Expr         → ExprAnd ExprOrResto
ExprOrResto  → OR ExprAnd ExprOrResto | ε
ExprAnd      → ExprNot ExprAndResto
ExprAndResto → AND ExprNot ExprAndResto | ε
ExprRel      → ExprAdd ExprRelResto
ExprRelResto → OpRel ExprAdd | ε
ExprAdd      → Termo ExprAddResto
ExprAddResto → OpAdd Termo ExprAddResto | ε
Termo        → Unario TermoResto
TermoResto   → OpMul Unario TermoResto | ε
```
Em `ExprRelResto` a cauda não se repete, por isso `a < b < c` não é aceito. Não havia recursão à esquerda indireta: em nenhum não-terminal da gramática final a primeira chamada leva de volta a ele mesmo sem antes consumir um token.

### 2.2 Fatoração à esquerda e outros conflitos LL(1)

| # | Antes | Problema | Depois |
| :--- | :--- | :--- | :--- |
| 1 | `CmdId → ID ASSIGN Expr \| ID LPAREN Args RPAREN` | (a) `ID` no FIRST das duas alternativas | `CmdId → ID SufixoId`<br>`SufixoId → ASSIGN Expr \| LPAREN Args RPAREN` |
| 2 | `Primario → … \| ID \| ID LPAREN Args RPAREN`<br>`Var → ID \| ID ASSIGN Expr` | (a) mesmo prefixo `ID` | `Primario → … \| ID SufixoChamada`<br>`Var → ID Init` |
| 3 | `CmdReturn → RETURN RetOpt`<br>`RetOpt → Expr \| ε` | (c) FIRST(Expr) ∩ FOLLOW(RetOpt) = {`ID`}: em `RETURN` seguido de `nTotal := 1` na linha de baixo, o parser não sabe se `nTotal` é o valor do RETURN | `CmdReturn → RETURN LPAREN RetValor RPAREN`<br>`RetValor → Expr \| ε` (FOLLOW = {`RPAREN`}) |
| 4 | `Funcao → FUNCTION Tipo ID … \| FUNCTION ID …` | (a) mesmo prefixo `FUNCTION` | `Funcao → FUNCTION TipoRet ID …`<br>`TipoRet → Tipo \| ε` |

## 3. Gramática final (sem recursão à esquerda e fatorada)

```
Programa      → Funcao ListaFuncoes EOF
ListaFuncoes  → Funcao ListaFuncoes | ε
Funcao        → FUNCTION TipoRet ID LPAREN Params RPAREN ListaCmd
TipoRet       → Tipo | ε
Params        → Param ParamsResto | ε
ParamsResto   → COMMA Param ParamsResto | ε
Param         → Tipo ID
Tipo          → INT | DOUBLE | BOOL | CHAR | STRING_TYPE
ListaCmd      → Cmd ListaCmd | ε
Cmd           → CmdBase PontoVirgula
PontoVirgula  → SEMI | ε
CmdBase       → Decl | CmdId | CmdIf | CmdWhile | CmdReturn
Decl          → LOCAL Tipo Var ListaVar
ListaVar      → COMMA Var ListaVar | ε
Var           → ID Init
Init          → ASSIGN Expr | ε
CmdId         → ID SufixoId
SufixoId      → ASSIGN Expr | LPAREN Args RPAREN
CmdIf         → IF Expr ListaCmd Senao ENDIF
Senao         → ELSE ListaCmd | ε
CmdWhile      → WHILE Expr ListaCmd ENDDO
CmdReturn     → RETURN LPAREN RetValor RPAREN
RetValor      → Expr | ε
Expr          → ExprAnd ExprOrResto
ExprOrResto   → OR ExprAnd ExprOrResto | ε
ExprAnd       → ExprNot ExprAndResto
ExprAndResto  → AND ExprNot ExprAndResto | ε
ExprNot       → NOT ExprNot | ExprRel
ExprRel       → ExprAdd ExprRelResto
ExprRelResto  → OpRel ExprAdd | ε
OpRel         → EQ | NEQ | LT | LE | GT | GE
ExprAdd       → Termo ExprAddResto
ExprAddResto  → OpAdd Termo ExprAddResto | ε
OpAdd         → PLUS | MINUS
Termo         → Unario TermoResto
TermoResto    → OpMul Unario TermoResto | ε
OpMul         → MULT | DIV
Unario        → MINUS Unario | Primario
Primario      → NUM | LIT_STRING | LOGICAL | LPAREN Expr RPAREN | ID SufixoChamada
SufixoChamada → LPAREN Args RPAREN | ε
Args          → Expr ArgsResto | ε
ArgsResto     → COMMA Expr ArgsResto | ε
```

## 4. FIRST e FOLLOW

Abreviações:
* **TIPOS** = {`INT`, `DOUBLE`, `BOOL`, `CHAR`, `STRING_TYPE`}
* **CMD** = {`LOCAL`, `ID`, `IF`, `WHILE`, `RETURN`} (início de comando)
* **FIM** = {`ELSE`, `ENDIF`, `ENDDO`, `FUNCTION`, `EOF`} (fim de bloco)
* **OPND** = {`NUM`, `LIT_STRING`, `LOGICAL`, `LPAREN`, `ID`, `MINUS`} (início de operando)
* **REL** = {`EQ`, `NEQ`, `LT`, `LE`, `GT`, `GE`}
* **FE** = CMD ∪ FIM ∪ {`SEMI`, `COMMA`, `RPAREN`} (o que vem depois de uma expressão)

| Não-terminal | FIRST | FOLLOW |
| :--- | :--- | :--- |
| Programa | `FUNCTION` | — |
| ListaFuncoes | `FUNCTION`, ε | `EOF` |
| Funcao | `FUNCTION` | `FUNCTION`, `EOF` |
| TipoRet | TIPOS, ε | `ID` |
| Params | TIPOS, ε | `RPAREN` |
| ParamsResto | `COMMA`, ε | `RPAREN` |
| Param | TIPOS | `COMMA`, `RPAREN` |
| Tipo | TIPOS | `ID` |
| ListaCmd | CMD, ε | FIM |
| Cmd | CMD | CMD ∪ FIM |
| PontoVirgula | `SEMI`, ε | CMD ∪ FIM |
| CmdBase | CMD | CMD ∪ FIM ∪ {`SEMI`} |
| Decl | `LOCAL` | CMD ∪ FIM ∪ {`SEMI`} |
| CmdId | `ID` | CMD ∪ FIM ∪ {`SEMI`} |
| CmdIf | `IF` | CMD ∪ FIM ∪ {`SEMI`} |
| CmdWhile | `WHILE` | CMD ∪ FIM ∪ {`SEMI`} |
| CmdReturn | `RETURN` | CMD ∪ FIM ∪ {`SEMI`} |
| SufixoId | `ASSIGN`, `LPAREN` | CMD ∪ FIM ∪ {`SEMI`} |
| ListaVar | `COMMA`, ε | CMD ∪ FIM ∪ {`SEMI`} |
| Var | `ID` | CMD ∪ FIM ∪ {`SEMI`, `COMMA`} |
| Init | `ASSIGN`, ε | CMD ∪ FIM ∪ {`SEMI`, `COMMA`} |
| Senao | `ELSE`, ε | `ENDIF` |
| RetValor | OPND ∪ {`NOT`}, ε | `RPAREN` |
| Expr | OPND ∪ {`NOT`} | FE |
| ExprOrResto | `OR`, ε | FE |
| ExprAnd | OPND ∪ {`NOT`} | FE ∪ {`OR`} |
| ExprAndResto | `AND`, ε | FE ∪ {`OR`} |
| ExprNot | OPND ∪ {`NOT`} | FE ∪ {`OR`, `AND`} |
| ExprRel | OPND | FE ∪ {`OR`, `AND`} |
| ExprRelResto | REL, ε | FE ∪ {`OR`, `AND`} |
| OpRel | REL | OPND |
| ExprAdd | OPND | FE ∪ {`OR`, `AND`} ∪ REL |
| ExprAddResto | `PLUS`, `MINUS`, ε | FE ∪ {`OR`, `AND`} ∪ REL |
| OpAdd | `PLUS`, `MINUS` | OPND |
| Termo | OPND | FOLLOW(ExprAdd) ∪ {`PLUS`, `MINUS`} |
| TermoResto | `MULT`, `DIV`, ε | FOLLOW(ExprAdd) ∪ {`PLUS`, `MINUS`} |
| OpMul | `MULT`, `DIV` | OPND |
| Unario | OPND | FOLLOW(Termo) ∪ {`MULT`, `DIV`} |
| Primario | `NUM`, `LIT_STRING`, `LOGICAL`, `LPAREN`, `ID` | FOLLOW(Unario) |
| SufixoChamada | `LPAREN`, ε | FOLLOW(Unario) |
| Args | OPND ∪ {`NOT`}, ε | `RPAREN` |
| ArgsResto | `COMMA`, ε | `RPAREN` |

## 5. Verificação LL(1)


| Não-terminal | (a) FIRST das alternativas disjuntos | (b) só uma anulável | (c) FIRST(alt. não vazia) ∩ FOLLOW = ∅ |
| :--- | :--- | :--- | :--- |
| Tipo | 5 tokens diferentes ✓ | nenhuma ✓ | — |
| CmdBase | `LOCAL` / `ID` / `IF` / `WHILE` / `RETURN` ✓ | nenhuma ✓ | — |
| SufixoId | `ASSIGN` / `LPAREN` ✓ | nenhuma ✓ | — |
| ExprNot | `NOT` / OPND ✓ | nenhuma ✓ | — |
| Unario | `MINUS` / {`NUM`,`LIT_STRING`,`LOGICAL`,`LPAREN`,`ID`} ✓ | nenhuma ✓ | — |
| Primario | `NUM` / `LIT_STRING` / `LOGICAL` / `LPAREN` / `ID` ✓ | nenhuma ✓ | — |
| OpRel, OpAdd, OpMul | um token por alternativa ✓ | nenhuma ✓ | — |
| ListaFuncoes | ✓ | só ε ✓ | {`FUNCTION`} ∩ {`EOF`} = ∅ ✓ |
| TipoRet | ✓ | só ε ✓ | TIPOS ∩ {`ID`} = ∅ ✓ |
| Params | ✓ | só ε ✓ | TIPOS ∩ {`RPAREN`} = ∅ ✓ |
| ParamsResto | ✓ | só ε ✓ | {`COMMA`} ∩ {`RPAREN`} = ∅ ✓ |
| ListaCmd | ✓ | só ε ✓ | CMD ∩ FIM = ∅ ✓ |
| PontoVirgula | ✓ | só ε ✓ | {`SEMI`} ∩ (CMD ∪ FIM) = ∅ ✓ |
| ListaVar | ✓ | só ε ✓ | {`COMMA`} ∩ FOLLOW = ∅ ✓ |
| Init | ✓ | só ε ✓ | {`ASSIGN`} ∩ FOLLOW = ∅ ✓ |
| Senao | ✓ | só ε ✓ | {`ELSE`} ∩ {`ENDIF`} = ∅ ✓ |
| RetValor | ✓ | só ε ✓ | FIRST(Expr) ∩ {`RPAREN`} = ∅ ✓ |
| ExprOrResto | ✓ | só ε ✓ | {`OR`} ∩ FE = ∅ ✓ |
| ExprAndResto | ✓ | só ε ✓ | {`AND`} ∩ FOLLOW = ∅ ✓ |
| ExprRelResto | ✓ | só ε ✓ | REL ∩ FOLLOW = ∅ ✓ |
| ExprAddResto | ✓ | só ε ✓ | {`PLUS`,`MINUS`} ∩ FOLLOW = ∅ ✓ (*) |
| TermoResto | ✓ | só ε ✓ | {`MULT`,`DIV`} ∩ FOLLOW = ∅ ✓ |
| SufixoChamada | ✓ | só ε ✓ | {`LPAREN`} ∩ FOLLOW = ∅ ✓ (*) |
| Args | ✓ | só ε ✓ | FIRST(Expr) ∩ {`RPAREN`} = ∅ ✓ |
| ArgsResto | ✓ | só ε ✓ | {`COMMA`} ∩ {`RPAREN`} = ∅ ✓ |

