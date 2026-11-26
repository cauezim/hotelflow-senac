# Etapa E7 – Diagrama de Sequência UML (Pagamento, Liquidação e Encerramento)
**Projeto:** HotelFlow – Sistema de Gestão Hoteleira (SENAC)  
**Disciplinas Integradas:** Análise Orientada a Objetos (AOO) e Programação Orientada a Objetos (POO)  
**Código-fonte PlantUML:** [E7_Diagrama_de_Sequencia.puml](diagramas/E7_Diagrama_de_Sequencia.puml) (`[Atende RN01, RN04, RN07, RN10..RN19, D02..D06]`)

---

## 1. Escopo e Alinhamento Arquitetural (BCE + Camadas)

Este Diagrama de Sequência modela ponta a ponta o cenário crítico especificado na [Etapa E3](E3_Especificacao_Caso_de_Uso.md), integrando o caso base **`UC13 – Processar Check-out e Faturamento`**, a inclusão **`UC14 – Registrar Pagamento da Fatura`** e as extensões condicionais **`UC16 – Aplicar Cupom de Desconto`** (`PE01`) e **`UC15 – Emitir Recibo de Hospedagem`** (`PE02`).

Todas as chamadas sobre objetos de negócio obedecem **100% às assinaturas do Diagrama de Classes ([E5](E5_Diagrama_de_Classes.md))**, separando claramente os estereótipos arquiteturais:
- **Atores:** `Recepcionista` (operação padrão) e `Gerente` (autorização de desconto — `RN14`);
- **Boundary (`view`):** `TelaCheckOut`;
- **Control (`service`):** `CheckOutService` (orquestração e demarcação transacional) e `GeradorReciboService` (formatação textual desacoplada — `RN18`);
- **Persistência (`dao`):** `CheckOutDAO` (evitando o antipadrão de fazer entidades buscarem a si próprias no banco de dados);
- **Entities (`model`):** `Usuario`, `Reserva`, `Quarto`, `CategoriaQuarto`, `Fatura`, `CupomDesconto` e `Pagamento`.

---

## 2. Decisões Técnicas Representadas nas Interações

1. **Prevenção de Duplicidade e Ajuste por Prorrogação na Retomada (`FA01` — `[Atende RN16, D05, D06]`):**
   - Antes de instanciar (`<<create>>`) uma nova `Fatura`, o `CheckOutService` consulta `buscarFaturaPorReserva(idReserva)`. Caso já exista fatura aberta, chama `fatura.atualizarSeProrrogada(hoje)` (cobrindo eventuais diárias adicionais se o hóspede permaneceu mais noites) sem zerar pagamentos prévios nem alterar a tarifa congelada.
2. **Consolidação Explícita da Fatura (`[Atende RN04, RN10, RN11, RN13, D05, D06]`):**
   - Na primeira abertura, `Fatura` interage explicitamente com `Reserva.calcularDiarias(hoje)`, `Quarto -> CategoriaQuarto.getTarifaDiaria()`, `Reserva.calcularTotalConsumos()` e `Reserva.calcularTotalServicos()`, congelando `tarifaDiariaAplicada` e `valorBruto`.
3. **Dupla Barreira de Segurança no Desconto (`PE01` / `UC16` — `[Atende RN01, RN14, D03, D04, D06]`):**
   - O fragmento `opt` verifica primeiro a autorização via `usuarioGerente.possuiPermissao("APLICAR_DESCONTO")`, depois garante `fatura.calcularTotalPago() == 0` (bloqueando descontos pós-parcelamento) e valida `cupom.validar()`, devolvendo o demonstrativo atualizado para a `TelaCheckOut`.
4. **Fracionamento de Pagamentos em Laço (`UC14` — `[Atende RN15, RN16]`):**
   - O fragmento `loop` permite múltiplos lançamentos (`PIX`, `Cartão de Crédito`, `Dinheiro`) na mesma sessão ou a interrupção parcial (`FA03b`), instanciando objetos `Pagamento` via mensagem `<<create>>`.
5. **Encerramento em Transação Única, Responsabilidade Única e Recibo Pós-Commit (`PE02` / `UC15` — `[Atende RN07, RN16, RN17, RN18, RN19]`):**
   - A parcela quitadora e o encerramento compartilham a **mesma transação JDBC (`commitPagamentoFinalEEncerramento`)**, impedindo que uma falha deixe a fatura `PAGA` com o quarto ainda `OCUPADO`.
   - A responsabilidade de chamar `quarto.liberar()` e `quarto.atualizarLimpeza(SUJO)` fica encapsulada exclusivamente dentro de `reserva.finalizar(agora)`, sem duplicidade no serviço.
   - Somente após o `commit` bem-sucedido é acionado `GeradorReciboService.emitirReciboTexto(fatura)`. No ramo `else`, se nenhum ou apenas parte do pagamento ocorreu, a fatura permanece `AGUARDANDO_PAGAMENTO` ou `PARCIALMENTE_PAGA`.

---

## 3. Rastreabilidade das Mensagens no Diagrama

| Requisito | Mensagens / Fragmentos Correspondentes |
| :--- | :--- |
| **RN01, D04** | `possuiPermissao("APLICAR_DESCONTO")` sobre `usuarioGerente : Usuario` |
| **RN04, RN10, RN11, RN13** | `<<create>> new Fatura(...)`, `calcularDiarias()`, `getTarifaDiaria()`, `calcularTotalConsumos()`, `calcularTotalServicos()`, `calcularValorBruto()` |
| **RN14, D03, D06** | Fragmento `opt [PE01]`, `buscarCupomPorCodigo()`, `cupom.validar()`, `fatura.aplicarDesconto(cupom)`, `calcularDesconto(valorBruto)` |
| **RN15, RN16** | Fragmento `loop`, `<<create>> new Pagamento(...)`, `fatura.registrarPagamento(pagamento)`, `calcularSaldoDevedor()`, ramo `else [quitada == false]` |
| **RN07, RN17, RN19, D05** | `group Transação Atômica de Encerramento`: `reserva.finalizar(agora)`, `quarto.liberar()`, `quarto.atualizarLimpeza(SUJO)` |
| **RN18** | `GeradorReciboService.emitirReciboTexto(fatura)` e retorno `concluirCheckOut(textoReciboFormatado)` |

---

## 4. Código PlantUML Consolidado

*(Espelho exato do arquivo [E7_Diagrama_de_Sequencia.puml](diagramas/E7_Diagrama_de_Sequencia.puml))*

```plantuml
@startuml E7_Diagrama_de_Sequencia
hide footbox
skinparam shadowing false
autonumber

title Diagrama de Sequência UML – Processar Check-out, Pagamento e Encerramento (UC13..UC16) [Atende RN01, RN04, RN07, RN10..RN19]

actor "Recepcionista" as Rec
actor "Gerente" as Ger
boundary "TelaCheckOut" as Tela
control "CheckOutService" as Ctrl
database "CheckOutDAO
(Reserva/Fatura/Cupom)" as DAO
entity "usuarioGerente :
Usuario" as Usr
entity "reserva :
Reserva" as Res
entity "quarto :
Quarto" as Qto
entity "categoria :
CategoriaQuarto" as Cat
entity "fatura :
Fatura" as Fat
entity "cupom :
CupomDesconto" as Cup
entity "pagamento :
Pagamento" as Pag
control "GeradorReciboService" as ReciboSvc

' =========================================================
' 1. ABERTURA OU RETOMADA DO CHECK-OUT [RN07, RN13, RN16, D05, D06]
' =========================================================
Rec -> Tela : iniciarCheckOut(idReserva)
activate Tela
Tela -> Ctrl : iniciarCheckOut(idReserva)
activate Ctrl

Ctrl -> DAO : buscarReservaPorId(idReserva)
activate DAO
DAO --> Ctrl : reserva
deactivate DAO

alt reserva == null || reserva.getStatus() != EM_ANDAMENTO [FE01, FE02]
    Ctrl --> Tela : lancarExcecao("Reserva inexistente ou fora de Em Andamento")
    Tela --> Rec : exibirMensagemErro()
else reserva EM_ANDAMENTO [RN07]
    Ctrl -> DAO : buscarFaturaPorReserva(idReserva)
    activate DAO
    DAO --> Ctrl : faturaExistente
    deactivate DAO

    alt faturaExistente != null [FA01 – Retomada de Fatura Aberta]
        Ctrl -> Fat : atualizarSeProrrogada(hoje)
        Ctrl -> Fat : calcularValorLiquido()
        Fat --> Ctrl : valorLiquido
    else primeira abertura do Check-out [RN13, D05, D06]
        Ctrl -> Fat ** : <<create>> new Fatura(reserva, agora)
        activate Fat
        Fat -> Res : calcularDiarias(hoje)
        activate Res
        Res --> Fat : qtdDiarias
        deactivate Res

        Fat -> Qto : getCategoria()
        Qto -> Cat : getTarifaDiaria()
        Cat --> Fat : tarifaDiariaAplicada

        Fat -> Res : calcularTotalConsumos()
        Res --> Fat : totalFrigobar
        Fat -> Res : calcularTotalServicos()
        Res --> Fat : totalServicos

        Fat -> Fat : calcularValorBruto() [congela tarifa e bruto]
        deactivate Fat

        Ctrl -> DAO : salvarFatura(fatura)
    end

    Ctrl -> Fat : calcularSaldoDevedor()
    Fat --> Ctrl : saldoAtual
    Ctrl --> Tela : apresentarDemonstrativo(fatura, saldoAtual)
    Tela --> Rec : exibirValoresDetalhados()

    ' =========================================================
    ' 2. EXTENSÃO OPCIONAL PE01 – CUPOM DE DESCONTO (UC16) [RN01, RN14, D03, D06]
    ' =========================================================
    opt [PE01: Gerente solicita desconto antes do 1º pagamento]
        Ger -> Tela : informarCupom(codigoCupom, usuarioGerente)
        Tela -> Ctrl : aplicarCupom(idFatura, codigoCupom, usuarioGerente)
        Ctrl -> Usr : possuiPermissao("APLICAR_DESCONTO")
        activate Usr
        Usr --> Ctrl : autorizado
        deactivate Usr

        alt autorizado == true [RN01, RN14, D04]
            Ctrl -> DAO : buscarCupomPorCodigo(codigoCupom)
            activate DAO
            DAO --> Ctrl : cupom
            deactivate DAO

            alt cupom != null && fatura.calcularTotalPago() == 0 [D03, D06]
                Ctrl -> Cup : validar()
                activate Cup
                Cup --> Ctrl : cupomAtivo
                deactivate Cup

                alt cupomAtivo == true
                    Ctrl -> Fat : aplicarDesconto(cupom)
                    activate Fat
                    Fat -> Cup : calcularDesconto(valorBruto)
                    Cup --> Fat : valorDesconto
                    Fat -> Fat : calcularValorLiquido() [congela desconto]
                    deactivate Fat
                    Ctrl -> DAO : atualizarFatura(fatura)
                    Ctrl -> Fat : calcularSaldoDevedor()
                    Fat --> Ctrl : novoSaldo
                    Ctrl --> Tela : atualizarDemonstrativo(fatura, novoSaldo)
                    Tela --> Rec : exibirFaturaComDesconto()
                else cupom inativo [FE04]
                    Ctrl --> Tela : rejeitarDesconto("Cupom inativo")
                    Tela --> Ger : exibirErroCupom()
                end
            else cupom inexistente ou pagamento parcial já iniciado [FE04]
                Ctrl --> Tela : rejeitarDesconto("Cupom inválido ou pagamento já iniciado")
                Tela --> Ger : exibirErroCupom()
            end
        else sem perfil GERENTE [FE03]
            Ctrl --> Tela : rejeitarDesconto("Permissão exclusiva do Gerente")
            Tela --> Ger : exibirErroPermissao()
        end
    end

    ' =========================================================
    ' 3. INCLUSÃO OBRIGATÓRIA – REGISTRAR PAGAMENTOS (UC14) [RN15, RN16]
    ' =========================================================
    loop [enquanto saldoDevedor > 0 && Recepcionista registrar parcela]
        Rec -> Tela : informarPagamento(valor, formaPagamento)
        Tela -> Ctrl : registrarPagamento(idFatura, valor, formaPagamento)
        Ctrl -> Fat : calcularSaldoDevedor()
        Fat --> Ctrl : saldoDevedor

        alt valor > 0 && valor <= saldoDevedor && formaPagamento != null
            Ctrl -> Pag ** : <<create>> new Pagamento(valor, agora, formaPagamento)
            Ctrl -> Fat : registrarPagamento(pagamento)
            activate Fat
            Fat -> Fat : atualizarStatusFatura() [PARCIALMENTE_PAGA ou PAGA]
            Fat --> Ctrl : statusAtualizado
            deactivate Fat

            alt fatura.estaQuitada() == false [Parcela Parcial]
                Ctrl -> DAO : salvarPagamentoEAtualizarFatura(pagamento, fatura)
                Ctrl -> Fat : calcularSaldoDevedor()
                Fat --> Ctrl : saldoRestante
                Ctrl --> Tela : confirmarParcela(statusAtualizado, saldoRestante)
                Tela --> Rec : exibirSaldoEStatusFatura()
            else fatura.estaQuitada() == true [Parcela Quitadora: persiste junto ao encerramento]
                Ctrl -> Fat : calcularSaldoDevedor()
                Fat --> Ctrl : saldoZero
            end
        else valor ou forma inválidos [FE05]
            Ctrl --> Tela : rejeitarPagamento("Valor deve ser positivo e <= saldo devedor")
            Tela --> Rec : solicitarNovoValor()
        end
    end

    ' =========================================================
    ' 4. VERIFICAÇÃO DE QUITAÇÃO, LIBERAÇÃO ATÔMICA E RECIBO (UC15) [RN17, RN18, RN19]
    ' =========================================================
    Ctrl -> Fat : estaQuitada()
    Fat --> Ctrl : quitada

    alt quitada == true [PE02 – Quitação Integral e Encerramento em Transação Única]
        group Transação Atômica Única JDBC [Pagamento Final + Fatura + Reserva + Quarto]
            Ctrl -> Res : finalizar(agora)
            activate Res
            Res -> Qto : liberar() [DISPONIVEL]
            Res -> Qto : atualizarLimpeza(SUJO) [RN17]
            Res --> Ctrl : status = FINALIZADA
            deactivate Res

            Ctrl -> DAO : commitPagamentoFinalEEncerramento(pagamentoFinal, fatura, reserva, quarto)
        end

        Ctrl -> ReciboSvc : emitirReciboTexto(fatura)
        activate ReciboSvc
        ReciboSvc --> Ctrl : textoReciboFormatado
        deactivate ReciboSvc

        Ctrl --> Tela : concluirCheckOut(textoReciboFormatado)
        Tela --> Rec : exibirReciboEMensagemSucesso()
    else quitada == false [Suspensão sem Quitação: AGUARDANDO_PAGAMENTO ou PARCIALMENTE_PAGA]
        Ctrl --> Tela : suspenderCheckOut(fatura.getStatus(), saldoAtual)
        Tela --> Rec : informarRetencao("Reserva mantida Em Andamento e Quarto Ocupado")
    end
end

deactivate Ctrl
deactivate Tela
@enduml
```
