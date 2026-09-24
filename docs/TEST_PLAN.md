# Manual test plan

End-to-end checks of the three services running together in `docker compose`, including the unhappy paths.
Run the blocks in order in one WSL shell: each block reuses the variables defined before it
(see "Resuming at a later step" to start in the middle).

## 0. Setup

```bash
cd ~/git/rpe_tech_challenge && git checkout dev && git pull
docker compose down -v                      # clean database: GOLD gets its fixed id, rpe_test is created
docker compose up --build -d --wait         # blocks until all 6 containers are healthy
docker ps --format '{{.Names}}\t{{.Status}}'

# helpers
j() { python3 -c 'import sys,json;d=json.load(sys.stdin)
for k in sys.argv[1].split("."): d=d[int(k)] if k.isdigit() else d.get(k)
print(d)' "$1"; }
pp() { python3 -m json.tool; }
newcpf() { python3 -c 'import random;print("".join(random.choices("0123456789",k=11)))'; }
GOLD=3f2b6c1e-8d4a-4f7b-9c2e-1a5d6e7f8a9b; PLATINUM=9e5a2d7c-1b3f-4c8a-a6d4-7b9e1f3a5c2d
CM=localhost:8082/api/v1; CAT=localhost:8080/api/v1; CP=localhost:8083/api/v1
```
✅ The 6 containers show `(healthy)`.

## 1. Swagger
Open http://localhost:8080/swagger-ui.html, http://localhost:8082/swagger-ui.html and http://localhost:8083/swagger-ui.html. On 8082, call login, click **Authorize**, paste the token, then call `GET /customers/{id}`.

## 2. Authentication (Portador)
```bash
curl -s -o /dev/null -w "%{http_code}\n" $CM/customers/$GOLD                                  # 401 (no token)
curl -s $CM/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"wrong-pass"}' | j code   # INVALID_CREDENTIALS
TOKEN=$(curl -s $CM/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin12345"}' | j accessToken)
H="Authorization: Bearer $TOKEN"
# a USER can't create users → 403
curl -s $CM/auth/users -H "$H" -H 'Content-Type: application/json' -d '{"username":"tester","password":"tester12345","role":"USER"}' | pp
UTOKEN=$(curl -s $CM/auth/login -H 'Content-Type: application/json' -d '{"username":"tester","password":"tester12345"}' | j accessToken)
curl -s -o /dev/null -w "%{http_code}\n" $CM/auth/users -H "Authorization: Bearer $UTOKEN" -H 'Content-Type: application/json' -d '{"username":"x2","password":"x2345678","role":"USER"}'   # 403
```

## 3. Produto Service (Catálogo)
```bash
curl -s $CAT/products/$GOLD | pp                                                   # seeded GOLD, ATIVO, createdAt/updatedAt
P=$(curl -s $CAT/products -H 'Content-Type: application/json' -d '{"name":" test card ","description":"QA"}' | j id); echo $P   # 201, name "TEST CARD"
curl -s $CAT/products -H 'Content-Type: application/json' -d '{"name":"test card"}' | j code      # DUPLICATE_PRODUCT_NAME (409)
curl -s -X PUT $CAT/products/$P -H 'Content-Type: application/json' -d '{"name":"test card","description":"changed"}' | j updatedAt
curl -s -o /dev/null -w "%{http_code}\n" -X DELETE $CAT/products/$P                # 204 → CANCELADO
curl -s -o /dev/null -w "%{http_code}\n" -X DELETE $CAT/products/$P                # 204 again (idempotent)
curl -s $CAT/products -H 'Content-Type: application/json' -d '{"name":"test card"}' | pp          # 409 CANCELLED_PRODUCT_EXISTS + productId
curl -s -X POST $CAT/products/$P/activate | j status                               # ATIVO
curl -s -X POST $CAT/products/$P/activate | j code                                 # PRODUCT_ALREADY_ACTIVE (422)
curl -s $CAT/products/not-a-uuid | j status; curl -s $CAT/products/$(uuidgen) | j code   # 400 / PRODUCT_NOT_FOUND
```

## 4. Issuance flow (Portador → SQS → Cartão → Produto + Redis)
```bash
C=$(curl -s $CM/customers -H "$H" -H 'Content-Type: application/json' \
  -d "{\"name\":\"Maria Silva\",\"cpf\":\"$(newcpf)\",\"birthDate\":\"1990-05-20\",\"credit_info\":\"$PLATINUM\"}" | j id); echo $C
sleep 3; curl -s $CM/customers/$C -H "$H" | pp        # combined: customer + card (ATIVO, masked) + product PLATINUM/ATIVO
curl -s $CP/customers/$C/card | pp                     # card API directly: only the masked number
curl -s $CP/customers/$(uuidgen)/card | j code         # CARD_NOT_FOUND (404)
docker logs rpe-client-manager 2>&1 | grep "Sent outbox event" | tail -1
docker logs rpe-card-processor 2>&1 | grep -E "Received|Produced card" | tail -2
docker exec rpe-redis redis-cli --scan --pattern 'rpe-card-processor::*'           # PLATINUM (and GOLD once used) cached
docker exec rpe-postgres psql -U app -d rpe -c "select masked_number, left(number_encrypted,20) enc, operator from card;"
```
✅ The `card` table holds ciphertext, and the logs contain no names or CPFs.

## 5. Portador rules
```bash
CPF=$(newcpf)
curl -s $CM/customers -H "$H" -H 'Content-Type: application/json' -d "{\"name\":\"Ana\",\"cpf\":\"12\",\"birthDate\":\"1990-01-01\"}" | j code          # INVALID_CPF
curl -s $CM/customers -H "$H" -H 'Content-Type: application/json' -d "{\"name\":\"Ana\",\"cpf\":\"$CPF\",\"birthDate\":\"2999-01-01\"}" | j status     # 400
A=$(curl -s $CM/customers -H "$H" -H 'Content-Type: application/json' -d "{\"name\":\"Ana\",\"cpf\":\"$CPF\",\"birthDate\":\"1990-01-01\"}" | j id)
curl -s $CM/customers -H "$H" -H 'Content-Type: application/json' -d "{\"name\":\"Ana\",\"cpf\":\"$CPF\",\"birthDate\":\"1990-01-01\"}" | j code          # CPF_ALREADY_EXISTS
curl -s -X PUT $CM/customers/$A -H "$H" -H 'Content-Type: application/json' -d '{"name":"Ana B","birthDate":"1990-01-01","status":"BLOQUEADO"}' | j status   # BLOQUEADO
curl -s -X PUT $CM/customers/$A -H "$H" -H 'Content-Type: application/json' -d '{"name":"Ana B","birthDate":"1990-01-01","status":"CANCELADO"}' | j code     # STATUS_CHANGE_NOT_ALLOWED (422)
curl -s -o /dev/null -w "%{http_code} " -X DELETE $CM/customers/$A -H "$H"; curl -s -o /dev/null -w "%{http_code}\n" -X DELETE $CM/customers/$A -H "$H"   # 204 204
curl -s $CM/customers -H "$H" -H 'Content-Type: application/json' -d "{\"name\":\"Ana\",\"cpf\":\"$CPF\",\"birthDate\":\"1990-01-01\"}" | pp                # CANCELLED_CUSTOMER_EXISTS + customerId
curl -s -X POST $CM/customers/$A/activate -H "$H" | j status; curl -s -X POST $CM/customers/$A/activate -H "$H" | j code   # ATIVO / CUSTOMER_ALREADY_ACTIVE
```

## Resuming at a later step

Shell variables don't survive a new terminal or a reboot. To pick up at step 6 (or later) without redoing
everything:

```bash
docker compose up -d --wait        # restarts anything left stopped by a half-finished resilience step
# then define the helpers and variables from step 0 (skip "down -v" to keep the data), and:
TOKEN=$(curl -s $CM/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin12345"}' | j accessToken)
H="Authorization: Bearer $TOKEN"
C=$(curl -s $CM/customers -H "$H" -H 'Content-Type: application/json' \
  -d "{\"name\":\"Maria Silva\",\"cpf\":\"$(newcpf)\",\"birthDate\":\"1990-05-20\",\"credit_info\":\"$PLATINUM\"}" | j id); echo $C
sleep 3; curl -s $CM/customers/$C -H "$H" | j card.product.name   # PLATINUM (6c needs this customer)
```
If LocalStack fails with "error mounting ... no such file or directory", run
`docker compose up -d --force-recreate --wait localstack`. LocalStack keeps no messages across restarts.

## 6. Resilience

**a) Cartão Service down.** The customer is still created and read, and the card appears once the service is back:
```bash
docker compose stop rpe-card-processor
C2=$(curl -s $CM/customers -H "$H" -H 'Content-Type: application/json' -d "{\"name\":\"Joao\",\"cpf\":\"$(newcpf)\",\"birthDate\":\"1985-03-10\"}" | j id)
curl -s $CM/customers/$C2 -H "$H" | pp               # "card": null, "cardInfoAvailable": false
docker compose start rpe-card-processor; sleep 25
curl -s $CM/customers/$C2 -H "$H" | j card.product.name   # GOLD: the message waited in the queue
```

**b) SQS (LocalStack) down.** The outbox keeps the event, and the customer is still created (201):
```bash
docker compose stop localstack
C3=$(curl -s $CM/customers -H "$H" -H 'Content-Type: application/json' -d "{\"name\":\"Rita\",\"cpf\":\"$(newcpf)\",\"birthDate\":\"1992-07-01\"}" | j id); echo $C3   # 201
docker exec rpe-postgres psql -U app -d rpe -c "select status, attempts, left(payload_encrypted,20) enc, last_error from outbox_event where aggregate_id='$C3';"   # PENDING, ciphertext
docker compose up -d --force-recreate --wait localstack   # recreates the queues (see the bind-mount note in CLAUDE.md)
sleep 40; docker exec rpe-postgres psql -U app -d rpe -c "select status, attempts, payload_encrypted is null cleared from outbox_event where aggregate_id='$C3';"   # SENT, cleared=t
curl -s $CM/customers/$C3 -H "$H" | j card.maskedNumber
```
Keep LocalStack down for under 30s: the relay's retry wait doubles on each attempt, so a longer outage means a longer wait for the send.

**c) Catalog down, product not cached.** The GET falls back to the product details stored on the card:
```bash
docker compose stop rpe-catalog
docker exec rpe-redis redis-cli del "rpe-card-processor::catalog-products::$PLATINUM"
curl -s $CM/customers/$C -H "$H" | j card.product    # PLATINUM from the stored copy, status None
```

**d) Retry, DLQ and redrive** (catalog still stopped):
```bash
docker exec rpe-redis redis-cli --scan --pattern 'rpe-card-processor::*' | xargs -r docker exec -i rpe-redis redis-cli del
C4=$(curl -s $CM/customers -H "$H" -H 'Content-Type: application/json' -d "{\"name\":\"Luis\",\"cpf\":\"$(newcpf)\",\"birthDate\":\"1980-01-01\"}" | j id)
docker logs -f rpe-card-processor 2>&1 | grep --line-buffered -E "Received|Dead-letter"   # attempts ~5s, ~20s apart; after ~2 min: ERROR "Dead-letter queue ... holds 1"; Ctrl+C
docker compose start rpe-catalog; sleep 20
docker exec rpe-localstack awslocal sqs start-message-move-task --source-arn arn:aws:sqs:us-east-1:000000000000:rpe-client-manager-queue-dlq
sleep 5; curl -s $CM/customers/$C4 -H "$H" | j card.product.name   # GOLD: produced after the redrive
```

## 7. Consumer idempotency (the same message delivered twice)
```bash
MSG="{\"eventId\":\"$(uuidgen)\",\"eventType\":\"CARD_PRODUCTION_REQUESTED\",\"occurredAt\":\"2026-09-24T12:00:00Z\",\"customerId\":\"$(uuidgen)\",\"customerName\":\"Dup Test\",\"cpf\":\"00000000000\",\"creditInfo\":null}"
Q=http://localhost:4566/000000000000/rpe-client-manager-queue
docker exec rpe-localstack awslocal sqs send-message --queue-url $Q --message-body "$MSG" >/dev/null
docker exec rpe-localstack awslocal sqs send-message --queue-url $Q --message-body "$MSG" >/dev/null
sleep 5; docker logs rpe-card-processor 2>&1 | grep -E "Produced card|already exists" | tail -2   # 1 produced, 1 "already exists"
```

## 8. Automated tests (they can run while the apps are up)
```bash
for s in rpe_catalog rpe_client_manager rpe_card_processor; do (cd $s && ./mvnw -q test && echo "$s OK"); done
```
✅ 258 tests pass: 50 catalog, 114 client_manager, 94 card_processor.
