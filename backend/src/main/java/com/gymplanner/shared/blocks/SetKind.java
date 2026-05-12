package com.gymplanner.shared.blocks;

/**
 * Describes the role of an individual set. SetKind.WARMUP is not the same as
 * BlockPurpose.WARMUP, which describes an entire block.
 */
public enum SetKind {
    NORMAL,
    WARMUP,
    FAILURE,
    DROP,
    REST_PAUSE_PORTION
}
