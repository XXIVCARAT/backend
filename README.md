# Badminton Backend (Spring Boot + PostgreSQL)

## One-click Render deploy

[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/XXIVCARAT/backend)

This repository includes a `render.yaml` Blueprint that creates:
- `badminton-db` (PostgreSQL 16)
- `badminton-backend` (Dockerized Spring Boot web service)

## Required post-deploy setting

Set `CORS_ALLOWED_ORIGINS` in the Render web service environment to your frontend URL.

## Local run

```bash
mvn spring-boot:run
```
