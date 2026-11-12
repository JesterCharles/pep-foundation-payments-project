# Release 5: the saveSettlement interface addition (you apply this)

Recording a settlement must change the status to PAID, store the settlement and append the
"paid" event together or not at all. That needs one new method on the storage seam. Releases
never overwrite your files, so you make the change yourself, in two steps.

## Step 1: add the method to `PaymentRepository` (byte-exact)

From the repository root:

```sh
git apply release/m5/PaymentRepository-saveSettlement.patch
```

This adds `import payments.domain.Settlement;` and, after `nextId()`:

```java
    void saveSettlement(PaymentRequest request, Settlement settlement);
```

Check the result (it must match exactly, so reviewers can verify it):

```sh
shasum -a 256 -c release/m5/PaymentRepository.after-m5.sha256   # macOS
sha256sum -c release/m5/PaymentRepository.after-m5.sha256        # Linux / Git Bash
```

Do not change anything else in `PaymentRepository.java`.

## Step 2: implement it everywhere

- `InMemoryPaymentRepository` (one method): add `saveSettlement(PaymentRequest request,
  Settlement settlement)` that stores the settlement (for example in a map keyed by reference) and
  saves the request. Reject a reference that is already stored with an unchecked exception, before
  changing anything.
- `JdbcPaymentRepository`: the stub already has the method; add `@Override` and implement it in
  one transaction (status update, settlement row, paid event; roll back and rethrow on failure).
- `PaymentDesk.markPaid`: after `PaymentRequest.markPaid` accepts the settlement, call
  `repository.saveSettlement(request, settlement)` instead of `repository.save(request)`.

Run `mvn -q test -Pm5`. The M3 and M4 tests must keep passing.
