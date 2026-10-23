import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from application_recovery import isolated_environment
from snapshot_storage import Storage


class RecoveryEnvironmentTests(unittest.TestCase):
    def environment(self, database='jinse_offsite_' + 'a' * 32, bucket='jinse-offsite-' + 'b' * 32):
        return isolated_environment(database, 'restricted-user', 'exercise-password', bucket,
                                    Storage('http://127.0.0.1:9000', 'exercise-access', 'exercise-secret'), Path(tempfile.gettempdir()))

    def test_rejects_existing_business_database_and_bucket(self):
        with self.assertRaises(ValueError):
            self.environment(database='jinse_wine')
        with self.assertRaises(ValueError):
            self.environment(bucket='jinse-wine')

    def test_environment_does_not_inherit_real_integrations_or_proxy(self):
        with patch.dict(os.environ, {'MINI_WECHAT_APP_SECRET': 'real-secret', 'MINI_WECHAT_PRIVATE_KEY': 'real-payment-key',
                                     'MINI_WECHAT_PAY_ENABLED': 'true', 'WECHAT_WORK_SECRET': 'real-work-secret',
                                     'HTTP_PROXY': 'http://external-proxy', 'DB_URL': 'jdbc:mysql://production:3306/jinse_wine'}):
            env = self.environment()
        for name in ('MINI_WECHAT_APP_SECRET', 'MINI_WECHAT_PRIVATE_KEY', 'WECHAT_WORK_SECRET', 'HTTP_PROXY'):
            self.assertNotIn(name, env)
        self.assertIn('127.0.0.1:3306/jinse_offsite_', env['DB_URL'])
        self.assertEqual('restricted-user', env['DB_USERNAME'])

    def test_jobs_payments_transfers_login_and_logistics_are_disabled(self):
        env = self.environment()
        for name in ('APP_SCHEDULING_ENABLED', 'MINI_WECHAT_PAY_ENABLED', 'MINI_WECHAT_TRANSFER_ENABLED', 'MINI_LOCAL_LOGIN_ENABLED', 'WECHAT_LOGISTICS_ENABLED'):
            self.assertEqual('false', env[name])
        self.assertEqual('recovery', env['APP_ENV'])
        self.assertNotEqual(self.environment()['JWT_SECRET'], env['JWT_SECRET'])


if __name__ == '__main__':
    unittest.main()
