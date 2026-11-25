# Etapa E3 – Especificação Detalhada de Caso de Uso
**Projeto:** HotelFlow – Sistema de Gestão Hoteleira (SENAC)  
**Caso de Uso Especificado:** `UC13 – Processar Check-out e Faturamento` (`[Atende RN01, RN04, RN05, RN07, RN09..RN19, D02..D06]`)

---

## 1. Cabeçalho de Identificação

| Campo | Descrição |
| :--- | :--- |
| **Código e Nome** | `UC13 – Processar Check-out e Faturamento` |
| **Ator Principal** | `Recepcionista` (e `Gerente`, por generalização de atores — `[Atende RN01, D04]`) |
| **Ator Participante** | `Gerente` (exclusivo na extensão de desconto — `[Atende RN14, D04]`) |
| **Objetivo** | Apurar (ou retomar) a fatura consolidada da estadia, processar pagamentos únicos ou fracionados e, mediante quitação integral, encerrar a hospedagem, liberar o quarto para limpeza e emitir o recibo textual. |
| **Casos Incluídos (`<<include>>`)** | `UC14 – Registrar Pagamento da Fatura` (`[Atende RN15, RN16]`) |
| **Casos Estendidos (`<<extend>>`)** | `UC16 – Aplicar Cupom de Desconto` (`[Atende RN14, D03, D06]`) no ponto `PE01`;<br>`UC15 – Emitir Recibo de Hospedagem` (`[Atende RN18]`) no ponto `PE02`. |

---

## 2. Pontos de Extensão UML (*Extension Points*)

- **`PE01 – Fatura Apresentada sem Pagamentos` (após o Passo 7 do Fluxo Principal):** Permite acionar `UC16 – Aplicar Cupom de Desconto` quando solicitado pelo `Gerente` e desde que `statusFatura == AGUARDANDO_PAGAMENTO` (`totalPago == 0`, conforme `[Atende RN14, D06]`).
- **`PE02 – Quitação Integral Atingida` (após o Passo 11 do Fluxo Principal):** Aciona `UC15 – Emitir Recibo de Hospedagem` exclusivamente quando `saldoDevedor == 0,00` e `statusFatura == PAGA` (`[Atende RN16, RN18]`).

---

## 3. Pré-condições

1. O operador está autenticado no sistema com perfil `Recepcionista` ou `Gerente` (`[Atende RN01, D04]`).
2. Existe uma `Reserva` com status `Em Andamento`, contendo `dataHoraCheckIn` efetiva registrada e vinculada a um `Hospede` e a um `Quarto` atualmente `Ocupado` (`[Atende RN07, RN09, D05]`).
3. A `CategoriaQuarto` do quarto possui `tarifaDiaria` válida cadastrada (`[Atende RN04]`).

---

## 4. Pós-condições

- **Pós-condição de Encerramento Integral (Sucesso Completo):**
  - `Fatura` atualizada para `PAGA`, com `saldoDevedor == 0,00` e todos os objetos `Pagamento` persistidos (`[Atende RN15, RN16]`).
  - `Reserva` atualizada para `FINALIZADA` com `dataHoraCheckOut` efetiva gravada (`[Atende RN07, D05]`), passando a compor o histórico consultável do hóspede (`[Atende RN19]`).
  - `Quarto` atualizado imediatamente para `DISPONIVEL` (ocupação) e `SUJO` (limpeza) (`[Atende RN12, RN17]`).
  - Recibo de hospedagem emitido em formato de texto (`[Atende RN18]`).
- **Pós-condição de Amortização Parcial (`FA02`):**
  - `Fatura` mantida como `PARCIALMENTE_PAGA`, preservando os valores congelados e os pagamentos já registrados (`[Atende RN16, D06]`).
  - `Reserva` permanece `EM_ANDAMENTO` (sem `dataHoraCheckOut`) e `Quarto` permanece `OCUPADO`; **nenhum** recibo de quitação é emitido (`[Atende RN17, RN18]`).

---

## 5. Fluxo Principal (`FP01` – Faturamento e Quitação Integral)

1. A `Recepcionista` acessa a funcionalidade **Processar Check-out** e informa o identificador da reserva (ou número do quarto/documento do hóspede).
2. O sistema localiza a `Reserva` e valida que seu status é `EM_ANDAMENTO` (`[Atende RN07]`).
3. O sistema verifica se já existe uma `Fatura` aberta vinculada à `Reserva` (`[Atende RN16, D06]`).
4. Não existindo fatura prévia, o sistema apura a data/hora atual de referência para saída, calcula a quantidade de diárias efetivas (`dataHoraCheckIn` até a data de saída, mínimo de 1 diária — `[Atende RN13, D02, D05]`), obtém a `tarifaDiaria` da `CategoriaQuarto` (`[Atende RN04]`), soma os subtotais de `ConsumoFrigobar` (`[Atende RN10]`) e os valores de `ServicoQuarto` (`[Atende RN11]`).
5. O sistema gera a `Fatura` com status `AGUARDANDO_PAGAMENTO`, congelando `tarifaDiariaAplicada` e `valorBruto` (`[Atende RN13, D06]`). *(Nota: após a emissão da fatura, novos lançamentos de consumo/serviço ficam bloqueados para preservar a integridade contábil).*
6. O sistema calcula o `valorLiquido` e o `saldoDevedor` atual da fatura.
7. O sistema apresenta na tela o demonstrativo detalhado: diárias, consumos de frigobar, serviços de quarto, desconto aplicado, total líquido, total já pago e saldo devedor. **[Ponto de Extensão `PE01`: `UC16 – Aplicar Cupom de Desconto`]**.
8. A `Recepcionista` seleciona a forma de pagamento (`Cartão de Crédito`, `PIX` ou `Dinheiro` — `[Atende RN15]`) e informa o valor a amortizar.
9. O sistema executa o caso incluído **`UC14 – Registrar Pagamento da Fatura`**, validando a forma e o valor (positivo e menor ou igual ao `saldoDevedor`), criando o registro de `Pagamento` e recalculando o `saldoDevedor` (`[Atende RN15, RN16]`).
10. O sistema verifica que o `saldoDevedor` atingiu `0,00` e atualiza o status da `Fatura` para `PAGA` (`[Atende RN16]`).
11. De forma atômica (transacional), o sistema:
    - a) Registra `dataHoraCheckOut` efetiva e altera o status da `Reserva` para `FINALIZADA` (`[Atende RN07, RN19, D05]`);
    - b) Altera o status de ocupação do `Quarto` para `DISPONIVEL` e o status de limpeza para `SUJO` (`[Atende RN17]`). **[Ponto de Extensão `PE02`: `UC15 – Emitir Recibo de Hospedagem`]**.
12. Como a condição `[fatura quitada]` é verdadeira, o sistema executa a extensão **`UC15 – Emitir Recibo de Hospedagem`**, gerando e exibindo o recibo textual completo para entrega ao hóspede (`[Atende RN18]`).
13. O sistema exibe mensagem de confirmação do encerramento do check-out e finaliza o caso de uso.

---

## 6. Fluxos Alternativos

- **`FA01 – Retomada de Fatura Parcialmente Paga` (no Passo 3):**
  1. O sistema identifica que já existe uma `Fatura` com status `PARCIALMENTE_PAGA` (ou `AGUARDANDO_PAGAMENTO`) para a `Reserva` (`[Atende RN16, D06]`).
  2. O sistema recupera a `Fatura` existente com seus valores congelados e a lista de `Pagamento`s anteriores, sem gerar nova fatura nem recalcular retroativamente a tarifa.
  3. O fluxo segue diretamente para o **Passo 6**.

- **`FA02 – Aplicação de Cupom de Desconto pelo Gerente` (`UC16`, no Ponto de Extensão `PE01` após o Passo 7):**
  1. O `Gerente` solicita a aplicação de um cupom de desconto informando o `codigo` (e autenticando/validando perfil `GERENTE` caso o terminal esteja operado pela recepção — `[Atende RN01, RN14, D04]`).
  2. O sistema verifica que a fatura está em `AGUARDANDO_PAGAMENTO` (`totalPago == 0` — `[Atende D06]`) e localiza o `CupomDesconto` ativo (`[Atende D03]`).
  3. O sistema calcula `valorDesconto = valorBruto * (percentual / 100)`, congela `percentualDesconto` e `valorDesconto` na `Fatura` e atualiza o `valorLiquido` e o `saldoDevedor` (`[Atende RN14, D06]`).
  4. O sistema reapresenta o demonstrativo atualizado e retorna ao **Passo 8**.

- **`FA03 – Pagamento Fracionado / Parcial` (nos Passos 9 e 10):**
  1. O valor registrado no `UC14` é inferior ao `saldoDevedor` total (`[Atende RN16]`).
  2. O sistema atualiza o status da `Fatura` para `PARCIALMENTE_PAGA` e exibe o novo `saldoDevedor` restante.
  3. **Subfluxo `FA03a` (Novo pagamento imediato, ex.: parte no PIX e parte no Cartão):** A `Recepcionista` opta por lançar outra parcela na sequência; o sistema retorna ao **Passo 8**.
  4. **Subfluxo `FA03b` (Suspensão para quitação posterior):** A `Recepcionista` encerra o atendimento parcial; o sistema mantém `Reserva = EM_ANDAMENTO` e `Quarto = OCUPADO`, não aciona `PE02` (`UC15`) e encerra a execução preservando a fatura para futura retomada via `FA01` (`[Atende RN16, RN17, RN18]`).

- **`FA04 – Hospedagem sem Consumos ou Serviços Extras` (no Passo 4):**
  1. A `Reserva` não possui registros de `ConsumoFrigobar` e/ou `ServicoQuarto`.
  2. O sistema assume subtotal `R$ 0,00` para as rubricas vazias e prossegue ao **Passo 5** (`[Atende RN10, RN11, RN13]`).

- **`FA05 – Saída no Mesmo Dia da Entrada (*Day-Use*)` (no Passo 4):**
  1. A data efetiva de saída coincide com a data efetiva de entrada (`diferença = 0 dias`).
  2. O sistema aplica a regra de fronteira `D02`, cobrando **1 diária mínima**, e prossegue ao **Passo 5** (`[Atende RN13, D02, D05]`).

- **`FA06 – Pagamento em Dinheiro com Cálculo de Troco` (no Passo 8):**
  1. Para a forma `Dinheiro`, caso o hóspede entregue cédulas em valor superior ao `saldoDevedor`, a interface auxilia exibindo o troco (`valorEntregue - saldoDevedor`), mas envia ao registro da fatura (`UC14`) estritamente o valor amortizado igual ao `saldoDevedor` (`[Atende RN15]`).

---

## 7. Fluxos de Exceção

| Código | Passo | Condição Disparadora | Tratamento pelo Sistema | Rastreabilidade |
| :--- | :---: | :--- | :--- | :--- |
| **FE01** | 2 | Reserva não encontrada. | Exibe mensagem *"Reserva não localizada"* e retorna ao Passo 1. | `[Atende RN05]` |
| **FE02** | 2 | Reserva em status diferente de `EM_ANDAMENTO` (ex.: `PENDENTE`, `CONFIRMADA`, `FINALIZADA`, `CANCELADA`). | Bloqueia o check-out informando o status atual da reserva. | `[Atende RN07, RN09]` |
| **FE03** | `FA02` | Operador sem perfil `GERENTE` tenta aplicar cupom. | Rejeita a operação com erro de autorização e mantém a fatura inalterada. | `[Atende RN01, RN14, D04]` |
| **FE04** | `FA02` | Cupom inexistente, inativo ou fatura já possui pagamento parcial registrado (`totalPago > 0`). | Exibe o motivo do impedimento e mantém os valores originais da fatura. | `[Atende RN14, D03, D06]` |
| **FE05** | 9 | Valor de pagamento $\le 0$ ou superior ao `saldoDevedor` (em PIX/Cartão ou valor amortizado). | Rejeita o lançamento e solicita valor válido no Passo 8. | `[Atende RN15, RN16]` |
| **FE06** | 9 / 11 | Falha de persistência no banco de dados durante pagamento ou encerramento. | Executa *rollback* transacional completo da operação atual, preservando a consistência anterior, e exibe alerta técnico ao operador. | `[Atende RN16, RN17]` |

---

## 8. Regras Finais de Cálculo Financeiro

1. $\text{Diárias} = \max\big(1,\; \text{diasEntre}(\text{dataHoraCheckIn.toLocalDate()},\; \text{dataSaidaEfetiva})\big)$ (`[Atende RN13, D02, D05]`)
2. $\text{TotalFrigobar} = \sum (\text{quantidade} \times \text{valorUnitario})$ (`[Atende RN10, RN13]`)
3. $\text{TotalServicos} = \sum \text{valorServico}$ (`[Atende RN11, RN13]`)
4. $\text{ValorBruto} = (\text{Diárias} \times \text{tarifaDiariaAplicada}) + \text{TotalFrigobar} + \text{TotalServicos}$ (`[Atende RN04, RN13, D06]`)
5. $\text{ValorDesconto} = \text{arredondar}_2\big(\text{ValorBruto} \times \frac{\text{percentualDesconto}}{100}\big)$ (`[Atende RN14, D06]`)
6. $\text{ValorLiquido} = \text{ValorBruto} - \text{ValorDesconto}$
7. $\text{TotalPago} = \sum \text{valorPagamento}$ (`[Atende RN15, RN16]`)
8. $\text{SaldoDevedor} = \text{ValorLiquido} - \text{TotalPago}$ (`[Atende RN16]`)

**Exemplo de Mesa:** 3 diárias $\times$ R$ 200,00 (R$ 600,00) + Frigobar R$ 80,00 + Serviços R$ 120,00 $\Rightarrow$ `ValorBruto = R$ 800,00`. Cupom `SENAC10` (10%) aplicado pelo Gerente antes do pagamento $\Rightarrow$ `ValorDesconto = R$ 80,00`; `ValorLiquido = R$ 720,00`. Pagamento 1 (PIX) de R$ 300,00 $\Rightarrow$ `StatusFatura = PARCIALMENTE_PAGA`, `SaldoDevedor = R$ 420,00` (quarto continua `OCUPADO`). Pagamento 2 (Cartão) de R$ 420,00 $\Rightarrow$ `SaldoDevedor = R$ 0,00`, `StatusFatura = PAGA`, `Reserva = FINALIZADA`, `Quarto = DISPONIVEL` e `SUJO`, e emissão do recibo textual (`UC15`).

---

## 9. Matriz de Rastreabilidade do Caso de Uso (`UC13`)

| Requisito / Decisão | Cobertura na Especificação (`UC13`) |
| :--- | :--- |
| **RN01, D04** | Pré-condição 1, `FA02` e `FE03` (autenticação da Recepção e autorização exclusiva do Gerente para desconto). |
| **RN04, RN05, D02, D05** | Pré-condição 3, Passo 4, Passo 11a e `FA05` (datas efetivas de entrada/saída, mínimo de 1 diária e tarifa da categoria). |
| **RN07, RN09** | Pré-condição 2, Passo 2, Passo 11a e `FE02` (validação de `EM_ANDAMENTO` e transição para `FINALIZADA`). |
| **RN10, RN11, RN13** | Passos 4, 5 e `FA04` (consolidação automática de diárias + frigobar + serviços de quarto). |
| **RN14, D03, D06** | Ponto `PE01`, `FA02` e `FE04` (cupom cadastrado aplicado pelo Gerente antes de pagamentos, congelando valores). |
| **RN15, RN16** | Passos 3, 8, 9, 10, `FA01`, `FA03`, `FA06` e `FE05` (formas aceitas, fracionamento e retomada de saldo). |
| **RN12, RN17** | Pós-condições e Passo 11b (transição imediata do quarto para `DISPONIVEL` e `SUJO` somente após quitação total). |
| **RN18** | Ponto `PE02` e Passo 12 (emissão do recibo textual somente quando `StatusFatura == PAGA`). |
| **RN19** | Pós-condição e Passo 11a (reserva `FINALIZADA` preservada para consulta histórica do hóspede). |
