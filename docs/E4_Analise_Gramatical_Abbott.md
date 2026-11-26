# Etapa E4 – Análise Gramatical de Abbott
**Projeto:** HotelFlow – Sistema de Gestão Hoteleira (SENAC)  
**Disciplinas Integradas:** Análise Orientada a Objetos (AOO) e Programação Orientada a Objetos (POO)  
**Base de Rastreabilidade:** `RN01` a `RN20`, Decisões `D01` a `D06` ([E1](E1_Analise_Requisitos_e_Glossario.md)), Casos de Uso ([E2](E2_Atores_e_Casos_de_Uso.md)) e Especificação `UC13` ([E3](E3_Especificacao_Caso_de_Uso.md)).

---

## 1. Fundamentação Metodológica (Técnica de Russell Abbott)

A Análise Textual de Abbott estabelece uma heurística sistemática para derivar o Modelo de Domínio a partir da especificação de requisitos em linguagem natural:
- **Substantivos comuns / sintagmas nominais** $\rightarrow$ candidatos a **Classes de Domínio**, **Atributos** ou **Valores de Enumeração**;
- **Verbos / locuções verbais transitivas** $\rightarrow$ candidatos a **Métodos (Operações de Negócio)** ou **Responsabilidades de Serviço**;
- **Adjetivos / particípios de situação** $\rightarrow$ candidatos a **Estados (`Enums`)** ou **Restrições (Regras/Invariantes)**.

Nem todo substantivo vira classe: termos sem identidade própria ou ciclo de vida independente são reclassificados como atributos, enumerações, cálculos derivados ou componentes de infraestrutura.

---

## 2. Extração Gramatical Rastreável (`RN01` a `RN20`)

| Requisito | Substantivos / Sintagmas Extraídos | Verbos / Locuções Verbais | Adjetivos / Estados Identificados |
| :--- | :--- | :--- | :--- |
| **RN01** | Autenticação, login, senha, perfil (`Recepcionista`, `Gerente`, `Camareira`) | Exigir autenticação, controlar acesso | Autenticado, autorizado |
| **RN02** | Cadastro, Hóspede, nome completo, CPF/passaporte, telefone, e-mail, endereço | Manter cadastro | — |
| **RN03** | Cadastro, Quarto, número, andar, categoria (`Standard`, `Luxo`, `Suíte`), capacidade máxima | Manter cadastro | — |
| **RN04** | Valor da tarifa diária, categoria de quarto | Registrar, manter | Associada, vigente |
| **RN05** | Reserva, Hóspede, Quarto, data prevista de check-in, data prevista de check-out | Criar, vincular | Prevista |
| **RN06** | Confirmação, reserva, Quarto, agendamento, período | Impedir confirmação, verificar sobreposição | Sobreposto |
| **RN07** | Status da Reserva | Controlar status | `Pendente`, `Confirmada`, `Em Andamento`, `Finalizada`, `Cancelada` |
| **RN08** | E-mail automático, confirmação, Hóspede, reserva | Enviar e-mail, atualizar | Automático, `Confirmada` |
| **RN09** | Check-in, status do Quarto, Reserva | Efetivar check-in, alterar status | `Ocupado`, `Em Andamento` |
| **RN10** | Consumo de Frigobar, estadia, produto, quantidade, valor, conta da reserva | Permitir lançamento, somar | — |
| **RN11** | Serviço de Quarto, lavanderia, refeição extra, data, descrição, valor | Solicitar, registrar | Extra |
| **RN12** | Camareira, status de limpeza, Quarto | Atualizar status de limpeza | `Sujo`, `Em Limpeza`, `Limpo` |
| **RN13** | Fatura, Check-out, diárias, datas, consumo de frigobar, serviços de quarto | Gerar automaticamente, calcular, somar | Consolidada |
| **RN14** | Gerente, cupom de desconto, percentual, valor total, Fatura | Aplicar cupom, abater | Percentual, ativo |
| **RN15** | Pagamento, Fatura, `Cartão de Crédito`, `PIX`, `Dinheiro` | Registrar pagamento, aceitar forma | — |
| **RN16** | Pagamentos fracionados, Fatura, quitação | Fracionar, manter status, quitar | `Parcialmente Paga`, integral (`Paga`) |
| **RN17** | Status do Quarto, quitação total, Check-out | Alterar imediatamente, liberar, encerrar | `Sujo`, `Disponível` |
| **RN18** | Recibo de Hospedagem, formato de texto, Hóspede, fatura | Emitir recibo | Quitada, textual |
| **RN19** | Histórico, reservas finalizadas, Hóspede | Manter histórico, consultar | `Finalizada` |
| **RN20** | Gerente, relatório gerencial, taxa de ocupação, quartos, mês específico | Gerar relatório, calcular ocupação | Mensal, ocupados |

---

## 3. Classes de Domínio Selecionadas, Atributos e Métodos (`[Atende RN01..RN20]`)

Legenda de classificação:
- **Atributos:** `(D)` Descritivo | `(E)` Estado | `(T)` Temporal | `(H)` Histórico congelado (`D06`).
- **Métodos:** `(N)` Regra de Negócio | `(S)` Transição de Estado | `(C)` Cálculo de Domínio. *(Getters/setters triviais omitidos).*

| Classe de Domínio | Atributos Essenciais (Java 17+) | Métodos de Domínio | Rastreabilidade e Justificativa |
| :--- | :--- | :--- | :--- |
| **`Usuario`** | `idUsuario: int (D)`<br>`nome: String (D)`<br>`login: String (D)`<br>`senhaHash: String (D)`<br>`perfil: PerfilUsuario (E)` | `+autenticar(senha: String): boolean (N)`<br>`+possuiPermissao(operacao: String): boolean (N)` | Representa o colaborador autenticado e encapsula a hierarquia de permissões. `[Atende RN01, D04]` |
| **`Hospede`** | `idHospede: int (D)`<br>`nomeCompleto: String (D)`<br>`documento: String (D)`<br>`telefone: String (D)`<br>`email: String (D)`<br>`endereco: String (D)` | `+consultarHistorico(): List<Reserva> (N)` | Titular das reservas; filtra em memória suas reservas com status `FINALIZADA` (apoiado por consulta DAO na persistência). `[Atende RN02, RN19]` |
| **`CategoriaQuarto`** | `idCategoria: int (D)`<br>`nome: String (D)`<br>`tarifaDiaria: BigDecimal (D)` | `+atualizarTarifa(novoValor: BigDecimal): void (N)` | Entidade (e não mero `Enum`) pois sua `tarifaDiaria` é mutável em tempo de execução. `[Atende RN03, RN04]` |
| **`Quarto`** | `idQuarto: int (D)`<br>`numero: String (D)`<br>`andar: int (D)`<br>`capacidadeMaxima: int (D)`<br>`statusOcupacao: StatusOcupacao (E)`<br>`statusLimpeza: StatusLimpeza (E)` | `+ocupar(): void (S)`<br>`+liberar(): void (S)`<br>`+atualizarLimpeza(status: StatusLimpeza): void (S)`<br>`+estaAptoParaCheckIn(): boolean (N)` | Acomodação física com dois eixos independentes de estado (ocupação e higienização). `[Atende RN03, RN09, RN12, RN17, D01]` |
| **`Reserva`** | `idReserva: int (D)`<br>`dataCheckInPrevista: LocalDate (T)`<br>`dataCheckOutPrevista: LocalDate (T)`<br>`dataHoraCheckIn: LocalDateTime (T)`<br>`dataHoraCheckOut: LocalDateTime (T)`<br>`status: StatusReserva (E)` | `+confirmar(): void (S)`<br>`+cancelar(): void (S)`<br>`+realizarCheckIn(entrada: LocalDateTime): void (S)`<br>`+finalizar(saida: LocalDateTime): void (S)`<br>`+possuiConflitoPeriodo(ini: LocalDate, fim: LocalDate): boolean (N)`<br>`+calcularDiarias(saidaRef: LocalDate): long (C)`<br>`+calcularTotalConsumos(): BigDecimal (C)`<br>`+calcularTotalServicos(): BigDecimal (C)`<br>`+adicionarConsumo(item: ConsumoFrigobar): void (N)`<br>`+adicionarServico(servico: ServicoQuarto): void (N)` | Agregado central da estadia; separa datas previstas (`RN05`, `RN06`) das efetivas (`D05`) e governa itens extras. `[Atende RN05..RN11, RN13, RN17, RN19, RN20, D01, D02, D05]` |
| **`ConsumoFrigobar`** | `idConsumo: int (D)`<br>`produto: String (D)`<br>`quantidade: int (D)`<br>`valorUnitario: BigDecimal (D)`<br>`dataHoraRegistro: LocalDateTime (T)` | `+calcularSubtotal(): BigDecimal (C)` | Item de despesa de frigobar lançado durante a estadia ativa. `[Atende RN10, RN13]` |
| **`ServicoQuarto`** | `idServico: int (D)`<br>`dataHoraSolicitacao: LocalDateTime (T)`<br>`descricao: String (D)`<br>`valor: BigDecimal (D)` | *(Objeto de valor/entidade parte sem cálculo adicional além do próprio `valor`)* | Serviço sob demanda prestado na estadia (lavanderia, refeição extra). `[Atende RN11, RN13]` |
| **`Fatura`** | `idFatura: int (D)`<br>`dataEmissao: LocalDateTime (T)`<br>`quantidadeDiarias: long (H)`<br>`tarifaDiariaAplicada: BigDecimal (H)`<br>`valorBruto: BigDecimal (H)`<br>`percentualDesconto: BigDecimal (H)`<br>`valorDesconto: BigDecimal (H)`<br>`status: StatusFatura (E)` | `+calcularValorBruto(): BigDecimal (C)`<br>`+aplicarDesconto(cupom: CupomDesconto): void (N)`<br>`+calcularValorLiquido(): BigDecimal (C)`<br>`+registrarPagamento(pag: Pagamento): void (N)`<br>`+calcularTotalPago(): BigDecimal (C)`<br>`+calcularSaldoDevedor(): BigDecimal (C)`<br>`+estaQuitada(): boolean (N)` | Documento financeiro único por reserva; congela tarifa, diárias e desconto (`D06`) e controla amortizações parciais ou totais. `[Atende RN13..RN18, D06]` |
| **`CupomDesconto`** | `idCupom: int (D)`<br>`codigo: String (D)`<br>`percentual: BigDecimal (D)`<br>`ativo: boolean (E)` | `+ativar(): void (S)`<br>`+desativar(): void (S)`<br>`+validar(): boolean (N)`<br>`+calcularDesconto(valorBruto: BigDecimal): BigDecimal (C)` | Benefício percentual pré-cadastrado e auditável operado pelo Gerente. `[Atende RN14, D03, D06]` |
| **`Pagamento`** | `idPagamento: int (D)`<br>`valor: BigDecimal (D)`<br>`dataPagamento: LocalDateTime (T)`<br>`formaPagamento: FormaPagamento (D)` | *(Registro imutável de transação financeira individual)* | Parcela individual paga pelo hóspede para amortizar o saldo da fatura. `[Atende RN15, RN16]` |

---

## 4. Candidatos Reclassificados ou Descartados (Com Justificativa Acadêmica)

| Termo Candidato | Destino na Modelagem | Justificativa de Engenharia de Software |
| :--- | :--- | :--- |
| **`Recepcionista`, `Gerente`, `Camareira`** | Valores do Enum `PerfilUsuario` (`[Atende RN01, D04]`) | Diferem apenas por permissões de acesso, sem atributos estruturais distintos (ex.: matrícula específica ou comissão). Criar 3 subclasses de `Usuario` geraria herança anêmica desnecessária; um `Enum` rico resolve com maior coesão. |
| **`Tarifa Diária`** | Atributo `tarifaDiaria` em `CategoriaQuarto` e `tarifaDiariaAplicada` em `Fatura` (`[Atende RN04, D06]`) | É um valor monetário escalar associado 1:1 à categoria vigente e congelado na fatura no momento do check-out. |
| **`Produto (Frigobar)`** | Atributo `produto: String` em `ConsumoFrigobar` (`[Atende RN10]`) | A `RN10` pede lançamento de produto, quantidade e valor na conta, sem exigir controle de estoque/almoxarifado. Criar catálogo de produtos violaria a regra de não inventar escopo. |
| **`Check-in` e `Check-out`** | Operações de domínio/serviço + timestamps em `Reserva` (`[Atende RN09, RN13, RN17, D05]`) | São eventos/transições de ciclo de vida sobre `Reserva`, `Quarto` e `Fatura`, não substantivos com identidade persistente independente. |
| **`Diária`** | Atributo derivado/congelado `quantidadeDiarias` (`[Atende RN13, D02]`) | Grandeza matemática obtida pela diferença entre datas de estadia multiplicada pela tarifa. |
| **`Recibo de Hospedagem`** | Saída textual gerada por `GeradorReciboService` (`[Atende RN18]`) | A `RN18` exige emissão de comprovante em formato de texto após quitação; todos os seus dados já estão persistidos e congelados em `Fatura`, `Pagamento`, `Reserva` e `Hospede`. |
| **`Histórico de Reservas`** | Consulta filtrada (`StatusReserva.FINALIZADA`) (`[Atende RN19]`) | Trata-se de uma visão/projeção sobre a coleção existente de `Reserva`s de um `Hospede`, não uma tabela duplicada. |
| **`Relatório de Taxa de Ocupação`** | DTO/Resultado de `RelatorioOcupacaoService` (`[Atende RN20]`) | Agregação analítica mensal calculada sob demanda sobre quartos e reservas efetivas no mês consultado. |
| **`Serviço de E-mail`** | Interface de infraestrutura `EmailService` (`[Atende RN08]`) | Componente técnico externo de notificação; não pertence ao pacote `model` de domínio. |

---

## 5. Enumerações Tipadas do Domínio (`Enums`)

1. **`PerfilUsuario`** (`[Atende RN01, D04]`): `CAMAREIRA`, `RECEPCIONISTA`, `GERENTE`.
2. **`StatusReserva`** (`[Atende RN07]`): `PENDENTE`, `CONFIRMADA`, `EM_ANDAMENTO`, `FINALIZADA`, `CANCELADA`.
3. **`StatusOcupacao`** (`[Atende RN09, RN17]`): `DISPONIVEL`, `OCUPADO`.
4. **`StatusLimpeza`** (`[Atende RN12, RN17]`): `SUJO`, `EM_LIMPEZA`, `LIMPO`.
5. **`StatusFatura`** (`[Atende RN13, RN16]`): `AGUARDANDO_PAGAMENTO`, `PARCIALMENTE_PAGA`, `PAGA`.
6. **`FormaPagamento`** (`[Atende RN15]`): `CARTAO_CREDITO`, `PIX`, `DINHEIRO`.

---

## 6. Responsabilidades da Camada de Serviço (`service`)

Para manter o domínio rico porém desacoplado de banco de dados (`DAO`) e infraestrutura externa, as orquestrações multi-entidade ficam mapeadas para a camada `service` (detalhada na Etapa `E9`):
- **`AutenticacaoService`** (`[Atende RN01, D04]`): valida credenciais e sessão ativa.
- **`ReservaService`** (`[Atende RN05..RN09, RN19, D01, D02, D05]`): verifica sobreposição de agenda no banco (`RN06`), confirma reserva acionando `EmailService` (`RN08`) e coordena check-in atômico (`RN09`).
- **`HospedagemService`** (`[Atende RN10..RN12]`): registra consumos de frigobar, serviços de quarto e atualização de governança/limpeza.
- **`CheckOutService`** (`[Atende RN13..RN18, D05, D06]`): gera ou retoma fatura, valida permissão gerencial de desconto, registra pagamentos e executa o encerramento transacional com `GeradorReciboService`.
- **`RelatorioOcupacaoService`** (`[Atende RN20, D05]`): apura quartos-dia ocupados versus capacidade instalada no mês/ano informado.
