# Stub payment provider contract (Fast W08)

Supplied with the Fast W08 release; do not edit. The Standard track receives the same contract in W09
(`W09-provider-stub.md`). The provider is a stub that runs inside the supplied checks
(`api/src/test/java/payments/project/support/ProviderStub.java`). There is no live provider, no banking
credential and no real money.

## Call

`POST {provider.base-url}/payments`

- Header `Idempotency-Key: payment-request-<id>`: required, one stable key per payment request, the same on
  every attempt. Without it the stub answers 400.
- Body: `{"reference":"PR-<id>","supplierId":"SUP-0042","amount":"1234.50","currency":"USD"}`.
- Answer 200: `{"providerPaymentId":"PP-000123","status":"ACCEPTED"|"REJECTED","reason":null|"..."}`.

## Behaviour your client must survive

| Stub behaviour | What it means | What your API does |
|---|---|---|
| ACCEPTED | payment created | request -> SENT; history `sent` with the provider payment id as detail |
| REJECTED | final refusal | request -> EXCEPTION; history `provider-rejected` with the reason; no retry |
| slow answer | the payment is created but the answer arrives after your timeout | retry with the SAME key; the stub replays the original answer (header `Idempotent-Replayed: true`), so exactly one payment exists |
| 500 | failed before creating a payment | retry with the same key |
| repeated key | duplicate-key replay | the original answer, never a second payment |

Retries use taught Java only: a bounded loop with exception handling around the RestClient call. Configure
`provider.timeout` as a Spring `Duration` (for example `2s`; the supplied checks set `provider.timeout=400ms`) and
`provider.max-attempts` as the number of attempts including the first (the checks set 3). Sending a request
that is already SENT answers 409 and never calls the provider twice.

## Health

`GET {provider.base-url}/health` answers 200 while the stub is up (Fast does not grade a Spring health check).
