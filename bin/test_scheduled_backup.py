import base64
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import struct
import tempfile
import unittest
import zipfile
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa
from cryptography.hazmat.primitives.ciphers.aead import AESGCM


def load(name, filename):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).with_name(filename))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


scheduler = load('scheduler', 'run-scheduled-backup.py')
decoder = load('decoder', 'decrypt-joint-snapshot.py')


class ScheduledBackupTests(unittest.TestCase):
    def make_snapshot(self, root, index, acknowledged=True, key='public-key'):
        directory = root / ('production-joint-20261009-' + str(index).zfill(6) + '-aabbccdd')
        directory.mkdir()
        receipt = {'backup': str(directory), 'status': 'success', 'encryptedSha256': 'cipher', 'recoveryKeySha256': key}
        scheduler.write_json(directory / 'receipt.json', receipt)
        if acknowledged:
            scheduler.write_json(directory / 'offsite-ack.json', {'encryptedSha256': 'cipher', 'recoveryKeySha256': key, 'decryptVerified': True})
        return directory

    def test_retention_keeps_recent_and_unacknowledged_snapshots(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            pending = self.make_snapshot(root, 1, acknowledged=False)
            old = self.make_snapshot(root, 2)
            self.make_snapshot(root, 3)
            self.make_snapshot(root, 4)
            scheduler.retain(root, scheduler.inventory(root, 'public-key'), keep=2)
            self.assertTrue(pending.exists())
            self.assertFalse(old.exists())
            self.assertEqual(3, len(scheduler.inventory(root, 'public-key')))

    def test_ack_requires_matching_cipher_key_and_decryption(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = self.make_snapshot(Path(temp), 1)
            receipt = json.loads((directory / 'receipt.json').read_text())
            for changes in ({'encryptedSha256': 'wrong'}, {'recoveryKeySha256': 'wrong'}, {'decryptVerified': False}):
                ack = {'encryptedSha256': 'cipher', 'recoveryKeySha256': 'public-key', 'decryptVerified': True, **changes}
                scheduler.write_json(directory / 'offsite-ack.json', ack)
                self.assertFalse(scheduler.acknowledged(directory, receipt))

    def test_inventory_preserves_other_keys_and_unmanaged_directories(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            self.make_snapshot(root, 1, key='older-key')
            self.make_snapshot(root, 2)
            (root / 'other-project').mkdir()
            items = scheduler.inventory(root, 'public-key')
            self.assertEqual(1, len(items))
            scheduler.retain(root, items, keep=0)
            self.assertTrue((root / 'other-project').exists())
            self.assertEqual(2, len(list(root.iterdir())))

    def test_retention_rejects_path_outside_backup_root(self):
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaises(ValueError):
                scheduler.retain(Path(temp), [{'name': '../elsewhere', 'acknowledged': True}], keep=0)

    def encrypted_fixture(self, root, bad_sql=False):
        key = rsa.generate_private_key(public_exponent=65537, key_size=3072)
        private = root / 'private.pem'
        private.write_bytes(key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
        archive = io.BytesIO()
        sql, image = b'CREATE TABLE example(id INT);', b'fixture-image'
        with zipfile.ZipFile(archive, 'w') as z:
            z.writestr('database.sql', sql)
            z.writestr('objects/0.bin', image)
            z.writestr('manifest.json', json.dumps({'sqlSha256': 'wrong' if bad_sql else hashlib.sha256(sql).hexdigest(), 'tables': {'example': 0},
                                                   'objects': [{'file': 'objects/0.bin', 'sha256': hashlib.sha256(image).hexdigest()}]}))
        plain = archive.getvalue()
        session, nonce = b'k' * 32, b'n' * 12
        wrapped = key.public_key().encrypt(session, padding.OAEP(mgf=padding.MGF1(hashes.SHA256()), algorithm=hashes.SHA256(), label=None))
        header = json.dumps({'format': 1, 'cipher': 'AES-256-GCM', 'keyWrap': 'RSA-OAEP-SHA256',
                             'wrappedKey': base64.b64encode(wrapped).decode(), 'nonce': base64.b64encode(nonce).decode(),
                             'plaintextBytes': len(plain), 'plaintextSha256': hashlib.sha256(plain).hexdigest()}).encode()
        aad = b'JINSE1\n' + struct.pack('>I', len(header)) + header
        encrypted = root / 'backup.enc'
        encrypted.write_bytes(aad + AESGCM(session).encrypt(nonce, plain, aad))
        return encrypted, private

    def test_verify_only_checks_archive_without_plaintext_output(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            encrypted, private = self.encrypted_fixture(root)
            receipt = decoder.decrypt(encrypted, private)
            self.assertTrue(receipt['archiveVerified'])
            self.assertEqual(1, receipt['objects'])
            self.assertEqual(1, receipt['tables'])
            self.assertEqual(2, len(list(root.iterdir())))

    def test_verify_rejects_tampered_ciphertext(self):
        with tempfile.TemporaryDirectory() as temp:
            encrypted, private = self.encrypted_fixture(Path(temp))
            data = bytearray(encrypted.read_bytes())
            data[-1] ^= 1
            encrypted.write_bytes(data)
            with self.assertRaises(Exception):
                decoder.decrypt(encrypted, private)

    def test_verify_rejects_internally_invalid_sql_checksum(self):
        with tempfile.TemporaryDirectory() as temp:
            encrypted, private = self.encrypted_fixture(Path(temp), bad_sql=True)
            with self.assertRaisesRegex(ValueError, 'SQL snapshot checksum differs'):
                decoder.decrypt(encrypted, private)


if __name__ == '__main__':
    unittest.main()
