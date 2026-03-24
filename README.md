# Driver Assistant Android

Proiect Android pentru un asistent de transport destinat soferilor.

## Ce include acum
- Kotlin + Jetpack Compose
- Room database
- Fused location client
- Dashboard cu status zilnic
- Reguli de baza pentru:
  - 4h30 condus continuu
  - pauza 45 min sau 15 + 30
  - limita zilnica 9h
  - limite saptamanale si pe 2 saptamani afisate in dashboard
- Istoric pe sesiuni
- Preview CSV pentru export
- Integrare pregatita pentru Google Routes API

## Ce mai trebuie facut in Android Studio
1. Adauga cheia ta API pentru Google Routes API.
2. Verifica dependintele Google Play Services / Retrofit / Room.
3. Testeaza permisiunile de locatie pe telefon.
4. Completeaza partea de notificari si foreground service pentru productie.
5. Valideaza logica juridica in toate scenariile reale de tahograf.

## Observatie importanta
Aplicatia este un asistent operational. Nu inlocuieste tahograful si nu reprezinta dovada oficiala de control.
