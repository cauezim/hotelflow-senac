# Etapa E9 – Definição da Arquitetura Java 17+ em Camadas e Conexão JDBC
**Projeto:** HotelFlow – Sistema de Gestão Hoteleira (SENAC)  
**Disciplina Foco:** Programação Orientada a Objetos (POO – Prof. Pedro) | Marco `v0.1-alpha`  
**Artefatos Criados:**
- Configuração Maven (nativa no NetBeans): [pom.xml](file:///home/cauez/hotelaria/pom.xml)
- Fábrica Singleton JDBC Transacional: [ConnectionFactory.java](file:///home/cauez/hotelaria/src/main/java/br/senac/hotelflow/config/ConnectionFactory.java) e [db.properties.example](file:///home/cauez/hotelaria/src/main/resources/db.properties.example)
- Hierarquia de Exceções Customizadas: pacote `br.senac.hotelflow.exception`
- Contratos Base (`CrudDAO`, `EmailService`) e Bootstrap (`HotelFlowApp`): `src/main/java/br/senac/hotelflow/...`
- Diagrama Arquitetural PlantUML: [E9_Arquitetura_Camadas.puml](file:///home/cauez/hotelaria/docs/diagramas/E9_Arquitetura_Camadas.puml)

---

## 1. Visão Geral e Compatibilidade com Apache NetBeans (`Maven`)

Para atender simultaneamente às diretrizes de Engenharia de Software e à exigência da disciplina de POO (**Projeto Java pelo NetBeans**, Java 17+, MySQL Workbench/JDBC e JUnit 5), o projeto adota a estrutura padrão **Apache Maven** (`pom.xml`):
- No **Apache NetBeans**, projetos Maven são nativos (*File $\rightarrow$ Open Project* reconhece automaticamente o `pom.xml`, baixa o `mysql-connector-j:8.4.0` e o `junit-jupiter:5.10.3` da Maven Central sem precisar versionar `.jar` manuais no Git!).
- Cada integrante da equipe pode configurar sua própria senha local do MySQL copiando `src/main/resources/db.properties.example` para `src/main/resources/db.properties` (já protegido no `.gitignore`), evitando conflitos de credenciais nos *commits*.

---

## 2. Organização de Pacotes e Regras de Dependência entre Camadas

```text
src/
├── main/
│   ├── java/br/senac/hotelflow/
│   │   ├── config/                 # ConnectionFactory (Singleton JDBC + controle transacional)
│   │   ├── exception/              # Exceções customizadas de negócio, acesso e persistência
│   │   ├── model/                  # Entidades de Domínio Rico (10 classes da E5, zero SQL/UI)
│   │   │   └── enums/              # 6 Enumerações tipadas (PerfilUsuario, StatusReserva, etc.)
│   │   ├── dao/                    # Persistência JDBC (PreparedStatement, ResultSet -> Model)
│   │   ├── service/                # Regras transversais, permissões, transações, recibo e ocupação
│   │   │   └── notificacao/        # Interface EmailService + ConsoleEmailService [RN08]
│   │   └── view/                   # Interface com o usuário (desacoplada de SQL)
│   └── resources/
│       └── db.properties.example   # Template versionado de conexão MySQL 8
└── test/
    └── java/br/senac/hotelflow/    # Suíte de testes automatizados JUnit 5 (Etapa E11)
```

### Matriz Estrita de Acoplamento Permitido (Prevenção de Modelo Anêmico e *Spaghetti Code*)

| Camada | Pode Depender De | **PROIBIDO** Depender De | Justificativa Acadêmica |
| :--- | :--- | :--- | :--- |
| **`view`** | `service`, `model` (leitura/DTO), `exception` | `dao`, `config`, `java.sql.*` | Impede SQL ou regras financeiras espalhadas em botões/menus de tela. |
| **`service`** | `model`, `dao`, `config` (transação), `exception` | `view` (`javax.swing.*` / `Scanner`) | Permite testar toda a lógica de negócio via JUnit 5 sem abrir janelas nem ler teclado. |
| **`dao`** | `model`, `config`, `exception`, `java.sql.*` | `view`, `service` | Isola 100% do dialeto MySQL 8 e uso de `PreparedStatement` contra *SQL Injection*. |
| **`model`** | `model.enums`, `exception` | `dao`, `service`, `view`, `config`, `java.sql.*` | Mantém o **Domínio Rico Puro**: transições de estado (`E6`) e cálculos (`E3`/`E5`) vivem nas próprias entidades! |

---

## 3. Padrões de Projeto (*Design Patterns*) Aplicados na Arquitetura

1. **Singleton + Factory Method (`ConnectionFactory`):**
   - Usa *Double-Checked Locking* (`volatile`) para carregar uma única vez as propriedades de conexão (`db.properties` ou variáveis de ambiente `HOTELFLOW_DB_*`), mas entrega objetos `Connection` independentes por operação (`getConnection()`) ou por transação atômica (`getTransactionalConnection()` com `autoCommit = false`, `commit()` e `rollback()`).
   - **Por que NÃO guardar uma única `Connection` estática global aberta para sempre?** Porque conexões MySQL expiram por *timeout* (`wait_timeout`), não são *thread-safe* em operações concorrentes e misturariam `commit`/`rollback` de transações distintas. O Singleton guarda a *fábrica configurada*, abrindo e fechando conexões em blocos `try-with-resources`!
2. **DAO (*Data Access Object*):**
   - Padronizado pela interface genérica `CrudDAO<T, ID>`, encapsulando `PreparedStatement` e hidratando objetos de domínio.
3. **Dependency Inversion / Strategy (`EmailService` — `[Atende RN08]`):**
   - `ReservaService` depende da abstração `EmailService` (e não de uma classe concreta rígida), permitindo injetar `ConsoleEmailService` na execução local e um *Mock/Spy* verificável nos testes JUnit 5 da `E11`.
4. **Tradutor de Exceções (*Exception Shielding*):**
   - Qualquer `SQLException` capturada nos DAOs ou na `ConnectionFactory` é convertida em `PersistenciaException`, enquanto violações funcionais lançam `AutenticacaoException` (`RN01`), `AutorizacaoException` (`RN14`, `RN20`, `D04`), `EntidadeNaoEncontradaException` ou `RegraNegocioException` (`RN06`, `RN07`, `RN09`, `RN16`, `D01`–`D06`).

---

## 4. Plano Incremental de Implementação da Etapa E10 (Alinhado ao Cronograma de POO)

| Marco POO | Módulo da Etapa E10 | Escopo de Código Java 17+ | Requisitos Cobertos |
| :---: | :--- | :--- | :--- |
| `v0.1-alpha` | **Base Arquitetural (E9 – Atual)** | `pom.xml`, pacotes, `ConnectionFactory`, exceções e contratos base | Fundação `RN01..RN20` |
| `v0.2-alpha` | **Módulo 10.1 – Domínio Rico (`model` + `enums`)** | Os 6 `Enums` e as 10 classes de domínio com todas as validações, estados (`E6`) e cálculos (`E3`/`E5`) | `RN01..RN19`, `D01..D06` |
| `v0.3-alpha` | **Módulo 10.2 – Persistência JDBC (`dao`)** | Os DAOs completos para as 10 tabelas MySQL 8 (incluindo query de conflito `RN06`, histórico `RN19` e ocupação `RN20`) | `RN01..RN20` |
| `v0.4`–`v0.8` | **Módulo 10.3 – Serviços de Negócio (`service`)** | `AutenticacaoService`, `CadastroService`, `ReservaService`, `HospedagemService`, `CheckOutService`, `GeradorReciboService`, `RelatorioOcupacaoService` | `RN01..RN20` |
| `v0.9-beta` | **Módulo 10.4 – Camada de Visão (`view`)** | Interface completa por perfil (`Gerente`, `Recepcionista`, `Camareira`) | `RN01..RN20` |
