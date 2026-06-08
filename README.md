# BudgetApp - Osobisty Menadżer Budżetu REST API

Aplikacja do zarządzania budżetem osobistym, napisana w technologii **Spring Boot 3.3.4** oraz **Java 17+** (w pełni przetestowana pod OpenJDK 25).
Pozwala użytkownikowi na śledzenie salda wielu kont bankowych/gotówkowych, ewidencjonowanie transakcji (przychody i wydatki), monitorowanie limitów wydatków dla poszczególnych kategorii oraz pobieranie raportów w formacie CSV.

W projekcie zawarto również nowoczesny interfejs graficzny GUI (Single Page App) wbudowany bezpośrednio w serwer aplikacji.

---

## Główne Funkcjonalności

### 1. Zarządzanie Kontami (Accounts)
- Tworzenie konta z nazwą i saldem początkowym.
- Listowanie wszystkich kont wraz z aktualnymi saldami.
- Usuwanie konta (dozwolone tylko w przypadku braku powiązanych transakcji – spójność danych).
- Eksport historii transakcji wybranego konta do pliku CSV z kodowaniem UTF-8 i znacznikiem BOM (umożliwia bezproblemowe otwieranie w programie Microsoft Excel z polskimi znakami).

### 2. Zarządzanie Transakcjami (Transactions)
- Dodawanie transakcji (przychód lub wydatek) z automatyczną, transakcyjną aktualizacją salda powiązanego konta.
- Usuwanie transakcji z automatycznym cofnięciem (wycofaniem) salda konta.
- Listowanie transakcji z filtrowaniem według:
  - Zakresu dat (`?from=` oraz `?to=`)
  - Kategorii (`?category=`)

### 3. Limity Budżetowe dla Kategorii
- Definiowanie miesięcznego limitu wydatków dla danej kategorii.
- Przy dodawaniu transakcji typu `EXPENSE` (wydatek) system weryfikuje sumę wydatków w danej kategorii w bieżącym miesiącu. W przypadku przekroczenia limitu transakcja zostaje zapisana pomyślnie, lecz serwer zwraca dedykowany nagłówek odpowiedzi `X-Budget-Warning`, co interfejs GUI interpretuje i sygnalizuje użytkownikowi w postaci czytelnego powiadomienia (Toast Warning).

### 4. Podsumowanie Finansowe
- Pobieranie danych sumarycznych: łączny przychód, łączny wydatek oraz zestawienie wydatków pogrupowanych po kategoriach (w wybranym zakresie dat).

---

## Architektura i Bezpieczeństwo Danych
- **Thread-Safety (Współbieżność):** W warstwie usług (`TransactionService`) zastosowano **blokowanie pesymistyczne (Pessimistic Write Lock)** na poziomie bazy danych przy pobieraniu obiektu konta podczas rejestracji lub usuwania transakcji. Uniemożliwia to powstanie zjawiska "Race Condition" (wyścigu) przy jednoczesnych zapytaniach modyfikujących saldo tego samego konta.
- **Obsługa Błędów:** Klasa `@RestControllerAdvice` (`GlobalExceptionHandler`) przechwytuje wyjątki walidacyjne, biznesowe oraz bazodanowe, formatując je do spójnej struktury JSON z poprawnymi kodami statusu HTTP (200, 201, 400, 404, 409).
- **Zasada DTO:** Komunikacja API odbywa się przez dedykowane obiekty transferu danych (DTO) oddzielone od encji JPA.

---

## Jak Uruchomić Aplikację

### Wymagania
- Zainstalowane środowisko JDK (Java 17 lub nowsza).
- Docker i Docker Compose (wymagane tylko w przypadku uruchamiania w kontenerze z bazą PostgreSQL).

---

### Metoda 1: Uruchomienie Lokalne (Domyślna baza H2 w pamięci)
Ta metoda jest najszybsza i nie wymaga żadnych zewnętrznych zależności bazodanowych.

1. Ustaw zmienną środowiskową `JAVA_HOME` wskazującą na Twój JDK (jeśli nie jest w PATH).
   - *Windows PowerShell:*
     ```powershell
     $env:JAVA_HOME="C:\Sciezka\Do\Twojego\JDK"
     ```
2. Uruchom aplikację za pomocą Maven Wrappera:
   - *Windows PowerShell:*
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```
3. Aplikacja będzie dostępna pod adresem: [http://localhost:8080](http://localhost:8080)

---

### Metoda 2: Konteneryzacja za pomocą Docker Compose (Baza PostgreSQL)
Ta metoda pozwala na uruchomienie pełnego środowiska produkcyjnego z bazą danych PostgreSQL.

1. Upewnij się, że silnik Docker jest włączony.
2. W katalogu głównym projektu wykonaj polecenie:
   ```bash
   docker compose up --build
   ```
3. Docker automatycznie zbuduje obraz aplikacji, pobierze bazę danych PostgreSQL, poczeka na jej uruchomienie i uruchomi serwer Spring Boot.
4. Aplikacja będzie dostępna pod adresem: [http://localhost:8080](http://localhost:8080)

---

## Przydatne Linki Lokalnej Aplikacji
Po uruchomieniu aplikacji możesz korzystać z następujących narzędzi:

- **Panel Użytkownika (GUI Dashboard):** [http://localhost:8080](http://localhost:8080)
- **Dokumentacja API Swagger UI:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **Konsola bazy danych H2 (dostępna tylko przy Metodzie 1):** [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - *JDBC URL:* `jdbc:h2:mem:budgetdb`
  - *User Name:* `sa`
  - *Password:* (brak / puste)

---

## Uruchamianie Testów Jednostkowych
Aby uruchomić zestaw testów jednostkowych weryfikujących warstwę biznesową (salda kont, limity kategorii, usuwanie), wykonaj polecenie:
```powershell
$env:JAVA_HOME="C:\Sciezka\Do\Twojego\JDK"
.\mvnw.cmd test
```
