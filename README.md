# BudgetApp

BudgetApp jest aplikacją do zarządzania budżetem domowym. Projekt został zorganizowany jako monorepozytorium zawierające zarówno warstwę kliencką (frontend), jak i serwerową (backend).

## Autor
- **Imię i nazwisko**: Nikita Parkovskyi

## Struktura Projektu

Projekt podzielony jest na dwa główne katalogi:
- **`frontend/`**: Aplikacja kliencka zbudowana przy użyciu biblioteki **React**, narzędzia **Vite** oraz języka **TypeScript**.
- **`backend/`**: Aplikacja serwerowa oparta na frameworku **Spring Boot** i języku **Java 21**, korzystająca z bazy danych w celu przechowywania informacji o budżecie.

## Uruchamianie

### Frontend
Aby uruchomić aplikację frontendową:
1. Przejdź do folderu `frontend/`.
2. Zainstaluj zależności: `npm install`.
3. Uruchom serwer deweloperski: `npm run dev`.

### Backend
Aby uruchomić aplikację backendową:
1. Przejdź do folderu `backend/`.
2. Uruchom projekt przy użyciu Maven: `./mvnw spring-boot:run` lub zaimportuj projekt w wybranym IDE (np. IntelliJ IDEA).
