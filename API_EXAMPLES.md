# API Examples

## Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "admin@opspilot.local",
  "password": "password"
}
```

## Start Quotation Comparison Workflow

```bash
curl -X POST "http://localhost:8080/api/v1/workflows/quotation-comparison" \
  -F "files=@/tmp/supplier-a.pdf" \
  -F "files=@/tmp/supplier-b.xlsx"
```

## List Workflows

```http
GET /api/v1/workflows
```

## Workflow Detail

```http
GET /api/v1/workflows/{id}
```

## Workflow Report

```http
GET /api/v1/workflows/{id}/report
```

## Workflow Logs

```http
GET /api/v1/workflows/{id}/logs
```
