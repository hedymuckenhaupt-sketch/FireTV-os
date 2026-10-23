# Chillzimmer-Launcher für den Fire TV Stick (3. Gen.)

Eigene Startseite im Glas-Stil auf einer Berglandschaft – wahlweise als Dock unten oder als große Liste links.

Bedienung: Pfeiltasten bewegen, OK startet die App, Zurück schließt Ansichten.
- „Alle Apps“ (oben rechts bzw. unten rechts): Menütaste ≡ auf einer App → zur Startseite hinzufügen/entfernen
- Auf der Startseite: Menütaste ≡ → weiter nach vorn/hinten schieben oder entfernen
- „Einstellungen“: Anordnung (Dock/Liste), Hintergrund (Bergtal, Blaue Gipfel, Abendrot, Nacht, eigenes Foto), Material (Frost/Klar)

Eigenes Foto: Bild als `hintergrund.jpg` (am besten 1920×1080, unter 1 MB) nach `app/src/main/assets/` legen und neu bauen.
Dann erscheint in den Einstellungen „Eigenes Foto“.

## 1. APK bauen lassen (einmalig, ca. 10 Minuten)

1. Auf github.com ein **öffentliches** Repository anlegen, z. B. `chillzimmer-launcher`.
2. „Add file → Upload files“ und den gesamten Inhalt dieses Ordners hineinziehen.
   Wichtig: Der versteckte Ordner `.github` muss mit hoch (unter macOS mit Cmd+Shift+. sichtbar machen).
3. Unter „Actions“ startet der Build automatisch. Danach liegt unter „Releases“ die Datei `ChillzimmerLauncher.apk`.
   Direkter Link: `https://github.com/DEIN-NAME/chillzimmer-launcher/releases/download/latest/ChillzimmerLauncher.apk`

## 2. Auf dem Fire TV installieren

1. Im Amazon Appstore die App **Downloader** installieren.
2. Einstellungen → Mein Fire TV → Entwickleroptionen → „Apps unbekannter Herkunft installieren“ → Downloader erlauben.
   (Fehlen die Entwickleroptionen: Mein Fire TV → Info → 7× auf den Gerätenamen klicken.)
3. In Downloader den Link von oben eingeben, installieren.
4. „Chillzimmer“ erscheint unter „Apps“. Am besten ganz nach vorne in die App-Leiste verschieben.

## 3. Home-Taste umleiten (optional)

Fire OS erlaubt keinen fremden Standard-Launcher. Der eingebaute Dienst „Chillzimmer Home-Taste“
holt die eigene Startseite nach vorn, sobald die Amazon-Startseite erscheint.

Einschalten: Einstellungen → Bedienungshilfen → „Chillzimmer Home-Taste“.
Taucht er dort nicht auf, einmalig per ADB vom PC (Fire TV-IP unter Mein Fire TV → Info → Netzwerk,
in den Entwickleroptionen „ADB-Debugging“ an):

```
adb connect 192.168.0.152
adb shell settings put secure enabled_accessibility_services de.muckenhaupt.chillzimmer/.HomeWatcher
adb shell settings put secure accessibility_enabled 1
```

Die Kachel „Amazon-Startseite“ öffnet die Original-Startseite bewusst, ohne Umleitung.
Hinweis: Amazon kann dieses Verhalten mit einem Update unterbinden.

## Änderungen

Aussehen und Bedienung stecken komplett in `app/src/main/assets/index.html`.
Datei in GitHub bearbeiten → Build läuft neu → in Downloader denselben Link erneut installieren.
Dank festem Signaturschlüssel wird drüberinstalliert, die Favoriten bleiben erhalten.
Vorschau am PC: `index.html` einfach im Browser öffnen (mit Testdaten, Taste M = Menütaste).
