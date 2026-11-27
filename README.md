# HotelFlow – Sistema de Gestão Hoteleira

Projeto integrador final das disciplinas de **Análise Orientada a Objetos (AOO)** e **Programação Orientada a Objetos (POO – Prof. Pedro)** – **SENAC** (`Versão v1.0`).

## Objetivo do Sistema
Substituir o controle manual de reservas, ocupação e faturamento hoteleiro por uma plataforma desktop centralizada em **Java 17 (Swing + JDBC)** integrada ao **MySQL 8**, apoiando as rotinas de **Recepção**, **Governança (Camareiras)** e **Gerência** (`[Atende RN01..RN20]`).

---

## Estrutura do Repositório

```text
hotelflow/
├── docs/                   # Documentação completa de Engenharia de Software (Etapas E1 a E12)
│   └── diagramas/          # Fontes PlantUML (.puml) e cadernos vetoriais renderizados (.pdf)
├── sql/                    # Scripts DDL idempotente (01_schema) e Seed inicial (02_seed) p/ MySQL 8
├── src/
│   ├── main/java/br/senac/hotelflow/
│   │   ├── config/         # ConnectionFactory Singleton JDBC + suporte a transações ACID
│   │   ├── exception/      # Exceções customizadas de negócio, autorização e persistência
│   │   ├── model/          # Domínio Rico: 10 entidades + 6 Enums tipados (model.enums)
│   │   ├── dao/            # Camada de persistência JDBC (10 DAOs c/ PreparedStatement)
│   │   ├── service/        # Orquestração, permissões, SMTP c/ fallback (RN08), recibo e ocupação
│   │   └── view/           # Interface gráfica desktop em Java Swing adaptativa por perfil
│   ├── main/resources/     # Template db.properties.example (MySQL 8 + SMTP opcional)
│   └── test/java/          # Suíte automatizada JUnit 5 (15 testes cobrindo RN01 a RN20)
├── GEMINI.md               # Diretrizes metodológicas e regras de negócio (RN01 a RN20)
├── pom.xml                 # Configuração Apache Maven (nativa no Apache NetBeans)
└── README.md               # Guia de instalação, execução, roadmap e equipe
```

---

## Roadmap Completo do Projeto (Etapas E1 a E12 — 100% Concluído)

- [x] **E1**: Entendimento e numeração dos requisitos (`RN01`..`RN20`), glossário e decisões `D01`..`D06` — [Ver Documento](docs/E1_Analise_Requisitos_e_Glossario.md)
- [x] **E2**: Identificação de Atores e Diagrama Geral de Casos de Uso — [Ver Documento](docs/E2_Atores_e_Casos_de_Uso.md) | [Diagrama .puml](docs/diagramas/E2_Casos_de_Uso_Geral.puml)
- [x] **E3**: Especificação detalhada do Caso de Uso *"Processar Check-out e Faturamento"* — [Ver Documento](docs/E3_Especificacao_Caso_de_Uso.md)
- [x] **E4**: Análise Gramatical de Abbott (classes, atributos, métodos e descartes) — [Ver Documento](docs/E4_Analise_Gramatical_Abbott.md)
- [x] **E5**: Diagrama de Classes de Domínio Rico — [Ver Documento](docs/E5_Diagrama_de_Classes.md) | [Diagrama .puml](docs/diagramas/E5_Diagrama_de_Classes.puml)
- [x] **E6**: Diagrama de Estados UML (`Reserva`, `Quarto` ortogonal e `Fatura`) — [Ver Documento](docs/E6_Diagrama_de_Estados.md) | [Diagrama .puml](docs/diagramas/E6_Diagrama_de_Estados.puml)
- [x] **E7**: Diagrama de Sequência UML (pagamento, liquidação e encerramento) — [Ver Documento](docs/E7_Diagrama_de_Sequencia.md) | [Diagrama .puml](docs/diagramas/E7_Diagrama_de_Sequencia.puml) | [Caderno UML 4 Páginas .pdf](docs/diagramas/HotelFlow_Modelagem_UML.pdf)
- [x] **E8**: Modelagem Lógica e Física de Banco de Dados (MySQL 8) — [Ver Documento](docs/E8_Modelagem_Banco_de_Dados.md) | [DER .puml](docs/diagramas/E8_DER_Logico_Fisico.puml) | [DER .pdf](docs/diagramas/E8_DER_Logico_Fisico.pdf) | [DDL SQL](sql/01_schema_hotelflow.sql) | [Seed SQL](sql/02_seed_dados_iniciais.sql)
- [x] **E9**: Definição da Arquitetura Java 17+ em Camadas + JDBC (`v0.1-alpha`) — [Ver Documento](docs/E9_Arquitetura_Camadas.md) | [Diagrama .puml](docs/diagramas/E9_Arquitetura_Camadas.puml) | [pom.xml](pom.xml)
- [x] **E10**: Implementação incremental em Java 17+ (`v0.2-alpha` `model`, `v0.3-alpha` `dao`, `v0.8-alpha` `service` e `v0.9-beta` `view` Swing)
- [x] **E11**: Testes automatizados (JUnit 5) e Matriz Oficial de Rastreabilidade — [Ver Documento](docs/E11_Testes_e_Matriz_Rastreabilidade.md)
- [x] **E12**: Revisão geral, fechamento e Roteiro de Apresentação (`v1.0`) — [Ver Documento](docs/E12_Revisao_Geral_e_Roteiro_Apresentacao.md)

---

## Como Executar o Projeto (MySQL Workbench + Apache NetBeans)

### 1. Preparar o Banco de Dados (MySQL 8+)
1. No **MySQL Workbench**, abra e execute em ordem:
   - [`sql/01_schema_hotelflow.sql`](sql/01_schema_hotelflow.sql) *(cria o banco `hotelflow` e as 10 tabelas InnoDB)*;
   - [`sql/02_seed_dados_iniciais.sql`](sql/02_seed_dados_iniciais.sql) *(popula usuários, categorias, quartos, cupons e reservas de exemplo)*.

### 2. Configurar e Rodar no Apache NetBeans (Java 17+)
1. No NetBeans, acesse **File $\rightarrow$ Open Project** e selecione a pasta raiz do projeto.
2. Caso a senha do seu usuário `root` local no MySQL seja diferente de `root` (ou caso deseje ativar o **envio real de e-mails via SMTP** na `RN08` com Gmail/Mailtrap), edite `src/main/resources/db.properties` seguindo o modelo comentado em [`db.properties.example`](src/main/resources/db.properties.example). Sem credenciais SMTP ou sem internet, o sistema realiza *fallback* automático exibindo o e-mail no console e na tela.
3. Clique com o botão direito no projeto $\rightarrow$ **Clean and Build**.
4. Pressione **`F6` (Run Project)** para abrir a interface gráfica **HotelFlow**.

### 3. Credenciais Iniciais de Demonstração (`RN01`)

| Perfil | Login | Senha | Acesso Liberado no Sistema |
| :--- | :---: | :---: | :--- |
| **Gerente** | `gerente` | `Gerente@123` | Todas as rotinas de Recepção + Quartos, Tarifas, Cupons, Descontos e Relatório Mensal (`RN20`) |
| **Recepcionista** | `recepcao` | `Recepcao@123` | Hóspedes, Histórico, Reservas, Check-in, Frigobar, Serviços, Check-out, Pagamentos e Recibo |
| **Camareira** | `camareira` | `Camareira@123` | Painel exclusivo de Governança e Limpeza de Quartos (`Sujo`, `Em Limpeza`, `Limpo` — `RN12`) |

### 4. Executar a Suíte de Testes Automatizados (JUnit 5)
No NetBeans, clique com o botão direito sobre o projeto $\rightarrow$ **Test** (ou pressione **`Alt + F6`**). Todos os testes de domínio e regras de negócio executam em memória sem alterar os dados do seu banco local.

---

## Tecnologias Adotadas
- **Linguagem:** Java 17 (`Eclipse Adoptium / OpenJDK 17`)
- **Interface Gráfica:** Java Swing (`Nimbus LookAndFeel`)
- **Persistência e Banco:** MySQL 8.0+ (`InnoDB`) + JDBC (`mysql-connector-j 8.4.0`) com transações ACID e `SELECT ... FOR UPDATE`
- **Notificações (`RN08`):** Jakarta Mail (`angus-mail 2.0.3`) com *fallback* local automático
- **Build & Testes:** Apache Maven + JUnit 5 (`junit-jupiter 5.10.3` — 15 testes automatizados)
- **Modelagem:** UML 2.5 em PlantUML (`.puml`) e PDFs vetoriais

---

## Equipe (SENAC)
- **Cauê** — [@cauezim](https://github.com/cauezim)
- **Vanderlei Ricardo**
