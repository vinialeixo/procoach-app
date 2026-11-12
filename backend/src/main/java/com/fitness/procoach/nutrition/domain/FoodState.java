package com.fitness.procoach.nutrition.domain;

/**
 * Estado do alimento para fins de gramatura (T-085 / docs/nutricao-plano-alimentar.md §3.1):
 * - RAW: peso cru / como comprado (quando passa por cocção)
 * - AS_SERVED: peso como consumido (frutas, laticínios, pães, embutidos prontos)
 */
public enum FoodState {
    RAW,
    AS_SERVED
}