-- =============================================================================
-- HotelFlow – Sistema de Gestão Hoteleira (SENAC)
-- Etapa E8: Script DML de Carga Inicial (Seed) e Cenários de Homologação
-- Compatível com MySQL 8.0+ | [Atende RN01 a RN20 e Decisões D01 a D06]
-- =============================================================================

USE hotelflow;

-- =============================================================================
-- 1. USUÁRIOS AUTENTICÁVEIS (Perfis da RN01 / D04)
-- Hash SHA-256 UTF-8 padrão (java.security.MessageDigest "SHA-256"):
--   • login: gerente   | senha em texto plano: Gerente@123
--   • login: recepcao  | senha em texto plano: Recepcao@123
--   • login: camareira | senha em texto plano: Camareira@123
-- =============================================================================
INSERT INTO usuario (nome, login, senha_hash, perfil) VALUES
('Carlos Eduardo Mendes', 'gerente',   '33117dd921f51939cf62d3c22efbb0559de983e77941352f453500d7cbd7dd4f', 'GERENTE'),
('Fernanda Alves Costa',  'recepcao',  '70f96b3ae3688b9d67735d3d01b5564dd487be0839e7c8659481b15a49f60066', 'RECEPCIONISTA'),
('Luciana Souza Rocha',   'camareira', '11492c2afbbab1668e584750b7886e046516982861118b1f40108f557efef518', 'CAMAREIRA');

-- =============================================================================
-- 2. CATEGORIAS E TARIFAS DIÁRIAS [Atende RN03, RN04]
-- =============================================================================
INSERT INTO categoria_quarto (id_categoria, nome, tarifa_diaria) VALUES
(1, 'Standard', 200.00),
(2, 'Luxo',     350.00),
(3, 'Suíte',    580.00);

-- =============================================================================
-- 3. QUARTOS POR ANDAR E CATEGORIA [Atende RN03, RN09, RN12, RN17, D01]
-- Inclui quarto OCUPADO (201) e quarto SUJO pós-check-out (102) para testes.
-- =============================================================================
INSERT INTO quarto (id_quarto, id_categoria, numero, andar, capacidade_maxima, status_ocupacao, status_limpeza) VALUES
(1, 1, '101', 1, 2, 'DISPONIVEL', 'LIMPO'),
(2, 1, '102', 1, 3, 'DISPONIVEL', 'SUJO'),
(3, 2, '201', 2, 2, 'OCUPADO',    'LIMPO'),
(4, 2, '202', 2, 4, 'DISPONIVEL', 'EM_LIMPEZA'),
(5, 3, '301', 3, 4, 'DISPONIVEL', 'LIMPO');

-- =============================================================================
-- 4. CUPONS DE DESCONTO GERENCIAIS [Atende RN14, D03, D06]
-- =============================================================================
INSERT INTO cupom_desconto (id_cupom, codigo, percentual, ativo) VALUES
(1, 'SENAC10',    10.00, TRUE),
(2, 'FIDELIDADE', 15.00, TRUE),
(3, 'PROMO20OFF', 20.00, FALSE);

-- =============================================================================
-- 5. HÓSPEDES CADASTRADOS (CPF Nacional e Passaporte Estrangeiro) [Atende RN02]
-- =============================================================================
INSERT INTO hospede (id_hospede, nome_completo, documento, telefone, email, endereco) VALUES
(1, 'Mariana Oliveira Santos', '391.482.718-05', '(11) 99812-3344', 'mariana.santos@email.com', 'Av. Paulista, 1578 - São Paulo/SP'),
(2, 'Roberto Almeida Prado',   '214.639.850-40', '(11) 98745-1020', 'roberto.prado@email.com',  'Rua Augusta, 890 - São Paulo/SP'),
(3, 'Elena Martín Gómez',      'ESP-AA849201',   '+34 612 489 103', 'elena.martin@correo.es',   'Calle Mayor, 42 - Madrid, Espanha');

-- =============================================================================
-- 6. CENÁRIOS DE RESERVAS, CONSUMOS, SERVIÇOS, FATURA E PAGAMENTOS
-- • Reserva 1 (FINALIZADA no Quarto 102): testa Histórico (RN19) e Ocupação (RN20)
-- • Reserva 2 (EM_ANDAMENTO no Quarto 201): pronta para testar Check-out (UC13 / RN13)
-- • Reserva 3 (CONFIRMADA no Quarto 101): pronta para testar Check-in (RN09) e Conflito (RN06)
-- =============================================================================
INSERT INTO reserva (
    id_reserva, id_hospede, id_quarto,
    data_checkin_prevista, data_checkout_prevista,
    data_hora_checkin, data_hora_checkout, status
) VALUES
(1, 1, 2, '2026-11-10', '2026-11-13', '2026-11-10 14:15:00', '2026-11-13 11:30:00', 'FINALIZADA'),
(2, 2, 3, '2026-11-24', '2026-11-27', '2026-11-24 15:00:00', NULL,                  'EM_ANDAMENTO'),
(3, 3, 1, '2026-12-05', '2026-12-08', NULL,                  NULL,                  'CONFIRMADA');

-- Consumos de Frigobar [Atende RN10, RN13]
INSERT INTO consumo_frigobar (id_reserva, produto, quantidade, valor_unitario, data_hora_registro) VALUES
(1, 'Água Mineral s/ Gás 500ml', 4, 8.00,  '2026-11-11 21:10:00'),
(1, 'Refrigerante Lata 350ml',   4, 12.00, '2026-11-12 19:40:00'),
(2, 'Suco Integral Uva 300ml',   2, 15.00, '2026-11-25 18:20:00'),
(2, 'Castanha de Caju 50g',      1, 22.00, '2026-11-25 22:05:00');

-- Serviços de Quarto [Atende RN11, RN13]
INSERT INTO servico_quarto (id_reserva, data_hora_solicitacao, descricao, valor) VALUES
(1, '2026-11-11 10:00:00', 'Lavanderia Expressa (2 camisas)', 60.00),
(1, '2026-11-12 20:30:00', 'Jantar Executivo no Quarto',      60.00),
(2, '2026-11-25 09:15:00', 'Café da Manhã Especial na Cama',  75.00);

-- Fatura Quitada da Reserva 1 (mesmo exemplo matemático da Especificação E3!)
-- 3 diárias x R$ 200,00 (R$ 600) + Frigobar R$ 80 + Serviços R$ 120 = Bruto R$ 800,00
-- Cupom SENAC10 (10%) = Desconto R$ 80,00 -> Líquido R$ 720,00 [Atende RN13..RN16, D06]
INSERT INTO fatura (
    id_fatura, id_reserva, id_cupom, data_emissao,
    quantidade_diarias, tarifa_diaria_aplicada, valor_bruto,
    percentual_desconto, valor_desconto, status
) VALUES
(1, 1, 1, '2026-11-13 11:20:00', 3, 200.00, 800.00, 10.00, 80.00, 'PAGA');

-- Pagamentos Fracionados da Fatura 1 (PIX R$ 300 + Cartão R$ 420 = R$ 720) [Atende RN15, RN16]
INSERT INTO pagamento (id_fatura, valor, data_pagamento, forma_pagamento) VALUES
(1, 300.00, '2026-11-13 11:24:00', 'PIX'),
(1, 420.00, '2026-11-13 11:28:00', 'CARTAO_CREDITO');
