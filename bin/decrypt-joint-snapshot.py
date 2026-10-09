#!/usr/bin/env python3
"""Decrypt a retained production backup with its separately retained RSA key."""
import argparse
import base64
import hashlib
import json
from pathlib import Path
import struct
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives.ciphers.aead import AESGCM


def decrypt(encrypted_path, private_key_path, output_path):
    data = Path(encrypted_path).read_bytes()
    if not data.startswith(b'JINSE1\n') or len(data) < 27:
        raise ValueError('Unknown encrypted snapshot format')
    size = struct.unpack('>I', data[7:11])[0]
    if size < 100 or size > 8192 or len(data) < 11 + size + 16:
        raise ValueError('Invalid encrypted header size')
    offset = 11 + size
    header = json.loads(data[11:offset])
    if header.get('format') != 1 or header.get('cipher') != 'AES-256-GCM' or header.get('keyWrap') != 'RSA-OAEP-SHA256':
        raise ValueError('Unexpected encryption algorithm')
    key = serialization.load_pem_private_key(Path(private_key_path).read_bytes(), password=None)
    session = key.decrypt(base64.b64decode(header['wrappedKey'], validate=True), padding.OAEP(mgf=padding.MGF1(hashes.SHA256()), algorithm=hashes.SHA256(), label=None))
    plaintext = AESGCM(session).decrypt(base64.b64decode(header['nonce'], validate=True), data[offset:], data[:offset])
    if len(plaintext) != header['plaintextBytes'] or hashlib.sha256(plaintext).hexdigest() != header['plaintextSha256']:
        raise ValueError('Decrypted snapshot checksum differs')
    output = Path(output_path).resolve()
    root = Path(__file__).resolve().parents[1] / 'backups'
    if root.resolve() not in output.parents:
        raise ValueError('Decrypted archive must stay in the private backups directory')
    with output.open('xb') as target:
        target.write(plaintext)
    return {'bytes': len(plaintext), 'sha256': hashlib.sha256(plaintext).hexdigest()}


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--encrypted', required=True)
    parser.add_argument('--private-key', required=True)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    print(json.dumps(decrypt(args.encrypted, args.private_key, args.output)))
