# Database Query Visual Simulator

An interactive what-if sandbox for SQL query cost explanation. Parses a hypothetical schema and
query with ANTLR4, estimates cost with a PostgreSQL-inspired heuristic model in a Java/Spring Boot
backend, and renders the resulting execution plan as an interactive node graph in a React frontend.

**Repository:** https://github.com/divyaraj24/database-query-visual-simulator

## Structure

- `backend/` — Spring Boot REST API (parser, cost engine, index recommender, sensitivity analyzer)
- `frontend/` — React + Vite UI (schema/query input, side-by-side plan visualization)

## Running locally

```
cd backend && mvn spring-boot:run   # http://localhost:8080
cd frontend && npm install && npm run dev   # http://localhost:5173
```

## Testing

```
cd backend && mvn test
```

## License

Course project for Advanced Java Programming (Review-3).
