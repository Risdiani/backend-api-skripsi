# backend-api-skripsi

Backend API (Spring Boot) untuk aplikasi **Algoritma Apriori - Apotek**.

## Prasyarat
- **Java 21**
- **Maven** (atau gunakan Maven Wrapper jika ada `mvnw`)
- **MySQL** (sesuai konfigurasi di `src/main/resources/application.yml`)

## Konfigurasi
File konfigurasi utama:
- `src/main/resources/application.yml`

Hal yang perlu dicek:
- `spring.datasource.url` (nama database, host, port)
- `spring.datasource.username`
- `spring.datasource.password`

Contoh (sesuai file saat ini):
- Database: `apotik_v2`
- Port API: `8080`
- Context path: `/api` → semua endpoint diawali **`/api/...`**

> Catatan: `spring.jpa.hibernate.ddl-auto: validate` artinya schema database harus sudah ada dan sesuai entity.

## Cara Menjalankan Project

### 1) Masuk ke folder project yang ada `pom.xml`
Pastikan menjalankan perintah di folder ini:
`backend-api-skripsi/`

Di Windows (Command Prompt / PowerShell):
```bat
cd /d C:\laragon\www\skripsi-apriori\skripsi-risdiani-backend\backend-api-skripsi
```

### 2) Build & install dependency
```bat
mvn clean install
```

### 3) Jalankan aplikasi
```bat
mvn spring-boot:run
```

Aplikasi akan jalan di:
- `http://localhost:8080/api`

## Swagger / OpenAPI
Swagger UI (sesuai `application.yml`):
- `http://localhost:8080/api/swagger-ui.html`

OpenAPI JSON:
- `http://localhost:8080/api/v3/api-docs`

## Struktur Endpoint (contoh)
Karena context-path `/api`, contoh endpoint:
- `GET http://localhost:8080/api/roles`
- `GET http://localhost:8080/api/users`

## Troubleshooting

### Error: "there is no POM in this directory"
Itu berarti perintah Maven dijalankan di folder yang tidak berisi `pom.xml`.

Solusi:
1) `cd` ke folder `backend-api-skripsi`, lalu jalankan `mvn clean install`, atau
2) gunakan opsi `-f`:
```bat
mvn -f C:\laragon\www\skripsi-apriori\skripsi-risdiani-backend\backend-api-skripsi\pom.xml clean install
```

## Logging
Log file akan ditulis ke:
- `backend-api-skripsi/logs/app.log`
(tergantung working directory saat aplikasi dijalankan)