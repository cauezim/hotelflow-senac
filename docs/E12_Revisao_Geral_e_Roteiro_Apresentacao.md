# Etapa E12 – Revisão Geral, Fechamento da Documentação e Roteiro de Apresentação
**Projeto:** HotelFlow – Sistema de Gestão Hoteleira (SENAC)  
**Disciplinas Integradas:** Análise Orientada a Objetos (AOO) e Programação Orientada a Objetos (POO – Prof. Pedro)  
**Versão de Fechamento:** `v1.0` (`1.0.0`)  
**Equipe:** Cauê (`@cauezim`) e Vanderlei Ricardo

---

## 1. Checklist Final de Auditoria Acadêmica (AOO + POO + Banco de Dados)

| Eixo de Avaliação | Entregável Exigido | Status | Evidência no Repositório |
| :--- | :--- | :---: | :--- |
| **Escopo Funcional** | Atendimento integral às 20 regras de negócio (`RN01`–`RN20`) sem extrapolação de escopo | **100% OK** | [E1](E1_Analise_Requisitos_e_Glossario.md) e [Matriz E11](E11_Testes_e_Matriz_Rastreabilidade.md) |
| **Modelagem AOO** | Casos de Uso (`E2`), Especificação `UC13` (`E3`), Abbott (`E4`), Classes (`E5`), Estados (`E6`) e Sequência (`E7`) | **100% OK** | `docs/E2..E7.md`, `.puml` e [HotelFlow_Modelagem_UML.pdf](diagramas/HotelFlow_Modelagem_UML.pdf) |
| **Banco de Dados** | Modelagem MySQL 8 (`InnoDB`), `PK`, `FK`, `UNIQUE`, `CHECK`, índices compostos e carga inicial | **100% OK** | [E8](E8_Modelagem_Banco_de_Dados.md), [DER PDF](diagramas/E8_DER_Logico_Fisico.pdf) e pasta `sql/` |
| **Arquitetura POO** | Projeto Java 17 Maven nativo no Apache NetBeans em camadas estritas (`model`, `dao`, `service`, `view`, `config`, `exception`) alinhado aos slides de POO | **100% OK** | [E9](E9_Arquitetura_Camadas.md), [pom.xml](../pom.xml) e 60 classes em `src/main/java` |
| **Domínio Rico** | Encapsulamento real nas entidades, `Enums` tipados, `BigDecimal`, SHA-256, laços `for-each` clássicos e exceções customizadas | **100% OK** | Pacotes `model`, `model.enums` e `exception` |
| **Persistência & Concorrência** | `ConnectionFactory` Singleton + transações ACID (`READ_COMMITTED` + `SELECT ... FOR UPDATE`) impedindo conflitos simultâneos em `RN06` e `RN15..RN17` | **100% OK** | `ConnectionFactory.java`, `ReservaService`, `CheckOutService` e 10 classes `DAO` |
| **Notificação E-mail (`RN08`)** | Envio real via protocolo SMTP (`SmtpEmailService` – Jakarta Mail) com *fallback* automático para console/modal caso offline | **100% OK** | Pacote `br.senac.hotelflow.service.notificacao` |
| **Interface Gráfica** | Aplicação Desktop Java Swing adaptativa por perfil (`Gerente`, `Recepcionista`, `Camareira`) com `ActionListener` tradicional | **100% OK** | Pacote `br.senac.hotelflow.view` |
| **Qualidade & Testes** | Suíte automatizada JUnit 5 com 15 testes compilando sem avisos e passando 100% no NetBeans (`Alt + F6`) | **100% OK** | 5 suítes em `src/test/java` |

---

## 2. Roteiro Sugerido de Apresentação para aEquipe (~12 Minutos)

### Parte 1 – Engenharia de Requisitos, UML e Banco de Dados (~6 min)
**Responsável sugerido:** **Cauê**

1. **Abertura e Problema (1 min):**
   - Apresentar o objetivo do **HotelFlow**: eliminar planilhas e controles manuais unificando **Recepção**, **Governança (Camareiras)** e **Gerência** sob uma base transacional única (`RN01` a `RN20`).
2. **Destaques de Modelagem UML (`E2`, `E5`, `E6`, `E7`) (3 min):**
   - Abrir o [HotelFlow_Modelagem_UML.pdf](diagramas/HotelFlow_Modelagem_UML.pdf):
     - **Página 1 (Casos de Uso):** destacar a generalização de atores (`Gerente` herda `Recepcionista` — `D04`), por que `Hóspede` é entidade e não ator (`RN01`) e por que `Emitir Recibo` é `«extend» [fatura quitada]` (`RN16`, `RN18`).
     - **Página 2 (Classes):** mostrar o **Domínio Rico** (evitando modelo anêmico), a separação entre datas previstas (`RN05`/`RN06`) e datas efetivas (`D05`) e o congelamento histórico na `Fatura` (`D06`).
     - **Página 3 (Estados):** destacar as **duas regiões ortogonais concorrentes** de `Quarto` (`Ocupação` $\parallel$ `Limpeza`), que garantem a trava do check-in apenas em quarto `Disponível` e `Limpo` (`D01`).
3. **Banco de Dados MySQL 8 (`E8`) (2 min):**
   - Mostrar o [DER Físico](diagramas/E8_DER_Logico_Fisico.pdf) e o MySQL Workbench: destacar `UNIQUE (id_reserva)` em `fatura` (garantindo `1 : 0..1`), `ON DELETE RESTRICT` preservando auditoria contábil (`RN19`) e os índices compostos para conflito de reservas (`RN06`) e ocupação mensal (`RN20`).

---

### Parte 2 – Arquitetura POO, Demonstração ao Vivo e Testes JUnit 5 (~6 min)
**Responsável sugerido:** **Vanderlei Ricardo**

1. **Arquitetura Java 17 no NetBeans (1,5 min):**
   - Mostrar a árvore de pacotes (`config`, `exception`, `model`, `dao`, `service`, `view`): explicar que a `view` **nunca** executa SQL diretamente, que `SmtpEmailService` implementa a interface `EmailService` com *fallback* seguro (`RN08`), e que `ReservaService` / `CheckOutService` usam transação JDBC com `SELECT ... FOR UPDATE` para impedir reservas conflitantes ou pagamentos duplicados simultâneos.
2. **Demonstração ao Vivo na Interface Swing (`F6`) (3,5 min):**
   - **Login 1 – `recepcao` (`Recepcao@123`):**
     1. Mostrar na aba *Reservas* a criação e confirmação de uma reserva $\rightarrow$ exibir o disparo do **E-mail Automático (`RN08`)** e tentar confirmar outra sobreposta para mostrar o bloqueio da **`RN06`**!
     2. Na aba *Estadia*, lançar 1 item de frigobar (`RN10`) na Reserva `#2` (Quarto `201`).
     3. Na aba *Check-out*, abrir a fatura da Reserva `#2` (`RN13`), clicar em **Aplicar Cupom `SENAC10`** $\rightarrow$ mostrar o sistema exigindo a senha do **Gerente** (`RN14`)!
     4. Fazer um **pagamento parcial** em `PIX` (`RN15`, `RN16` $\rightarrow$ fatura `Parcialmente Paga`, quarto segue `Ocupado`) e em seguida quitar o restante no `Cartão` $\rightarrow$ mostrar o **Recibo de Hospedagem (`RN18`)** na tela!
   - **Login 2 – `camareira` (`Camareira@123`):**
     - Clicar em *Trocar Perfil*, entrar como Camareira, mostrar que ela só enxerga a aba de Governança (`RN01`) e que o Quarto `201` acabou de ficar **`Sujo`** (`RN17`) $\rightarrow$ mudar para `Em Limpeza` e `Limpo` (`RN12`).
   - **Login 3 – `gerente` (`Gerente@123`):**
     - Entrar como Gerente, consultar o **Histórico Finalizado (`RN19`)** do hóspede e gerar na aba *Gerência* o **Relatório de Taxa de Ocupação Mensal (`RN20`)** de `11/2026`.
3. **Fechamento com Testes Automatizados (`Alt + F6`) (1 min):**
   - Rodar **`Test`** no NetBeans e mostrar os **15 testes JUnit 5 passando 100% verdes** cobrindo casos de borda (virada de mês, *day-use*, concorrência em reservas/pagamentos, fallback SMTP, tentativa de burlar permissão e congelamento de tarifa).

---

## 3. Perguntas Clássicas de Banca (Cola Técnica de Defesa)

1. **"Por que vocês usaram `BigDecimal` em Java e `DECIMAL(10,2)` no MySQL em vez de `double`?"**
   - *Resposta:* Tipos de ponto flutuante binário (`float`/`double`, padrão IEEE 754) geram imprecisão de centavos (ex.: `0.1 + 0.2 = 0.30000000000000004`), o que impediria uma fatura fracionada de zerar exatamente em `0.00` na `RN16`. `BigDecimal` com `RoundingMode.HALF_UP` e `DECIMAL(10,2)` garantem aritmética decimal exata.

2. **"Por que `CategoriaQuarto` virou classe/tabela e `FormaPagamento` virou apenas `Enum`?"**
   - *Resposta:* Pela `RN04`, o Gerente precisa alterar o valor da tarifa diária das categorias em tempo de execução no banco de dados (estado mutável persistente). Já `FormaPagamento` (`RN15`: Cartão, PIX, Dinheiro) é um conjunto fechado e imutável de constantes sem atributos mutáveis.

3. **"Se o Gerente aumentar a diária do quarto Standard hoje de R$ 200 para R$ 300, o que acontece com as faturas do mês passado?"**
   - *Resposta:* Nada muda nas faturas antigas! Pela nossa decisão `D06`, no momento em que a `Fatura` é gerada no check-out, ela congela `tarifaDiariaAplicada`, `quantidadeDiarias` e `valorBruto` em colunas próprias na tabela `fatura`, preservando 100% a fidelidade histórica exigida na `RN19`.

4. **"Como o sistema impede que dois recepcionistas confirmem reservas sobrepostas ao mesmo tempo (`RN06`) ou lancem pagamentos simultâneos sobre o mesmo saldo (`RN16`)?"**
   - *Resposta:* Abrimos uma transação JDBC (`setAutoCommit(false)` com isolamento `READ_COMMITTED`) e executamos `SELECT ... FOR UPDATE` no MySQL InnoDB seguindo sempre a mesma ordem `reserva -> quarto -> fatura`. O primeiro operador trava o registro e o segundo aguarda na fila até o `commit`; quando o segundo prossegue, lê os dados já atualizados e a regra de negócio barra o conflito ou o pagamento excedente.

5. **"Como funciona o envio de e-mail da `RN08` se o computador da sala não tiver internet?"**
   - *Resposta:* Aplicamos **Polimorfismo por Interface** (`EmailService`). A classe `SmtpEmailService` usa Jakarta Mail para enviar o e-mail real via SMTP quando configurado em `db.properties`, mas possui *timeout* de 5s e *fallback* automático para `ConsoleEmailService` + exibição em janela modal caso esteja sem rede, nunca travando a recepção do hotel.
