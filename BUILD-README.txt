DRIVER ASSISTANT - BUILD README

1) Ce poti compila imediat:
- debug APK (fara cheie de release)

2) Ce trebuie sa completezi optional:
- cheia Google Routes API in buildConfigField("ROUTES_API_KEY", ...)
  sau ca variabila/secrets pe platforma de build.

3) Variante de compilare:
A. Android Studio local:
   - deschizi proiectul
   - Build > Build Bundle(s) / APK(s) > Build APK(s)

B. Codemagic:
   - urci proiectul intr-un repo GitHub
   - conectezi repo-ul la Codemagic
   - folosesti fisierul codemagic.yaml din radacina
   - setezi optional secret ROUTES_API_KEY
   - rulezi workflow-ul android-apk

C. GitHub Actions:
   - urci proiectul in GitHub
   - Actions > Build Android APK
   - optional adaugi secret ROUTES_API_KEY
   - descarci artifact-ul APK dupa build

4) Output APK asteptat:
- app/build/outputs/apk/debug/app-debug.apk

5) Observatii:
- Este proiect MVP si poate necesita ajustari minore daca Android Gradle Plugin sau dependentele cer update.
- Pentru release in Play Store trebuie configurata semnare release separata.
