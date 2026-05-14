create extension if not exists vector;

create table if not exists organizations (
  id uuid primary key,
  name text not null,
  created_at timestamptz not null
);

create table if not exists users (
  id uuid primary key,
  email text not null unique,
  password_hash text not null,
  organization_id uuid not null references organizations(id),
  role text not null,
  created_at timestamptz not null
);

create table if not exists workflow_executions (
  id uuid primary key,
  organization_id uuid not null references organizations(id),
  workflow_type text not null,
  status text not null,
  started_at timestamptz not null,
  completed_at timestamptz,
  triggered_by uuid not null references users(id),
  logs jsonb not null default '[]'::jsonb,
  execution_metadata jsonb not null default '{}'::jsonb
);

create table if not exists uploaded_documents (
  id uuid primary key,
  organization_id uuid not null references organizations(id),
  workflow_execution_id uuid not null references workflow_executions(id),
  filename text not null,
  file_type text not null,
  storage_path text not null,
  extracted_text text,
  uploaded_at timestamptz not null
);

create table if not exists supplier_quotations (
  id uuid primary key,
  workflow_execution_id uuid not null references workflow_executions(id),
  supplier_name text not null,
  quotation_date date,
  total_price numeric,
  currency text,
  extracted_metadata jsonb not null default '{}'::jsonb
);

create table if not exists quotation_items (
  id uuid primary key,
  quotation_id uuid not null references supplier_quotations(id),
  item_name text not null,
  quantity numeric,
  unit text,
  unit_price numeric,
  total_price numeric,
  normalized_name text
);

create table if not exists anomaly_detections (
  id uuid primary key,
  workflow_execution_id uuid not null references workflow_executions(id),
  anomaly_type text not null,
  severity text not null,
  explanation text not null,
  affected_item text
);

create table if not exists generated_reports (
  id uuid primary key,
  workflow_execution_id uuid not null references workflow_executions(id),
  markdown_content text not null,
  json_summary jsonb not null default '{}'::jsonb,
  generated_at timestamptz not null
);

create table if not exists skill_definitions (
  id uuid primary key,
  skill_name text not null,
  version text not null,
  description text,
  skill_path text not null,
  metadata_json jsonb not null default '{}'::jsonb
);

insert into organizations (id, name, created_at)
values ('11111111-1111-1111-1111-111111111111'::uuid, 'Demo Org', now())
on conflict do nothing;

insert into users (id, email, password_hash, organization_id, role, created_at)
values (
  '22222222-2222-2222-2222-222222222222'::uuid,
  'admin@opspilot.local',
  '{noop}password',
  '11111111-1111-1111-1111-111111111111'::uuid,
  'ADMIN',
  now()
)
on conflict (email) do nothing;
