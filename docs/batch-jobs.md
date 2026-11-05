# Batch jobs

A batch job walks the whole approvals table and loads one work item per matching row.

It runs on two functions:

- **StartBatchJobHandler** scans the table and puts work items on `BatchJobWorkQueue`
- **ProcessBatchJobHandler** turns one work item into a message on the queue that owns the work,
  for reindexing that is `IndexApprovalQueue`

Both read from a queue, and the scanner writes to the queue it reads from, so a job is
started with a single message and continues on its own.

Start a job by putting the message on `BatchJobQueue`, or by invoking
`NvaStartBatchJobFunction` with the same message as its payload. The handler reads both the
queue envelope and a bare request, so the Lambda console test tab works without wrapping the
request. Continuation always goes back through the queue.

## Job types

### Reindex approvals

Reads every approval and indexes it. Needed because `ApprovalsStreamPipe` only reacts to
inserted events and revisions, so an approval written back to the table is not reindexed.

```json
{
  "type": "StartBatchJob",
  "jobType": "REINDEX_APPROVALS",
  "totalSegments": 10,
  "pageSize": 700,
  "maxItems": 5000
}
```

Only `type` and `jobType` are required. `totalSegments` defaults to 10, `pageSize` to 700,
and `maxItems` is unlimited when left out.

| Parameter       | Default  | Description                                                |
| --------------- | -------- | ---------------------------------------------------------- |
| `totalSegments` | 10       | Parallel scan segments, each scanned by its own invocation |
| `pageSize`      | 700      | Rows read per invocation                                   |
| `maxItems`      | No limit | Work items to load in total, divided across the segments   |

### Adding a job type

1. Add a constant to `BatchJobType` and say which entity type it scans.
2. Route the constant to the queue that owns its work in `ProcessBatchJobService`.
3. Document it here.

### How to trigger

1. AWS Console → Lambda → `NvaStartBatchJobFunction`
2. Test tab
3. Paste the request:

```json
{
  "type": "StartBatchJob",
  "jobType": "REINDEX_APPROVALS"
}
```

Start with `maxItems` set to a handful of rows before running the whole table. Fewer items
than segments means only that many segments are started.

### How it runs

One start message fans out into one scan message per segment. Each scan message reads a
single page and, if the segment has more rows, puts its cursor back on `BatchJobQueue`.
Progress therefore lives on the queue, not in memory: a failed invocation replays at most
one page.

### Adjusting throughput

- Scanning: `MaximumConcurrency` on the `BatchJobRequested` event source, and `totalSegments`
- Processing: `MaximumConcurrency`, `BatchSize` and `MaximumBatchingWindowInSeconds` on the
  `BatchJobWorkItem` event source. A batch size above 10 requires a batching window. A failure
  retries the whole batch, so an item can be sent for indexing twice; indexing the same approval
  twice produces the same document.

### Emergency stop

- **Stop scanning**: set `PROCESSING_ENABLED` to `false` on `NvaStartBatchJobFunction` in the console. Scan
  messages already on `BatchJobQueue` are consumed and discarded, so the job does not pause,
  it ends. Work items already on `BatchJobWorkQueue` are still processed. Re-enabling does
  not resume anything: start the job again, from the beginning.
- **Stop processing**: disable the SQS trigger on `NvaProcessBatchJobFunction`, and purge
  `BatchJobWorkQueue` if the items should be discarded.

### Error recovery

A scan or work message that fails three times lands on `BatchJobDlq` or `BatchJobWorkDlq`,
and the alarm on that queue fires. A dead lettered scan message means the rest of that
segment was never scanned.

1. AWS Console → SQS → the dead letter queue
2. Start DLQ redrive, back to the source queue
3. Watch the function logs
