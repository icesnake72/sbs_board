# 1) React Code Build Stage
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm install

COPY . .
RUN npm run build


#
FROM nginx:1.27-alpine
LABEL authors="User"

# 기본 nginx 서버 설정을 우리가 작성한 설정 파일로 교체
COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

ENTRYPOINT ["nginx", "-g", "daemon off;"]