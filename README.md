# HotelFlow – Sistema de Gestão Hoteleira

Projeto integrador das disciplinas de **Análise Orientada a Objetos (AOO)** e **Programação Orientada a Objetos (POO)** – **SENAC**.

## Objetivo do Sistema
Substituir o controle manual de reservas, ocupação e faturamento hoteleiro por uma plataforma centralizada que apoia as operações de **Recepção**, **Governança (Camareiras)** e **Gerência** (`[Atende RN01..RN20]`).

---

## Estrutura do Repositório

```text
hotelflow/
├── docs/                   # Documentação de Engenharia de Software (Etapas E1 a E7, E11, E12)
│   └── diagramas/          # Código-fonte dos diagramas UML em PlantUML (.puml)
├── sql/                    # Modelagem física e scripts DDL/DML para MySQL 8 (Etapa E8)
├── src/                    # Código-fonte Java 17+ em camadas: model, dao, service, view (Etapas E9 a E11)
├── GEMINI.md               # Diretrizes metodológicas e regras de negócio (RN01 a RN20)
└── README.md               # Visão geral, roadmap e guia de colaboração da equipe
```

---

## Roadmap do Projeto (Etapas E1 a E12)

- [x] **E1**: Entendimento e numeração dos requisitos (`RN01`..`RN20`), glossário e levantamento de dúvidas — [Ver Documento](docs/E1_Analise_Requisitos_e_Glossario.md)
- [x] **E2**: Identificação de Atores e Diagrama Geral de Casos de Uso (PlantUML) — [Ver Documento](docs/E2_Atores_e_Casos_de_Uso.md) | [Diagrama .puml](docs/diagramas/E2_Casos_de_Uso_Geral.puml)
- [ ] **E3**: Especificação detalhada do Caso de Uso *"Processar Check-out e Faturamento"*
- [ ] **E4**: Análise Gramatical de Abbott (candidatos a classes, atributos e métodos)
- [ ] **E5**: Diagrama de Classes de Domínio
- [ ] **E6**: Diagrama de Estados UML (`Reserva`, `Quarto` e `Fatura`)
- [ ] **E7**: Diagrama de Sequência UML (pagamento, liquidação e encerramento de conta)
- [ ] **E8**: Modelagem Lógica e Física de Banco de Dados (MySQL 8)
- [ ] **E9**: Definição da Arquitetura Java 17+ (`model`, `dao`, `service`, `view` + JDBC)
- [ ] **E10**: Implementação incremental dos módulos em Java 17+
- [ ] **E11**: Testes automatizados (JUnit 5) e Matriz de Rastreabilidade
- [ ] **E12**: Revisão geral, fechamento da documentação e roteiro de apresentação

---

## Tecnologias Adotadas
- **Linguagem:** Java 17+
- **Banco de Dados:** MySQL 8 (conexão via JDBC)
- **Testes:** JUnit 5
- **Modelagem UML:** PlantUML (`.puml`) e Markdown (`.md`)

---

## Guia Rápido para a Equipe (Fluxo Git)

1. **Clonar o repositório (fora de pastas sincronizadas pelo OneDrive/Google Drive):**
   ```bash
   git clone https://github.com/cauezim/hotelflow-senac.git
   cd hotelflow-senac
   ```
2. **Antes de começar uma tarefa, atualizar a branch principal:**
   ```bash
   git pull origin main
   ```
3. **Registrar e enviar alterações com rastreabilidade no commit:**
   ```bash
   git add .
   git commit -m "docs(E1): adiciona matriz de requisitos RN01 a RN20"
   git push origin main
   ```

## Equipe
- Integrante 1: [@cauezim](https://github.com/cauezim)
- Integrante 2: *(preencher nome)*
- Integrante 3: *(preencher nome)*
