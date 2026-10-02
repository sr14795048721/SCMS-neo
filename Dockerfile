FROM node:18-alpine
WORKDIR /app
COPY . .
RUN cd frontend && npm install --registry=https://registry.npmmirror.com && npm run build
EXPOSE 3001
CMD ["sh", "-c", "cd frontend && npx next start --port 3001 --hostname 0.0.0.0"]
