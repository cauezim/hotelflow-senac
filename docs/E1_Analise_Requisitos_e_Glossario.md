# Etapa E1 – Análise Consolidada de Requisitos, Glossário e Levantamento de Dúvidas
**Projeto:** HotelFlow – Sistema de Gestão Hoteleira (SENAC)  
**Disciplinas Integradas:** Análise Orientada a Objetos (AOO) e Programação Orientada a Objetos (POO)

---

## 1. Visão Macro em Módulos Funcionais

Para organizar a complexidade do domínio hoteleiro, os 20 requisitos de negócio (fonte da verdade do projeto) foram decompostos em **5 módulos funcionais coesos**:

1. **Módulo 1 – Acesso e Segurança (`RN01`):** Autenticação e controle de acesso baseado em perfis de funcionários.
2. **Módulo 2 – Cadastros Estruturais (`RN02`, `RN03`, `RN04`):** Gestão de clientes (hóspedes), infraestrutura física (quartos) e precificação (categorias/tarifas).
3. **Módulo 3 – Ciclo de Reserva e Hospedagem (`RN05`, `RN06`, `RN07`, `RN08`, `RN09`, `RN12`, `RN17`, `RN19`):** Agendamento, prevenção de conflitos, check-in, governança (limpeza) e histórico.
4. **Módulo 4 – Consumo e Serviços na Estadia (`RN10`, `RN11`):** Lançamento de despesas extras (frigobar e serviços de quarto) vinculadas à estadia ativa.
5. **Módulo 5 – Check-out, Faturamento e Gestão (`RN13`, `RN14`, `RN15`, `RN16`, `RN18`, `RN20`):** Consolidação da fatura, descontos gerenciais, pagamentos (únicos ou fracionados), recibo e relatório de ocupação.

---

## 2. Matriz Consolidada de Requisitos de Negócio (`RN01` a `RN20`)

| ID | Módulo | Título Resumido | Regra de Negócio (Fonte da Verdade) | Entidades de Domínio Envolvidas | Impacto Principal (AOO / POO / BD) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **RN01** | 1. Acesso | Autenticação e Perfis | Exigir autenticação por login e senha, com controle de acesso para os perfis: `Recepcionista`, `Gerente` e `Camareira`. | `Usuario`, `PerfilUsuario` | Atores UML; controle de sessão e permissões na camada `service`/`view`; tabela `usuario`. |
| **RN02** | 2. Cadastros | Cadastro de Hóspedes | Manter cadastro de Hóspedes (nome completo, CPF/passaporte, telefone, e-mail e endereço). | `Hospede` | UC Manter Hóspedes; unicidade de documento; tabela `hospede`. |
| **RN03** | 2. Cadastros | Cadastro de Quartos | Manter cadastro de Quartos (número, andar, categoria: `Standard`, `Luxo`, `Suíte`, e capacidade máxima de pessoas). | `Quarto`, `CategoriaQuarto` | UC Manter Quartos; associação com categoria; tabela `quarto`. |
| **RN04** | 2. Cadastros | Gestão de Tarifas | Registrar e manter o valor da tarifa diária associada a cada categoria de quarto. | `CategoriaQuarto` | UC Manter Tarifas/Categorias; base para cálculo da `RN13`; tabela `categoria_quarto`. |
| **RN05** | 3. Hospedagem | Criação de Reserva | Criar Reserva vinculando Hóspede, Quarto, data prevista de check-in e data prevista de check-out. | `Reserva`, `Hospede`, `Quarto` | UC Criar Reserva; associações obrigatórias e validação de datas; tabela `reserva`. |
| **RN06** | 3. Hospedagem | Bloqueio de Sobreposição | Impedir confirmação de reserva para Quarto com agendamento sobreposto no mesmo período. | `Reserva`, `Quarto` | Regra crítica em `ReservaService` + consulta SQL de interseção de datas; exceção de negócio. |
| **RN07** | 3. Hospedagem | Ciclo de Vida da Reserva | Controlar status da Reserva: `Pendente`, `Confirmada`, `Em Andamento`, `Finalizada`, `Cancelada`. | `Reserva`, `StatusReserva` | Enum `StatusReserva`; Diagrama de Estados; transições protegidas nos métodos de domínio. |
| **RN08** | 3. Hospedagem | Confirmação por E-mail | Enviar e-mail automático de confirmação para o Hóspede quando a reserva for atualizada para `Confirmada`. | `Reserva`, `Hospede`, `ServicoEmail` | Ator secundário no Caso de Uso; interface de serviço de notificação desacoplada. |
| **RN09** | 3. Hospedagem | Efetivação de Check-in | Permitir efetivação do Check-in, alterando o status do Quarto para `Ocupado` e da Reserva para `Em Andamento`. | `Reserva`, `Quarto`, `StatusOcupacao`, `StatusReserva` | UC Realizar Check-in; atualização atômica (transacional) de `Reserva` e `Quarto`. |
| **RN10** | 4. Consumo | Consumo de Frigobar | Permitir lançamento de Consumo de Frigobar durante a estadia (produto, quantidade, valor) na conta da reserva. | `ConsumoFrigobar`, `Reserva` | UC Registrar Consumo de Frigobar; composição com `Reserva`; tabela `consumo_frigobar`. |
| **RN11** | 4. Consumo | Serviços de Quarto | Permitir solicitação e registro de Serviços de Quarto (ex: lavanderia, refeição extra: data, descrição, valor). | `ServicoQuarto`, `Reserva` | UC Registrar Serviço de Quarto; composição com `Reserva`; tabela `servico_quarto`. |
| **RN12** | 3. Hospedagem | Governança de Limpeza | Permitir que a Camareira atualize o status de limpeza do Quarto (`Sujo`, `Em Limpeza`, `Limpo`). | `Quarto`, `StatusLimpeza` | UC Atualizar Limpeza do Quarto; Enum `StatusLimpeza`; perfil `Camareira`. |
| **RN13** | 5. Financeiro | Geração de Fatura no Check-out | Gerar Fatura automaticamente no Check-out somando: diárias (calculadas pelas datas), consumo de frigobar e serviços de quarto. | `Fatura`, `Reserva`, `CategoriaQuarto`, `ConsumoFrigobar`, `ServicoQuarto` | UC Processar Check-out e Faturamento (especificação detalhada em E3); cálculo polimórfico/domínio. |
| **RN14** | 5. Financeiro | Desconto Gerencial | Permitir que o Gerente aplique cupom de desconto percentual sobre o valor total da Fatura. | `Fatura`, `CupomDesconto`, `Usuario` (`Gerente`) | Fluxo alternativo/extensão no Check-out; validação de perfil `Gerente` e recálculo de saldo. |
| **RN15** | 5. Financeiro | Formas de Pagamento | Registrar Pagamento da Fatura aceitando: `Cartão de Crédito`, `PIX` ou `Dinheiro`. | `Pagamento`, `Fatura`, `FormaPagamento` | Diagrama de Sequência (E7); Enum/Polimorfismo de pagamento; tabela `pagamento`. |
| **RN16** | 5. Financeiro | Pagamento Fracionado | Permitir pagamentos fracionados, mantendo Fatura como `Parcialmente Paga` até quitação integral. | `Fatura`, `Pagamento`, `StatusFatura` | Multiplicidade `1` `Fatura` para `0..*` `Pagamentos`; controle de saldo restante e `StatusFatura`. |
| **RN17** | 3. Hospedagem | Liberação Pós-Check-out | Alterar status do Quarto para `Sujo` e `Disponível` imediatamente após quitação total e encerramento do Check-out. | `Quarto`, `Reserva`, `Fatura` | Encerramento transacional no Diagrama de Sequência (E7): `Fatura` quitada $\rightarrow$ `Reserva` `Finalizada` $\rightarrow$ `Quarto` `Disponível` + `Sujo`. |
| **RN18** | 5. Financeiro | Emissão de Recibo | Emitir Recibo de Hospedagem em formato de texto para o Hóspede após quitação total da fatura. | `Recibo`, `Fatura`, `Reserva`, `Hospede` | Geração de comprovante textual formatado somente quando `StatusFatura == PAGA`. |
| **RN19** | 3. Hospedagem | Histórico do Hóspede | Manter histórico de todas as reservas finalizadas vinculadas a um Hóspede. | `Hospede`, `Reserva` | UC Consultar Histórico do Hóspede; filtro por `StatusReserva.FINALIZADA`. |
| **RN20** | 5. Financeiro | Relatório de Taxa de Ocupação | Permitir que o Gerente gere relatório gerencial de taxa de ocupação dos quartos em um mês específico. | `RelatorioOcupacao`, `Quarto`, `Reserva` | UC Gerar Relatório de Ocupação (exclusivo `Gerente`); agregação de diárias ocupadas vs. capacidade mensal do hotel. |

---

## 3. Glossário Oficial de Termos do Domínio

Padronização conceitual e terminológica para uso consistente nos Diagramas UML, Banco de Dados e Código Java:

- **Categoria de Quarto (`CategoriaQuarto`):** Classificação do padrão de conforto da acomodação (`Standard`, `Luxo` ou `Suíte`), que determina o valor da tarifa diária (`RN03`, `RN04`).
- **Check-in:** Ato operacional de entrada efetiva do hóspede no hotel, iniciando a estadia (`Reserva` passa para `Em Andamento` e `Quarto` passa para `Ocupado` — `RN09`).
- **Check-out:** Processo de encerramento da estadia, compreendendo apuração da fatura, liquidação total dos pagamentos, emissão de recibo e liberação do quarto para limpeza (`RN13` a `RN18`).
- **Consumo de Frigobar (`ConsumoFrigobar`):** Registro de itens alimentícios/bebidas consumidos no quarto durante a estadia, contendo nome do produto, quantidade e valor unitário/total (`RN10`).
- **Cupom de Desconto (`CupomDesconto`):** Benefício percentual autorizado exclusivamente pelo `Gerente` para abatimento sobre o valor bruto da `Fatura` (`RN14`).
- **Diária:** Unidade financeira de cobrança equivalente a um pernoite/ciclo de hospedagem, cujo valor base depende da `CategoriaQuarto` (`RN04`, `RN13`).
- **Fatura (`Fatura`):** Documento financeiro consolidado no check-out que agrega o valor das diárias, consumos de frigobar e serviços de quarto, deduz eventuais descontos e controla o saldo pago (`RN13`, `RN14`, `RN16`).
- **Governança:** Setor hoteleiro responsável pela higienização e arrumação das acomodações, operado no sistema pelo perfil `Camareira` (`RN12`).
- **Hóspede (`Hospede`):** Cliente pessoa física (nacional com CPF ou estrangeiro com Passaporte) titular de reservas no hotel (`RN02`, `RN19`).
- **Pagamento (`Pagamento`):** Transação financeira individual (integral ou parcial/fracionada) realizada via `Cartão de Crédito`, `PIX` ou `Dinheiro` para amortizar o saldo de uma `Fatura` (`RN15`, `RN16`).
- **Quarto (`Quarto`):** Unidade física habitacional identificada por número e andar, que possui dois estados independentes e simultâneos: **Ocupação** (`Disponível` / `Ocupado`) e **Limpeza** (`Limpo` / `Em Limpeza` / `Sujo`) (`RN03`, `RN09`, `RN12`, `RN17`).
- **Recibo de Hospedagem (`Recibo`):** Comprovante textual emitido ao hóspede atestando a quitação integral da fatura e detalhando os valores pagos (`RN18`).
- **Reserva (`Reserva`):** Vínculo contratual e temporal entre um `Hospede` e um `Quarto` para um intervalo previsto de datas, passando pelos estados `Pendente`, `Confirmada`, `Em Andamento`, `Finalizada` ou `Cancelada` (`RN05`, `RN07`).
- **Serviço de Quarto (`ServicoQuarto`):** Prestação de serviço sob demanda durante a estadia (lavanderia, refeições extras, etc.), registrada com data, descrição e valor (`RN11`).
- **Taxa de Ocupação:** Indicador gerencial percentual calculado pela razão entre o total de quartos-dia efetivamente ocupados no mês e o total de quartos-dia disponíveis no hotel naquele mesmo mês (`RN20`).

---

## 4. Levantamento de Ambiguidades e Decisões de Projeto

Durante a análise rigorosa das regras `RN01` a `RN20`, foram identificadas **6 lacunas/ambiguidades naturais** de especificação que impactam os diagramas, o banco de dados e o código Java. Cada uma possui duas interpretações válidas deliberadas pela equipe:

### D01 – Condição de Limpeza para Realizar o Check-in (`RN09`, `RN12`, `RN17`)
- **Ambiguidade:** A `RN17` define que no pós-check-out o quarto fica `Disponível` e `Sujo`. A `RN09` não explicita se o check-in do próximo hóspede deve ser bloqueado enquanto o quarto ainda estiver `Sujo` ou `Em Limpeza`.
- **Opção A (Recomendada):** Bloquear o Check-in caso o quarto não esteja simultaneamente `Disponível` **e** `Limpo`. Garante coerência real com o trabalho da `Camareira` (`RN12`).
- **Opção B:** Permitir o Check-in exigindo apenas que esteja `Disponível`, exibindo no máximo um aviso sobre a limpeza.
- **Decisão da Equipe:** **Opção A aprovada** – Check-in exige quarto `Disponível` e `Limpo`.

### D02 – Contagem de Diárias e Regra de Fronteira nas Datas (`RN05`, `RN06`, `RN13`)
- **Ambiguidade:** Como tratar reservas em que um hóspede faz check-out no mesmo dia em que outro faz check-in (ex.: saída dia 15 e entrada de outro dia 15), e como cobrar caso o check-out ocorra no mesmo dia do check-in?
- **Opção A (Recomendada):** Adotar intervalo semiaberto `[dataCheckIn, dataCheckOut)` (padrão hoteleiro: saída no dia 15 permite nova entrada no dia 15 sem conflito na `RN06`), cobrando `dias = checkOut - checkIn` com **mínimo de 1 diária** caso a saída ocorra na mesma data de entrada.
- **Opção B:** Considerar a data de check-out como dia bloqueado integralmente (novo check-in só no dia seguinte) e cobrar estritamente os dias previstos no agendamento.
- **Decisão da Equipe:** **Opção A aprovada** – Intervalo semiaberto `[checkIn, checkOut)` e cobrança mínima de 1 diária.

### D03 – Cadastro Prévio vs. Aplicação Direta de Cupom de Desconto (`RN14`)
- **Ambiguidade:** A `RN14` cita aplicação de "cupom de desconto percentual" pelo `Gerente`, sem especificar se os cupons são entidades previamente cadastradas (ex.: `SENAC10` = 10%) ou informados livremente no ato.
- **Opção A (Recomendada):** Modelar a classe/tabela `CupomDesconto` (`codigo`, `percentual`, `ativo`), onde o Gerente informa o código de um cupom válido para abater na `Fatura`. Enriquece o Diagrama de Classes e evita erros de digitação de percentuais arbitrários.
- **Opção B:** Não criar entidade `CupomDesconto`; o Gerente apenas digita um código textual e a porcentagem diretamente nos atributos da `Fatura`.
- **Decisão da Equipe:** **Opção A aprovada** – Entidade `CupomDesconto` previamente cadastrada e validada na aplicação.

### D04 – Matriz de Permissões entre Recepcionista e Gerente (`RN01` a `RN20`)
- **Ambiguidade:** As regras especificam o ator em `RN12` (`Camareira`) e em `RN14`/`RN20` (`Gerente`), mas não delimitam quem mantém os cadastros estruturais (`Quartos` e `Tarifas` – `RN03`, `RN04`) nem se o `Gerente` herda os poderes da `Recepcionista`.
- **Opção A (Recomendada):** Hierarquia realista (Generalização de Atores na UML):
  - `Camareira`: apenas status de limpeza (`RN12`);
  - `Recepcionista`: operações diárias de atendimento (`Hóspedes`, `Reservas`, `Check-in`, `Frigobar`, `Serviços`, `Check-out`, `Pagamentos`, `Recibo`, `Histórico`);
  - `Gerente`: herda as permissões de `Recepcionista` + exclusividade em `Quartos`/`Tarifas` (`RN03`, `RN04`), `Cupom de Desconto` (`RN14`) e `Relatório de Ocupação` (`RN20`).
- **Opção B:** Segregação estrita sem herança (cada perfil vê apenas suas telas específicas, ficando `Quartos` e `Tarifas` com o Gerente, mas sem que o Gerente possa operar reservas/check-out).
- **Decisão da Equipe:** **Opção A aprovada** – Generalização de atores (`Gerente` herda permissões de `Recepcionista`).

### D05 – Datas Previstas vs. Datas Efetivas de Estadia (`RN05`, `RN06`, `RN09`, `RN13`, `RN20`)
- **Ambiguidade:** A `RN05` define o agendamento com `data prevista de check-in` e `data prevista de check-out`, enquanto `RN09`, `RN13` e `RN20` tratam da entrada real, apuração de diárias no check-out e taxa de ocupação mensal efetiva.
- **Opção A (Recomendada):** Manter `dataCheckInPrevista` e `dataCheckOutPrevista` para agendamento e validação de sobreposição (`RN05`, `RN06`), e adicionar `dataHoraCheckIn` e `dataHoraCheckOut` em `Reserva` para registrar a entrada e a saída reais (`RN09`, `RN13`, `RN20`), calculando as diárias e a ocupação efetiva com mínimo de 1 diária (`D02`).
- **Opção B:** Utilizar apenas `dataCheckInPrevista` e `dataCheckOutPrevista` tanto na reserva quanto no faturamento e no relatório mensal.
- **Decisão da Equipe:** **Opção A aprovada** – Registro separado de datas previstas (agendamento) e datas/horas efetivas (estadia real, cobrança e relatório).

### D06 – Congelamento Histórico da Fatura e Momento de Aplicação do Cupom (`RN04`, `RN13`, `RN14`, `RN16`, `RN19`)
- **Ambiguidade:** Caso o Gerente atualize futuramente a tarifa diária de uma categoria (`RN04`) ou o percentual de um cupom (`RN14`), faturas já emitidas no histórico (`RN19`) não podem sofrer alteração retroativa. Além disso, aplicar desconto percentual após pagamentos parciais (`RN16`) poderia reduzir o valor líquido abaixo do montante já pago.
- **Opção A (Recomendada):** Congelar na entidade/tabela `Fatura` os valores vigentes no ato do faturamento (`tarifaDiariaAplicada`, `valorBruto`, `percentualDesconto`, `valorDesconto`) e permitir a aplicação de cupom (`RN14`) somente enquanto a fatura estiver `Aguardando Pagamento` (`totalPago == 0`), limitando cupons a percentuais entre `> 0%` e `< 100%`.
- **Opção B:** Recalcular faturas dinamicamente a partir do cadastro vigente e permitir aplicação de desconto em qualquer etapa do pagamento parcial.
- **Decisão da Equipe:** **Opção A aprovada** – Congelamento histórico na `Fatura` e desconto permitido apenas antes do primeiro pagamento.
