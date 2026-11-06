package com.pizzaparker07.spacebreaker.content.rocketry.nav;

import java.util.OptionalDouble;

/**
 * Real Δv (m/s) for a transfer between two locations, or empty when there is no direct route.
 * The default is a table of textbook values ({@link SimpleTransfers}); a full orbital mechanics library
 * (Hohmann transfers, launch windows, aerobraking) can replace it through {@link Transfers#setModel}.
 */
@FunctionalInterface
public interface TransferModel {
    OptionalDouble deltaV(Location from, Location to);
}
