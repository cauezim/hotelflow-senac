# Etapa E9 – Definição da Arquitetura Java 17+ em Camadas e Conexão JDBC
**Projeto:** HotelFlow – Sistema de Gestão Hoteleira (SENAC)  
**Disciplina Foco:** Programação Orientada a Objetos (POO – Prof. Pedro) | Marco `v0.1-alpha`  
**Artefatos Criados:**
- Configuração Maven (nativa no NetBeans): [pom.xml](../pom.xml)
- Fábrica Singleton JDBC Transacional: [ConnectionFactory.java](../src/main/java/br/senac/hotelflow/config/ConnectionFactory.java) e [db.properties.example](../src/main/resources/db.properties.example)
- Hierarquia de Exceções Customizadas: pacote `br.senac.hotelflow.exception`
- Interface de Notificação (`EmailService`, `SmtpEmailService`, `ConsoleEmailService`) e Bootstrap (`HotelFlowApp`)
- Diagrama Arquitetural PlantUML: [E9_Arquitetura_Camadas.puml](diagramas/E9_Arquitetura_Camadas.puml)

---

## 1. Visão Geral e Compatibilidade com Apache NetBeans (`Maven`)

Para atender simultaneamente às diretrizes de Engenharia de Software e à exigência da disciplina de POO (**Projeto Java pelo NetBeans**, Java 17+, MySQL Workbench/JDBC e JUnit 5), o projeto adota a estrutura padrão **Apache Maven** (`pom.xml`):
- No **Apache NetBeans**, projetos Maven são nativos (*File $\rightarrow$ Open Project* reconhece automaticamente o `pom.xml`, gerenciando `mysql-connector-j:8.4.0`, `angus-mail:2.0.3` e `junit-jupiter:5.10.3` sem precisar versionar `.jar` manuais no Git).
- Cada integrante da equipe pode configurar sua própria senha local do MySQL e credenciais opcionais de SMTP copiando `src/main/resources/db.properties.example` para `src/main/resources/db.properties` (protegido no `.gitignore`).

---

## 2. Organização de Pacotes e Regras de Dependência entre Camadas

```text
src/
├── main/
│   ├── java/br/senac/hotelflow/
│   │   ├── config/                 # ConnectionFactory (Singleton JDBC + transações e isolamento)
│   │   ├── exception/              # Exceções customizadas de negócio, acesso e persistência
│   │   ├── model/                  # Entidades de Domínio Rico (10 classes da E5, zero SQL/UI)
│   │   │   └── enums/              # 6 Enumerações tipadas (PerfilUsuario, StatusReserva, etc.)
│   │   ├── dao/                    # 10 DAOs JDBC (PreparedStatement, SELECT ... FOR UPDATE, ResultSet -> Model)
│   │   ├── service/                # Regras transversais, permissões, transações, recibo e ocupação
│   │   │   └── notificacao/        # Interface EmailService + SmtpEmailService + ConsoleEmailService [RN08]
│   │   └── view/                   # Interface desktop Swing adaptativa por perfil (desacoplada de SQL)
│   └── resources/
│       └── db.properties.example   # Template versionado de conexão MySQL 8 e SMTP
└── test/
    └── java/br/senac/hotelflow/    # Suíte de testes automatizados JUnit 5 (Etapa E11)
```

### Matriz Estrita de Acoplamento Permitido (Prevenção de Modelo Anêmico e *Spaghetti Code*)

| Camada | Pode Depender De | **PROIBIDO** Depender De | Justificativa Acadêmica |
| :--- | :--- | :--- | :--- |
| **`view`** | `service`, `model` (leitura), `exception` | `dao`, `config`, `java.sql.*` | Impede SQL ou regras financeiras espalhadas em botões/menus de tela. |
| **`service`** | `model`, `dao`, `config` (transação), `exception` | `view` (`javax.swing.*`) | Permite testar toda a lógica de negócio via JUnit 5 sem abrir janelas gráficas. |
| **`dao`** | `model`, `config`, `exception`, `java.sql.*` | `view`, `service` | Isola 100% do dialeto MySQL 8 e uso de `PreparedStatement` contra *SQL Injection*. |
| **`model`** | `model.enums`, `exception` | `dao`, `service`, `view`, `config`, `java.sql.*` | Mantém o **Domínio Rico Puro**: transições de estado (`E6`) e cálculos (`E3`/`E5`) vivem nas próprias entidades! |

---

## 3. Decisões Didáticas e Padrões de Projeto Aplicados

1. **Singleton Simples + Fábrica de Conexões (`ConnectionFactory`):**
   - Carrega uma única vez as propriedades de conexão (`db.properties` ou variáveis de ambiente `HOTELFLOW_DB_*`), entregando objetos `Connection` independentes por operação (`getConnection()`) ou por transação atômica (`getTransactionalConnection()` com `READ_COMMITTED` + `autoCommit = false`).
   - **Por que NÃO guardar uma única `Connection` estática global aberta para sempre?** Porque conexões MySQL expiram por *timeout*, não são seguras em operações simultâneas e misturariam `commit`/`rollback` de operadores distintos.
2. **Controle de Concorrência Pessimista (`SELECT ... FOR UPDATE` — `[Atende RN06, RN09, RN13..RN17]`):**
   - Em `ReservaService.confirmarReserva` e `CheckOutService`, o sistema adquire bloqueio exclusivo de linha no MySQL InnoDB na ordem estrita **`reserva -> quarto -> fatura`** antes de checar conflito de datas ou consultar saldo devedor, impedindo reservas conflitantes simultâneas e pagamentos duplicados sem risco de *deadlock*.
3. **Polimorfismo por Interface no Envio de E-mail (`EmailService` — `[Atende RN08]`):**
   - `ReservaService` depende apenas da interface `EmailService`. Em produção/apresentação, `SmtpEmailService` realiza envio real via **Jakarta Mail (SMTP)** caso configurado em `db.properties` e aciona *fallback* automático para `ConsoleEmailService` caso o ambiente esteja offline.
4. **Código POO Clássico Alinhado à Ementa da Disciplina:**
   - Uso consistente de encapsulamento (`private`), construtores sobrecarregados, coleções `ArrayList<T>` com laços `for-each` tradicionais, `switch/case/break` explícitos, retorno direto de entidades nos DAOs e `ActionListener` padrão no Java Swing — facilitando a leitura e a defesa oral de cada linha pela equipe.
5. **Tradutor de Exceções (*Exception Shielding*):**
   - Qualquer `SQLException` capturada nos DAOs é convertida em `PersistenciaException`, enquanto violações de domínio lançam `AutenticacaoException` (`RN01`), `AutorizacaoException` (`RN14`, `RN20`, `D04`), `EntidadeNaoEncontradaException` ou `RegraNegocioException`.

---

## 4. Plano Incremental de Implementação da Etapa E10 (Alinhado ao Cronograma de POO)

| Marco POO | Módulo da Etapa E10 | Escopo de Código Java 17+ | Requisitos Cobertos |
| :---: | :--- | :--- | :--- |
| `v0.1-alpha` | **Base Arquitetural (E9)** | `pom.xml`, pacotes, `ConnectionFactory`, exceções e contratos base | Fundação `RN01..RN20` |
| `v0.2-alpha` | **Módulo 10.1 – Domínio Rico (`model` + `enums`)** | Os 6 `Enums` e as 10 classes de domínio com validações, estados (`E6`) e cálculos (`E3`/`E5`) | `RN01..RN19`, `D01..D06` |
| `v0.3-alpha` | **Módulo 10.2 – Persistência JDBC (`dao`)** | Os 10 DAOs para MySQL 8 (com bloqueio `FOR UPDATE`, conflito `RN06`, histórico `RN19` e ocupação `RN20`) | `RN01..RN20` |
| `v0.4`–`v0.8` | **Módulo 10.3 – Serviços de Negócio (`service`)** | `AutenticacaoService`, `CadastroService`, `ReservaService`, `SmtpEmailService`, `HospedagemService`, `CheckOutService`, `GeradorReciboService`, `RelatorioOcupacaoService` | `RN01..RN20` |
| `v0.9`–`v1.0` | **Módulo 10.4 – Camada de Visão (`view`) e Testes** | Interface Swing por perfil (`Gerente`, `Recepcionista`, `Camareira`) + 15 testes JUnit 5 | `RN01..RN20` |
