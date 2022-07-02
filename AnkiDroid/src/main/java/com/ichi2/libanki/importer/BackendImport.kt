package com.ichi2.libanki.importer

import com.ichi2.libanki.CollectionV16

// These take and return bytes that the frontend TypeScript code will encode/decode.

fun CollectionV16.getNotetypeNamesRaw(input: ByteArray): ByteArray {
    return backend.getNotetypeNamesRaw(input)
}

fun CollectionV16.getDeckNamesRaw(input: ByteArray): ByteArray {
    return backend.getDeckNamesRaw(input)
}

fun CollectionV16.getCsvMetadataRaw(input: ByteArray): ByteArray {
    return backend.getCsvMetadataRaw(input)
}

fun CollectionV16.getFieldNamesRaw(input: ByteArray): ByteArray {
    return backend.getFieldNamesRaw(input)
}

fun CollectionV16.importCsvRaw(input: ByteArray): ByteArray {
    return backend.importCsvRaw(input)
}
