# React + Supabase Scripts

This directory contains example React components and setup for integrating the
Java ADK development server with a web application backed by Supabase.

The scripts are intended as a starting point and can be copied into your own
project. They assume the ADK development server is running and exposes the
following endpoints:

- `GET /agents` — returns `{ agents: string[] }` with the names of compiled agents.
- `POST /run_sse` — run an agent via server‑sent events (not used directly here).
- `WS /run_live` — WebSocket endpoint for interactive agent sessions.

Supabase is used for storing uploaded agent source files in a bucket called
`agent-source`.

## Files

- `package.json` — dependencies and dev scripts using Vite.
- `vite.config.ts` — Vite configuration with the React plugin.
- `src/supabaseClient.ts` — initializes the Supabase client using environment variables `VITE_SUPABASE_URL` and `VITE_SUPABASE_ANON_KEY`.
- `src/components/AgentUploader.tsx` — upload agent source files to Supabase storage.
- `src/components/AgentList.tsx` — fetch and display available agents from the backend.
- `src/components/AgentChat.tsx` — connect to the WebSocket endpoint and chat with an agent.
- `src/App.tsx` — simple application wiring the components together.
- `src/main.tsx` — React entry point.

## Usage

1. Populate a `.env` file with your Supabase credentials:

   ```bash
   VITE_SUPABASE_URL=https://your-project.supabase.co
   VITE_SUPABASE_ANON_KEY=public-anon-key
   ```

2. Install dependencies and start the dev server:

   ```bash
   npm install
   npm run dev
   ```

3. Open `http://localhost:3000` to upload agent code and interact with the ADK server.

These scripts are intentionally lightweight and omit styling and error handling
for brevity.
