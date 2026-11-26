-- =============================================================================
-- HotelFlow – Sistema de Gestão Hoteleira (SENAC)
-- Etapa E8: Script DDL Físico para MySQL 8.0+
-- Rastreabilidade Completa: [Atende RN01 a RN20 e Decisões D01 a D06]
-- Engine: InnoDB (Suporte ACID a Transações e Chaves Estrangeiras)
-- Charset: utf8mb4 / Collation: utf8mb4_0900_ai_ci
-- =============================================================================

CREATE DATABASE IF NOT EXISTS hotelflow
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE hotelflow;

-- Ordem reversa de dependências para recriação limpa em ambiente de testes
DROP TABLE IF EXISTS pagamento;
DROP TABLE IF EXISTS fatura;
DROP TABLE IF EXISTS servico_quarto;
DROP TABLE IF EXISTS consumo_frigobar;
DROP TABLE IF EXISTS reserva;
DROP TABLE IF EXISTS cupom_desconto;
DROP TABLE IF EXISTS quarto;
DROP TABLE IF EXISTS categoria_quarto;
DROP TABLE IF EXISTS hospede;
DROP TABLE IF EXISTS usuario;

-- =============================================================================
-- 1. TABELA: usuario [Atende RN01, D04]
-- Autenticação por login/senha (hash SHA-256) e controle de acesso por perfil.
-- =============================================================================
CREATE TABLE usuario (
    id_usuario INT AUTO_INCREMENT,
    nome       VARCHAR(120) NOT NULL,
    login      VARCHAR(60)  NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    perfil     VARCHAR(20)  NOT NULL,

    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uk_usuario_login UNIQUE (login),
    CONSTRAINT chk_usuario_perfil CHECK (perfil IN ('CAMAREIRA', 'RECEPCIONISTA', 'GERENTE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =============================================================================
-- 2. TABELA: hospede [Atende RN02, RN19]
-- Cadastro de clientes (CPF ou Passaporte único) e base do histórico.
-- =============================================================================
CREATE TABLE hospede (
    id_hospede    INT AUTO_INCREMENT,
    nome_completo VARCHAR(150) NOT NULL,
    documento     VARCHAR(30)  NOT NULL,
    telefone      VARCHAR(25)  NOT NULL,
    email         VARCHAR(150) NOT NULL,
    endereco      VARCHAR(255) NOT NULL,

    CONSTRAINT pk_hospede PRIMARY KEY (id_hospede),
    CONSTRAINT uk_hospede_documento UNIQUE (documento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =============================================================================
-- 3. TABELA: categoria_quarto [Atende RN03, RN04]
-- Classificação das acomodações (Standard, Luxo, Suíte) e tarifa diária vigente.
-- =============================================================================
CREATE TABLE categoria_quarto (
    id_categoria  INT AUTO_INCREMENT,
    nome          VARCHAR(30)    NOT NULL,
    tarifa_diaria DECIMAL(10, 2) NOT NULL,

    CONSTRAINT pk_categoria_quarto PRIMARY KEY (id_categoria),
    CONSTRAINT uk_categoria_nome UNIQUE (nome),
    CONSTRAINT chk_categoria_nome CHECK (nome IN ('Standard', 'Luxo', 'Suíte')),
    CONSTRAINT chk_categoria_tarifa CHECK (tarifa_diaria > 0.00)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =============================================================================
-- 4. TABELA: quarto [Atende RN03, RN09, RN12, RN17, D01]
-- Unidade habitacional com estados ortogonais de Ocupação e Governança/Limpeza.
-- =============================================================================
CREATE TABLE quarto (
    id_quarto         INT AUTO_INCREMENT,
    id_categoria      INT         NOT NULL,
    numero            VARCHAR(10) NOT NULL,
    andar             INT         NOT NULL,
    capacidade_maxima INT         NOT NULL,
    status_ocupacao   VARCHAR(20) NOT NULL DEFAULT 'DISPONIVEL',
    status_limpeza    VARCHAR(20) NOT NULL DEFAULT 'LIMPO',

    CONSTRAINT pk_quarto PRIMARY KEY (id_quarto),
    CONSTRAINT uk_quarto_numero UNIQUE (numero),
    CONSTRAINT fk_quarto_categoria FOREIGN KEY (id_categoria)
        REFERENCES categoria_quarto (id_categoria)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_quarto_andar CHECK (andar >= 0),
    CONSTRAINT chk_quarto_capacidade CHECK (capacidade_maxima > 0),
    CONSTRAINT chk_quarto_ocupacao CHECK (status_ocupacao IN ('DISPONIVEL', 'OCUPADO')),
    CONSTRAINT chk_quarto_limpeza CHECK (status_limpeza IN ('SUJO', 'EM_LIMPEZA', 'LIMPO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_quarto_status ON quarto (status_ocupacao, status_limpeza);

-- =============================================================================
-- 5. TABELA: cupom_desconto [Atende RN14, D03, D06]
-- Cupons percentuais pré-cadastrados para abatimento autorizado pelo Gerente.
-- =============================================================================
CREATE TABLE cupom_desconto (
    id_cupom   INT AUTO_INCREMENT,
    codigo     VARCHAR(30)   NOT NULL,
    percentual DECIMAL(5, 2) NOT NULL,
    ativo      BOOLEAN       NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_cupom_desconto PRIMARY KEY (id_cupom),
    CONSTRAINT uk_cupom_codigo UNIQUE (codigo),
    CONSTRAINT chk_cupom_percentual CHECK (percentual > 0.00 AND percentual < 100.00)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =============================================================================
-- 6. TABELA: reserva [Atende RN05..RN09, RN13, RN17, RN19, RN20, D01, D02, D05]
-- Agendamento previsto e registro efetivo de entrada/saída da estadia.
-- =============================================================================
CREATE TABLE reserva (
    id_reserva             INT AUTO_INCREMENT,
    id_hospede             INT         NOT NULL,
    id_quarto              INT         NOT NULL,
    data_checkin_prevista  DATE        NOT NULL,
    data_checkout_prevista DATE        NOT NULL,
    data_hora_checkin      DATETIME    NULL,
    data_hora_checkout     DATETIME    NULL,
    status                 VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',

    CONSTRAINT pk_reserva PRIMARY KEY (id_reserva),
    CONSTRAINT fk_reserva_hospede FOREIGN KEY (id_hospede)
        REFERENCES hospede (id_hospede)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_reserva_quarto FOREIGN KEY (id_quarto)
        REFERENCES quarto (id_quarto)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_reserva_status CHECK (
        status IN ('PENDENTE', 'CONFIRMADA', 'EM_ANDAMENTO', 'FINALIZADA', 'CANCELADA')
    ),
    CONSTRAINT chk_reserva_datas_previstas CHECK (
        data_checkout_prevista >= data_checkin_prevista
    ),
    CONSTRAINT chk_reserva_datas_efetivas CHECK (
        data_hora_checkout IS NULL
        OR (data_hora_checkin IS NOT NULL AND data_hora_checkout >= data_hora_checkin)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Índices compostos orientados às consultas críticas do sistema:
-- [Atende RN06, D02]: Verificação rápida de sobreposição de datas por quarto
CREATE INDEX idx_reserva_conflito
    ON reserva (id_quarto, status, data_checkin_prevista, data_checkout_prevista);

-- [Atende RN19]: Histórico de reservas finalizadas de um hóspede
CREATE INDEX idx_reserva_historico
    ON reserva (id_hospede, status);

-- [Atende RN20, D05]: Apuração de taxa de ocupação efetiva em um mês específico
CREATE INDEX idx_reserva_ocupacao
    ON reserva (status, data_hora_checkin, data_hora_checkout);

-- =============================================================================
-- 7. TABELA: consumo_frigobar [Atende RN10, RN13]
-- Itens consumidos no frigobar durante a estadia ativa.
-- =============================================================================
CREATE TABLE consumo_frigobar (
    id_consumo         INT AUTO_INCREMENT,
    id_reserva         INT            NOT NULL,
    produto            VARCHAR(100)   NOT NULL,
    quantidade         INT            NOT NULL,
    valor_unitario     DECIMAL(10, 2) NOT NULL,
    data_hora_registro DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_consumo_frigobar PRIMARY KEY (id_consumo),
    CONSTRAINT fk_consumo_reserva FOREIGN KEY (id_reserva)
        REFERENCES reserva (id_reserva)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_consumo_quantidade CHECK (quantidade > 0),
    CONSTRAINT chk_consumo_valor_unit CHECK (valor_unitario > 0.00)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_consumo_reserva ON consumo_frigobar (id_reserva);

-- =============================================================================
-- 8. TABELA: servico_quarto [Atende RN11, RN13]
-- Solicitações e registros de serviços extras (lavanderia, refeições, etc.).
-- =============================================================================
CREATE TABLE servico_quarto (
    id_servico            INT AUTO_INCREMENT,
    id_reserva            INT            NOT NULL,
    data_hora_solicitacao DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    descricao             VARCHAR(200)   NOT NULL,
    valor                 DECIMAL(10, 2) NOT NULL,

    CONSTRAINT pk_servico_quarto PRIMARY KEY (id_servico),
    CONSTRAINT fk_servico_reserva FOREIGN KEY (id_reserva)
        REFERENCES reserva (id_reserva)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_servico_valor CHECK (valor > 0.00)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_servico_reserva ON servico_quarto (id_reserva);

-- =============================================================================
-- 9. TABELA: fatura [Atende RN13..RN18, D06]
-- Consolidação financeira única por reserva (UNIQUE id_reserva) com congelamento
-- histórico de diárias, tarifa aplicada e desconto gerencial.
-- =============================================================================
CREATE TABLE fatura (
    id_fatura              INT AUTO_INCREMENT,
    id_reserva             INT            NOT NULL,
    id_cupom               INT            NULL,
    data_emissao           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    quantidade_diarias     BIGINT         NOT NULL,
    tarifa_diaria_aplicada DECIMAL(10, 2) NOT NULL,
    valor_bruto            DECIMAL(10, 2) NOT NULL,
    percentual_desconto    DECIMAL(5, 2)  NOT NULL DEFAULT 0.00,
    valor_desconto         DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    status                 VARCHAR(25)    NOT NULL DEFAULT 'AGUARDANDO_PAGAMENTO',

    CONSTRAINT pk_fatura PRIMARY KEY (id_fatura),
    CONSTRAINT uk_fatura_reserva UNIQUE (id_reserva),
    CONSTRAINT fk_fatura_reserva FOREIGN KEY (id_reserva)
        REFERENCES reserva (id_reserva)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_fatura_cupom FOREIGN KEY (id_cupom)
        REFERENCES cupom_desconto (id_cupom)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_fatura_diarias CHECK (quantidade_diarias >= 1),
    CONSTRAINT chk_fatura_tarifa CHECK (tarifa_diaria_aplicada > 0.00),
    CONSTRAINT chk_fatura_bruto CHECK (valor_bruto > 0.00),
    CONSTRAINT chk_fatura_perc_desconto CHECK (
        percentual_desconto >= 0.00 AND percentual_desconto < 100.00
    ),
    CONSTRAINT chk_fatura_val_desconto CHECK (
        valor_desconto >= 0.00 AND valor_desconto < valor_bruto
    ),
    CONSTRAINT chk_fatura_status CHECK (
        status IN ('AGUARDANDO_PAGAMENTO', 'PARCIALMENTE_PAGA', 'PAGA')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =============================================================================
-- 10. TABELA: pagamento [Atende RN15, RN16]
-- Transações financeiras individuais (únicas ou fracionadas) de uma fatura.
-- =============================================================================
CREATE TABLE pagamento (
    id_pagamento    INT AUTO_INCREMENT,
    id_fatura       INT            NOT NULL,
    valor           DECIMAL(10, 2) NOT NULL,
    data_pagamento  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    forma_pagamento VARCHAR(20)    NOT NULL,

    CONSTRAINT pk_pagamento PRIMARY KEY (id_pagamento),
    CONSTRAINT fk_pagamento_fatura FOREIGN KEY (id_fatura)
        REFERENCES fatura (id_fatura)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_pagamento_valor CHECK (valor > 0.00),
    CONSTRAINT chk_pagamento_forma CHECK (
        forma_pagamento IN ('CARTAO_CREDITO', 'PIX', 'DINHEIRO')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_pagamento_fatura ON pagamento (id_fatura);
