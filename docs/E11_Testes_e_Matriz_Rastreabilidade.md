# Etapa E11 – Testes Automatizados (JUnit 5) e Matriz Oficial de Rastreabilidade
**Projeto:** HotelFlow – Sistema de Gestão Hoteleira (SENAC)  
**Disciplinas Integradas:** Análise Orientada a Objetos (AOO) e Programação Orientada a Objetos (POO)

---

## 1. Estratégia da Suíte de Testes Automatizados (`JUnit 5`)

A suíte automatizada localizada em `src/test/java/br/senac/hotelflow/` foi desenhada para executar em **menos de 1 segundo em memória** (via *Test Project / `Alt+F6`* no Apache NetBeans ou `mvn test`), sem depender de estado externo nem sobrescrever os dados locais do MySQL Workbench da equipe:

| Classe de Teste JUnit 5 | Cenários e Casos de Borda Verificados | Requisitos Cobertos |
| :--- | :--- | :--- |
| [AutenticacaoEPermissoesTest.java](../src/test/java/br/senac/hotelflow/AutenticacaoEPermissoesTest.java) | Hash SHA-256 válido/inválido; herança de permissões do `GERENTE` sobre `RECEPCIONISTA` (`D04`); bloqueio de descontos e relatórios para `RECEPCIONISTA`; restrição estrita da `CAMAREIRA` à limpeza. | `RN01`, `RN12`, `RN14`, `RN20`, `D04` |
| [CadastrosEQuartoEstadosTest.java](../src/test/java/br/senac/hotelflow/CadastrosEQuartoEstadosTest.java) | Normalização de CPF/Passaporte contra duplicidade por pontuação; validação das 3 categorias (`Standard`, `Luxo`, `Suíte`) e tarifas positivas; estados ortogonais de `Quarto` bloqueando check-in quando `SUJO` ou `EM_LIMPEZA` (`D01`). | `RN02`, `RN03`, `RN04`, `RN09`, `RN12`, `RN17`, `D01` |
| [ReservaConflitosECicloVidaTest.java](../src/test/java/br/senac/hotelflow/ReservaConflitosECicloVidaTest.java) | Intervalo semiaberto `[ini, fim)` permitindo saída e nova entrada no mesmo dia (`D02`); bloqueio de conflito *day-use* no mesmo dia; disparo de `EmailService` na confirmação (`RN08`); filtro de histórico exclusivo para `FINALIZADA` (`RN19`). | `RN05`–`RN11`, `RN19`, `D01`, `D02`, `D05` |
| [FaturamentoPagamentosEReciboTest.java](../src/test/java/br/senac/hotelflow/FaturamentoPagamentosEReciboTest.java) | Consolidação automática de diárias + frigobar + serviços (`RN13`); congelamento histórico de tarifa (`D06`); cupom `SENAC10` (`RN14`); pagamentos fracionados (`RN15`, `RN16`); bloqueio de cupom pós-pagamento parcial; prorrogação de diárias; liberação `DISPONIVEL` + `SUJO` (`RN17`); emissão textual de recibo (`RN18`). | `RN10`, `RN11`, `RN13`–`RN18`, `D02`, `D03`, `D05`, `D06` |
| [RelatorioOcupacaoMensalTest.java](../src/test/java/br/senac/hotelflow/RelatorioOcupacaoMensalTest.java) | Apuração mensal exata com estadias que atravessam virada de mês (Outubro $\rightarrow$ Novembro) e estadias `EM_ANDAMENTO` limitadas até a data atual sem projetar futuro. | `RN20`, `D05` |

---

## 2. Matriz Oficial Ponta a Ponta de Rastreabilidade (`RN01` a `RN20`)

| ID | Requisito de Negócio (Fonte da Verdade) | Caso(s) de Uso (`E2`/`E3`) | Domínio (`model` – `E5`/`E6`) | Tabela SQL (`E8`) | Serviço & Tela (`service`/`view` – `E10`) | Teste Automatizado (`E11`) |
| :-: | :--- | :---: | :--- | :--- | :--- | :--- |
| **RN01** | Autenticação por login/senha e perfis `Recepcionista`, `Gerente`, `Camareira` | `UC01` | `Usuario`, `PerfilUsuario` | `usuario` | `AutenticacaoService`, `TelaLoginFrame`, `TelaPrincipalFrame` | `AutenticacaoEPermissoesTest` |
| **RN02** | Manter cadastro de Hóspedes (nome, CPF/passaporte, tel, e-mail, endereço) | `UC02` | `Hospede` (`normalizarDocumento`) | `hospede` | `CadastroService`, `PainelHospedes` | `CadastrosEQuartoEstadosTest` |
| **RN03** | Manter cadastro de Quartos (número, andar, `Standard`/`Luxo`/`Suíte`, capacidade) | `UC04`, `UC05` | `Quarto`, `CategoriaQuarto` | `quarto`, `categoria_quarto` | `CadastroService`, `PainelGerenciaCadastrosRelatorio` | `CadastrosEQuartoEstadosTest` |
| **RN04** | Registrar e manter tarifa diária por categoria de quarto | `UC05` | `CategoriaQuarto.atualizarTarifa()` | `categoria_quarto` | `CadastroService`, `PainelGerenciaCadastrosRelatorio` | `CadastrosEQuartoEstadosTest`, `FaturamentoPagamentosEReciboTest` |
| **RN05** | Criar Reserva vinculando Hóspede, Quarto e datas previstas | `UC07` | `Reserva` | `reserva` | `ReservaService.criarReserva()`, `PainelReservasCheckIn` | `ReservaConflitosECicloVidaTest` |
| **RN06** | Impedir confirmação de reserva com agendamento sobreposto | `UC08` | `Reserva.possuiConflitoPeriodo()` | `reserva` (`idx_reserva_conflito`) | `ReservaService.confirmarReserva()`, `ReservaDAO` | `ReservaConflitosECicloVidaTest` |
| **RN07** | Controlar status da Reserva (`Pendente`, `Confirmada`, `Em Andamento`, `Finalizada`, `Cancelada`) | `UC07`–`UC09`, `UC13` | `Reserva`, `StatusReserva` | `reserva` (`chk_reserva_status`) | `ReservaService`, `CheckOutService`, `PainelReservasCheckIn` | `ReservaConflitosECicloVidaTest`, `FaturamentoPagamentosEReciboTest` |
| **RN08** | Enviar e-mail automático ao confirmar reserva | `UC08.1` | `Reserva.confirmar()`, `Hospede.email` | `reserva`, `hospede` | `EmailService`, `ConsoleEmailService`, `PainelReservasCheckIn` | `ReservaConflitosECicloVidaTest` |
| **RN09** | Efetivar Check-in (`Quarto = Ocupado`, `Reserva = Em Andamento`) | `UC09` | `Reserva.realizarCheckIn()`, `Quarto.ocupar()`, `StatusOcupacao` | `reserva`, `quarto` | `ReservaService.realizarCheckIn()`, `PainelReservasCheckIn` | `CadastrosEQuartoEstadosTest`, `ReservaConflitosECicloVidaTest` |
| **RN10** | Lançar Consumo de Frigobar (produto, quantidade, valor) na reserva | `UC10` | `ConsumoFrigobar`, `Reserva.adicionarConsumo()` | `consumo_frigobar` | `HospedagemService`, `PainelEstadiaConsumos` | `ReservaConflitosECicloVidaTest`, `FaturamentoPagamentosEReciboTest` |
| **RN11** | Solicitar e registrar Serviços de Quarto (data, descrição, valor) | `UC11` | `ServicoQuarto`, `Reserva.adicionarServico()` | `servico_quarto` | `HospedagemService`, `PainelEstadiaConsumos` | `FaturamentoPagamentosEReciboTest` |
| **RN12** | Camareira atualizar status de limpeza (`Sujo`, `Em Limpeza`, `Limpo`) | `UC12` | `Quarto.atualizarLimpeza()`, `StatusLimpeza` | `quarto` (`idx_quarto_status`) | `HospedagemService`, `PainelGovernancaLimpeza` | `AutenticacaoEPermissoesTest`, `CadastrosEQuartoEstadosTest` |
| **RN13** | Gerar Fatura automática no Check-out (diárias + frigobar + serviços) | `UC13` | `Fatura`, `Reserva.calcularDiarias()` | `fatura` (`uk_fatura_reserva`) | `CheckOutService.iniciarOuRetomarCheckOut()`, `PainelCheckOutFaturamento` | `FaturamentoPagamentosEReciboTest` |
| **RN14** | Permitir ao Gerente aplicar cupom de desconto percentual na Fatura | `UC06`, `UC16` | `CupomDesconto`, `Fatura.aplicarDesconto()` | `cupom_desconto`, `fatura` | `CheckOutService.aplicarCupomDesconto()`, `PainelCheckOutFaturamento` | `AutenticacaoEPermissoesTest`, `FaturamentoPagamentosEReciboTest` |
| **RN15** | Registrar Pagamento aceitando `Cartão de Crédito`, `PIX` ou `Dinheiro` | `UC14` | `Pagamento`, `FormaPagamento` | `pagamento` | `CheckOutService.registrarPagamento()`, `PainelCheckOutFaturamento` | `FaturamentoPagamentosEReciboTest` |
| **RN16** | Permitir pagamentos fracionados (`Parcialmente Paga` até quitação) | `UC14` | `Fatura.registrarPagamento()`, `StatusFatura` | `fatura`, `pagamento` | `CheckOutService`, `PainelCheckOutFaturamento` | `FaturamentoPagamentosEReciboTest` |
| **RN17** | Alterar Quarto para `Sujo` e `Disponível` imediatamente após quitação total | `UC13` | `Reserva.finalizar()`, `Quarto.liberar()` | `quarto`, `reserva`, `fatura` | `CheckOutService` (transação única ACID) | `FaturamentoPagamentosEReciboTest` |
| **RN18** | Emitir Recibo de Hospedagem em texto após quitação total | `UC15` | `Fatura.estaQuitada()` | `fatura`, `pagamento` | `GeradorReciboService`, `DialogoTextoModal` | `FaturamentoPagamentosEReciboTest` |
| **RN19** | Manter histórico de todas as reservas finalizadas de um Hóspede | `UC03` | `Hospede.consultarHistorico()` | `reserva` (`idx_reserva_historico`) | `ReservaService.consultarHistoricoHospede()`, `PainelHospedes` | `ReservaConflitosECicloVidaTest` |
| **RN20** | Permitir ao Gerente gerar relatório mensal de taxa de ocupação | `UC17` | `Reserva` (`dataHoraCheckIn`/`Out`) | `reserva` (`idx_reserva_ocupacao`) | `RelatorioOcupacaoService`, `PainelGerenciaCadastrosRelatorio` | `AutenticacaoEPermissoesTest`, `RelatorioOcupacaoMensalTest` |
