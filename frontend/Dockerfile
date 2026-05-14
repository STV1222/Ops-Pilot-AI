FROM node:20-alpine
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm config set fetch-retries 10 \
 && npm config set fetch-retry-mintimeout 20000 \
 && npm config set fetch-retry-maxtimeout 120000 \
 && npm ci --no-audit --no-fund
COPY . .
RUN npm run build
EXPOSE 3000
CMD ["npm","run","start"]
