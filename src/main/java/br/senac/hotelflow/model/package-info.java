/**
 * Camada de Domínio Rico ({@code model}): concentra as 10 entidades de negócio do HotelFlow
 * com encapsulamento rigoroso e invariantes protegidas diretamente nos métodos de domínio.
 *
 * <p>Regra Arquitetural: classes deste pacote NÃO dependem de {@code dao}, {@code service},
 * {@code view} ou {@code java.sql.*}.
 *
 * <p>Rastreabilidade: {@code [Atende RN01..RN20, D01..D06]}
 */
package br.senac.hotelflow.model;
