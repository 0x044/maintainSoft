#!/usr/bin/env bash
#
# Generates the RSA key pair used to sign and verify JSON Web Tokens.
#
# The key pair is written to config/ and is deliberately not tracked by git.
# Run this once before starting the application locally.
#
set -euo pipefail

target_dir="${1:-config}"
private_key="${target_dir}/private.key"
public_key="${target_dir}/public.key"

if [[ -e "${private_key}" || -e "${public_key}" ]]; then
    echo "Refusing to overwrite an existing key pair in ${target_dir}." >&2
    echo "Delete the existing files first if you really want to rotate the key." >&2
    exit 1
fi

mkdir -p "${target_dir}"
umask 077

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out "${private_key}"
openssl pkey -in "${private_key}" -pubout -out "${public_key}"
chmod 600 "${private_key}"
chmod 644 "${public_key}"

echo "Wrote ${private_key} and ${public_key}"
