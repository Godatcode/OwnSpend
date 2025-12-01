#!/bin/bash
# Start the OwnSpend backend server

cd "$(dirname "$0")/.."
/Users/arkaghosh/Documents/GitHub/OwnSpend/.venv/bin/python -m uvicorn backend.main:app --reload --host 0.0.0.0 --port 8000
