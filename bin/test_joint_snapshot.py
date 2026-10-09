import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch
from snapshot_storage import Storage, object_metadata

spec = importlib.util.spec_from_file_location('restore_snapshot', Path(__file__).with_name('restore-joint-snapshot.py'))
restore_module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(restore_module)


class SnapshotTests(unittest.TestCase):
    def test_schema_comparison_ignores_cross_version_table_and_index_sorting(self):
        source_columns = 'table_b\tid\tbigint\tNO\ntable_a\tid\tint\tNO\ntable_a\tname\tvarchar(64)\tYES'
        restored_columns = 'table_a\tid\tint\tNO\ntable_a\tname\tvarchar(64)\tYES\ntable_b\tid\tbigint\tNO'
        source_indexes = 'table_b\tPRIMARY\t1\tid\t0\ntable_a\tPRIMARY\t1\tid\t0'
        restored_indexes = '\n'.join(reversed(source_indexes.splitlines()))
        self.assertEqual(restore_module.schema_shape(source_columns, source_indexes), restore_module.schema_shape(restored_columns, restored_indexes))

    def test_schema_comparison_preserves_column_order(self):
        source = 'table_a\tid\tint\tNO\ntable_a\tname\tvarchar(64)\tYES'
        self.assertNotEqual(restore_module.schema_shape(source, ''), restore_module.schema_shape('\n'.join(reversed(source.splitlines())), ''))

    def test_schema_comparison_rejects_type_nullability_and_index_changes(self):
        source = 'table_a\tid\tint\tNO'
        for changed in ['table_a\tid\tbigint\tNO', 'table_a\tid\tint\tYES']:
            self.assertNotEqual(restore_module.schema_shape(source, ''), restore_module.schema_shape(changed, ''))
        self.assertNotEqual(restore_module.schema_shape(source, 'table_a\tidx\t1\tid\t0'), restore_module.schema_shape(source, 'table_a\tidx\t2\tid\t1'))

    def test_restore_rejects_files_outside_private_backup_root(self):
        with self.assertRaisesRegex(ValueError, 'private backups directory'):
            restore_module.restore(Path(__file__).with_name('not-a-snapshot.zip'), Path(__file__).with_name('not-a-receipt.json'))

    def test_storage_listing_consumes_all_pages_and_keeps_object_names(self):
        storage = Storage('http://127.0.0.1:9000', 'test-access', 'test-secret')
        first = b'<ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/"><IsTruncated>true</IsTruncated><NextContinuationToken>page-two</NextContinuationToken><Contents><Key>z.png</Key><Size>2</Size><ETag>etag-z</ETag><LastModified>2026-10-09</LastModified></Contents></ListBucketResult>'
        second = b'<ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/"><IsTruncated>false</IsTruncated><Contents><Key>a.png</Key><Size>1</Size><ETag>etag-a</ETag></Contents></ListBucketResult>'
        with patch.object(storage, 'request', side_effect=[(first, {}), (second, {})]) as request:
            objects = storage.objects('test-bucket')
        self.assertEqual(['a.png', 'z.png'], [item['key'] for item in objects])
        self.assertEqual('page-two', request.call_args_list[1].kwargs['query']['continuation-token'])

    def test_storage_listing_rejects_missing_continuation_and_filters_metadata(self):
        storage = Storage('http://127.0.0.1:9000', 'test-access', 'test-secret')
        with patch.object(storage, 'request', return_value=(b'<ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/"><IsTruncated>true</IsTruncated></ListBucketResult>', {})):
            with self.assertRaisesRegex(RuntimeError, 'continuation token'):
                storage.objects('test-bucket')
        self.assertEqual({'content-type': 'image/png', 'x-amz-meta-origin': 'upload'}, object_metadata({'content-type': 'image/png', 'x-amz-meta-origin': 'upload', 'date': 'today', 'authorization': 'withheld'}))


if __name__ == '__main__':
    unittest.main()
