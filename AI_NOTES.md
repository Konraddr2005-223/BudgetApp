# Notatki dotyczące użycia AI (AI_NOTES.md)

Projekt ten został napisany z wykorzystaniem modelu **Gemini** jako asystenta i konsultanta w trakcie prac programistycznych.

---

## Zakres wykorzystania asystenta AI

Model Gemini został wykorzystany jako narzędzie dokumentacyjne, doradcze oraz pomocnicze w następujących obszarach:

1. **Konsultacja rozwiązań współbieżnych:**
   - Omówienie i dobór odpowiedniego mechanizmu blokowania bazy danych w celu ochrony spójności salda konta. Zdecydowano się na użycie blokady pesymistycznej (`LockModeType.PESSIMISTIC_WRITE`) w Spring Data JPA.
2. **Składnia i adnotacje (JSR-380):**
   - Szybkie wyszukanie właściwych adnotacji walidacyjnych (np. `@Positive`, `@NotBlank`) oraz konfiguracji parametrów walidacji w DTO.
3. **Konfiguracja Spring Boot 3 & Maven:**
   - Weryfikacja kompatybilności wersji zależności w pliku `pom.xml` (w szczególności integracji wtyczki Lombok z kompilatorem Maven na nowszych wersjach JDK).
4. **Pomoc przy konfiguracji Dockera:**
   - Przygotowanie optymalnego szkieletu pliku `Dockerfile` (multi-stage build) w celu zminimalizowania obrazu wynikowego.
5. **Wsparcie w budowie front-endu:**
   - Konsultacja układu HTML oraz pomoc w przygotowaniu czystych reguł CSS dla klasycznego wyglądu dashboardu.
6. **Generowanie powtarzalnych części kodu (boilerplate):**
   - Wsparcie przy tworzeniu powtarzalnych struktur klas DTO oraz podstawowych metod CRUD w kontrolerach, co pozwoliło zaoszczędzić czas przy pisaniu powtarzalnego kodu.
