# CCAvenue Student Fees Payment — Spring Boot + HTML Sample

A standalone Spring Boot application that demonstrates **CCAvenue** payment-gateway
integration around a **student fees payment** flow, with plain HTML (Thymeleaf) pages.

The CCAvenue encryption here is **byte-for-byte identical** to the NestJS `hdma-backend`
integration (AES-128-CBC, key = `MD5(workingKey)`, fixed IV `00 01 … 0f`, hex on the wire),
so the **same CCAvenue credentials work unchanged**.

## Flow

```
 [Browser]                [Spring Boot app]                  [CCAvenue]
    |  GET /                    |                                 |
    |-------------------------> |  render fee form                |
    |  POST /pay (form)         |                                 |
    |-------------------------> |  build params -> encrypt        |
    |   auto-submit form        |  (encRequest + access_code)     |
    |================================================================>|  pay
    |                           |   POST /payment/response (encResp)  |
    |                           |<================================ |
    |                           |  decrypt -> parse -> result page|
    |<------------------------- |                                 |
```

1. `GET /` — student fee form (name, roll no, course, fee type, email, mobile, amount).
2. `POST /pay` — builds the CCAvenue request string, encrypts it, and returns an
   auto-submitting HTML form that POSTs `encRequest` + `access_code` to CCAvenue.
3. `POST /payment/response` — CCAvenue posts back `encResp`; the app decrypts it,
   parses the status, and renders a receipt/result page.

## Run

Requires **Java 17+**. Uses Maven (via your IDE, a local `mvn`, or the wrapper if you add one).

```bash
# with a local Maven install
mvn spring-boot:run

# or build a jar
mvn clean package
java -jar target/ccavenue-student-fees-1.0.0.jar
```

Then open <http://localhost:8080/>.

## Configuration

All settings live in `src/main/resources/application.properties` under `ccavenue.*`
and can be overridden with environment variables:

| Property                   | Env var                   | Notes |
|----------------------------|---------------------------|-------|
| `ccavenue.merchant-id`     | `CCAVENUE_MERCHANT_ID`    | CCAvenue merchant id |
| `ccavenue.access-code`     | `CCAVENUE_ACCESS_CODE`    | CCAvenue access code |
| `ccavenue.working-key`     | `CCAVENUE_WORKING_KEY`    | **Secret** — used for AES |
| `ccavenue.transaction-url` | `CCAVENUE_TRANSACTION_URL`| test vs `secure.ccavenue.com` |
| `ccavenue.redirect-url`    | `CCAVENUE_REDIRECT_URL`   | must be **publicly reachable** in real use |
| `ccavenue.cancel-url`      | `CCAVENUE_CANCEL_URL`     | |

The defaults are the same **test** credentials as `hdma-backend/.env` so it runs out of the box.

> **Note:** `redirect_url` / `cancel_url` must be URLs CCAvenue can reach from the
> internet. For local testing behind CCAvenue, expose port 8080 with a tunnel
> (e.g. ngrok) and set `CCAVENUE_REDIRECT_URL` to that public URL.

## Security notes (same as production guidance for the Node backend)

- Never commit the real **working key**; inject it via environment variables.
- The decrypted callback is authentic (only the key holder can produce a valid `encResp`),
  but you should still **verify the returned `amount`** against the expected fee before
  marking a payment successful.
- Use `https://secure.ccavenue.com/...` for the transaction URL in production.

## Project layout

```
src/main/java/com/example/studentfees/
  StudentFeesApplication.java        # Spring Boot entry point
  config/CcavenueProperties.java     # ccavenue.* config binding
  util/AesCryptUtil.java             # AES-128-CBC (CCAvenue compatible)
  service/CcavenueService.java       # build request / parse response
  controller/PaymentController.java  # / , /pay , /payment/response
  model/StudentFeeForm.java          # validated form model
src/main/resources/
  application.properties
  templates/index.html               # fee form
  templates/redirect-to-ccavenue.html# auto-submit to gateway
  templates/payment-result.html      # receipt / result
```
